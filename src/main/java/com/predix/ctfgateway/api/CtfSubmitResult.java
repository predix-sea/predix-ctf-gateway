package com.predix.ctfgateway.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CtfSubmitResult {
    String txHash;
    String status;
}
