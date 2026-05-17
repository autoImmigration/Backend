package com.yongsik.immigrationops.casework.presentation;

import com.yongsik.immigrationops.casework.application.SchoolQueryService;
import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/students")
public class SchoolOperationsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final SchoolQueryService schoolQueryService;

    public SchoolOperationsController(SchoolQueryService schoolQueryService) {
        this.schoolQueryService = schoolQueryService;
    }

    @GetMapping
    List<SchoolStudentRowResponse> list(
            @RequestParam(defaultValue = "name") String searchField,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "전체") String status,
            @RequestParam(defaultValue = "전체") String visaType
    ) {
        return schoolQueryService.findLatestStudentCases(searchField, search, status, visaType).stream()
                .map(this::toResponse)
                .toList();
    }

    private SchoolStudentRowResponse toResponse(ApplicationCase applicationCase) {
        return new SchoolStudentRowResponse(
                applicationCase.id(),
                applicationCase.student().name(),
                applicationCase.student().nationality(),
                applicationCase.visaType().displayName(),
                applicationCase.applicationKind().displayName(),
                applicationCase.status().displayName(),
                applicationCase.student().schoolDepartment(),
                applicationCase.agencyName() == null ? "직접 신청" : applicationCase.agencyName(),
                DATE_FORMAT.format(applicationCase.updatedAt().toLocalDate()),
                applicationCase.missingDocumentCount()
        );
    }
}

record SchoolStudentRowResponse(
        String id,
        String name,
        String nationality,
        String visaType,
        String applicationType,
        String status,
        String schoolDepartment,
        String agencyName,
        String lastUpdated,
        int missingCount
) {
}
