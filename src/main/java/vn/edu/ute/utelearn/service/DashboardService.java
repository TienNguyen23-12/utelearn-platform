package vn.edu.ute.utelearn.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardStatistics(String filter);
}
