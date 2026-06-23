package com.flowbridge.finance.events;

/**
 * Centralised topic name registry for all FlowBridge events in the finance tracker.
 * Use these constants everywhere — never hard-code topic strings.
 */
public final class FinanceTopics {

    private FinanceTopics() {}

    /** Published every time a transaction (income or expense) is created. */
    public static final String TRANSACTION_CREATED  = "finance.transaction.created";

    /** Published every time a budget limit is set or updated for a category. */
    public static final String BUDGET_CREATED       = "finance.budget.created";

    /**
     * Published when an attempted expense would push spending over the configured budget.
     * The transaction is REJECTED — consumers treat this as an alert/notification signal.
     */
    public static final String BUDGET_EXCEEDED      = "finance.budget.exceeded";
}
