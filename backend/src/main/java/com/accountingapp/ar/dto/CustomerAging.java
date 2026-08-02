package com.accountingapp.ar.dto;

import com.accountingapp.common.dto.AgingBucket;
import java.util.UUID;

public record CustomerAging(UUID customerId, String customerName, AgingBucket bucket) {
}
