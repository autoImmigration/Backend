package com.yongsik.immigrationops.casework.infrastructure.persistence;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseworkQueryRepository;
import com.yongsik.immigrationops.casework.domain.StudentLookupCriteria;
import com.yongsik.immigrationops.casework.domain.StudentRecord;
import com.yongsik.immigrationops.casework.domain.UploadBatch;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseCaseworkQueryRepository implements CaseworkQueryRepository {

    private final StudentJpaRepository studentJpaRepository;
    private final ApplicationCaseJpaRepository applicationCaseJpaRepository;
    private final UploadBatchJpaRepository uploadBatchJpaRepository;
    private final CaseworkJpaMapper mapper;

    public DatabaseCaseworkQueryRepository(
            StudentJpaRepository studentJpaRepository,
            ApplicationCaseJpaRepository applicationCaseJpaRepository,
            UploadBatchJpaRepository uploadBatchJpaRepository,
            CaseworkJpaMapper mapper
    ) {
        this.studentJpaRepository = studentJpaRepository;
        this.applicationCaseJpaRepository = applicationCaseJpaRepository;
        this.uploadBatchJpaRepository = uploadBatchJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<StudentRecord> findStudentByLookup(StudentLookupCriteria criteria) {
        return studentJpaRepository.findForLookup(
                        criteria.nationality(),
                        criteria.passportNumber(),
                        criteria.birthDate()
                )
                .map(mapper::toStudentRecord);
    }

    @Override
    public List<ApplicationCase> findStudentCases(String studentId) {
        return deduplicateCases(applicationCaseJpaRepository.findAllByStudentExternalIdOrderByApplicationDateDesc(studentId))
                .stream()
                .map(mapper::toApplicationCase)
                .toList();
    }

    @Override
    public List<ApplicationCase> findAllCases() {
        return deduplicateCases(applicationCaseJpaRepository.findAllByOrderByUpdatedAtDesc())
                .stream()
                .map(mapper::toApplicationCase)
                .toList();
    }

    @Override
    public Optional<ApplicationCase> findCaseById(String caseId) {
        return applicationCaseJpaRepository.findByExternalId(caseId)
                .map(mapper::toApplicationCase);
    }

    @Override
    public List<UploadBatch> findUploadBatches() {
        return deduplicateBatches(uploadBatchJpaRepository.findAllByOrderByUploadedAtDesc()).stream()
                .map(mapper::toUploadBatch)
                .toList();
    }

    @Override
    public Optional<UploadBatch> findUploadBatchById(String batchId) {
        return uploadBatchJpaRepository.findByExternalId(batchId)
                .map(mapper::toUploadBatch);
    }

    private List<ApplicationCaseEntity> deduplicateCases(List<ApplicationCaseEntity> entities) {
        Map<String, ApplicationCaseEntity> values = entities.stream()
                .collect(Collectors.toMap(
                        ApplicationCaseEntity::getExternalId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return List.copyOf(values.values());
    }

    private List<UploadBatchEntity> deduplicateBatches(List<UploadBatchEntity> entities) {
        Map<String, UploadBatchEntity> values = entities.stream()
                .collect(Collectors.toMap(
                        UploadBatchEntity::getExternalId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return List.copyOf(values.values());
    }
}
