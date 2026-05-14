package com.portfolio.backtester.domain;

import java.time.LocalDate;

public enum RebalancePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    public boolean shouldRebalance(LocalDate prev, LocalDate curr) {
        return switch (this) {
            case DAILY -> true;
            case WEEKLY -> prev.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
                    != curr.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            case MONTHLY -> prev.getMonthValue() != curr.getMonthValue()
                    || prev.getYear() != curr.getYear();
            case QUARTERLY -> ((prev.getMonthValue() - 1) / 3)
                    != ((curr.getMonthValue() - 1) / 3)
                    || prev.getYear() != curr.getYear();
            case YEARLY -> prev.getYear() != curr.getYear();
        };
    }
}
