package com.flowbridge.finance.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowbridge.finance.FinanceTrackerApplication;
import com.flowbridge.finance.application.port.in.CreateBudgetCommand;
import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.LoginCommand;
import com.flowbridge.finance.application.port.in.RegisterCommand;
import com.flowbridge.finance.application.port.out.AuditEventRepository;
import com.flowbridge.finance.domain.model.AuditEvent;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = FinanceTrackerApplication.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:finance_tracker_events_it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false",
        "flowbridge.provider=local"
    }
)
@AutoConfigureMockMvc
public class FlowBridgeEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "eventuser_" + UUID.randomUUID() + "@test.com";
        RegisterCommand reg = new RegisterCommand("Event User", email, "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginCommand login = new LoginCommand(email, "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> loginResp = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        userToken = (String) loginResp.get("accessToken");
    }

    @Test
    public void testEventRoundtripAndDatabaseAuditPersistence() throws Exception {
        // Initial count of audit events
        long initialCount = auditEventRepository.count();

        // 1. Create a Budget (Limits FOOD to $100.00)
        CreateBudgetCommand budgetCmd = new CreateBudgetCommand(null, Category.FOOD, new BigDecimal("100.00"), BudgetPeriod.MONTHLY);
        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetCmd)))
                .andExpect(status().isCreated());

        // Wait a brief moment for the event loop routing
        Thread.sleep(200);

        // Verify BudgetCreated event is persisted in AuditLog
        List<AuditEvent> auditEvents1 = auditEventRepository.findAll();
        assertEquals(initialCount + 1, auditEvents1.size());
        AuditEvent budgetCreatedAudit = auditEvents1.stream()
                .filter(e -> "BudgetCreated".equals(e.getEventType()))
                .findFirst()
                .orElse(null);
        assertNotNull(budgetCreatedAudit);
        assertTrue(budgetCreatedAudit.getPayload().contains("100.00"));
        assertTrue(budgetCreatedAudit.getPayload().contains("FOOD"));

        // 2. Create a Transaction under the budget (EXPENSE FOOD of $40.00)
        CreateTransactionCommand txCmd1 = new CreateTransactionCommand(
                null, TransactionType.EXPENSE, Category.FOOD,
                new BigDecimal("40.00"), "Grocery shopping", Instant.now());
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txCmd1)))
                .andExpect(status().isCreated());

        Thread.sleep(200);

        // Verify TransactionCreated event is persisted in AuditLog
        List<AuditEvent> auditEvents2 = auditEventRepository.findAll();
        assertEquals(initialCount + 2, auditEvents2.size());
        AuditEvent txCreatedAudit = auditEvents2.stream()
                .filter(e -> "TransactionCreated".equals(e.getEventType()))
                .findFirst()
                .orElse(null);
        assertNotNull(txCreatedAudit);
        assertTrue(txCreatedAudit.getPayload().contains("40.00"));
        assertTrue(txCreatedAudit.getPayload().contains("Grocery shopping"));

        // 3. Create a Transaction that exceeds the budget (EXPENSE FOOD of $80.00, total = 120 > 100)
        CreateTransactionCommand txCmd2 = new CreateTransactionCommand(
                null, TransactionType.EXPENSE, Category.FOOD,
                new BigDecimal("80.00"), "Fancy dinner", Instant.now());
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txCmd2)))
                .andExpect(status().isUnprocessableEntity());

        Thread.sleep(200);

        // Verify BudgetExceeded event is persisted in AuditLog
        List<AuditEvent> auditEvents3 = auditEventRepository.findAll();
        assertEquals(initialCount + 3, auditEvents3.size());
        AuditEvent budgetExceededAudit = auditEvents3.stream()
                .filter(e -> "BudgetExceeded".equals(e.getEventType()))
                .findFirst()
                .orElse(null);
        assertNotNull(budgetExceededAudit);
        assertTrue(budgetExceededAudit.getPayload().contains("120.00")); // spent projected
        assertTrue(budgetExceededAudit.getPayload().contains("100.00")); // limit
    }
}
