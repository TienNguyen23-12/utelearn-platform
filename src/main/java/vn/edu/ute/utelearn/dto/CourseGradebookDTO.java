package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseGradebookDTO {
    private Course course;
    private int totalStudents;
    private int completedCount;
    private int inProgressCount;
    private double passRate;
    private BigDecimal averageGrade;
    private List<Cohort> modules;
    private List<CourseStudentGradeDTO> students;
}
