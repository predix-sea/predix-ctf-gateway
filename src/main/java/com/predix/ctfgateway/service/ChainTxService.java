package com.predix.ctfgateway.service;

import com.predix.ctfgateway.api.CtfSubmitResult;
import com.predix.ctfgateway.api.TxStatusDto;

import java.util.Map;

public interface ChainTxService {

    CtfSubmitResult submitTrade(Map<String, Object> payload);

    CtfSubmitResult cancelOrder(Map<String, Object> payload);

    TxStatusDto queryTxStatus(String txHash);
}
