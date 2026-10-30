package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import vn.edu.ute.utelearn.controller.CohortChatController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortMessageRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.ChatMessageRequestDTO;
import vn.edu.ute.utelearn.dto.ChatMessageResponseDTO;
import vn.edu.ute.utelearn.dto.ChatNotificationDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.impl.CohortChatServiceImpl;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CohortChatFeatureTest {

    private CohortRepository cohortRepository;
    private CohortMemberRepository cohortMemberRepository;
    private CohortMessageRepository cohortMessageRepository;
    private AuthService authService;
    private UserRepository userRepository;

    private CohortChatServiceImpl cohortChatService;
    private CohortChatController cohortChatController;

    private User studentUser;
    private User instructorUser;
    private User adminUser;
    private Cohort testCohort;
    private Course testCourse;

    private final List<CohortMessage> savedMessages = new ArrayList<>();
    private final Map<String, Object> broadcastedMessages = new HashMap<>();

    @BeforeEach
    void setUp() {
        savedMessages.clear();
        broadcastedMessages.clear();

        Role studentRole = Role.builder().id(1L).code("STUDENT").name("Học viên").build();
        Role instructorRole = Role.builder().id(2L).code("INSTRUCTOR").name("Giảng viên").build();
        Role adminRole = Role.builder().id(3L).code("ADMIN").name("Quản trị viên").build();

        studentUser = User.builder()
                .id(10L)
                .username("student1")
                .fullName("Nguyễn Văn Học")
                .email("student1@utelearn.edu.vn")
                .roles(Set.of(studentRole))
                .build();

        instructorUser = User.builder()
                .id(20L)
                .username("instructor1")
                .fullName("Thầy Hoàng Nam")
                .email("instructor1@utelearn.edu.vn")
                .roles(Set.of(instructorRole))
                .build();

        adminUser = User.builder()
                .id(30L)
                .username("admin1")
                .fullName("Quản Trị Viên")
                .email("admin@utelearn.edu.vn")
                .roles(Set.of(adminRole))
                .build();

        testCourse = Course.builder()
                .id(100L)
                .title("Lập trình Java Web Spring Boot")
                .slug("lap-trinh-java-web-spring-boot")
                .createdBy(instructorUser)
                .build();

        testCohort = Cohort.builder()
                .id(500L)
                .code("JAVA-K24-01")
                .name("Lớp Java Web K24 Đợt 1")
                .course(testCourse)
                .createdBy(instructorUser)
                .build();

        cohortRepository = (CohortRepository) Proxy.newProxyInstance(
                CohortRepository.class.getClassLoader(),
                new Class<?>[]{CohortRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName()) && args != null && args.length > 0) {
                        Long id = (Long) args[0];
                        if (Long.valueOf(500L).equals(id)) {
                            return Optional.of(testCohort);
                        }
                        return Optional.empty();
                    }
                    if ("findByInstructorId".equals(method.getName())) {
                        return new PageImpl<>(List.of(testCohort));
                    }
                    return null;
                }
        );

        cohortMemberRepository = (CohortMemberRepository) Proxy.newProxyInstance(
                CohortMemberRepository.class.getClassLoader(),
                new Class<?>[]{CohortMemberRepository.class},
                (proxy, method, args) -> {
                    if ("existsByCohort_IdAndUser_Id".equals(method.getName())) {
                        Long cohortId = (Long) args[0];
                        Long userId = (Long) args[1];
                        return Long.valueOf(500L).equals(cohortId) && Long.valueOf(10L).equals(userId);
                    }
                    if ("findByCohort_IdAndUser_Id".equals(method.getName())) {
                        Long cohortId = (Long) args[0];
                        Long userId = (Long) args[1];
                        if (Long.valueOf(500L).equals(cohortId) && Long.valueOf(10L).equals(userId)) {
                            return Optional.of(CohortMember.builder()
                                    .id(1L)
                                    .cohort(testCohort)
                                    .user(studentUser)
                                    .roleInCohort("STUDENT")
                                    .build());
                        }
                        return Optional.empty();
                    }
                    if ("findByCohort_Id".equals(method.getName())) {
                        return List.of(CohortMember.builder()
                                .id(1L)
                                .cohort(testCohort)
                                .user(studentUser)
                                .roleInCohort("STUDENT")
                                .build());
                    }
                    if ("findByUserOrderByEnrolledAtDesc".equals(method.getName())) {
                        return List.of(CohortMember.builder()
                                .id(1L)
                                .cohort(testCohort)
                                .user(studentUser)
                                .roleInCohort("STUDENT")
                                .build());
                    }
                    return null;
                }
        );

        cohortMessageRepository = (CohortMessageRepository) Proxy.newProxyInstance(
                CohortMessageRepository.class.getClassLoader(),
                new Class<?>[]{CohortMessageRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        CohortMessage msg = (CohortMessage) args[0];
                        if (msg.getId() == null) {
                            msg.setId((long) (savedMessages.size() + 1));
                        }
                        if (msg.getCreatedAt() == null) {
                            msg.setCreatedAt(Instant.now());
                        }
                        if (!savedMessages.contains(msg)) {
                            savedMessages.add(msg);
                        }
                        return msg;
                    }
                    if ("findById".equals(method.getName()) && args != null && args.length > 0) {
                        Long id = (Long) args[0];
                        return savedMessages.stream().filter(m -> id.equals(m.getId())).findFirst();
                    }
                    if ("findRecentMessagesByCohortIdDesc".equals(method.getName()) || "findRecentMessagesByCohortId".equals(method.getName())) {
                        return new ArrayList<>(savedMessages);
                    }
                    if ("findRecentMessagesByCohortIdsAndNotSender".equals(method.getName())) {
                        return new ArrayList<>(savedMessages);
                    }
                    return null;
                }
        );

        SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate((message, timeout) -> true) {
            @Override
            public void convertAndSend(String destination, Object payload) throws MessagingException {
                broadcastedMessages.put(destination, payload);
            }
        };

        cohortChatService = new CohortChatServiceImpl(
                cohortRepository,
                cohortMemberRepository,
                cohortMessageRepository,
                messagingTemplate
        );

        authService = (AuthService) Proxy.newProxyInstance(
                AuthService.class.getClassLoader(),
                new Class<?>[]{AuthService.class},
                (proxy, method, args) -> {
                    if ("getCurrentAuthenticatedUser".equals(method.getName())) {
                        return Optional.of(studentUser);
                    }
                    return null;
                }
        );

        userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByUsernameOrEmail".equals(method.getName())) {
                        String name = (String) args[0];
                        if ("student1".equals(name)) return Optional.of(studentUser);
                        if ("instructor1".equals(name)) return Optional.of(instructorUser);
                        if ("admin1".equals(name)) return Optional.of(adminUser);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        cohortChatController = new CohortChatController(
                cohortChatService,
                authService,
                userRepository,
                cohortRepository
        );
    }

    @Test
    void testCanUserAccessCohort_AdminReturnsTrue() {
        assertTrue(cohortChatService.canUserAccessCohort(adminUser, 500L));
    }

    @Test
    void testCanUserAccessCohort_InstructorReturnsTrue() {
        assertTrue(cohortChatService.canUserAccessCohort(instructorUser, 500L));
    }

    @Test
    void testCanUserAccessCohort_EnrolledStudentReturnsTrue() {
        assertTrue(cohortChatService.canUserAccessCohort(studentUser, 500L));
    }

    @Test
    void testCanUserAccessCohort_UnenrolledUserReturnsFalse() {
        User outsider = User.builder().id(999L).username("outsider").build();
        assertFalse(cohortChatService.canUserAccessCohort(outsider, 500L));
    }

    @Test
    void testProcessAndSendMessage_Success_BroadcastsMessageAndNotifiesInstructor() {
        ChatMessageRequestDTO request = ChatMessageRequestDTO.builder()
                .message("Xin chào cả lớp!")
                .messageType("TEXT")
                .build();

        ChatMessageResponseDTO response = cohortChatService.processAndSendMessage(500L, studentUser, request);

        assertNotNull(response);
        assertEquals("Xin chào cả lớp!", response.getMessage());
        assertEquals("Nguyễn Văn Học", response.getSenderName());
        assertEquals("STUDENT", response.getSenderRole());
        assertEquals(1, savedMessages.size());
        assertTrue(broadcastedMessages.containsKey("/topic/cohort/500"));
        assertTrue(broadcastedMessages.containsKey("/topic/users/20/notifications"));

        Object notifObj = broadcastedMessages.get("/topic/users/20/notifications");
        assertTrue(notifObj instanceof ChatNotificationDTO);
        ChatNotificationDTO notif = (ChatNotificationDTO) notifObj;
        assertEquals("Nguyễn Văn Học", notif.getSenderName());
        assertEquals("Xin chào cả lớp!", notif.getMessage());
        assertEquals("/modules/500/learn", notif.getActionUrl());
    }

    @Test
    void testProcessAndSendMessage_InstructorSends_NotifiesStudent() {
        ChatMessageRequestDTO request = ChatMessageRequestDTO.builder()
                .message("Chào các em, hôm nay chúng ta học WebSocket!")
                .messageType("TEXT")
                .build();

        ChatMessageResponseDTO response = cohortChatService.processAndSendMessage(500L, instructorUser, request);

        assertNotNull(response);
        assertEquals("INSTRUCTOR", response.getSenderRole());
        assertTrue(broadcastedMessages.containsKey("/topic/cohort/500"));
        assertTrue(broadcastedMessages.containsKey("/topic/users/10/notifications"));
    }

    @Test
    void testProcessAndSendMessage_BlankThrowsException() {
        ChatMessageRequestDTO request = ChatMessageRequestDTO.builder().message("   ").build();
        assertThrows(IllegalArgumentException.class, () ->
                cohortChatService.processAndSendMessage(500L, studentUser, request));
    }

    @Test
    void testProcessAndSendMessage_MessageTooLongThrowsException() {
        String longMsg = "a".repeat(1001);
        ChatMessageRequestDTO request = ChatMessageRequestDTO.builder().message(longMsg).build();
        assertThrows(IllegalArgumentException.class, () ->
                cohortChatService.processAndSendMessage(500L, studentUser, request));
    }

    @Test
    void testProcessAndSendMessage_SpamCooldownThrowsException() {
        ChatMessageRequestDTO req1 = ChatMessageRequestDTO.builder().message("Tin nhắn 1").build();
        ChatMessageRequestDTO req2 = ChatMessageRequestDTO.builder().message("Tin nhắn 2").build();

        cohortChatService.processAndSendMessage(500L, studentUser, req1);
        assertThrows(IllegalArgumentException.class, () ->
                cohortChatService.processAndSendMessage(500L, studentUser, req2));
    }

    @Test
    void testProcessAndSendMessage_UnenrolledThrowsAccessDenied() {
        User outsider = User.builder().id(999L).username("outsider").build();
        ChatMessageRequestDTO request = ChatMessageRequestDTO.builder().message("Test").build();

        assertThrows(AccessDeniedException.class, () ->
                cohortChatService.processAndSendMessage(500L, outsider, request));
    }

    @Test
    void testGetRecentMessages_Success() {
        savedMessages.add(CohortMessage.builder()
                .id(1L)
                .cohort(testCohort)
                .sender(instructorUser)
                .message("Chào mừng bạn!")
                .messageType("TEXT")
                .createdAt(Instant.now())
                .build());

        List<ChatMessageResponseDTO> list = cohortChatService.getRecentMessages(500L, studentUser);
        assertEquals(1, list.size());
        assertEquals("Chào mừng bạn!", list.get(0).getMessage());
        assertEquals("INSTRUCTOR", list.get(0).getSenderRole());
    }

    @Test
    void testGetRecentNotifications_Success() {
        savedMessages.add(CohortMessage.builder()
                .id(1L)
                .cohort(testCohort)
                .sender(studentUser)
                .message("Thầy ơi cho em hỏi câu này...")
                .messageType("TEXT")
                .createdAt(Instant.now())
                .build());

        List<ChatNotificationDTO> notifications = cohortChatService.getRecentNotifications(instructorUser);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());
        assertEquals("Nguyễn Văn Học", notifications.get(0).getSenderName());
        assertEquals("Thầy ơi cho em hỏi câu này...", notifications.get(0).getMessage());
        assertEquals("/modules/500/learn", notifications.get(0).getActionUrl());
    }

    @Test
    void testCohortChatController_GetAndSendMessages() {
        ResponseEntity<List<ChatMessageResponseDTO>> getResp = cohortChatController.getCohortMessages(500L);
        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        assertNotNull(getResp.getBody());

        ChatMessageRequestDTO postReq = ChatMessageRequestDTO.builder().message("Tin nhắn Controller").build();
        ResponseEntity<ChatMessageResponseDTO> postResp = cohortChatController.sendCohortMessage(500L, postReq);
        assertEquals(HttpStatus.OK, postResp.getStatusCode());
        assertEquals("Tin nhắn Controller", postResp.getBody().getMessage());
    }

    @Test
    void testCohortChatController_GetRecentNotifications() {
        ResponseEntity<List<ChatNotificationDTO>> notifResp = cohortChatController.getRecentNotifications();
        assertEquals(HttpStatus.OK, notifResp.getStatusCode());
        assertNotNull(notifResp.getBody());
    }

    @Test
    void testCohortChatController_ShowInstructorDiscussionsRedirects() {
        String view = cohortChatController.showInstructorDiscussions();
        assertEquals("redirect:/instructor/cohorts/500/chat", view);
    }

    @Test
    void testCohortChatController_HandleWebSocketMessage() {
        Principal principal = () -> "instructor1";
        ChatMessageRequestDTO wsReq = ChatMessageRequestDTO.builder().message("Tin nhắn STOMP").build();
        assertDoesNotThrow(() -> cohortChatController.handleChatMessage(500L, wsReq, principal));
        assertTrue(broadcastedMessages.containsKey("/topic/cohort/500"));
    }

    @Test
    void testDeleteMessage_Self_HidesForCallerOnly() {
        cohortChatService.processAndSendMessage(500L, studentUser, ChatMessageRequestDTO.builder().message("Tin nhắn của sinh viên").build());
        assertEquals(1, savedMessages.size());
        Long msgId = savedMessages.get(0).getId();

        cohortChatService.deleteMessage(500L, msgId, studentUser, "SELF");

        List<ChatMessageResponseDTO> studentView = cohortChatService.getRecentMessages(500L, studentUser);
        assertTrue(studentView.isEmpty());

        List<ChatMessageResponseDTO> instructorView = cohortChatService.getRecentMessages(500L, instructorUser);
        assertEquals(1, instructorView.size());
        assertEquals("Tin nhắn của sinh viên", instructorView.get(0).getMessage());
    }

    @Test
    void testDeleteMessage_All_BySender() {
        cohortChatService.processAndSendMessage(500L, studentUser, ChatMessageRequestDTO.builder().message("Tin nhắn cần thu hồi").build());
        Long msgId = savedMessages.get(0).getId();

        cohortChatService.deleteMessage(500L, msgId, studentUser, "ALL");

        assertTrue(savedMessages.get(0).getIsDeletedForAll());
        assertEquals(studentUser.getId(), savedMessages.get(0).getDeletedByUserId());

        assertTrue(cohortChatService.getRecentMessages(500L, studentUser).isEmpty());
        assertTrue(cohortChatService.getRecentMessages(500L, instructorUser).isEmpty());

        assertTrue(broadcastedMessages.containsKey("/topic/cohort/500"));
        ChatMessageResponseDTO event = (ChatMessageResponseDTO) broadcastedMessages.get("/topic/cohort/500");
        assertTrue(event.getIsDeleted());
        assertEquals("DELETED", event.getMessageType());
    }

    @Test
    void testDeleteMessage_All_ByInstructor_Violation() {
        cohortChatService.processAndSendMessage(500L, studentUser, ChatMessageRequestDTO.builder().message("Nội dung vi phạm quy tắc").build());
        Long msgId = savedMessages.get(0).getId();

        cohortChatService.deleteMessage(500L, msgId, instructorUser, "ALL");

        assertTrue(savedMessages.get(0).getIsDeletedForAll());
        assertEquals(instructorUser.getId(), savedMessages.get(0).getDeletedByUserId());

        assertTrue(cohortChatService.getRecentMessages(500L, studentUser).isEmpty());
        assertTrue(cohortChatService.getRecentMessages(500L, instructorUser).isEmpty());

        ChatMessageResponseDTO event = (ChatMessageResponseDTO) broadcastedMessages.get("/topic/cohort/500");
        assertTrue(event.getIsDeleted());
        assertEquals(instructorUser.getId(), event.getDeletedByUserId());
    }

    @Test
    void testDeleteMessage_All_ByOtherStudent_ThrowsAccessDenied() {
        cohortChatService.processAndSendMessage(500L, instructorUser, ChatMessageRequestDTO.builder().message("Thông báo từ giảng viên").build());
        Long msgId = savedMessages.get(0).getId();

        assertThrows(AccessDeniedException.class, () ->
                cohortChatService.deleteMessage(500L, msgId, studentUser, "ALL")
        );
    }

    @Test
    void testCohortChatController_DeleteCohortMessage() {
        cohortChatService.processAndSendMessage(500L, studentUser, ChatMessageRequestDTO.builder().message("Tin xóa qua API").build());
        Long msgId = savedMessages.get(0).getId();

        ResponseEntity<?> resp = cohortChatController.deleteCohortMessage(500L, msgId, "SELF");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
