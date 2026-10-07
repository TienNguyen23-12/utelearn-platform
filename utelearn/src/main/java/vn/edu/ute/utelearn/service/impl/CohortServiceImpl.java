package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.CohortRequestDTO;
import vn.edu.ute.utelearn.dto.CohortScheduleRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CohortService;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortServiceImpl implements CohortService {

    private final CohortRepository cohortRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Override
    public Page<Cohort> getAllCohorts(Pageable pageable) {
        return cohortRepository.findAll(pageable);
    }

    @Override
    public Page<Cohort> getCohortsByInstructor(Long instructorId, Pageable pageable) {
        return cohortRepository.findByInstructorId(instructorId, pageable);
    }

    @Override
    public Page<Cohort> searchCohortsByInstructor(Long instructorId, String keyword, String status, Pageable pageable) {
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String safeStatus = (status == null) ? "" : status.trim();
        return cohortRepository.searchCohortsByInstructor(instructorId, safeKeyword, safeStatus, pageable);
    }

    @Override
    public Page<Cohort> searchPublicCohorts(String keyword, String categorySlug, Pageable pageable) {
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String safeCategory = (categorySlug == null) ? "" : categorySlug.trim();
        return cohortRepository.searchPublicCohorts(safeKeyword, safeCategory, pageable);
    }

    @Override
    public Cohort getCohortById(Long id) {
        return cohortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cohort not found"));
    }

    @Override
    public Cohort getCohortByIdAndInstructor(Long id, Long instructorId) {
        return cohortRepository.findByIdAndInstructorId(id, instructorId)
                .orElseThrow(() -> new RuntimeException("Cohort not found or unauthorized"));
    }

    @Override
    @Transactional
    public Cohort createCohort(CohortRequestDTO dto, User instructor) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getCreatedBy().getId().equals(instructor.getId())) {
            throw new RuntimeException("Not authorized to create cohort for this course");
        }

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            long count = cohortRepository.countByCourseId(course.getId());
            String year = String.valueOf(java.time.Year.now().getValue());
            String autoCode = String.format("%s-%s-%02d", course.getCode(), year, count + 1);
            while (cohortRepository.existsByCode(autoCode)) {
                count++;
                autoCode = String.format("%s-%s-%02d", course.getCode(), year, count + 1);
            }
            dto.setCode(autoCode);
        } else if (cohortRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Cohort code already exists");
        }

        Cohort cohort = Cohort.builder()
                .course(course)
                .code(dto.getCode())
                .name(dto.getName())
                .enrollmentStart(dto.getEnrollmentStart() != null ? dto.getEnrollmentStart().atZone(ZoneId.systemDefault()).toInstant() : null)
                .enrollmentEnd(dto.getEnrollmentEnd() != null ? dto.getEnrollmentEnd().atZone(ZoneId.systemDefault()).toInstant() : null)
                .studyStart(dto.getStudyStart() != null ? dto.getStudyStart().atZone(ZoneId.systemDefault()).toInstant() : null)
                .studyEnd(dto.getStudyEnd() != null ? dto.getStudyEnd().atZone(ZoneId.systemDefault()).toInstant() : null)
                .maxCapacity(dto.getMaxCapacity())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .build();

        Cohort savedCohort = cohortRepository.save(cohort);
        saveSchedules(savedCohort, dto.getSchedules());
        return savedCohort;
    }

    @Override
    @Transactional
    public Cohort updateCohort(Long id, CohortRequestDTO dto, User instructor) {
        Cohort cohort = getCohortByIdAndInstructor(id, instructor.getId());

        if (!cohort.getCode().equals(dto.getCode()) && cohortRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Cohort code already exists");
        }

        cohort.setCode(dto.getCode());
        cohort.setName(dto.getName());
        if (dto.getEnrollmentStart() != null) cohort.setEnrollmentStart(dto.getEnrollmentStart().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setEnrollmentStart(null);
        if (dto.getEnrollmentEnd() != null) cohort.setEnrollmentEnd(dto.getEnrollmentEnd().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setEnrollmentEnd(null);
        if (dto.getStudyStart() != null) cohort.setStudyStart(dto.getStudyStart().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setStudyStart(null);
        if (dto.getStudyEnd() != null) cohort.setStudyEnd(dto.getStudyEnd().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setStudyEnd(null);
        cohort.setMaxCapacity(dto.getMaxCapacity());
        cohort.setPrice(dto.getPrice());
        cohort.setStatus(dto.getStatus());

        Cohort savedCohort = cohortRepository.save(cohort);
        cohortScheduleRepository.deleteByCohortId(savedCohort.getId());
        saveSchedules(savedCohort, dto.getSchedules());
        return savedCohort;
    }

    private void saveSchedules(Cohort cohort, List<CohortScheduleRequestDTO> scheduleDTOs) {
        if (scheduleDTOs == null || scheduleDTOs.isEmpty()) return;
        for (CohortScheduleRequestDTO req : scheduleDTOs) {
            if (req.getUnlockAt() == null) continue; // Skip if no unlock time
            lessonRepository.findById(req.getLessonId()).ifPresent(lesson -> {
                CohortSchedule schedule = CohortSchedule.builder()
                        .cohort(cohort)
                        .lesson(lesson)
                        .unlockAt(req.getUnlockAt().atZone(ZoneId.systemDefault()).toInstant())
                        .deadlineAt(req.getDeadlineAt() != null ? req.getDeadlineAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                        .showScoreType(req.getShowScoreType())
                        .allowReviewAnswers(req.getAllowReviewAnswers() != null ? req.getAllowReviewAnswers() : false)
                        .isScorePublished(true)
                        .build();
                cohortScheduleRepository.save(schedule);
            });
        }
    }

    @Override
    @Transactional
    public void deleteCohort(Long id, User instructor) {
        Cohort cohort = getCohortByIdAndInstructor(id, instructor.getId());
        cohortScheduleRepository.deleteByCohortId(id);
        cohortRepository.delete(cohort);
    }

    @Override
    public List<CohortSchedule> getCohortSchedules(Long cohortId) {
        return cohortScheduleRepository.findByCohortId(cohortId);
    }

    @Override
    @Transactional
    public void updateSchedules(Long cohortId, List<CohortSchedule> schedules, User instructor) {
        Cohort cohort = getCohortByIdAndInstructor(cohortId, instructor.getId());
        // For simplicity, we assume frontend sends the full configured list
        for (CohortSchedule schedule : schedules) {
            schedule.setCohort(cohort);
            cohortScheduleRepository.save(schedule);
        }
    }
}
