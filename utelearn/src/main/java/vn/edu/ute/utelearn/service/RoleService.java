package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.RoleRequestDTO;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;

import java.util.List;
import java.util.Map;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    Role createRole(RoleRequestDTO requestDTO);
    Role updateRole(Long id, RoleRequestDTO requestDTO);
    void deleteRole(Long id);
    Map<String, List<Permission>> getPermissionsGroupedByModule();
}
