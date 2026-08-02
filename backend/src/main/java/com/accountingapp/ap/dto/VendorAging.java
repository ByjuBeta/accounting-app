package com.accountingapp.ap.dto;

import com.accountingapp.common.dto.AgingBucket;
import java.util.UUID;

public record VendorAging(UUID vendorId, String vendorName, AgingBucket bucket) {
}
