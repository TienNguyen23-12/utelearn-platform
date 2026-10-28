package vn.edu.ute.utelearn.controller.instructor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.GradebookDTO;
import vn.edu.ute.utelearn.dto.GradebookImportResultDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.GradebookService;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/instructor/cohorts")
@RequiredArgsConstructor
public class InstructorGradebookController {

    private final GradebookService gradebookService;
    private final AuthService authService;

    @GetMapping("/{id}/gradebook")
    public String showGradebook(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("[InstructorGradebookController] Accessing gradebook for cohort {}", id);
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        try {
            GradebookDTO gradebook = gradebookService.getGradebook(id, currentUser.getId());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("gradebook", gradebook);
            model.addAttribute("cohort", gradebook.getCohort());
            return "instructor/cohorts/gradebook";
        } catch (Exception e) {
            log.error("[InstructorGradebookController] Lỗi khi truy cập bảng điểm cohort {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/instructor/cohorts";
        }
    }

    @PostMapping("/{id}/gradebook/calculate")
    public String calculateGrades(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        try {
            gradebookService.calculateAndSaveGrades(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã tính toán bảng điểm thành công cho toàn bộ học viên theo quy tắc đã thiết lập!");
        } catch (Exception e) {
            log.error("[InstructorGradebookController] Lỗi khi tính điểm cho cohort {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tính điểm: " + e.getMessage());
        }
        return "redirect:/instructor/cohorts/" + id + "/gradebook";
    }

    @PostMapping("/{id}/gradebook/publish")
    public String publishGrades(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        try {
            gradebookService.publishGrades(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã công bố bảng điểm chính thức! Học viên đã có thể xem kết quả và tải chứng chỉ nếu đạt.");
        } catch (Exception e) {
            log.error("[InstructorGradebookController] Lỗi khi công bố bảng điểm cho cohort {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể công bố bảng điểm: " + e.getMessage());
        }
        return "redirect:/instructor/cohorts/" + id + "/gradebook";
    }

    @GetMapping("/{id}/gradebook/export")
    public ResponseEntity<byte[]> exportGradebook(@PathVariable("id") Long id) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = currentUserOpt.get();
        try {
            byte[] excelBytes = gradebookService.exportGradebookToExcel(id, currentUser.getId());
            String filename = "Bang_Diem_Cohort_" + id + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("[InstructorGradebookController] Lỗi khi xuất Excel cohort {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/gradebook/import")
    public String importGradebook(@PathVariable("id") Long id,
                                  @RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file Excel để nhập!");
            return "redirect:/instructor/cohorts/" + id + "/gradebook";
        }

        try {
            GradebookImportResultDTO result = gradebookService.importGradebookFromExcel(id, file, currentUser.getId());
            if (result.getFailedCount() == 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Nhập điểm thành công cho " + result.getUpdatedCount() + "/" + result.getTotalRows() + " học viên!");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Đã cập nhật " + result.getUpdatedCount() + "/" + result.getTotalRows() + " học viên. Có "
                                + result.getFailedCount() + " dòng gặp lỗi.");
                redirectAttributes.addFlashAttribute("importErrors", result.getErrors());
            }
        } catch (Exception e) {
            log.error("[InstructorGradebookController] Lỗi khi nhập điểm Excel cohort {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi nhập file: " + e.getMessage());
        }
        return "redirect:/instructor/cohorts/" + id + "/gradebook";
    }
}
