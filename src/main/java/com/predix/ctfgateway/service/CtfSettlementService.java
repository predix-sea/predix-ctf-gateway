package com.predix.ctfgateway.service;

import com.predix.ctfgateway.api.CtfSubmitResult;
import com.predix.ctfgateway.api.TxStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CtfSettlementService {

    private final ChainTxService chainTxService;

    public CtfSubmitResult submitTrade(Map<String, Object> payload) {
        return chainTxService.submitTrade(payload);
    }

    public CtfSubmitResult cancel(Map<String, Object> payload) {
        return chainTxService.cancelOrder(payload);
    }

    public TxStatusDto txStatus(String txHash) {
        return chainTxService.queryTxStatus(txHash);
    }
}
