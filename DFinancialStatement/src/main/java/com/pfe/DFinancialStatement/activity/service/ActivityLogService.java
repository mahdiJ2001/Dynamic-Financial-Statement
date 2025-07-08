package com.pfe.DFinancialStatement.activity.service;

import com.pfe.DFinancialStatement.activity.entity.ActivityLog;
import com.pfe.DFinancialStatement.activity.enums.ActionType;
import com.pfe.DFinancialStatement.activity.mapper.ActivityLogMapper;
import com.pfe.DFinancialStatement.activity.repository.ActivityLogRepository;
import com.pfe.DFinancialStatement.activity.dto.ActivityLogDTO;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.repository.UserRepository;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository repository;
    private final AuthService authService;
    private final ActivityLogMapper mapper;
    private final UserRepository userRepository; // Needed to lookup user by username

    @Autowired
    public ActivityLogService(ActivityLogRepository repository, AuthService authService, ActivityLogMapper mapper, UserRepository userRepository) {
        this.repository = repository;
        this.authService = authService;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    // Updated to avoid NPE in background processing by resolving user from params if possible
    public void log(ActionType actionType, String messageKey, Map<String, String> params) {
        User user = null;

        if (params != null && params.containsKey("username")) {
            String username = params.get("username");
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                user = userOpt.get();
            }
        }

        if (user == null) {
            try {
                user = authService.getCurrentUser();
            } catch (Exception e) {
                // AuthService.getCurrentUser() might fail if no auth context is available,
                // so just log and proceed with user == null
            }
        }

        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setActionType(actionType);
        log.setMessageKey(messageKey);
        log.setParams(params);
        repository.save(log);
    }

    public List<ActivityLogDTO> getAllLogs() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
}
