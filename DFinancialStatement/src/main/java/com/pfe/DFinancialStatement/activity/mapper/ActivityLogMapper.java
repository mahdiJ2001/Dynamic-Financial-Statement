package com.pfe.DFinancialStatement.activity.mapper;

import com.pfe.DFinancialStatement.activity.dto.ActivityLogDTO;
import com.pfe.DFinancialStatement.activity.entity.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {

    public ActivityLogDTO toDTO(ActivityLog log) {
        if (log == null) return null;

        ActivityLogDTO dto = new ActivityLogDTO();
        dto.setId(log.getId());
        dto.setActionType(log.getActionType().name());
        dto.setMessageKey(log.getMessageKey());
        dto.setParams(log.getParams());
        dto.setTimestamp(log.getTimestamp().toString());

        if (log.getUser() != null) {
            ActivityLogDTO.UserDTO userDto = new ActivityLogDTO.UserDTO();
            userDto.setId(log.getUser().getId());
            userDto.setUsername(log.getUser().getUsername());
            userDto.setEmail(log.getUser().getEmail());
            dto.setUser(userDto);
        } else {
            dto.setUser(null);
        }

        return dto;
    }
}
