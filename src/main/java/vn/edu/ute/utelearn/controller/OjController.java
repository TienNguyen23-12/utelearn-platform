package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Optional;

@Controller
@RequestMapping("/oj")
@RequiredArgsConstructor
public class OjController {

    private final AuthService authService;

    @GetMapping({"", "/problems"})
    public String showOjProblemsPage(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        model.addAttribute("activePage", "oj");
        return "oj/problems";
    }
}
