package com.yongsik.immigrationops.ocr.infrastructure;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateFileRequest;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateFilesRequest;
import com.google.cloud.vision.v1.BatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.InputConfig;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.Vertex;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import com.yongsik.immigrationops.ocr.config.OcrProperties;
import com.yongsik.immigrationops.ocr.domain.OcrEngine;
import com.yongsik.immigrationops.ocr.domain.OcrResult;
import com.yongsik.immigrationops.ocr.domain.TextSpan;
import com.yongsik.immigrationops.storage.domain.StoredFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "google-vision")
public class GoogleVisionOcrEngine implements OcrEngine {

    private static final List<String> FILE_MIME_TYPES = List.of(
            "application/pdf",
            "image/tiff",
            "image/gif"
    );

    private final OcrProperties properties;

    public GoogleVisionOcrEngine(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public OcrResult extract(StoredFile storedFile) throws IOException {
        String mediaType = normalizeMediaType(storedFile.mediaType());

        if (mediaType.startsWith("text/")) {
            String text = Files.readString(storedFile.path(), StandardCharsets.UTF_8);
            return new OcrResult(text, 0.99, List.of(new TextSpan(text, 0, 0, 0, 0, 0.99)));
        }

        try (ImageAnnotatorClient client = createClient()) {
            if (mediaType.startsWith("image/") && !FILE_MIME_TYPES.contains(mediaType)) {
                return extractImage(client, storedFile);
            }
            if (FILE_MIME_TYPES.contains(mediaType)) {
                return extractFile(client, storedFile, mediaType);
            }
        }

        throw new IOException("Unsupported Google Vision OCR media type: " + mediaType);
    }

    private ImageAnnotatorClient createClient() throws IOException {
        OcrProperties.GoogleVision config = properties.googleVision();
        ImageAnnotatorSettings.Builder settings = ImageAnnotatorSettings.newBuilder();

        if (config != null && config.credentialsPath() != null && !config.credentialsPath().isBlank()) {
            Path credentialsPath = Path.of(config.credentialsPath()).toAbsolutePath().normalize();
            try (InputStream inputStream = Files.newInputStream(credentialsPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
                settings.setCredentialsProvider(FixedCredentialsProvider.create(credentials));
            }
        }

        return ImageAnnotatorClient.create(settings.build());
    }

    private OcrResult extractImage(ImageAnnotatorClient client, StoredFile storedFile) throws IOException {
        ByteString content = ByteString.readFrom(Files.newInputStream(storedFile.path()));
        Image image = Image.newBuilder().setContent(content).build();
        Feature feature = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();

        BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
        if (response.getResponsesCount() == 0) {
            throw new IOException("Google Vision OCR returned no image response.");
        }

        AnnotateImageResponse imageResponse = response.getResponses(0);
        handleImageError(imageResponse);
        return toOcrResult(imageResponse);
    }

    private OcrResult extractFile(ImageAnnotatorClient client, StoredFile storedFile, String mediaType) throws IOException {
        ByteString content = ByteString.readFrom(Files.newInputStream(storedFile.path()));
        Feature feature = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
        InputConfig inputConfig = InputConfig.newBuilder()
                .setMimeType(mediaType)
                .setContent(content)
                .build();

        AnnotateFileRequest.Builder fileRequest = AnnotateFileRequest.newBuilder()
                .setInputConfig(inputConfig)
                .addFeatures(feature);

        int maxPages = properties.googleVision() == null ? 5 : Math.max(1, properties.googleVision().maxPages());
        for (int page = 1; page <= Math.min(maxPages, 5); page++) {
            fileRequest.addPages(page);
        }

        BatchAnnotateFilesRequest.Builder batchRequest = BatchAnnotateFilesRequest.newBuilder()
                .addRequests(fileRequest.build());

        if (properties.googleVision() != null
                && properties.googleVision().parent() != null
                && !properties.googleVision().parent().isBlank()) {
            batchRequest.setParent(properties.googleVision().parent());
        }

        BatchAnnotateFilesResponse response = client.batchAnnotateFiles(batchRequest.build());
        if (response.getResponsesCount() == 0) {
            throw new IOException("Google Vision OCR returned no file response.");
        }

        AnnotateFileResponse fileResponse = response.getResponses(0);
        if (fileResponse.getResponsesCount() == 0) {
            throw new IOException("Google Vision OCR returned an empty page set.");
        }

        List<AnnotateImageResponse> pageResponses = fileResponse.getResponsesList();
        StringBuilder text = new StringBuilder();
        List<TextSpan> spans = new ArrayList<>();
        List<Double> confidences = new ArrayList<>();

        for (AnnotateImageResponse pageResponse : pageResponses) {
            handleImageError(pageResponse);
            if (!pageResponse.getFullTextAnnotation().getText().isBlank()) {
                if (text.length() > 0) {
                    text.append(System.lineSeparator());
                }
                text.append(pageResponse.getFullTextAnnotation().getText().trim());
            }
            spans.addAll(extractWordSpans(pageResponse));
            collectPageConfidences(pageResponse, confidences);
        }

        if (text.isEmpty()) {
            throw new IOException("Google Vision OCR returned blank text.");
        }

        return new OcrResult(
                text.toString(),
                average(confidences).orElse(0.0),
                spans
        );
    }

    private OcrResult toOcrResult(AnnotateImageResponse response) throws IOException {
        String text = response.getFullTextAnnotation().getText();
        if (text == null || text.isBlank()) {
            throw new IOException("Google Vision OCR returned blank text.");
        }

        List<Double> confidences = new ArrayList<>();
        collectPageConfidences(response, confidences);

        return new OcrResult(
                text.trim(),
                average(confidences).orElse(0.0),
                extractWordSpans(response)
        );
    }

    private void collectPageConfidences(AnnotateImageResponse response, List<Double> confidences) {
        for (Page page : response.getFullTextAnnotation().getPagesList()) {
            confidences.add((double) page.getConfidence());
        }
    }

    private List<TextSpan> extractWordSpans(AnnotateImageResponse response) {
        List<TextSpan> spans = new ArrayList<>();

        for (Page page : response.getFullTextAnnotation().getPagesList()) {
            page.getBlocksList().forEach(block ->
                    block.getParagraphsList().forEach(paragraph ->
                            paragraph.getWordsList().forEach(word ->
                                    spans.add(toTextSpan(word))
                            )
                    )
            );
        }

        spans.sort(Comparator.comparingInt(TextSpan::y).thenComparingInt(TextSpan::x));
        return spans;
    }

    private TextSpan toTextSpan(Word word) {
        StringBuilder text = new StringBuilder();
        for (Symbol symbol : word.getSymbolsList()) {
            text.append(symbol.getText());
        }

        BoundingPoly boundingPoly = word.getBoundingBox();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Vertex vertex : boundingPoly.getVerticesList()) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
        }

        if (minX == Integer.MAX_VALUE) {
            minX = 0;
            minY = 0;
            maxX = 0;
            maxY = 0;
        }

        return new TextSpan(
                text.toString(),
                minX,
                minY,
                Math.max(maxX - minX, 0),
                Math.max(maxY - minY, 0),
                word.getConfidence()
        );
    }

    private void handleImageError(AnnotateImageResponse response) throws IOException {
        if (response.hasError() && !response.getError().getMessage().isBlank()) {
            throw new IOException("Google Vision OCR error: " + response.getError().getMessage());
        }
    }

    private OptionalDouble average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average();
    }

    private String normalizeMediaType(String mediaType) {
        return mediaType == null ? "" : mediaType.toLowerCase(Locale.ROOT);
    }
}

