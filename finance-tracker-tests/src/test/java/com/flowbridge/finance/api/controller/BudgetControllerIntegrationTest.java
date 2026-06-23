package com.flowbridge.finance.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowbridge.finance.FinanceTrackerApplication;
import com.flowbridge.finance.application.port.in.CreateBudgetCommand;
import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.LoginCommand;
import com.flowbridge.finance.application.port.in.RegisterCommand;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = FinanceTrackerApplication.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:finance_tracker_budgets;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
    }
)
@AutoConfigureMockMvc
class BudgetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "budgetuser_" + UUID.randomUUID() + "@test.com";
        RegisterCommand reg = new RegisterCommand("Budget User", email, "password123");
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
    void createBudget_shouldReturn201WithBudgetBody() throws Exception {
        CreateBudgetCommand cmd = new CreateBudgetCommand(null, Category.FOOD, new BigDecimal("500.00"), BudgetPeriod.MONTHLY);

        mockMvc.perform(post("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.category", is("FOOD")))
                .andExpect(jsonPath("$.limitAmount", is(500.00)))
                .andExpect(jsonPath("$.period", is("MONTHLY")));
    }

    @Test
    void createBudget_updatesExisting_whenSameCategory() throws Exception {
        // Create initial
        CreateBudgetCommand cmd1 = new CreateBudgetCommand(null, Category.TRANSPORTATION, new BigDecimal("200.00"), BudgetPeriod.MONTHLY);
        mockMvc.perform(post("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd1)))
                .andExpect(status().isCreated());

        // Upsert with new limit
        CreateBudgetCommand cmd2 = new CreateBudgetCommand(null, Category.TRANSPORTATION, new BigDecimal("400.00"), BudgetPeriod.YEARLY);
        mockMvc.perform(post("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitAmount", is(400.00)))
                .andExpect(jsonPath("$.period", is("YEARLY")));
    }

    @Test
    void getBudgets_shouldReturn200WithList() throws Exception {
        // Create a budget first
        CreateBudgetCommand cmd = new CreateBudgetCommand(null, Category.OTHER, new BigDecimal("300.00"), BudgetPeriod.MONTHLY);
        mockMvc.perform(post("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createTransaction_shouldReturn422_whenBudgetExceeded() throws Exception {
        // Set a very low budget for ENTERTAINMENT
        CreateBudgetCommand budgetCmd = new CreateBudgetCommand(null, Category.ENTERTAINMENT, new BigDecimal("10.00"), BudgetPeriod.MONTHLY);
        mockMvc.perform(post("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(budgetCmd)))
                .andExpect(status().isCreated());

        // Try creating a transaction that exceeds it
        CreateTransactionCommand txCmd = new CreateTransactionCommand(
                null, TransactionType.EXPENSE, Category.ENTERTAINMENT,
                new BigDecimal("500.00"), "Night out", Instant.now());

        mockMvc.perform(post("/api/v1/transactions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txCmd)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", is("Budget Exceeded")))
                .andExpect(jsonPath("$.category", is("ENTERTAINMENT")));
    }

    @Test
    void deleteBudget_shouldReturn204() throws Exception {
        CreateBudgetCommand cmd = new CreateBudgetCommand(null, Category.UTILITIES, new BigDecimal("150.00"), BudgetPeriod.MONTHLY);
        MvcResult createResult = mockMvc.perform(post("/api/v1/budgets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        String id = (String) created.get("id");

        mockMvc.perform(delete("/api/v1/budgets/" + id)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBudgets_shouldReturn403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isForbidden());
    }
}
