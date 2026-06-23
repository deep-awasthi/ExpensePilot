package com.flowbridge.finance.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowbridge.finance.FinanceTrackerApplication;
import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.LoginCommand;
import com.flowbridge.finance.application.port.in.RegisterCommand;
import com.flowbridge.finance.application.port.in.UpdateTransactionCommand;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = FinanceTrackerApplication.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:finance_tracker_transactions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
    }
)
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login user 1
        String email1 = "user1_" + UUID.randomUUID() + "@test.com";
        RegisterCommand reg1 = new RegisterCommand("User One", email1, "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg1)))
                .andExpect(status().isOk());

        LoginCommand login1 = new LoginCommand(email1, "password123");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> tokens1 = objectMapper.readValue(res1.getResponse().getContentAsString(), Map.class);
        user1Token = (String) tokens1.get("accessToken");

        // Register and login user 2
        String email2 = "user2_" + UUID.randomUUID() + "@test.com";
        RegisterCommand reg2 = new RegisterCommand("User Two", email2, "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg2)))
                .andExpect(status().isOk());

        LoginCommand login2 = new LoginCommand(email2, "password123");
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> tokens2 = objectMapper.readValue(res2.getResponse().getContentAsString(), Map.class);
        user2Token = (String) tokens2.get("accessToken");
    }

    @Test
    void shouldPerformTransactionCrudOperations() throws Exception {
        // 1. Create Transaction for User 1
        CreateTransactionCommand createCmd = CreateTransactionCommand.builder()
                .type(TransactionType.EXPENSE)
                .category(Category.FOOD)
                .amount(new BigDecimal("25.75"))
                .description("Lunch")
                .transactionDate(Instant.now())
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.amount").value(25.75))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andReturn();

        Map<?, ?> createdTx = objectMapper.readValue(createRes.getResponse().getContentAsString(), Map.class);
        String txId = (String) createdTx.get("id");

        // 2. Fetch all transactions for User 1
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(txId));

        // 3. Fetch all transactions for User 2 (should be empty)
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 4. Try to fetch User 1's transaction using User 2's token (should return 401 Unauthorized since service ownership checks throws AuthenticationException)
        mockMvc.perform(get("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isUnauthorized());

        // 5. Update Transaction for User 1
        UpdateTransactionCommand updateCmd = UpdateTransactionCommand.builder()
                .type(TransactionType.EXPENSE)
                .category(Category.UTILITIES)
                .amount(new BigDecimal("30.00"))
                .description("Updated lunch cost")
                .transactionDate(Instant.now())
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("UTILITIES"))
                .andExpect(jsonPath("$.amount").value(30.00))
                .andExpect(jsonPath("$.description").value("Updated lunch cost"));

        // 6. Delete Transaction
        mockMvc.perform(delete("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // 7. Get transaction again (should return 404 Not Found)
        mockMvc.perform(get("/api/v1/transactions/" + txId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }
}
