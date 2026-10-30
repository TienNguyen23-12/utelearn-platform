package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortMessageRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dto.ChatMessageRequestDTO;
import vn.edu.ute.utelearn.dto.ChatMessageResponseDTO;
import vn.edu.ute.utelearn.dto.ChatNotificationDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.CohortMessage;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CohortChatService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CohortChatServiceImpl implements CohortChatService {

    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CohortMessageRepository cohortMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<Long, Instant> userLastMessageTimes = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessCohort(User user, Long cohortId) {
        if (user == null || cohortId == null) {
            return false;
        }

        if (SecurityUtils.isAdmin(user) || SecurityUtils.isModerator(user)) {
            return true;
        }

        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null) {
            return false;
        }

        if (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        if (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null
                && cohort.getCourse().getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        return cohortMemberRepository.existsByCohort_IdAndUser_Id(cohortId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserModerateCohort(User user, Long cohortId) {
        if (user == null || cohortId == null) {
            return false;
        }

        if (SecurityUtils.isAdmin(user) || SecurityUtils.isModerator(user) || SecurityUtils.isInstructor(user)) {
            return true;
        }

        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null) {
            return false;
        }

        if (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        if (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null
                && cohort.getCourse().getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        Optional<CohortMember> memberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(cohortId, user.getId());
        if (memberOpt.isPresent()) {
            String role = memberOpt.get().getRoleInCohort();
            return "INSTRUCTOR".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role) || "TA".equalsIgnoreCase(role);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getRecentMessages(Long cohortId, User user) {
        if (!canUserAccessCohort(user, cohortId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập thảo luận của lớp học này.");
        }

        List<CohortMessage> rawMessages = cohortMessageRepository.findRecentMessagesByCohortIdDesc(cohortId, PageRequest.of(0, 100));
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }

        String userToken = user != null ? ("," + user.getId() + ",") : null;
        List<CohortMessage> messages = new ArrayList<>(rawMessages.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeletedForAll()))
                .filter(m -> userToken == null || m.getHiddenForUserIds() == null || !m.getHiddenForUserIds().contains(userToken))
                .toList());
        Collections.reverse(messages);

        Map<Long, String> roleMap = cohortMemberRepository.findByCohort_Id(cohortId).stream()
                .filter(m -> m.getUser() != null)
                .collect(Collectors.toMap(m -> m.getUser().getId(), CohortMember::getRoleInCohort, (a, b) -> a));

        return messages.stream()
                .map(m -> {
                    String role = determineSenderRole(m.getSender(), m.getCohort(), roleMap);
                    return toResponseDTO(m, role);
                })
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponseDTO processAndSendMessage(Long cohortId, User user, ChatMessageRequestDTO request) {
        if (user == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập trước khi gửi tin nhắn.");
        }

        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }

        String trimmedMessage = request.getMessage().trim();
        if (trimmedMessage.length() > 1000) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được vượt quá 1000 ký tự.");
        }

        Instant lastSent = userLastMessageTimes.get(user.getId());
        if (lastSent != null && Duration.between(lastSent, Instant.now()).toMillis() < 500) {
            throw new IllegalArgumentException("Bạn đang gửi tin nhắn quá nhanh. Vui lòng thử lại sau giây lát.");
        }
        userLastMessageTimes.put(user.getId(), Instant.now());

        if (!canUserAccessCohort(user, cohortId)) {
            throw new AccessDeniedException("Bạn không có quyền gửi tin nhắn trong lớp học này.");
        }

        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt học: " + cohortId));

        String messageType = (request.getMessageType() != null && !request.getMessageType().isBlank())
                ? request.getMessageType()
                : "TEXT";

        CohortMessage entity = CohortMessage.builder()
                .cohort(cohort)
                .sender(user)
                .message(trimmedMessage)
                .messageType(messageType)
                .build();

        CohortMessage saved = cohortMessageRepository.save(entity);
        String role = determineSenderRole(user, cohort, null);
        ChatMessageResponseDTO responseDTO = toResponseDTO(saved, role);

        messagingTemplate.convertAndSend("/topic/cohort/" + cohortId, responseDTO);
        dispatchNotifications(cohort, saved, role);

        return responseDTO;
    }

    @Override
    @Transactional
    public void deleteMessage(Long cohortId, Long messageId, User user, String mode) {
        if (user == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập trước khi thao tác tin nhắn.");
        }

        if (!canUserAccessCohort(user, cohortId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập thảo luận của lớp học này.");
        }

        CohortMessage message = cohortMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn: " + messageId));

        if (message.getCohort() == null || !message.getCohort().getId().equals(cohortId)) {
            throw new IllegalArgumentException("Tin nhắn không thuộc đợt học này.");
        }

        if ("ALL".equalsIgnoreCase(mode)) {
            boolean isSender = message.getSender() != null && message.getSender().getId().equals(user.getId());
            boolean isModeratorOrInstructor = canUserModerateCohort(user, cohortId);

            if (!isSender && !isModeratorOrInstructor) {
                throw new AccessDeniedException("Bạn không có quyền xóa tin nhắn này đối với mọi người.");
            }

            message.setIsDeletedForAll(true);
            message.setDeletedByUserId(user.getId());
            cohortMessageRepository.save(message);

            ChatMessageResponseDTO event = ChatMessageResponseDTO.builder()
                    .id(messageId)
                    .cohortId(cohortId)
                    .messageType("DELETED")
                    .isDeleted(true)
                    .deletedByUserId(user.getId())
                    .build();
            messagingTemplate.convertAndSend("/topic/cohort/" + cohortId, event);
            return;
        }

        String currentHidden = message.getHiddenForUserIds();
        String userToken = "," + user.getId() + ",";
        if (currentHidden == null || currentHidden.isBlank()) {
            message.setHiddenForUserIds(userToken);
        } else if (!currentHidden.contains(userToken)) {
            message.setHiddenForUserIds(currentHidden + user.getId() + ",");
        }
        cohortMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatNotificationDTO> getRecentNotifications(User user) {
        if (user == null) {
            return Collections.emptyList();
        }

        Set<Long> cohortIds = new HashSet<>();
        cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user).stream()
                .filter(cm -> cm.getCohort() != null)
                .forEach(cm -> cohortIds.add(cm.getCohort().getId()));

        try {
            cohortRepository.findByInstructorId(user.getId(), PageRequest.of(0, 50)).getContent().stream()
                    .forEach(c -> cohortIds.add(c.getId()));
        } catch (Exception ignored) {
        }

        if (cohortIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CohortMessage> recentMessages = cohortMessageRepository.findRecentMessagesByCohortIdsAndNotSender(
                new ArrayList<>(cohortIds),
                user.getId(),
                PageRequest.of(0, 10)
        );

        String userToken = "," + user.getId() + ",";
        return recentMessages.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeletedForAll()))
                .filter(m -> m.getHiddenForUserIds() == null || !m.getHiddenForUserIds().contains(userToken))
                .map(m -> {
                    Cohort c = m.getCohort();
                    String cohortTitle = c != null && c.getName() != null && !c.getName().isBlank()
                            ? c.getName()
                            : (c != null && c.getCourse() != null ? c.getCourse().getTitle() : "Lớp học");
                    String rawContent = m.getMessage() != null ? m.getMessage() : "";
                    String preview = rawContent.length() > 80 ? rawContent.substring(0, 80) + "..." : rawContent;
                    String role = determineSenderRole(m.getSender(), c, null);

                    return ChatNotificationDTO.builder()
                            .cohortId(c != null ? c.getId() : null)
                            .cohortName(cohortTitle)
                            .senderId(m.getSender() != null ? m.getSender().getId() : null)
                            .senderName(m.getSender() != null ? m.getSender().getFullName() : "Người dùng")
                            .senderAvatar(m.getSender() != null ? m.getSender().getAvatarUrl() : null)
                            .senderRole(role)
                            .message(preview)
                            .actionUrl(c != null ? "/modules/" + c.getId() + "/learn" : "#")
                            .createdAtFormatted(TIME_FORMATTER.format(m.getCreatedAt() != null ? m.getCreatedAt() : Instant.now()))
                            .build();
                })
                .toList();
    }

    private void dispatchNotifications(Cohort cohort, CohortMessage message, String senderRole) {
        try {
            Set<Long> targetUserIds = new HashSet<>();
            Long senderId = message.getSender() != null ? message.getSender().getId() : null;

            if ("STUDENT".equalsIgnoreCase(senderRole)) {
                if (cohort.getCreatedBy() != null) {
                    targetUserIds.add(cohort.getCreatedBy().getId());
                }
                if (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null) {
                    targetUserIds.add(cohort.getCourse().getCreatedBy().getId());
                }
                cohortMemberRepository.findByCohort_Id(cohort.getId()).stream()
                        .filter(m -> m.getUser() != null && ("TA".equalsIgnoreCase(m.getRoleInCohort()) || "INSTRUCTOR".equalsIgnoreCase(m.getRoleInCohort())))
                        .forEach(m -> targetUserIds.add(m.getUser().getId()));
            } else {
                cohortMemberRepository.findByCohort_Id(cohort.getId()).stream()
                        .filter(m -> m.getUser() != null)
                        .forEach(m -> targetUserIds.add(m.getUser().getId()));
                if (cohort.getCreatedBy() != null) {
                    targetUserIds.add(cohort.getCreatedBy().getId());
                }
            }

            if (senderId != null) {
                targetUserIds.remove(senderId);
            }

            if (targetUserIds.isEmpty()) {
                return;
            }

            String cohortTitle = cohort.getName() != null && !cohort.getName().isBlank()
                    ? cohort.getName()
                    : (cohort.getCourse() != null ? cohort.getCourse().getTitle() : "Lớp học");

            String rawContent = message.getMessage() != null ? message.getMessage() : "";
            String preview = rawContent.length() > 80 ? rawContent.substring(0, 80) + "..." : rawContent;

            ChatNotificationDTO notification = ChatNotificationDTO.builder()
                    .cohortId(cohort.getId())
                    .cohortName(cohortTitle)
                    .senderId(senderId)
                    .senderName(message.getSender() != null ? message.getSender().getFullName() : "Người dùng")
                    .senderAvatar(message.getSender() != null ? message.getSender().getAvatarUrl() : null)
                    .senderRole(senderRole)
                    .message(preview)
                    .actionUrl("/modules/" + cohort.getId() + "/learn")
                    .createdAtFormatted(TIME_FORMATTER.format(message.getCreatedAt() != null ? message.getCreatedAt() : Instant.now()))
                    .build();

            for (Long targetUserId : targetUserIds) {
                messagingTemplate.convertAndSend("/topic/users/" + targetUserId + "/notifications", notification);
            }
        } catch (Exception ignored) {
        }
    }

    private String determineSenderRole(User sender, Cohort cohort, Map<Long, String> roleMap) {
        if (sender == null) {
            return "STUDENT";
        }

        if (SecurityUtils.isAdmin(sender)) {
            return "ADMIN";
        }

        if (cohort != null && cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(sender.getId())) {
            return "INSTRUCTOR";
        }

        if (cohort != null && cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null
                && cohort.getCourse().getCreatedBy().getId().equals(sender.getId())) {
            return "INSTRUCTOR";
        }

        if (SecurityUtils.isInstructor(sender)) {
            return "INSTRUCTOR";
        }

        if (roleMap != null && roleMap.containsKey(sender.getId())) {
            String cachedRole = roleMap.get(sender.getId());
            if ("TA".equalsIgnoreCase(cachedRole)) return "TA";
            if ("INSTRUCTOR".equalsIgnoreCase(cachedRole) || "TEACHER".equalsIgnoreCase(cachedRole)) return "INSTRUCTOR";
            return "STUDENT";
        }

        if (cohort != null) {
            Optional<CohortMember> memberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(cohort.getId(), sender.getId());
            if (memberOpt.isPresent()) {
                String memberRole = memberOpt.get().getRoleInCohort();
                if ("TA".equalsIgnoreCase(memberRole)) {
                    return "TA";
                }
                if ("INSTRUCTOR".equalsIgnoreCase(memberRole) || "TEACHER".equalsIgnoreCase(memberRole)) {
                    return "INSTRUCTOR";
                }
            }
        }

        return "STUDENT";
    }

    private ChatMessageResponseDTO toResponseDTO(CohortMessage message, String senderRole) {
        User sender = message.getSender();
        String senderName = sender != null ? sender.getFullName() : "Người dùng";
        String senderAvatar = sender != null ? sender.getAvatarUrl() : null;
        Instant createdAt = message.getCreatedAt() != null ? message.getCreatedAt() : Instant.now();

        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .cohortId(message.getCohort() != null ? message.getCohort().getId() : null)
                .senderId(sender != null ? sender.getId() : null)
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .senderRole(senderRole)
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .isDeleted(Boolean.TRUE.equals(message.getIsDeletedForAll()))
                .deletedByUserId(message.getDeletedByUserId())
                .createdAt(createdAt)
                .createdAtFormatted(TIME_FORMATTER.format(createdAt))
                .build();
    }
}
