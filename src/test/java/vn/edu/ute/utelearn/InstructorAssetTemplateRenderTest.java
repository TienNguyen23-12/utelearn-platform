package vn.edu.ute.utelearn;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.security.RbacHelper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstructorAssetTemplateRenderTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRenderInstructorAssetIndex() {
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
        Role instructorRole = Role.builder().id(2L).code("INSTRUCTOR").name("Giảng viên").permissions(Set.of(p1)).build();
        User user = User.builder().id(10L).username("instructor2").fullName("Võ Đức Duy").email("instructor2@utelearn.edu.vn").roles(Set.of(instructorRole)).build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("INSTRUCTOR"), new SimpleGrantedAuthority("ASSET_MANAGE")))
        );

        InstructorAsset asset1 = InstructorAsset.builder()
                .id(1L)
                .name("TaiLieu1.pdf")
                .instructor(user)
                .assetType("DOCUMENT")
                .folderPath("Thư mục chung")
                .visibility("PUBLIC")
                .status("APPROVED")
                .fileSizeBytes(2048000L)
                .cloudinaryUrl("https://res.cloudinary.com/demo/sample.pdf")
                .createdAt(Instant.now())
                .build();

        Page<InstructorAsset> assets = new PageImpl<>(List.of(asset1), PageRequest.of(0, 12), 1);

        WebContext context = new WebContext(exchange);
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(appContext, null));
        context.setVariable("currentUser", user);
        context.setVariable("assets", assets);
        context.setVariable("folders", List.of("Thư mục chung"));
        context.setVariable("keyword", null);
        context.setVariable("type", null);
        context.setVariable("scope", "ALL");
        context.setVariable("isAdmin", false);

        String html = engine.process("instructor/assets/index", context);
        assertNotNull(html);
        assertTrue(html.contains("Kho tài nguyên số cá nhân"));
    }

    @Test
    void testRenderModeratorQueueHistory() {
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

        Permission p1 = Permission.builder().id(1L).code("CONTENT_APPROVE").name("Duyệt nội dung").build();
        Role modRole = Role.builder().id(3L).code("MODERATOR").name("Kiểm duyệt viên").permissions(Set.of(p1)).build();
        User user = User.builder().id(10L).username("mod1").fullName("Kiểm Duyệt Viên").email("mod1@utelearn.edu.vn").roles(Set.of(modRole)).build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("MODERATOR"), new SimpleGrantedAuthority("CONTENT_APPROVE")))
        );

        ModerationTask task1 = ModerationTask.builder()
                .id(100L)
                .itemType("ASSET")
                .itemId(26L)
                .author(user)
                .assignedTo(user)
                .status("APPROVED")
                .feedback("Tài liệu đầy đủ")
                .createdAt(Instant.now())
                .build();

        WebContext context = new WebContext(exchange);
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(appContext, null));
        context.setVariable("currentUser", user);
        context.setVariable("tasks", List.of(task1));
        context.setVariable("itemTitles", Map.of(100L, "BaoCao_Project_DBMS_ITOJ.docx"));
        context.setVariable("tab", "HISTORY");
        context.setVariable("pendingCount", 0L);
        context.setVariable("historyCount", 1L);
        context.setVariable("isCheckedIn", true);

        String html = engine.process("moderator/queue", context);
        assertNotNull(html);
        assertTrue(html.contains("Lịch sử kiểm duyệt"));
        assertTrue(html.contains("Đã duyệt"));
    }
}
