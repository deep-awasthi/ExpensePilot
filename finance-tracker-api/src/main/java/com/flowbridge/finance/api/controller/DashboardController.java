package com.flowbridge.finance.api.controller;

import com.flowbridge.finance.infrastructure.persistence.repository.JpaUserRepository;
import com.flowbridge.finance.infrastructure.persistence.repository.JpaTransactionRepository;
import com.flowbridge.finance.infrastructure.persistence.repository.JpaBudgetRepository;
import com.flowbridge.finance.infrastructure.persistence.repository.JpaAuditEventRepository;
import org.flowbridge.core.application.port.in.EventBus;
import org.flowbridge.core.application.service.DefaultEventBus;
import org.flowbridge.core.application.port.out.DeadLetterStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.Set;

/**
 * Controller serving the Thymeleaf admin dashboard and login page.
 */
@Controller
public class DashboardController {

    private final JpaUserRepository userRepository;
    private final JpaTransactionRepository transactionRepository;
    private final JpaBudgetRepository budgetRepository;
    private final JpaAuditEventRepository auditEventRepository;
    private final EventBus eventBus;
    private final DeadLetterStore deadLetterStore;

    public DashboardController(JpaUserRepository userRepository,
                               JpaTransactionRepository transactionRepository,
                               JpaBudgetRepository budgetRepository,
                               JpaAuditEventRepository auditEventRepository,
                               EventBus eventBus,
                               DeadLetterStore deadLetterStore) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.auditEventRepository = auditEventRepository;
        this.eventBus = eventBus;
        this.deadLetterStore = deadLetterStore;
    }

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        // System metrics
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalTransactions", transactionRepository.count());
        model.addAttribute("totalBudgets", budgetRepository.count());
        model.addAttribute("totalAuditLogs", auditEventRepository.count());

        // FlowBridge active topics
        Set<String> topics = Collections.emptySet();
        if (eventBus instanceof DefaultEventBus) {
            topics = ((DefaultEventBus) eventBus).getSubscribedTopics();
        }
        model.addAttribute("topics", topics);

        // DLQ records
        model.addAttribute("dlqRecords", deadLetterStore.findAll());

        // Recent Audit Logs
        model.addAttribute("recentLogs", auditEventRepository.findAll());

        return "admin/dashboard";
    }
}
