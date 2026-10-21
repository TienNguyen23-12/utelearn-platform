package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CohortDynamicStatusTest {

    @Mock
    private CohortRepository cohortRepository;

    @InjectMocks
    private CohortServiceImpl cohortService;

    @Test
    void testDynamicStatus_EnrollmentStartPassed_ShouldBeEnrolling() {
        Cohort cohort = Cohort.builder()
                .status("UPCOMING")
                .enrollmentStart(Instant.now().minus(1, ChronoUnit.DAYS))
                .enrollmentEnd(null)
                .build();

        assertEquals("ENROLLING", cohort.getStatus());
    }

    @Test
    void testDynamicStatus_EnrollmentStartInFuture_ShouldBeUpcoming() {
        Cohort cohort = Cohort.builder()
                .status("UPCOMING")
                .enrollmentStart(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();

        assertEquals("UPCOMING", cohort.getStatus());
    }

    @Test
    void testDynamicStatus_StudyStartPassed_ShouldBeInProgress() {
        Cohort cohort = Cohort.builder()
                .status("ENROLLING")
                .enrollmentStart(Instant.now().minus(5, ChronoUnit.DAYS))
                .enrollmentEnd(Instant.now().minus(2, ChronoUnit.DAYS))
                .studyStart(Instant.now().minus(1, ChronoUnit.DAYS))
                .studyEnd(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();

        assertEquals("IN_PROGRESS", cohort.getStatus());
    }

    @Test
    void testDynamicStatus_StudyEndPassed_ShouldBeCompleted() {
        Cohort cohort = Cohort.builder()
                .status("IN_PROGRESS")
                .studyStart(Instant.now().minus(20, ChronoUnit.DAYS))
                .studyEnd(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        assertEquals("COMPLETED", cohort.getStatus());
    }

    @Test
    void testDynamicStatus_ExplicitCompleted_ShouldRemainCompleted() {
        Cohort cohort = Cohort.builder()
                .status("COMPLETED")
                .enrollmentStart(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        assertEquals("COMPLETED", cohort.getStatus());
    }

    @Test
    void testSyncCohortStatuses() {
        when(cohortRepository.autoPromoteUpcomingToEnrolling(any(Instant.class))).thenReturn(2);
        when(cohortRepository.autoPromoteToInProgress(any(Instant.class))).thenReturn(1);
        when(cohortRepository.autoPromoteToCompleted(any(Instant.class))).thenReturn(0);
        when(cohortRepository.autoCloseExpiredEnrollmentWithoutStudy(any(Instant.class))).thenReturn(0);

        cohortService.syncCohortStatuses();

        verify(cohortRepository).autoPromoteUpcomingToEnrolling(any(Instant.class));
        verify(cohortRepository).autoPromoteToInProgress(any(Instant.class));
        verify(cohortRepository).autoPromoteToCompleted(any(Instant.class));
        verify(cohortRepository).autoCloseExpiredEnrollmentWithoutStudy(any(Instant.class));
    }
}
