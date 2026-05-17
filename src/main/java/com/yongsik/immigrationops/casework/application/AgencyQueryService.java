package com.yongsik.immigrationops.casework.application;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseworkQueryRepository;
import com.yongsik.immigrationops.casework.domain.UploadBatch;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgencyQueryService {

    private final CaseworkQueryRepository repository;

    public AgencyQueryService(CaseworkQueryRepository repository) {
        this.repository = repository;
    }

    public List<ApplicationCase> findCases(String searchField, String search, String status) {
        return repository.findAllCases().stream()
                .filter(ApplicationCase::isAgencyManaged)
                .filter(applicationCase -> matchesSearch(applicationCase, searchField, search))
                .filter(applicationCase -> matchesStatus(applicationCase, status))
                .sorted(Comparator.comparing(ApplicationCase::updatedAt).reversed())
                .toList();
    }

    public ApplicationCase getCase(String caseId) {
        ApplicationCase applicationCase = repository.findCaseById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Application case not found: " + caseId));

        if (!applicationCase.isAgencyManaged()) {
            throw new IllegalArgumentException("Agency application case not found: " + caseId);
        }

        return applicationCase;
    }

    public List<UploadBatch> findUploadBatches() {
        return repository.findUploadBatches().stream()
                .sorted(Comparator.comparing(UploadBatch::uploadedAt).reversed())
                .toList();
    }

    public UploadBatch getUploadBatch(String batchId) {
        return repository.findUploadBatchById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Upload batch not found: " + batchId));
    }

    private boolean matchesSearch(ApplicationCase applicationCase, String searchField, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String normalizedSearch = search.trim().toLowerCase();
        String value = switch (searchField) {
            case "schoolName" -> applicationCase.schoolName();
            case "applicationType" -> applicationCase.applicationKind().displayName();
            case "visaType" -> applicationCase.visaType().displayName();
            case "coordinator" -> applicationCase.coordinatorName();
            case "studentName" -> applicationCase.student().name();
            default -> applicationCase.student().name();
        };
        return value != null && value.toLowerCase().contains(normalizedSearch);
    }

    private boolean matchesStatus(ApplicationCase applicationCase, String status) {
        return status == null
                || status.isBlank()
                || "전체".equals(status)
                || applicationCase.status().displayName().equals(status)
                || applicationCase.status().name().equalsIgnoreCase(status);
    }
}
