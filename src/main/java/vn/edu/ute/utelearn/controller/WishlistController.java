package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.WishlistRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.entity.Wishlist;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final CourseRepository courseRepository;
    private final AuthService authService;

    @GetMapping("/wishlist")
    public String viewWishlist(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        List<Wishlist> wishlists = wishlistRepository.findByUserIdWithCourse(user.getId());
        model.addAttribute("currentUser", user);
        model.addAttribute("wishlists", wishlists);
        model.addAttribute("activePage", "wishlist");
        return "wishlist/index";
    }

    @PostMapping("/api/wishlist/toggle/{courseId}")
    @ResponseBody
    public ResponseEntity<?> toggleWishlist(@PathVariable Long courseId) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập để lưu khóa học."));
        }
        User user = currentUserOpt.get();
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Khóa học không tồn tại."));
        }
        Course course = courseOpt.get();
        Optional<Wishlist> existing = wishlistRepository.findByUserAndCourse(user, course);
        boolean inWishlist;
        String message;
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            inWishlist = false;
            message = "Đã bỏ lưu khóa học khỏi danh sách yêu thích.";
        } else {
            Wishlist w = Wishlist.builder().user(user).course(course).build();
            wishlistRepository.save(w);
            inWishlist = true;
            message = "Đã lưu khóa học vào danh sách yêu thích.";
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "inWishlist", inWishlist,
                "message", message
        ));
    }
}
