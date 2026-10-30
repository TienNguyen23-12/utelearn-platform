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
        LocalDate today = nowHcm.toLocalDate();
        Instant now = nowHcm.toInstant();

        boolean isOutsideStandardHours = timeHcm.isBefore(SHIFT_START_TIME) || !timeHcm.isBefore(SHIFT_END_TIME);

        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);
        if (existing.isPresent()) {
            ModeratorAttendance attendance = existing.get();
            applyAutoCheckOutIfNeeded(attendance);

            if (Boolean.TRUE.equals(attendance.getIsActive())) {
                return attendance;
            }

            if (isOutsideStandardHours || attendance.getCheckOutAt() != null) {
                attendance.setIsOvertime(true);
                attendance.setOtCheckInAt(now);
                attendance.setOtCheckOutAt(null);
                attendance.setIsActive(true);
                return attendanceRepository.save(attendance);
            }

            if (attendance.getCheckInAt() == null) {
                attendance.setCheckInAt(now);
                attendance.setIsOvertime(false);
                attendance.setIsActive(true);
                return attendanceRepository.save(attendance);
            }

            attendance.setIsOvertime(true);
            attendance.setOtCheckInAt(now);
            attendance.setOtCheckOutAt(null);
            attendance.setIsActive(true);
            return attendanceRepository.save(attendance);
        }

        ModeratorAttendance newAttendance = ModeratorAttendance.builder()
                .moderator(moderator)
                .workDate(today)
                .isActive(true)
                .isOvertime(isOutsideStandardHours)
                .assignedTasksCount(0)
                .completedTasksCount(0)
                .activeTasksCount(0)
                .totalWorkloadMinutes(0)
                .otWorkloadMinutes(0)
                .maxDailyMinutes(480)
                .build();

        if (isOutsideStandardHours) {
            newAttendance.setOtCheckInAt(now);
        } else {
            newAttendance.setCheckInAt(now);
        }

        return attendanceRepository.save(newAttendance);
    }

    @Override
    public ModeratorAttendance checkOut(User moderator) {
        LocalDate today = LocalDate.now(HCM_ZONE);
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);
        if (existing.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy thông tin ca trực cho ngày hôm nay.");
        }
        ModeratorAttendance attendance = existing.get();
        if (!Boolean.TRUE.equals(attendance.getIsActive())) {
            return attendance;
        }

        Instant now = Instant.now();

        if (attendance.getOtCheckInAt() != null && attendance.getOtCheckOutAt() == null) {
            attendance.setOtCheckOutAt(now);
            attendance.setIsActive(false);
            int sessionOt = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getOtCheckInAt(), now));
            int prevOt = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
            int totalOt = prevOt + sessionOt;
            attendance.setOtWorkloadMinutes(totalOt);

            int standardMinutes = 0;
            if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() != null) {
                standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), attendance.getCheckOutAt()));
            }
            attendance.setTotalWorkloadMinutes(standardMinutes + totalOt);
            return attendanceRepository.save(attendance);
        }

        if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
            Instant cutoff18h = today.atTime(SHIFT_END_TIME).atZone(HCM_ZONE).toInstant();
            attendance.setIsActive(false);

            if (now.isAfter(cutoff18h)) {
                attendance.setCheckOutAt(cutoff18h);
                if (attendance.getOtCheckInAt() == null) {
                    attendance.setOtCheckInAt(cutoff18h);
                }
                attendance.setOtCheckOutAt(now);
                attendance.setIsOvertime(true);

                int standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), cutoff18h));
                int sessionOt = (int) Math.max(0, ChronoUnit.MINUTES.between(cutoff18h, now));
                int prevOt = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
                int totalOt = prevOt + sessionOt;
                attendance.setOtWorkloadMinutes(totalOt);
                attendance.setTotalWorkloadMinutes(standardMinutes + totalOt);
            } else {
                attendance.setCheckOutAt(now);
                int standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), now));
                int otMinutes = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
                attendance.setTotalWorkloadMinutes(standardMinutes + otMinutes);
            }
            return attendanceRepository.save(attendance);
        }

        attendance.setIsActive(false);
        return attendanceRepository.save(attendance);
    }

    @Override
    public ModeratorAttendance checkInOvertime(User moderator) {
        return checkIn(moderator);
    }

    @Override
    public ModeratorAttendance checkOutOvertime(User moderator) {
        return checkOut(moderator);
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

    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    @Scheduled(fixedDelay = 900000)
    @Override
    public void autoCheckOutExpiredAttendances() {
        List<ModeratorAttendance> list = attendanceRepository.findAll();
        for (ModeratorAttendance att : list) {
            if (Boolean.TRUE.equals(att.getIsActive())) {
                applyAutoCheckOutIfNeeded(att);
            }
        }
    }

    private ModeratorAttendance applyAutoCheckOutIfNeeded(ModeratorAttendance attendance) {
        if (attendance == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(attendance.getIsActive())) {
            return attendance;
        }

        ZonedDateTime nowHcm = ZonedDateTime.now(HCM_ZONE);
        LocalDate today = nowHcm.toLocalDate();

        if (attendance.getWorkDate().isBefore(today)) {
            Instant endOfDay = attendance.getWorkDate().atTime(23, 59, 59).atZone(HCM_ZONE).toInstant();
            attendance.setIsActive(false);

            if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                Instant cutoff18h = attendance.getWorkDate().atTime(SHIFT_END_TIME).atZone(HCM_ZONE).toInstant();
                attendance.setCheckOutAt(cutoff18h);
                int standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), cutoff18h));
                int otMinutes = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
                attendance.setTotalWorkloadMinutes(standardMinutes + otMinutes);
            }

            if (attendance.getOtCheckInAt() != null && attendance.getOtCheckOutAt() == null) {
                attendance.setOtCheckOutAt(endOfDay);
                int sessionOt = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getOtCheckInAt(), endOfDay));
                int prevOt = attendance.getOtWorkloadMinutes() != null ? attendance.getOtWorkloadMinutes() : 0;
                int totalOt = prevOt + sessionOt;
                attendance.setOtWorkloadMinutes(totalOt);

                int standardMinutes = 0;
                if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() != null) {
                    standardMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(attendance.getCheckInAt(), attendance.getCheckOutAt()));
                }
                attendance.setTotalWorkloadMinutes(standardMinutes + totalOt);
            }

            return attendanceRepository.save(attendance);
        }

        return attendance;
    }
}
