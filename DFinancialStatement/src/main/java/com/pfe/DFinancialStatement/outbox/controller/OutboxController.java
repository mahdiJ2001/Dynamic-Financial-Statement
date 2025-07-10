package com.pfe.DFinancialStatement.outbox.controller;

import com.pfe.DFinancialStatement.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxEventService outboxEventService;

    @PostMapping("/test")
    public String testEvent(@RequestBody Map<String, Object> payload) {
        outboxEventService.saveEvent("Notification", "TEST_EVENT", payload);
        return "Event créé.";
    }
}
