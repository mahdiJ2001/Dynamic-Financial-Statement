package com.pfe.DFinancialStatement.activity.service;

import com.pfe.DFinancialStatement.activity.entity.ActivityLog;
import com.pfe.DFinancialStatement.activity.enums.ActionType;
import com.pfe.DFinancialStatement.activity.mapper.ActivityLogMapper;
import com.pfe.DFinancialStatement.activity.repository.ActivityLogRepository;
import com.pfe.DFinancialStatement.activity.dto.ActivityLogDTO;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository repository;
    private final AuthService authService;
    private final ActivityLogMapper mapper;

    @Autowired
    public ActivityLogService(ActivityLogRepository repository, AuthService authService, ActivityLogMapper mapper) {
        this.repository = repository;
        this.authService = authService;
        this.mapper = mapper;
    }

    public void log(ActionType actionType, String description) {
        User user = authService.getCurrentUser();
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setActionType(actionType);
        log.setDescription(description);
        repository.save(log);
    }

    public List<ActivityLogDTO> getAllLogs() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
}
