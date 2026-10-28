package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AttendanceService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalTime SHIFT_START_TIME = LocalTime.of(7, 0);
    private static final LocalTime SHIFT_END_TIME = LocalTime.of(18, 0);

    private final ModeratorAttendanceRepository attendanceRepository;

    @Override
    public ModeratorAttendance checkIn(User moderator) {
        ZonedDateTime nowHcm = ZonedDateTime.now(HCM_ZONE);
        LocalTime timeHcm = nowHcm.toLocalTime();
        if (timeHcm.isBefore(SHIFT_START_TIME)) {
            throw new IllegalStateException("Hệ thống chỉ cho phép bắt đầu ca trực (Check-in) từ 07:00 sáng.");
        }
        if (!timeHcm.isBefore(SHIFT_END_TIME)) {
            throw new IllegalStateException("Ca trực tiêu chuẩn kết thúc lúc 18:00. Vui lòng chọn Bắt đầu ca làm thêm (Check-in OT) nếu cần làm việc ngoài giờ.");
        }

        LocalDate today = nowHcm.toLocalDate();
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);

        if (existing.isPresent()) {
            ModeratorAttendance attendance = existing.get();
            applyAutoCheckOutIfNeeded(attendance);
            if (attendance.getCheckInAt() == null) {
                attendance.setCheckInAt(Instant.now());
                attendance.setIsActive(true);
                return attendanceRepository.save(attendance);
            }
            if (attendance.getCheckOutAt() != null) {
                throw new IllegalStateException("Bạn đã hoàn thành ca trực tiêu chuẩn hôm nay. Vui lòng chọn Bắt đầu ca làm thêm (Check-in OT) để tiếp tục làm việc.");
            }
            return attendance;
        }

        ModeratorAttendance newAttendance = ModeratorAttendance.builder()
                .moderator(moderator)
                .workDate(today)
                .checkInAt(Instant.now())
                .isActive(true)
                .isOvertime(false)
                .assignedTasksCount(0)
                .completedTasksCount(0)
                .activeTasksCount(0)
                .totalWorkloadMinutes(0)
                .otWorkloadMinutes(0)
                .maxDailyMinutes(480)
                .build();
        return attendanceRepository.save(newAttendance);
    }

    @Override
    public ModeratorAttendance checkOut(User moderator) {
        LocalDate today = LocalDate.now(HCM_ZONE);
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);

        if (existing.isPresent()) {
            ModeratorAttendance attendance = existing.get();
            if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                Instant now = Instant.now();
                Instant cutoff18h = today.atTime(SHIFT_END_TIME).atZone(HCM_ZONE).toInstant();
                Instant checkoutTime = now.isAfter(cutoff18h) ? cutoff18h : now;
                attendance.setCheckOutAt(checkoutTime);
                attendance.setIsActive(false);
                int standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), checkoutTime));
                int otMinutes = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
                attendance.setTotalWorkloadMinutes(standardMinutes + otMinutes);
                return attendanceRepository.save(attendance);
            }
            return attendance;
        }
        throw new IllegalStateException("Không tìm thấy thông tin ca trực cho ngày hôm nay.");
    }

    @Override
    public ModeratorAttendance checkInOvertime(User moderator) {
        LocalDate today = LocalDate.now(HCM_ZONE);
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);
        ModeratorAttendance attendance;

        if (existing.isPresent()) {
            attendance = existing.get();
            applyAutoCheckOutIfNeeded(attendance);
            if (Boolean.TRUE.equals(attendance.getIsActive()) && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                throw new IllegalStateException("Bạn đang trong ca trực tiêu chuẩn. Vui lòng kết thúc ca trực tiêu chuẩn trước khi bắt đầu OT.");
            }
            if (attendance.getOtCheckInAt() != null && attendance.getOtCheckOutAt() == null) {
                return attendance;
            }
            attendance.setIsOvertime(true);
            attendance.setOtCheckInAt(Instant.now());
            attendance.setOtCheckOutAt(null);
            attendance.setIsActive(true);
            return attendanceRepository.save(attendance);
        }

        attendance = ModeratorAttendance.builder()
                .moderator(moderator)
                .workDate(today)
                .isActive(true)
                .isOvertime(true)
                .otCheckInAt(Instant.now())
                .assignedTasksCount(0)
                .completedTasksCount(0)
                .activeTasksCount(0)
                .totalWorkloadMinutes(0)
                .otWorkloadMinutes(0)
                .maxDailyMinutes(480)
                .build();
        return attendanceRepository.save(attendance);
    }

    @Override
    public ModeratorAttendance checkOutOvertime(User moderator) {
        LocalDate today = LocalDate.now(HCM_ZONE);
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);
        if (existing.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy thông tin ca làm việc cho ngày hôm nay.");
        }
        ModeratorAttendance attendance = existing.get();
        if (attendance.getOtCheckInAt() == null) {
            throw new IllegalStateException("Bạn chưa điểm danh bắt đầu ca làm việc OT.");
        }
        if (attendance.getOtCheckOutAt() != null) {
            return attendance;
        }

        Instant otCheckout = Instant.now();
        attendance.setOtCheckOutAt(otCheckout);
        attendance.setIsActive(false);
        int otMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getOtCheckInAt(), otCheckout));
        attendance.setOtWorkloadMinutes(otMinutes);

        int standardMinutes = 0;
        if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() != null) {
            standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), attendance.getCheckOutAt()));
        }
        attendance.setTotalWorkloadMinutes(standardMinutes + otMinutes);
        return attendanceRepository.save(attendance);
    }

    @Override
    public ModeratorAttendance getCurrentAttendance(User moderator) {
        LocalDate today = LocalDate.now(HCM_ZONE);
        ModeratorAttendance attendance = attendanceRepository.findByModeratorAndWorkDate(moderator, today).orElse(null);
        return applyAutoCheckOutIfNeeded(attendance);
    }

    @Override
    public List<ModeratorAttendance> getAttendanceHistory(User moderator) {
        List<ModeratorAttendance> list = attendanceRepository.findByModeratorOrderByWorkDateDesc(moderator);
        list.forEach(this::applyAutoCheckOutIfNeeded);
        return list;
    }

    @Override
    public List<ModeratorAttendance> getAllAttendances() {
        List<ModeratorAttendance> list = attendanceRepository.findAllByOrderByWorkDateDesc();
        list.forEach(this::applyAutoCheckOutIfNeeded);
        return list;
    }

    @Scheduled(cron = "0 0 18 * * ?", zone = "Asia/Ho_Chi_Minh")
    @Scheduled(fixedDelay = 900000)
    @Override
    public void autoCheckOutExpiredAttendances() {
        List<ModeratorAttendance> list = attendanceRepository.findAll();
        for (ModeratorAttendance att : list) {
            if (Boolean.TRUE.equals(att.getIsActive()) && att.getCheckInAt() != null && att.getCheckOutAt() == null && !Boolean.TRUE.equals(att.getIsOvertime())) {
                applyAutoCheckOutIfNeeded(att);
            }
        }
    }

    private ModeratorAttendance applyAutoCheckOutIfNeeded(ModeratorAttendance attendance) {
        if (attendance == null || attendance.getCheckInAt() == null) {
            return attendance;
        }
        ZonedDateTime nowHcm = ZonedDateTime.now(HCM_ZONE);
        LocalDate today = nowHcm.toLocalDate();

        if (Boolean.TRUE.equals(attendance.getIsActive()) && attendance.getCheckOutAt() == null && !Boolean.TRUE.equals(attendance.getIsOvertime())) {
            boolean isPastDay = attendance.getWorkDate().isBefore(today);
            boolean isPast18hToday = attendance.getWorkDate().isEqual(today) && !nowHcm.toLocalTime().isBefore(SHIFT_END_TIME);
            if (isPastDay || isPast18hToday) {
                Instant cutoff18h = attendance.getWorkDate().atTime(SHIFT_END_TIME).atZone(HCM_ZONE).toInstant();
                Instant checkIn = attendance.getCheckInAt();
                Instant checkout = cutoff18h.isAfter(checkIn) ? cutoff18h : checkIn;
                attendance.setCheckOutAt(checkout);
                attendance.setIsActive(false);
                int standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(checkIn, checkout));
                int otMinutes = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
                attendance.setTotalWorkloadMinutes(standardMinutes + otMinutes);
                return attendanceRepository.save(attendance);
            }
        }
        return attendance;
    }
}
