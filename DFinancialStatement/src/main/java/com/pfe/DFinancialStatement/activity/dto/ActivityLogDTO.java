package com.pfe.DFinancialStatement.activity.dto;

import com.pfe.DFinancialStatement.activity.enums.ActionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityLogDTO {
    private String username;
    private ActionType actionType;
    private String description;
    private LocalDateTime timestamp;
}
