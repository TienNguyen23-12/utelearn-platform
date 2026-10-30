package vn.edu.ute.utelearn;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.security.RbacHelper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DashboardTemplateRenderTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRenderDashboardIndex() {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();

        IWebExchange exchange = application.buildExchange(request, response);

        GenericApplicationContext appContext = new GenericApplicationContext();
        RbacHelper rbacHelper = new RbacHelper();
        appContext.getBeanFactory().registerSingleton("rbac", rbacHelper);
        appContext.refresh();

        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(appContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setEnableSpringELCompiler(false);

        Permission p1 = Permission.builder().id(1L).code("ASSET_MANAGE").name("Kho tài nguyên").build();
        Permission p2 = Permission.builder().id(2L).code("CONTENT_APPROVE").name("Kiểm duyệt").build();
        Role modRole = Role.builder().id(1L).code("MODERATOR").name("Người kiểm duyệt").permissions(Set.of(p1, p2)).build();
        User user = User.builder().id(1L).username("mod").fullName("Moderator").email("mod@utelearn.edu.vn").roles(Set.of(modRole)).build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("MODERATOR"), new SimpleGrantedAuthority("ASSET_MANAGE"), new SimpleGrantedAuthority("CONTENT_APPROVE")))
        );

        WebContext context = new WebContext(exchange);
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(appContext, null));
        context.setVariable("currentUser", user);
        context.setVariable("activePage", "dashboard");
        context.setVariable("totalUsers", 10L);
        context.setVariable("totalStudents", 5L);
        context.setVariable("totalCourses", 2L);
        context.setVariable("totalRevenue", BigDecimal.valueOf(100000));
        context.setVariable("pendingModerationTasks", 1L);
        context.setVariable("escalatedModerationTasks", 0L);
        context.setVariable("totalAttendances", 3L);
        context.setVariable("currentFilter", "all");

        String html = engine.process("dashboard/index", context);
        assertNotNull(html);
        assertTrue(html.contains("Bảng điều khiển"));
        assertTrue(html.contains("Kho tài nguyên số"));
        assertFalse(html.contains("Tổng Doanh Thu"));
        assertFalse(html.contains("Lọc dữ liệu:"));
    }
}
