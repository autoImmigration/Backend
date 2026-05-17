package com.yongsik.immigrationops.casework.application;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseworkQueryRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SchoolQueryService {

    private final CaseworkQueryRepository repository;

    public SchoolQueryService(CaseworkQueryRepository repository) {
        this.repository = repository;
    }

    public List<ApplicationCase> findLatestStudentCases(String searchField, String search, String status, String visaType) {
        Map<String, ApplicationCase> latestByStudent = repository.findAllCases().stream()
                .collect(Collectors.toMap(
                        applicationCase -> applicationCase.student().id(),
                        Function.identity(),
                        this::pickLatest,
                        LinkedHashMap::new
                ));

        return latestByStudent.values().stream()
                .filter(applicationCase -> matchesSearch(applicationCase, searchField, search))
                .filter(applicationCase -> matchesStatus(applicationCase, status))
                .filter(applicationCase -> matchesVisa(applicationCase, visaType))
                .sorted(Comparator.comparing(ApplicationCase::updatedAt).reversed())
                .toList();
    }

    private ApplicationCase pickLatest(ApplicationCase left, ApplicationCase right) {
        return left.updatedAt().isAfter(right.updatedAt()) ? left : right;
    }

    private boolean matchesSearch(ApplicationCase applicationCase, String searchField, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String normalizedSearch = search.trim().toLowerCase();
        String value = switch (searchField) {
            case "nationality" -> applicationCase.student().nationality();
            case "agencyName" -> applicationCase.agencyName() == null ? "직접 신청" : applicationCase.agencyName();
            case "name" -> applicationCase.student().name();
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

    private boolean matchesVisa(ApplicationCase applicationCase, String visaType) {
        return visaType == null
                || visaType.isBlank()
                || "전체".equals(visaType)
                || applicationCase.visaType().displayName().equals(visaType)
                || applicationCase.visaType().code().equalsIgnoreCase(visaType);
    }
}
