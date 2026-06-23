package com.flowbridge.finance.events.config;

import org.flowbridge.core.application.port.out.DeadLetterStore;
import org.flowbridge.core.application.port.out.ReplayableStore;
import org.flowbridge.core.domain.model.DeadLetterRecord;
import org.flowbridge.core.domain.model.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Provides in-memory implementations of DeadLetterStore and ReplayableStore
 * when running under the "local" provider, ensuring that the Admin APIs do not
 * throw startup exceptions and can be simulated or verified gracefully.
 */
@Configuration
public class FlowBridgeLocalFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(DeadLetterStore.class)
    public DeadLetterStore inMemoryDeadLetterStore() {
        return new DeadLetterStore() {
            private final List<DeadLetterRecord> records = new CopyOnWriteArrayList<>();

            @Override
            public void saveDeadLetter(Event event, Throwable throwable) {
                records.add(new DeadLetterRecord(
                        event,
                        throwable.getMessage() != null ? throwable.getMessage() : throwable.toString(),
                        Arrays.toString(throwable.getStackTrace()),
                        System.currentTimeMillis()
                ));
            }

            @Override
            public List<DeadLetterRecord> findAll() {
                return new ArrayList<>(records);
            }

            @Override
            public void delete(String id) {
                records.removeIf(r -> r.getEvent().getId().equals(id));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ReplayableStore.class)
    public ReplayableStore inMemoryReplayableStore() {
        return new ReplayableStore() {
            private final Map<String, List<Event>> store = new ConcurrentHashMap<>();

            @Override
            public void save(Event event) {
                store.computeIfAbsent(event.getTopic(), k -> new CopyOnWriteArrayList<>()).add(event);
            }

            @Override
            public List<Event> findByTopic(String topic) {
                return new ArrayList<>(store.getOrDefault(topic, Collections.emptyList()));
            }

            @Override
            public List<Event> findByTopicFromTimestamp(String topic, Instant timestamp) {
                List<Event> events = store.getOrDefault(topic, Collections.emptyList());
                List<Event> result = new ArrayList<>();
                for (Event e : events) {
                    if (e.getTimestamp() >= timestamp.toEpochMilli()) {
                        result.add(e);
                    }
                }
                return result;
            }

            @Override
            public List<Event> findByTopicFromOffset(String topic, long offset) {
                List<Event> events = store.getOrDefault(topic, Collections.emptyList());
                List<Event> result = new ArrayList<>();
                for (Event e : events) {
                    if (e.getOffset() >= offset) {
                        result.add(e);
                    }
                }
                return result;
            }
        };
    }
}
