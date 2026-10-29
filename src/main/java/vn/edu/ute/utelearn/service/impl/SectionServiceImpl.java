package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.SectionRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.SectionService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseService courseService;
    private final CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final UserRepository userRepository;
    private final vn.edu.ute.utelearn.dao.LessonRepository lessonRepository;

    private boolean isAuthorized(Course course, Cohort cohort, Long instructorId) {
        if (instructorId == null) {
            return false;
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user != null && user.getRoles() != null) {
            boolean hasAdminOrMod = SecurityUtils.hasRole(user, "ADMIN", "MODERATOR");
            if (hasAdminOrMod) {
                return true;
            }
        }
        if (course != null && course.getCreatedBy() != null && course.getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (cohort != null && cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (user != null && user.getRoles() != null) {
            boolean isInstructor = user.getRoles().stream().anyMatch(r ->
                    "INSTRUCTOR".equalsIgnoreCase(r.getCode()) || "ROLE_INSTRUCTOR".equalsIgnoreCase(r.getCode()) ||
                    "TEACHER".equalsIgnoreCase(r.getCode()));
            if (isInstructor && ((course != null && course.getCreatedBy() == null) ||
                    (cohort != null && cohort.getCreatedBy() == null))) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public List<Section> getSectionsByCourseId(Long courseId) {
        List<Section> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        for (Section s : sections) {
            if (s.getCohort() == null) {
                List<vn.edu.ute.utelearn.entity.CohortSchedule> schedules = cohortScheduleRepository.findBySectionId(s.getId());
                if (!schedules.isEmpty() && schedules.get(0).getCohort() != null) {
                    s.setCohort(schedules.get(0).getCohort());
                    sectionRepository.save(s);
                }
            }
        }

        List<Cohort> courseCohorts = cohortRepository.findByCourseIdOrdered(courseId);
        java.util.Map<Long, Integer> cohortOrderMap = new java.util.HashMap<>();
        for (int i = 0; i < courseCohorts.size(); i++) {
            cohortOrderMap.put(courseCohorts.get(i).getId(), i);
        }

        sections.sort((s1, s2) -> {
            Long c1Id = s1.getCohort() != null ? s1.getCohort().getId() : null;
            Long c2Id = s2.getCohort() != null ? s2.getCohort().getId() : null;

            if (java.util.Objects.equals(c1Id, c2Id)) {
                int ord1 = s1.getOrderIndex() != null ? s1.getOrderIndex() : 0;
                int ord2 = s2.getOrderIndex() != null ? s2.getOrderIndex() : 0;
                if (ord1 != ord2) {
                    return Integer.compare(ord1, ord2);
                }
                return Long.compare(s1.getId(), s2.getId());
            }

            int rank1 = c1Id != null ? cohortOrderMap.getOrDefault(c1Id, 9999) : 99999;
            int rank2 = c2Id != null ? cohortOrderMap.getOrDefault(c2Id, 9999) : 99999;
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }

            int ord1 = s1.getOrderIndex() != null ? s1.getOrderIndex() : 0;
            int ord2 = s2.getOrderIndex() != null ? s2.getOrderIndex() : 0;
            return Integer.compare(ord1, ord2);
        });

        return sections;
    }

    @Override
    @Transactional
    public List<Section> getSectionsByCohortId(Long cohortId) {
        List<Section> directSections = sectionRepository.findByCohortIdOrderByOrderIndexAsc(cohortId);
        if (!directSections.isEmpty()) {
            java.util.Set<Long> directSectionIds = directSections.stream()
                    .map(Section::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<vn.edu.ute.utelearn.entity.CohortSchedule> straySchedules = cohortScheduleRepository.findByCohortId(cohortId).stream()
                    .filter(cs -> cs.getLesson() != null && cs.getLesson().getSection() != null && !directSectionIds.contains(cs.getLesson().getSection().getId()))
                    .toList();
            if (!straySchedules.isEmpty()) {
                cohortScheduleRepository.deleteAll(straySchedules);
                log.info("[SectionService] Đã dọn dẹp {} lịch học thừa ngoài các chương trực tiếp của module {}", straySchedules.size(), cohortId);
            }
            return directSections;
        }
        return sectionRepository.findSectionsForCohort(cohortId);
    }

    @Override
    public Section getSectionById(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương (section) ID: " + sectionId));
    }

    @Override
    @Transactional
    public Section createSection(Long courseId, SectionRequestDTO request, Long instructorId) {
        Course course = courseId != null ? courseService.getCourseById(courseId) : null;
        Cohort cohort = request.getCohortId() != null ? cohortRepository.findById(request.getCohortId()).orElse(null) : null;

        if (!isAuthorized(course, cohort, instructorId)) {
            throw new RuntimeException("Bạn không có quyền thêm chương vào nội dung này");
        }

        if (course == null && cohort != null && cohort.getCourse() != null) {
            course = cohort.getCourse();
        }

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            if (cohort != null) {
                orderIndex = sectionRepository.findSectionsForCohort(cohort.getId()).size() + 1;
            } else if (course != null) {
                orderIndex = sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).size() + 1;
            } else {
                orderIndex = 1;
            }
        }

        Section.SectionBuilder builder = Section.builder()
                .course(course)
                .cohort(cohort)
                .title(request.getTitle())
                .orderIndex(orderIndex);

        Section saved = sectionRepository.save(builder.build());
        revertSectionParents(saved);

        log.info("[SectionService] Đã tạo thành công chương ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Section updateSection(Long sectionId, SectionRequestDTO request, Long instructorId) {
        Section section = getSectionById(sectionId);
        
        if (!isAuthorized(section.getCourse(), section.getCohort(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền sửa chương này");
        }

        section.setTitle(request.getTitle());
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        if (request.getCohortId() != null) {
            cohortRepository.findById(request.getCohortId()).ifPresent(section::setCohort);
        } else {
            section.setCohort(null);
        }

        Section saved = sectionRepository.save(section);

        log.info("[SectionService] Đã cập nhật thành công chương ID: {}", saved.getId());
        revertSectionParents(saved);
        return saved;
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId, Long instructorId) {
        Section section = getSectionById(sectionId);
        
        if (!isAuthorized(section.getCourse(), section.getCohort(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền xóa chương này");
        }

        List<vn.edu.ute.utelearn.entity.Lesson> lessons = (lessonRepository != null)
                ? lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId)
                : java.util.Collections.emptyList();
        boolean hasPublishedLessons = lessons.stream().anyMatch(vn.edu.ute.utelearn.entity.Lesson::isPublished);
        if (hasPublishedLessons) {
            revertSectionParents(section);
        }
        sectionRepository.delete(section);
        log.info("[SectionService] Đã xóa thành công chương ID: {}", sectionId);
    }

    @Override
    @Transactional
    public void reorderSections(List<Long> sectionIds, Long instructorId) {
        if (sectionIds == null || sectionIds.isEmpty()) return;
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sId = sectionIds.get(i);
            Section s = sectionRepository.findById(sId).orElse(null);
            if (s != null) {
                s.setOrderIndex(i + 1);
                sectionRepository.save(s);
                revertSectionParents(s);
            }
        }
    }

    private void revertSectionParents(Section section) {
        if (section == null) {
            return;
        }
        if (section.getCohort() != null) {
            Cohort cohort = section.getCohort();
            if ("APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
                cohort.setModerationStatus("DRAFT");
                cohortRepository.save(cohort);
                log.info("[SectionService] Chuyển module id: {} về DRAFT do cập nhật chương", cohort.getId());
            }
            if (cohort.getCourse() != null) {
                courseService.revertToDraftIfPublished(cohort.getCourse());
            }
        }
        if (section.getCourse() != null) {
            courseService.revertToDraftIfPublished(section.getCourse());
        }
    }
}
