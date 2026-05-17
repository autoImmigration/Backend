package com.yongsik.immigrationops.casework.presentation;

import com.yongsik.immigrationops.casework.application.StudentAccessService;
import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseDocument;
import com.yongsik.immigrationops.casework.domain.StudentLookupCriteria;
import com.yongsik.immigrationops.casework.domain.StudentRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-access")
public class StudentAccessController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final StudentAccessService studentAccessService;

    public StudentAccessController(StudentAccessService studentAccessService) {
        this.studentAccessService = studentAccessService;
    }

    @PostMapping("/lookup")
    StudentLookupResponse lookup(@Valid @RequestBody StudentLookupRequest request) {
        StudentAccessService.StudentAccessResult result = studentAccessService.lookup(
                new StudentLookupCriteria(request.nationality(), request.passportNumber(), request.birthDate())
        );
        return new StudentLookupResponse(
                toProfile(result.student()),
                result.applications().stream().map(this::toStudentApplication).toList()
        );
    }

    private StudentProfileResponse toProfile(StudentRecord student) {
        return new StudentProfileResponse(
                student.name(),
                student.nationality(),
                student.passportNumber(),
                student.birthDate().toString(),
                student.schoolName(),
                student.agencyName() == null ? "직접 신청" : student.agencyName(),
                student.term()
        );
    }

    private StudentApplicationResponse toStudentApplication(ApplicationCase applicationCase) {
        return new StudentApplicationResponse(
                applicationCase.id(),
                applicationCase.applicationKind().displayName(),
                applicationCase.visaType().displayName(),
                DATE_FORMAT.format(applicationCase.applicationDate()),
                applicationCase.status().displayName(),
                applicationCase.lane(),
                applicationCase.note(),
                applicationCase.documents().stream().map(this::toDocument).toList()
        );
    }

    private StudentDocumentResponse toDocument(CaseDocument document) {
        return new StudentDocumentResponse(
                document.type().code(),
                document.type().displayName(),
                document.type().category(),
                document.type().reviewRule(),
                document.status().displayName(),
                document.submittedAt() == null ? "-" : DATE_FORMAT.format(document.submittedAt()),
                document.note(),
                document.preview()
        );
    }
}

record StudentLookupRequest(
        @NotBlank String nationality,
        @NotBlank String passportNumber,
        @NotNull LocalDate birthDate
) {
}

record StudentLookupResponse(
        StudentProfileResponse student,
        List<StudentApplicationResponse> applications
) {
}

record StudentProfileResponse(
        String name,
        String nationality,
        String passportNumber,
        String birthDate,
        String schoolName,
        String agencyName,
        String term
) {
}

record StudentApplicationResponse(
        String id,
        String applicationType,
        String visaType,
        String submittedAt,
        String status,
        String lane,
        String note,
        List<StudentDocumentResponse> documents
) {
}

record StudentDocumentResponse(
        String code,
        String name,
        String category,
        String rule,
        String status,
        String submittedAt,
        String note,
        String preview
) {
}
