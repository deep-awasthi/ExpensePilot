package com.flowbridge.finance.api.controller;

import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.TransactionUseCase;
import com.flowbridge.finance.application.port.in.UpdateTransactionCommand;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;
import com.flowbridge.finance.domain.model.TransactionType;
import com.flowbridge.finance.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    public TransactionController(TransactionUseCase transactionUseCase) {
        this.transactionUseCase = transactionUseCase;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @Valid @RequestBody CreateTransactionCommand command,
            @AuthenticationPrincipal UserPrincipal principal) {
        command.setUserId(principal.id());
        Transaction created = transactionUseCase.createTransaction(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionCommand command,
            @AuthenticationPrincipal UserPrincipal principal) {
        command.setUserId(principal.id());
        Transaction updated = transactionUseCase.updateTransaction(id, command);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        transactionUseCase.deleteTransaction(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Transaction transaction = transactionUseCase.getTransactionById(id, principal.id());
        return ResponseEntity.ok(transaction);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false) Category category,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Transaction> transactions;
        if (category != null) {
            transactions = transactionUseCase.getTransactionsByUserIdAndCategory(principal.id(), category);
        } else {
            transactions = transactionUseCase.getTransactionsByUserId(principal.id());
        }
        return ResponseEntity.ok(transactions);
    }
}
