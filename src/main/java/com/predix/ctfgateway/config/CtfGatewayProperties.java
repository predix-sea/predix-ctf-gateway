package com.predix.ctfgateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "predix.ctf")
public class CtfGatewayProperties {
    /** When true, do not broadcast; return deterministic mock tx hashes (local/dev). */
    private boolean mockChain = true;
    private String rpcUrl = "http://127.0.0.1:8545";
    private long chainId = 80002L;
    private String operatorPrivateKey = "";
    private String ctfOpsAddress = "";
    private String ctfAddress = "";
    private String collateralToken = "";
}
