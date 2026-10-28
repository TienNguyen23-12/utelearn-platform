package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradebookImportResultDTO {
    private int totalRows;
    private int updatedCount;
    private int failedCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
