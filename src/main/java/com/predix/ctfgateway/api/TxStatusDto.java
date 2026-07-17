package com.predix.ctfgateway.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TxStatusDto {
    String txHash;
    String status;
    int confirmations;
}
