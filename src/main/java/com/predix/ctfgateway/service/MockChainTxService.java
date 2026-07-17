package com.predix.ctfgateway.service;

import com.predix.ctfgateway.api.CtfSubmitResult;
import com.predix.ctfgateway.api.TxStatusDto;
import com.predix.ctfgateway.config.CtfGatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dev/test chain stub: deterministic tx hashes keyed by tradeId (idempotent).
 * Replace with {@link Web3jChainTxService} when RPC + operator key are configured.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "predix.ctf.mock-chain", havingValue = "true", matchIfMissing = true)
public class MockChainTxService implements ChainTxService {

    private final CtfGatewayProperties properties;
    private final Map<String, TxStatusDto> txStore = new ConcurrentHashMap<>();
    private final Map<String, String> idempotency = new ConcurrentHashMap<>();

    @Override
    public CtfSubmitResult submitTrade(Map<String, Object> payload) {
        String tradeId = String.valueOf(payload.getOrDefault("tradeId", "unknown"));
        String existing = idempotency.get(tradeId);
        if (existing != null) {
            return CtfSubmitResult.builder().txHash(existing).status("SUBMITTED").build();
        }
        String txHash = "0x" + sha256Hex("submit:" + tradeId + ":" + properties.getCtfOpsAddress()).substring(0, 64);
        idempotency.put(tradeId, txHash);
        txStore.put(txHash, TxStatusDto.builder().txHash(txHash).status("CONFIRMED").confirmations(1).build());
        return CtfSubmitResult.builder().txHash(txHash).status("SUBMITTED").build();
    }

    @Override
    public CtfSubmitResult cancelOrder(Map<String, Object> payload) {
        String orderId = String.valueOf(payload.getOrDefault("orderId", "unknown"));
        String txHash = "0x" + sha256Hex("cancel:" + orderId).substring(0, 64);
        txStore.put(txHash, TxStatusDto.builder().txHash(txHash).status("CONFIRMED").confirmations(1).build());
        return CtfSubmitResult.builder().txHash(txHash).status("SUBMITTED").build();
    }

    @Override
    public TxStatusDto queryTxStatus(String txHash) {
        return txStore.getOrDefault(txHash,
                TxStatusDto.builder().txHash(txHash).status("UNKNOWN").confirmations(0).build());
    }

    private static String sha256Hex(String input) {
        try {
            byte[] dig = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
