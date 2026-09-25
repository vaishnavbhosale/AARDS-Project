package com.aards.dashboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    public Map<String, Object> getDashboard(String session, String year, String semester, String department) {
        log.info("Dashboard requested: session={} year={} semester={} dept={}", session, year, semester, department);
        return Map.of(
                "totalStudents", 0,
                "passed", 0,
                "failed", 0,
                "overallPassPct", 0.0,
                "averageSgpa", 0.0,
                "filters", Map.of("session", String.valueOf(session),
                        "year", String.valueOf(year),
                        "semester", String.valueOf(semester),
                        "department", String.valueOf(department)));
    }
}
