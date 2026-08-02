package com.accountingapp.ar.dto;

import java.math.BigDecimal;

public record AgingBucket(
        BigDecimal current,
        BigDecimal days1to30,
        BigDecimal days31to60,
        BigDecimal days61to90,
        BigDecimal over90) {

    public static AgingBucket zero() {
        return new AgingBucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public AgingBucket plus(AgingBucket other) {
        return new AgingBucket(
                current.add(other.current),
                days1to30.add(other.days1to30),
                days31to60.add(other.days31to60),
                days61to90.add(other.days61to90),
                over90.add(other.over90));
    }

    public BigDecimal total() {
        return current.add(days1to30).add(days31to60).add(days61to90).add(over90);
    }

    public static AgingBucket forDaysOverdue(long daysOverdue, BigDecimal amount) {
        if (daysOverdue <= 0) {
            return new AgingBucket(amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        } else if (daysOverdue <= 30) {
            return new AgingBucket(BigDecimal.ZERO, amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        } else if (daysOverdue <= 60) {
            return new AgingBucket(BigDecimal.ZERO, BigDecimal.ZERO, amount, BigDecimal.ZERO, BigDecimal.ZERO);
        } else if (daysOverdue <= 90) {
            return new AgingBucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, amount, BigDecimal.ZERO);
        }
        return new AgingBucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, amount);
    }
}
