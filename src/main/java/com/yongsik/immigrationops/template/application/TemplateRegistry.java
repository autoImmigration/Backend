package com.yongsik.immigrationops.template.application;

import com.yongsik.immigrationops.document.domain.DocumentType;
import com.yongsik.immigrationops.template.domain.DocumentTemplate;
import com.yongsik.immigrationops.template.domain.TemplateFieldDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TemplateRegistry {

    private final Map<DocumentType, DocumentTemplate> templates = new LinkedHashMap<>();

    public TemplateRegistry() {
        templates.put(DocumentType.APPLICATION_FORM, template(DocumentType.APPLICATION_FORM, "studentName", "birthDate", "university", "applicationType"));
        templates.put(DocumentType.ENROLLMENT_CERTIFICATE, template(DocumentType.ENROLLMENT_CERTIFICATE, "studentName", "university", "program"));
        templates.put(DocumentType.ADMISSION_LETTER, template(DocumentType.ADMISSION_LETTER, "studentName", "university", "admissionDate"));
        templates.put(DocumentType.TUITION_RECEIPT, template(DocumentType.TUITION_RECEIPT, "studentName", "amount", "paymentDate"));
        templates.put(DocumentType.FINANCIAL_STATEMENT, template(DocumentType.FINANCIAL_STATEMENT, "accountHolder", "balance", "issueDate"));
        templates.put(DocumentType.RESIDENCE_APPLICATION, template(DocumentType.RESIDENCE_APPLICATION, "studentName", "passportNumber", "applicationNumber"));
        templates.put(DocumentType.CONSENT_FORM, template(DocumentType.CONSENT_FORM, "studentName", "signedDate"));
        templates.put(DocumentType.ID_PHOTO_FORM, template(DocumentType.ID_PHOTO_FORM, "studentName", "photoAttached"));
        templates.put(DocumentType.PASSPORT, template(DocumentType.PASSPORT, "passportNumber", "surname", "givenNames", "birthDate", "expiryDate"));
    }

    public List<String> expectedFields(DocumentType documentType) {
        return templates.getOrDefault(documentType, template(documentType)).fields().stream()
                .map(TemplateFieldDefinition::name)
                .toList();
    }

    public Map<String, String> extractFields(DocumentType documentType, String extractedText) {
        List<TemplateFieldDefinition> fields = templates.getOrDefault(documentType, template(documentType)).fields();
        String[] lines = extractedText.split("\\R");
        Map<String, String> result = new LinkedHashMap<>();

        for (TemplateFieldDefinition field : fields) {
            result.put(field.name(), findLineForAnchor(lines, field.anchor()));
        }

        return result;
    }

    private String findLineForAnchor(String[] lines, String anchor) {
        String normalizedAnchor = anchor.toLowerCase(Locale.ROOT);
        for (String line : lines) {
            if (line.toLowerCase(Locale.ROOT).contains(normalizedAnchor)) {
                return line.trim();
            }
        }
        return "";
    }

    private DocumentTemplate template(DocumentType documentType, String... fieldNames) {
        return new DocumentTemplate(
                documentType,
                java.util.Arrays.stream(fieldNames)
                        .map(name -> new TemplateFieldDefinition(name, name))
                        .toList()
        );
    }

    private DocumentTemplate template(DocumentType documentType) {
        return new DocumentTemplate(documentType, List.of());
    }
}

