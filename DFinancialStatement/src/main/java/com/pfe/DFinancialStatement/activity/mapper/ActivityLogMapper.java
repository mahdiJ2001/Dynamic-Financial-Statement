package com.pfe.DFinancialStatement.activity.mapper;

import com.pfe.DFinancialStatement.activity.dto.ActivityLogDTO;
import com.pfe.DFinancialStatement.activity.entity.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {

    public ActivityLogDTO toDTO(ActivityLog log) {
        ActivityLogDTO dto = new ActivityLogDTO();
        dto.setUsername(log.getUser().getUsername());
        dto.setActionType(log.getActionType());
        dto.setDescription(log.getDescription());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }
}
