package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.UserRoleAssignmentDTO;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User updateUserRoles(Long id, UserRoleAssignmentDTO dto);
    void toggleUserStatus(Long id);
}
