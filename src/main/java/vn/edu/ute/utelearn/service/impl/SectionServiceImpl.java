package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.dto.SectionRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.SectionService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseService courseService;
    private final CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;

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
        return sections;
    }

    @Override
    public List<Section> getSectionsByCohortId(Long cohortId) {
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

        boolean authorized = false;
        if (course != null && course.getCreatedBy() != null && course.getCreatedBy().getId().equals(instructorId)) {
            authorized = true;
        } else if (cohort != null && cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(instructorId)) {
            authorized = true;
        }
        if (!authorized) {
            throw new RuntimeException("Bạn không có quyền thêm chương vào nội dung này");
        }

        if (course == null && cohort != null && cohort.getCourse() != null) {
            course = cohort.getCourse();
        }

        Section.SectionBuilder builder = Section.builder()
                .course(course)
                .cohort(cohort)
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 1);

        return sectionRepository.save(builder.build());
    }

    @Override
    @Transactional
    public Section updateSection(Long sectionId, SectionRequestDTO request, Long instructorId) {
        Section section = getSectionById(sectionId);
        
        boolean authorized = false;
        if (section.getCourse() != null && section.getCourse().getCreatedBy() != null && section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            authorized = true;
        } else if (section.getCohort() != null && section.getCohort().getCreatedBy() != null && section.getCohort().getCreatedBy().getId().equals(instructorId)) {
            authorized = true;
        }
        if (!authorized) {
            throw new RuntimeException("Bạn không có quyền sửa chương này");
        }

        section.setTitle(request.getTitle());
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        if (request.getCohortId() != null) {
            cohortRepository.findById(request.getCohortId()).ifPresent(section::setCohort);
        }

        return sectionRepository.save(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId, Long instructorId) {
        Section section = getSectionById(sectionId);
        
        boolean authorized = false;
        if (section.getCourse() != null && section.getCourse().getCreatedBy() != null && section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            authorized = true;
        } else if (section.getCohort() != null && section.getCohort().getCreatedBy() != null && section.getCohort().getCreatedBy().getId().equals(instructorId)) {
            authorized = true;
        }
        if (!authorized) {
            throw new RuntimeException("Bạn không có quyền xóa chương này");
        }

        sectionRepository.delete(section);
    }
}
