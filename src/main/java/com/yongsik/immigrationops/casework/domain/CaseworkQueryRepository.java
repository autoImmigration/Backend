package com.yongsik.immigrationops.casework.domain;

import java.util.List;
import java.util.Optional;

public interface CaseworkQueryRepository {

    Optional<StudentRecord> findStudentByLookup(StudentLookupCriteria criteria);

    List<ApplicationCase> findStudentCases(String studentId);

    List<ApplicationCase> findAllCases();

    Optional<ApplicationCase> findCaseById(String caseId);

    List<UploadBatch> findUploadBatches();

    Optional<UploadBatch> findUploadBatchById(String batchId);
}
