package com.flowbridge.finance.api.controller;

import com.flowbridge.finance.application.port.in.BudgetUseCase;
import com.flowbridge.finance.application.port.in.CreateBudgetCommand;
import com.flowbridge.finance.domain.model.Budget;
import com.flowbridge.finance.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetUseCase budgetUseCase;

    public BudgetController(BudgetUseCase budgetUseCase) {
        this.budgetUseCase = budgetUseCase;
    }

    @PostMapping
    public ResponseEntity<Budget> createOrUpdateBudget(
            @Valid @RequestBody CreateBudgetCommand command,
            @AuthenticationPrincipal UserPrincipal principal) {
        command.setUserId(principal.id());
        Budget budget = budgetUseCase.createOrUpdateBudget(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetUseCase.getBudgetsByUserId(principal.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudget(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetUseCase.getBudgetById(id, principal.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        budgetUseCase.deleteBudget(id, principal.id());
        return ResponseEntity.noContent().build();
    }
}
