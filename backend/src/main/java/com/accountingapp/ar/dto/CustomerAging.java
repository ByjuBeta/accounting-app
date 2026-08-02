package com.accountingapp.ar.dto;

import java.util.UUID;

public record CustomerAging(UUID customerId, String customerName, AgingBucket bucket) {
}
