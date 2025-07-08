package com.pfe.DFinancialStatement.outbox.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.repository.UserRepository;
import com.pfe.DFinancialStatement.notification.service.NotificationService;
import com.pfe.DFinancialStatement.outbox.entity.OutboxEvent;
import com.pfe.DFinancialStatement.outbox.repository.OutboxEventRepository;
import com.pfe.DFinancialStatement.activity.service.ActivityLogService;
import com.pfe.DFinancialStatement.activity.enums.ActionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalse();

        for (OutboxEvent event : events) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
                Map<String, String> stringPayload = convertToStringMap(payload);

                switch (event.getAggregateType()) {
                    case "Notification" -> {
                        Long userId = Long.valueOf(stringPayload.get("userId"));
                        User user = userRepository.findById(userId).orElseThrow(() ->
                                new IllegalStateException("Utilisateur introuvable pour l'ID: " + userId));
                        notificationService.createNotification(user, event.getEventType(), stringPayload);
                    }

                    case "ActivityLog" -> {
                        ActionType actionType = mapEventToActionType(event.getEventType());
                        activityLogService.log(actionType, event.getEventType(), stringPayload);
                    }

                    default -> {
                        log.warn("Type d'agrégat inconnu : {}", event.getAggregateType());
                        continue;
                    }
                }

                event.setProcessed(true);
                outboxEventRepository.save(event);

            } catch (Exception e) {
                log.error("Erreur lors du traitement de l'événement Outbox ID {} : {}", event.getId(), e.getMessage(), e);
            }
        }
    }

    private Map<String, String> convertToStringMap(Map<String, Object> input) {
        return input.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toString()
                ));
    }

    private ActionType mapEventToActionType(String eventType) {
        return switch (eventType) {
            case "CREATE_TEMPLATE_LOG" -> ActionType.CREATE_TEMPLATE;
            case "DELETE_TEMPLATE_LOG" -> ActionType.DELETE_TEMPLATE;
            case "SUBMIT_REPORT_LOG" -> ActionType.SUBMIT_REPORT;
            case "VALIDATE_REPORT_LOG" -> ActionType.VALIDATE_REPORT;
            case "REJECT_REPORT_LOG" -> ActionType.REJECT_REPORT;
            case "CREATE_VALIDATION_MODEL_LOG" -> ActionType.CREATE_VALIDATION_MODEL;
            case "DELETE_VALIDATION_MODEL_LOG" -> ActionType.DELETE_VALIDATION_MODEL;
            default -> throw new IllegalArgumentException("eventType non reconnu pour ActionType: " + eventType);
        };
    }
}
