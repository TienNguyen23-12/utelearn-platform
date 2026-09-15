package vn.edu.ute.utelearn.service;

import jakarta.servlet.http.HttpServletResponse;
import vn.edu.ute.utelearn.dto.AuthResponseDTO;
import vn.edu.ute.utelearn.dto.LoginRequestDTO;
import vn.edu.ute.utelearn.dto.RegisterRequestDTO;
import vn.edu.ute.utelearn.entity.User;

import java.util.Optional;

public interface AuthService {
    User register(RegisterRequestDTO requestDTO);
    AuthResponseDTO login(LoginRequestDTO requestDTO, HttpServletResponse response);
    void logout(HttpServletResponse response);
    Optional<User> getCurrentAuthenticatedUser();
}
