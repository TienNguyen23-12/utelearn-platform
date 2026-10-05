package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.dto.SectionRequestDTO;
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

    @Override
    public List<Section> getSectionsByCourseId(Long courseId) {
        return sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Override
    public Section getSectionById(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương (section) ID: " + sectionId));
    }

    @Override
    @Transactional
    public Section createSection(Long courseId, SectionRequestDTO request, Long instructorId) {
        Course course = courseService.getCourseById(courseId);
        
        if (!course.getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền sửa khóa học này");
        }

        Section section = Section.builder()
                .course(course)
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 1)
                .build();

        return sectionRepository.save(section);
    }

    @Override
    @Transactional
    public Section updateSection(Long sectionId, SectionRequestDTO request, Long instructorId) {
        Section section = getSectionById(sectionId);
        
        if (!section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền sửa khóa học này");
        }

        section.setTitle(request.getTitle());
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }

        return sectionRepository.save(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId, Long instructorId) {
        Section section = getSectionById(sectionId);
        
        if (!section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền xóa chương này");
        }

        sectionRepository.delete(section);
    }
}
