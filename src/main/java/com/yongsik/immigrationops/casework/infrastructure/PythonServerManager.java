package com.yongsik.immigrationops.casework.infrastructure;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@Component
public class PythonServerManager implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PythonServerManager.class);

    @Value("${app.python-ocr.enabled:true}")
    private boolean pythonOcrEnabled;

    @Value("${app.python-ocr.auto-start:false}")
    private boolean autoStart;

    @Value("${app.python-ocr.base-url:http://localhost:8001}")
    private String pythonBaseUrl;

    @Value("${app.python-ocr.working-dir:}")
    private String workingDir;

    @Value("${app.python-ocr.startup-command:python -m doc_classifier.batch_api}")
    private String startupCommand;

    private Process pythonProcess;

    @Override
    public void run(ApplicationArguments args) {
        if (!pythonOcrEnabled || !autoStart) {
            return;
        }

        if (isServerAlive()) {
            log.info("[PythonServerManager] Python OCR server already running at {}", pythonBaseUrl);
            return;
        }

        log.info("[PythonServerManager] Starting Python OCR server: command='{}' workingDir='{}'",
                startupCommand, workingDir);
        try {
            startServer();
            waitForReady();
        } catch (Exception e) {
            log.error("[PythonServerManager] Failed to start Python OCR server: {}", e.getMessage());
            log.warn("[PythonServerManager] Backend will continue without Python OCR — batches will fail until server is available.");
        }
    }

    private void startServer() throws Exception {
        List<String> command = Arrays.asList(startupCommand.split("\\s+"));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        if (workingDir != null && !workingDir.isBlank()) {
            pb.directory(new File(workingDir));
        }

        pythonProcess = pb.start();

        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pythonProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[python-ocr] {}", line);
                }
            } catch (Exception ignored) {
            }
        }, "python-ocr-logger");
        logThread.setDaemon(true);
        logThread.start();
    }

    private void waitForReady() throws InterruptedException {
        int maxAttempts = 30;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Thread.sleep(1000);

            if (pythonProcess != null && !pythonProcess.isAlive()) {
                throw new RuntimeException(
                        "[PythonServerManager] Python OCR process exited prematurely (exit code: "
                                + pythonProcess.exitValue() + ")");
            }

            if (isServerAlive()) {
                log.info("[PythonServerManager] Python OCR server is ready.");
                return;
            }

            log.debug("[PythonServerManager] Waiting for Python OCR server... {}/{}", attempt, maxAttempts);
        }
        throw new RuntimeException("[PythonServerManager] Python OCR server did not become ready within 30 seconds.");
    }

    private boolean isServerAlive() {
        try {
            RestClient.create()
                    .get()
                    .uri(pythonBaseUrl + "/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            log.info("[PythonServerManager] Stopping Python OCR server...");
            pythonProcess.destroy();
        }
    }
}
