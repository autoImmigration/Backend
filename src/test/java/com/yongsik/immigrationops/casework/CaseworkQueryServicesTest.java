package com.yongsik.immigrationops.casework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yongsik.immigrationops.casework.application.AgencyQueryService;
import com.yongsik.immigrationops.casework.application.SchoolQueryService;
import com.yongsik.immigrationops.casework.application.StudentAccessService;
import com.yongsik.immigrationops.casework.domain.ApplicationCaseStatus;
import com.yongsik.immigrationops.casework.domain.StudentLookupCriteria;
import com.yongsik.immigrationops.casework.infrastructure.InMemoryCaseworkQueryRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CaseworkQueryServicesTest {

    private final InMemoryCaseworkQueryRepository repository = new InMemoryCaseworkQueryRepository();

    @Test
    void studentLookupReturnsAllCasesForMatchingStudent() {
        StudentAccessService service = new StudentAccessService(repository);

        var result = service.lookup(new StudentLookupCriteria("베트남", "M38492017", LocalDate.of(2002, 11, 14)));

        assertEquals("린응옥안", result.student().name());
        assertEquals(3, result.applications().size());
        assertEquals("APP-2026-0412-001", result.applications().getFirst().id());
    }

    @Test
    void schoolQueryReturnsLatestCasePerStudentWithFilters() {
        SchoolQueryService service = new SchoolQueryService(repository);

        var rows = service.findLatestStudentCases("nationality", "베트남", "보완", "전체");

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.status() == ApplicationCaseStatus.NEEDS_SUPPLEMENT));
    }

    @Test
    void agencyQueryReturnsManagedCasesAndUploadBatches() {
        AgencyQueryService service = new AgencyQueryService(repository);

        var cases = service.findCases("studentName", "린응옥안", "전체");
        var uploadBatch = service.getUploadBatch("BATCH-2026-0414-A");

        assertEquals(3, cases.size());
        assertEquals("hanbit_spring_batch_a.zip", uploadBatch.fileName());
        assertEquals(4, uploadBatch.previewFiles().size());
    }
}
