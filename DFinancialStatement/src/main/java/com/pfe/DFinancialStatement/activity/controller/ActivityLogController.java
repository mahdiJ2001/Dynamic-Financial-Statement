package com.pfe.DFinancialStatement.activity.controller;

import com.pfe.DFinancialStatement.activity.dto.ActivityLogDTO;
import com.pfe.DFinancialStatement.activity.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
@CrossOrigin(origins = "*")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @Autowired
    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public List<ActivityLogDTO> getAllLogs() {
        return activityLogService.getAllLogs();
    }
}
