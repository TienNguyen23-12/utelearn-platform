package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.util.ValidationUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortApiController {

    private final CohortService cohortService;

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPublicCohorts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        String safeQ = ValidationUtils.stripHtmlTags(q);
        String safeCategory = ValidationUtils.stripHtmlTags(category);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Cohort> cohortPage = cohortService.searchPublicCohorts(safeQ, safeCategory, pageable);

        List<Map<String, Object>> cohorts = cohortPage.getContent().stream().map(cohort -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cohort.getId());
            map.put("code", cohort.getCode());
            map.put("name", cohort.getName());
            map.put("status", cohort.getStatus());
            map.put("price", cohort.getPrice());
            map.put("currentEnrolled", cohort.getCurrentEnrolled());
            map.put("maxCapacity", cohort.getMaxCapacity());
            
            if (cohort.getCourse() != null) {
                map.put("courseTitle", cohort.getCourse().getTitle());
                map.put("courseSlug", cohort.getCourse().getSlug());
                map.put("thumbnailUrl", cohort.getCourse().getThumbnailUrl());
                map.put("instructorName", cohort.getCourse().getCreatedBy() != null ? cohort.getCourse().getCreatedBy().getFullName() : "Giảng viên");
                map.put("categoryName", cohort.getCourse().getCategory() != null ? cohort.getCourse().getCategory().getName() : null);
            }
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", cohorts);
        response.put("currentPage", cohortPage.getNumber());
        response.put("totalItems", cohortPage.getTotalElements());
        response.put("totalPages", cohortPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}
