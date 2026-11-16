package com.mypolicy.policy;

/**
 * Whether the official notice still recruits. ACTIVE notices are open or closed by their dates;
 * CLOSED is the source saying recruitment ended without dates (it may reopen); CANCELLED is final.
 */
public enum PolicyStatus {
    ACTIVE,
    CLOSED,
    CANCELLED
}
