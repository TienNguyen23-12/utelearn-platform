package vn.edu.ute.utelearn.service.impl;

import vn.edu.ute.utelearn.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.RoleRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.UserRoleAssignmentDTO;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + id));
    }

    @Override
    @Transactional
    public User updateUserRoles(Long id, UserRoleAssignmentDTO dto) {
        User user = getUserById(id);

        Set<Role> updatedRoles = new HashSet<>();
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            updatedRoles.addAll(roleRepository.findAllById(dto.getRoleIds()));
        }

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            boolean hasAdminRole = updatedRoles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
            if (!hasAdminRole) {
                throw new IllegalArgumentException("Không thể gỡ bỏ vai trò ADMIN của tài khoản quản trị viên tối cao!");
            }
        }

        user.setRoles(updatedRoles);
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }

        User saved = userRepository.save(user);
        log.info("Cập nhật vai trò cho người dùng: username={}, số vai trò={}", saved.getUsername(), updatedRoles.size());
        return saved;
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        User user = getUserById(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("Không thể khóa tài khoản quản trị viên tối cao!");
        }
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
        log.info("Chuyển trạng thái người dùng: username={}, isActive={}", user.getUsername(), user.getIsActive());
    }
}
