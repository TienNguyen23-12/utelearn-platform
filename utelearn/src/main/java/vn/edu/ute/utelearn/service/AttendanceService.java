package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;

public interface AttendanceService {
    ModeratorAttendance checkIn(User moderator);
    ModeratorAttendance checkOut(User moderator);
    ModeratorAttendance getCurrentAttendance(User moderator);
    List<ModeratorAttendance> getAttendanceHistory(User moderator);
    List<ModeratorAttendance> getAllAttendances();
}
