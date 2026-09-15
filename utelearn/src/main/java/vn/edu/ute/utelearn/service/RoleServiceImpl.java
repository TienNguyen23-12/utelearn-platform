package vn.edu.ute.utelearn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.PermissionRepository;
import vn.edu.ute.utelearn.dao.RoleRepository;
import vn.edu.ute.utelearn.dto.RoleRequestDTO;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Không tìm thấy vai trò với ID: " + id));
    }

    @Override
    @Transactional
    public Role createRole(RoleRequestDTO requestDTO) {
        String cleanCode = requestDTO.getCode().trim().toUpperCase();

        if (roleRepository.findByCode(cleanCode).isPresent()) {
            throw new IllegalArgumentException("Mã vai trò '" + cleanCode + "' đã tồn tại trên hệ thống!");
        }

        Set<Permission> permissions = new HashSet<>();
        if (requestDTO.getPermissionIds() != null && !requestDTO.getPermissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(requestDTO.getPermissionIds()));
        }

        Role newRole = Role.builder()
            .code(cleanCode)
            .name(requestDTO.getName().trim())
            .description(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null)
            .permissions(permissions)
            .build();

        Role savedRole = roleRepository.save(newRole);
        log.info("Tạo mới vai trò thành công: code={}, số quyền={}", savedRole.getCode(), permissions.size());
        return savedRole;
    }

    @Override
    @Transactional
    public Role updateRole(Long id, RoleRequestDTO requestDTO) {
        Role role = getRoleById(id);
        String newCode = requestDTO.getCode().trim().toUpperCase();

        if ("ADMIN".equalsIgnoreCase(role.getCode()) && !newCode.equalsIgnoreCase("ADMIN")) {
            throw new IllegalArgumentException("Không thể đổi mã của vai trò hệ thống tối cao ADMIN!");
        }

        if (!role.getCode().equalsIgnoreCase(newCode)) {
            Optional<Role> existingOpt = roleRepository.findByCode(newCode);
            if (existingOpt.isPresent() && !existingOpt.get().getId().equals(id)) {
                throw new IllegalArgumentException("Mã vai trò '" + newCode + "' đã được sử dụng bởi vai trò khác!");
            }
            role.setCode(newCode);
        }

        role.setName(requestDTO.getName().trim());
        role.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null);

        Set<Permission> updatedPermissions = new HashSet<>();
        if (requestDTO.getPermissionIds() != null && !requestDTO.getPermissionIds().isEmpty()) {
            updatedPermissions.addAll(permissionRepository.findAllById(requestDTO.getPermissionIds()));
        }
        role.setPermissions(updatedPermissions);

        Role updatedRole = roleRepository.save(role);
        log.info("Cập nhật vai trò thành công: id={}, code={}, số quyền={}", id, updatedRole.getCode(), updatedPermissions.size());
        return updatedRole;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleById(id);

        if ("ADMIN".equalsIgnoreCase(role.getCode())) {
            throw new IllegalArgumentException("Không thể xóa vai trò hệ thống tối cao ADMIN!");
        }

        roleRepository.delete(role);
        log.info("Đã xóa vai trò: id={}, code={}", id, role.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Permission>> getPermissionsGroupedByModule() {
        List<Permission> allPermissions = permissionRepository.findAll();
        return allPermissions.stream()
            .collect(Collectors.groupingBy(
                Permission::getModule,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
}
