package vn.edu.ute.utelearn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemDTO {
    private String type;
    private Long courseId;
    private String courseSlug;
    private Long cohortId;
    private String cohortCode;
    private String name;
    private BigDecimal price;
    private Boolean isUpgrade;

    @Builder.Default
    private List<Long> cohortIds = new ArrayList<>();
}
