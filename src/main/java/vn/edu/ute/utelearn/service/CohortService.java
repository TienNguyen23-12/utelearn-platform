package vn.edu.ute.utelearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.ute.utelearn.dto.CohortRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;

public interface CohortService {
    Page<Cohort> getAllCohorts(Pageable pageable);
    
    Page<Cohort> getCohortsByInstructor(Long instructorId, Pageable pageable);
    
    Page<Cohort> searchCohortsByInstructor(Long instructorId, String keyword, String status, Pageable pageable);
    
    Page<Cohort> searchPublicCohorts(String keyword, String categorySlug, Pageable pageable);
    
    Cohort getCohortById(Long id);
    
    Cohort getCohortByIdAndInstructor(Long id, Long instructorId);
    
    Cohort createCohort(CohortRequestDTO dto, User instructor);
    
    Cohort updateCohort(Long id, CohortRequestDTO dto, User instructor);
    
    void deleteCohort(Long id, User instructor);
    
    // Schedule management
    List<CohortSchedule> getCohortSchedules(Long cohortId);
    
    void updateSchedules(Long cohortId, List<CohortSchedule> schedules, User instructor);
}
