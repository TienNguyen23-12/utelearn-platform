package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import vn.edu.ute.utelearn.controller.admin.RoleController;
import vn.edu.ute.utelearn.controller.admin.UserController;
import vn.edu.ute.utelearn.dto.RoleRequestDTO;
import vn.edu.ute.utelearn.dto.UserRoleAssignmentDTO;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.RoleService;
import vn.edu.ute.utelearn.service.UserService;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserAndRoleControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserController userController;

    @InjectMocks
    private RoleController roleController;

    private MockMvc userMockMvc;
    private MockMvc roleMockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver userViewResolver = new InternalResourceViewResolver();
        userViewResolver.setPrefix("/templates/");
        userViewResolver.setSuffix(".html");

        InternalResourceViewResolver roleViewResolver = new InternalResourceViewResolver();
        roleViewResolver.setPrefix("/templates/");
        roleViewResolver.setSuffix(".html");

        userMockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setViewResolvers(userViewResolver)
                .build();

        roleMockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setViewResolvers(roleViewResolver)
                .build();
    }

    @Test
    void testListUsersReturnsViewAndModel() throws Exception {
        Role role = Role.builder().id(1L).code("ADMIN").name("Quản trị viên").permissions(new HashSet<>()).build();
        User user = User.builder()
                .id(1L)
                .username("admin")
                .fullName("Quản trị viên")
                .email("admin@utelearn.edu.vn")
                .isActive(true)
                .balance(BigDecimal.ZERO)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(user));

        userMockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/index"))
                .andExpect(model().attributeExists("users"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void testShowEditRolesForm() throws Exception {
        Role role = Role.builder().id(1L).code("ADMIN").name("Quản trị viên").permissions(new HashSet<>()).build();
        User user = User.builder()
                .id(2L)
                .username("student1")
                .fullName("Học viên 1")
                .email("student1@utelearn.edu.vn")
                .isActive(true)
                .balance(BigDecimal.ZERO)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        when(userService.getUserById(2L)).thenReturn(user);
        when(roleService.getAllRoles()).thenReturn(List.of(role));

        userMockMvc.perform(get("/admin/users/2/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attributeExists("userForm"))
                .andExpect(model().attributeExists("allRoles"));
    }

    @Test
    void testProcessUpdateUserRoles() throws Exception {
        userMockMvc.perform(post("/admin/users/2/edit")
                .param("id", "2")
                .param("username", "student1")
                .param("roleIds", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService, times(1)).updateUserRoles(eq(2L), any(UserRoleAssignmentDTO.class));
    }

    @Test
    void testListRolesReturnsViewAndModel() throws Exception {
        Permission perm = Permission.builder().id(1L).code("COURSE_CREATE").name("Tạo khóa học").module("COURSE").build();
        Role role = Role.builder()
                .id(1L)
                .code("ADMIN")
                .name("Quản trị viên")
                .description("Toàn quyền")
                .permissions(new HashSet<>(Set.of(perm)))
                .build();

        when(roleService.getAllRoles()).thenReturn(List.of(role));

        roleMockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles/index"))
                .andExpect(model().attributeExists("roles"));

        verify(roleService, times(1)).getAllRoles();
    }

    @Test
    void testShowCreateRoleForm() throws Exception {
        when(roleService.getPermissionsGroupedByModule()).thenReturn(new LinkedHashMap<>());

        roleMockMvc.perform(get("/admin/roles/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles/form"))
                .andExpect(model().attributeExists("roleForm"))
                .andExpect(model().attributeExists("modulesMap"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    void testShowEditRoleForm() throws Exception {
        Permission perm = Permission.builder().id(1L).code("COURSE_CREATE").name("Tạo khóa học").module("COURSE").build();
        Role role = Role.builder()
                .id(2L)
                .code("INSTRUCTOR")
                .name("Giảng viên")
                .permissions(new HashSet<>(Set.of(perm)))
                .build();

        when(roleService.getRoleById(2L)).thenReturn(role);
        when(roleService.getPermissionsGroupedByModule()).thenReturn(new LinkedHashMap<>());

        roleMockMvc.perform(get("/admin/roles/2/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles/form"))
                .andExpect(model().attributeExists("roleForm"))
                .andExpect(model().attributeExists("modulesMap"))
                .andExpect(model().attribute("isEdit", true));
    }

    @Test
    void testProcessCreateRole() throws Exception {
        roleMockMvc.perform(post("/admin/roles/create")
                .param("code", "INSTRUCTOR")
                .param("name", "Giảng viên")
                .param("description", "Giảng viên phụ trách môn học"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(roleService, times(1)).createRole(any(RoleRequestDTO.class));
    }

    @Test
    void testProcessUpdateRole() throws Exception {
        roleMockMvc.perform(post("/admin/roles/2/edit")
                .param("id", "2")
                .param("code", "INSTRUCTOR")
                .param("name", "Giảng viên cập nhật")
                .param("description", "Mô tả mới"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(roleService, times(1)).updateRole(eq(2L), any(RoleRequestDTO.class));
    }

    @Test
    void testDeleteRole() throws Exception {
        roleMockMvc.perform(post("/admin/roles/2/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(roleService, times(1)).deleteRole(2L);
    }
}
