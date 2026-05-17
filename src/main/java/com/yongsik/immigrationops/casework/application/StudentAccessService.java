package com.yongsik.immigrationops.casework.application;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseworkQueryRepository;
import com.yongsik.immigrationops.casework.domain.StudentLookupCriteria;
import com.yongsik.immigrationops.casework.domain.StudentRecord;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StudentAccessService {

    private final CaseworkQueryRepository repository;

    public StudentAccessService(CaseworkQueryRepository repository) {
        this.repository = repository;
    }

    public StudentAccessResult lookup(StudentLookupCriteria criteria) {
        StudentRecord student = repository.findStudentByLookup(criteria.normalized())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        List<ApplicationCase> applications = repository.findStudentCases(student.id()).stream()
                .sorted(Comparator.comparing(ApplicationCase::applicationDate).reversed())
                .toList();

        return new StudentAccessResult(student, applications);
    }

    public record StudentAccessResult(StudentRecord student, List<ApplicationCase> applications) {
    }
}
