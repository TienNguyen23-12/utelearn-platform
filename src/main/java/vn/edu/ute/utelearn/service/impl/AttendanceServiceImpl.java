package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AttendanceService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final ModeratorAttendanceRepository attendanceRepository;

    @Override
    public ModeratorAttendance checkIn(User moderator) {
        LocalDate today = LocalDate.now();
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);

        if (existing.isPresent()) {
            ModeratorAttendance attendance = existing.get();
            if (attendance.getCheckInAt() == null) {
                attendance.setCheckInAt(Instant.now());
                attendance.setIsActive(true);
                return attendanceRepository.save(attendance);
            }
            return attendance;
        }

        ModeratorAttendance newAttendance = ModeratorAttendance.builder()
                .moderator(moderator)
                .workDate(today)
                .checkInAt(Instant.now())
                .isActive(true)
                .assignedTasksCount(0)
                .completedTasksCount(0)
                .activeTasksCount(0)
                .totalWorkloadMinutes(0)
                .maxDailyMinutes(480)
                .build();
        return attendanceRepository.save(newAttendance);
    }

    @Override
    public ModeratorAttendance checkOut(User moderator) {
        LocalDate today = LocalDate.now();
        Optional<ModeratorAttendance> existing = attendanceRepository.findByModeratorAndWorkDate(moderator, today);

        if (existing.isPresent()) {
            ModeratorAttendance attendance = existing.get();
            if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                attendance.setCheckOutAt(Instant.now());
                attendance.setIsActive(false);
                
                long minutes = ChronoUnit.MINUTES.between(attendance.getCheckInAt(), attendance.getCheckOutAt());
                attendance.setTotalWorkloadMinutes((int) minutes);
                
                return attendanceRepository.save(attendance);
            }
            return attendance;
        }
        throw new IllegalStateException("Không tìm thấy thông tin điểm danh cho ngày hôm nay.");
    }

    @Override
    public ModeratorAttendance getCurrentAttendance(User moderator) {
        return attendanceRepository.findByModeratorAndWorkDate(moderator, LocalDate.now()).orElse(null);
    }

    @Override
    public List<ModeratorAttendance> getAttendanceHistory(User moderator) {
        return attendanceRepository.findByModeratorOrderByWorkDateDesc(moderator);
    }

    @Override
    public List<ModeratorAttendance> getAllAttendances() {
        return attendanceRepository.findAllByOrderByWorkDateDesc();
    }
}
