package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.utelearn.entity.Cohort;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradebookDTO {
    private Cohort cohort;
    private int totalStudents;
    private int passedCount;
    private int failedCount;
    private double passRate;
    private BigDecimal averageGrade;
    private List<StudentGradeItemDTO> students;
    private List<GradeColumnDTO> columns;
    private List<GradeColumnDTO> processColumns;
    private GradeColumnDTO finalExamColumn;
}
