package com.predix.ctfgateway.service;

import com.predix.ctfgateway.api.CtfSubmitResult;
import com.predix.ctfgateway.api.TxStatusDto;
import com.predix.ctfgateway.config.CtfGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real RPC profile: records operator readiness and queries receipts.
 * MVP settlement encodes a no-op self-tx marker when CTF ABI wiring is incomplete;
 * production should call PredixCtfOps.split via generated Web3j wrappers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "predix.ctf.mock-chain", havingValue = "false")
public class Web3jChainTxService implements ChainTxService {

    private final CtfGatewayProperties properties;
    private final Map<String, String> idempotency = new ConcurrentHashMap<>();

    private Web3j web3j;
    private Credentials credentials;

    @PostConstruct
    void init() {
        if (properties.getOperatorPrivateKey() == null || properties.getOperatorPrivateKey().isBlank()) {
            throw new IllegalStateException("OPERATOR_PRIVATE_KEY required when predix.ctf.mock-chain=false");
        }
        if (properties.getCtfOpsAddress() == null || properties.getCtfOpsAddress().isBlank()) {
            throw new IllegalStateException("CTF_OPS_ADDRESS required when predix.ctf.mock-chain=false");
        }
        web3j = Web3j.build(new HttpService(properties.getRpcUrl()));
        String key = properties.getOperatorPrivateKey().startsWith("0x")
                ? properties.getOperatorPrivateKey()
                : "0x" + properties.getOperatorPrivateKey();
        credentials = Credentials.create(key);
        log.info("Web3jChainTxService ready chainId={} ops={} operator={}",
                properties.getChainId(), properties.getCtfOpsAddress(), credentials.getAddress());
    }

    @Override
    public CtfSubmitResult submitTrade(Map<String, Object> payload) {
        String tradeId = String.valueOf(payload.getOrDefault("tradeId", "unknown"));
        String existing = idempotency.get(tradeId);
        if (existing != null) {
            return CtfSubmitResult.builder().txHash(existing).status("SUBMITTED").build();
        }
        try {
            // MVP: zero-value self transfer as on-chain receipt anchor until ABI-bound split is wired.
            RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, properties.getChainId());
            DefaultGasProvider gas = new DefaultGasProvider();
            var sent = txManager.sendTransaction(
                    gas.getGasPrice(),
                    gas.getGasLimit(),
                    credentials.getAddress(),
                    Numeric.toHexString(tradeId.getBytes()),
                    BigInteger.ZERO);
            if (sent.hasError()) {
                throw new IllegalStateException(sent.getError().getMessage());
            }
            String txHash = sent.getTransactionHash();
            idempotency.put(tradeId, txHash);
            log.info("submitTrade tradeId={} txHash={} marketId={} (ops={})",
                    tradeId, txHash, payload.get("marketId"), properties.getCtfOpsAddress());
            return CtfSubmitResult.builder().txHash(txHash).status("SUBMITTED").build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit CTF trade tx: " + e.getMessage(), e);
        }
    }

    @Override
    public CtfSubmitResult cancelOrder(Map<String, Object> payload) {
        // MVP: same pattern as submit for cancel acknowledgment on-chain.
        return submitTrade(Map.of("tradeId", "cancel:" + payload.getOrDefault("orderId", "unknown")));
    }

    @Override
    public TxStatusDto queryTxStatus(String txHash) {
        try {
            EthGetTransactionReceipt resp = web3j.ethGetTransactionReceipt(txHash).send();
            Optional<TransactionReceipt> receipt = resp.getTransactionReceipt();
            if (receipt.isEmpty()) {
                return TxStatusDto.builder().txHash(txHash).status("PENDING").confirmations(0).build();
            }
            TransactionReceipt r = receipt.get();
            boolean ok = r.isStatusOK();
            BigInteger latest = web3j.ethBlockNumber().send().getBlockNumber();
            BigInteger txBlock = Numeric.decodeQuantity(r.getBlockNumberRaw());
            int conf = latest.subtract(txBlock).intValue() + 1;
            return TxStatusDto.builder()
                    .txHash(txHash)
                    .status(ok ? "CONFIRMED" : "FAILED")
                    .confirmations(Math.max(conf, 0))
                    .build();
        } catch (Exception e) {
            log.warn("queryTxStatus failed for {}: {}", txHash, e.getMessage());
            return TxStatusDto.builder().txHash(txHash).status("UNKNOWN").confirmations(0).build();
        }
    }
}
