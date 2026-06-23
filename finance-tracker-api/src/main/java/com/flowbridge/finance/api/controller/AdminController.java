package com.flowbridge.finance.api.controller;

import com.flowbridge.finance.domain.exception.ResourceNotFoundException;
import org.flowbridge.core.application.port.in.EventBus;
import org.flowbridge.core.application.port.out.DeadLetterStore;
import org.flowbridge.core.domain.model.DeadLetterRecord;
import org.flowbridge.core.domain.model.Event;
import org.flowbridge.core.domain.model.ReplayOptions;
import org.flowbridge.core.domain.model.ReplayOptions.ReplayType;
import org.flowbridge.core.infrastructure.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing administrative tasks for FlowBridge like Replaying events,
 * inspecting and managing the Dead Letter Queue (DLQ).
 * 
 * Accessible only by users with the role ADMIN (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final EventBus eventBus;
    private final DeadLetterStore deadLetterStore;
    private final Serializer serializer;

    public AdminController(EventBus eventBus, DeadLetterStore deadLetterStore, Serializer serializer) {
        this.eventBus = eventBus;
        this.deadLetterStore = deadLetterStore;
        this.serializer = serializer;
    }

    /**
     * POST /api/v1/admin/replay
     * Triggers a manual event replay for the specified topic and options.
     */
    @PostMapping("/replay")
    public ResponseEntity<Map<String, String>> replayEvents(@RequestBody ReplayRequest request) {
        log.info("Triggering event replay for topic: {}, type: {}", request.getTopic(), request.getType());

        ReplayOptions options;
        if ("FROM_TIMESTAMP".equalsIgnoreCase(request.getType())) {
            if (request.getTimestamp() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "timestamp is required for FROM_TIMESTAMP replay type"));
            }
            options = ReplayOptions.fromTimestamp(Instant.parse(request.getTimestamp()));
        } else if ("FROM_OFFSET".equalsIgnoreCase(request.getType())) {
            if (request.getOffset() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "offset is required for FROM_OFFSET replay type"));
            }
            options = ReplayOptions.fromOffset(request.getOffset());
        } else {
            options = ReplayOptions.all();
        }

        eventBus.replay(request.getTopic(), options);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Replay triggered successfully for topic: " + request.getTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/dlq
     * Lists all failed records inside the Dead Letter Queue.
     */
    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterRecord>> getDeadLetters() {
        log.info("Fetching all Dead Letter records");
        return ResponseEntity.ok(deadLetterStore.findAll());
    }

    /**
     * POST /api/v1/admin/dlq/retry
     * Retries a failed event from the Dead Letter Queue.
     */
    @PostMapping("/dlq/retry")
    public ResponseEntity<Map<String, String>> retryEvent(@RequestParam String eventId) {
        log.info("Attempting to retry failed event ID: {}", eventId);

        DeadLetterRecord record = deadLetterStore.findAll().stream()
                .filter(r -> r.getEvent().getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Dead Letter record not found for event: " + eventId));

        Event event = record.getEvent();
        try {
            Class<?> payloadClass = Class.forName(event.getPayloadType());
            Object payload = serializer.deserialize(event.getPayload(), payloadClass);

            // Resubmit to EventBus
            eventBus.publish(event.getTopic(), payload, event.getHeaders());

            // Remove from DLQ on successful retry
            deadLetterStore.delete(eventId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Event successfully retried and published to EventBus");
            response.put("eventId", eventId);
            return ResponseEntity.ok(response);
        } catch (ClassNotFoundException e) {
            log.error("Failed to find payload class during event retry: {}", event.getPayloadType(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payload class " + event.getPayloadType() + " not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error occurred while retrying event: {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Exception during retry: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/admin/dlq
     * Deletes a failed record from the DLQ without retrying.
     */
    @DeleteMapping("/dlq")
    public ResponseEntity<Map<String, String>> deleteDeadLetter(@RequestParam String eventId) {
        log.info("Deleting dead letter record for event ID: {}", eventId);
        deadLetterStore.delete(eventId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Event successfully deleted from DLQ");
        response.put("eventId", eventId);
        return ResponseEntity.ok(response);
    }

    /**
     * DTO for event replay requests.
     */
    public static class ReplayRequest {
        private String topic;
        private String type; // ALL, FROM_TIMESTAMP, FROM_OFFSET
        private String timestamp; // Instant parsed as ISO-8601 string
        private Long offset;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Long getOffset() {
            return offset;
        }

        public void setOffset(Long offset) {
            this.offset = offset;
        }
    }
}
