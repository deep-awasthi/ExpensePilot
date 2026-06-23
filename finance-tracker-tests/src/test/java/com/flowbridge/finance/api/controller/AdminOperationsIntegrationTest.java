package com.flowbridge.finance.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowbridge.finance.FinanceTrackerApplication;
import com.flowbridge.finance.application.port.in.CreateBudgetCommand;
import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.LoginCommand;
import com.flowbridge.finance.application.port.in.RegisterCommand;
import org.flowbridge.core.application.port.out.DeadLetterStore;
import org.flowbridge.core.domain.model.DeadLetterRecord;
import com.flowbridge.finance.domain.model.BudgetPeriod;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = FinanceTrackerApplication.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:finance_tracker_admin_it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false",
        "flowbridge.provider=embedded",
        "flowbridge.embedded.data-dir=target/rocksdb-admin-it"
    }
)
@AutoConfigureMockMvc
public class AdminOperationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeadLetterStore deadLetterStore;

    @Autowired
    private com.flowbridge.finance.infrastructure.persistence.repository.JpaUserRepository jpaUserRepository;

    @Autowired
    private com.flowbridge.finance.infrastructure.persistence.repository.JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private com.flowbridge.finance.infrastructure.persistence.repository.JpaBudgetRepository jpaBudgetRepository;

    @Autowired
    private com.flowbridge.finance.infrastructure.persistence.repository.JpaAuditEventRepository jpaAuditEventRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clear database records to prevent test pollution
        jpaTransactionRepository.deleteAll();
        jpaBudgetRepository.deleteAll();
        jpaUserRepository.deleteAll();
        jpaAuditEventRepository.deleteAll();

        // Clear dead letters first to isolate test runs
        for (DeadLetterRecord record : deadLetterStore.findAll()) {
            deadLetterStore.delete(record.getEvent().getId());
        }

        // Register user 1: will have ADMIN role as count is 0
        String adminEmail = "admin_" + UUID.randomUUID() + "@test.com";
        RegisterCommand regAdmin = new RegisterCommand("Admin User", adminEmail, "adminPass123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regAdmin)))
                .andExpect(status().isOk());

        LoginCommand loginAdmin = new LoginCommand(adminEmail, "adminPass123");
        MvcResult resAdmin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> tokensAdmin = objectMapper.readValue(resAdmin.getResponse().getContentAsString(), Map.class);
        adminToken = (String) tokensAdmin.get("accessToken");

        // Register user 2: will have USER role
        String userEmail = "user_" + UUID.randomUUID() + "@test.com";
        RegisterCommand regUser = new RegisterCommand("Regular User", userEmail, "userPass123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regUser)))
                .andExpect(status().isOk());

        LoginCommand loginUser = new LoginCommand(userEmail, "userPass123");
        MvcResult resUser = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> tokensUser = objectMapper.readValue(resUser.getResponse().getContentAsString(), Map.class);
        userToken = (String) tokensUser.get("accessToken");
    }

    @Test
    void adminEndpoints_shouldEnforceRoleRestrictions() throws Exception {
        // GET /dlq -> Forbidden for USER
        mockMvc.perform(get("/api/v1/admin/dlq")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // GET /dlq -> OK for ADMIN
        mockMvc.perform(get("/api/v1/admin/dlq")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void failedEventRouting_shouldPopulateDlq_andAllowAdminActions() throws Exception {
        // 1. Create a budget for category HOUSING (which triggers permanent failures in retry loop)
        CreateBudgetCommand budgetCmd = new CreateBudgetCommand(null, Category.HOUSING, new BigDecimal("10.00"), BudgetPeriod.MONTHLY);
        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetCmd)))
                .andExpect(status().isCreated());

        // 2. Create a transaction that exceeds the budget to trigger BudgetExceededEvent
        CreateTransactionCommand txCmd = new CreateTransactionCommand(
                null, TransactionType.EXPENSE, Category.HOUSING,
                new BigDecimal("50.00"), "Monthly rent payment", Instant.now());
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txCmd)))
                .andExpect(status().isUnprocessableEntity());

        // Wait a moment for retries (3 attempts with exponential backoff) and DLQ routing
        // Attempt 1: 0ms, Attempt 2: 1s, Attempt 3: 2s. Total ~3.5s.
        // Let's sleep for 4.5 seconds to make sure it finishes.
        Thread.sleep(4500);

        // 3. Admin views DLQ
        MvcResult dlqResult = mockMvc.perform(get("/api/v1/admin/dlq")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].event.topic").value("finance.budget.exceeded"))
                .andExpect(jsonPath("$[0].errorMessage", containsString("Listener invocation failed")))
                .andReturn();

        List<?> dlqList = objectMapper.readValue(dlqResult.getResponse().getContentAsString(), List.class);
        Map<?, ?> record = (Map<?, ?>) dlqList.get(0);
        Map<?, ?> event = (Map<?, ?>) record.get("event");
        String eventId = (String) event.get("id");
        assertNotNull(eventId);

        // 4. Admin retries the failed event
        mockMvc.perform(post("/api/v1/admin/dlq/retry")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("eventId", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Wait a brief moment for the retry to fail again and create a new record in DLQ
        Thread.sleep(4500);

        // Admin views DLQ again to get the new event ID
        MvcResult dlqResult2 = mockMvc.perform(get("/api/v1/admin/dlq")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        List<?> dlqList2 = objectMapper.readValue(dlqResult2.getResponse().getContentAsString(), List.class);
        Map<?, ?> record2 = (Map<?, ?>) dlqList2.get(0);
        Map<?, ?> event2 = (Map<?, ?>) record2.get("event");
        String eventId2 = (String) event2.get("id");

        // 5. Admin deletes the failed event from DLQ
        mockMvc.perform(delete("/api/v1/admin/dlq")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("eventId", eventId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify DLQ is now empty
        mockMvc.perform(get("/api/v1/admin/dlq")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void replayEvents_shouldReturnSuccess() throws Exception {
        // Trigger event replay
        String requestBody = """
            {
                "topic": "finance.budget.created",
                "type": "ALL"
            }
            """;

        mockMvc.perform(post("/api/v1/admin/replay")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("Replay triggered successfully")));
    }
}
