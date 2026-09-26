package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.dto.SectionRequestDTO;

import java.util.List;

public interface SectionService {
    List<Section> getSectionsByCourseId(Long courseId);
    Section getSectionById(Long sectionId);
    Section createSection(Long courseId, SectionRequestDTO request, Long instructorId);
    Section updateSection(Long sectionId, SectionRequestDTO request, Long instructorId);
    void deleteSection(Long sectionId, Long instructorId);
}
