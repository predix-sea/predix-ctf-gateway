package com.predix.ctfgateway.api;

import com.predix.ctfgateway.service.CtfSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ctf")
@RequiredArgsConstructor
public class CtfGatewayController {

    private final CtfSettlementService settlementService;

    @PostMapping("/submit-trade")
    public ApiResponse<CtfSubmitResult> submitTrade(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(settlementService.submitTrade(payload));
    }

    @PostMapping("/cancel")
    public ApiResponse<CtfSubmitResult> cancel(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(settlementService.cancel(payload));
    }

    @GetMapping("/tx/{txHash}")
    public ApiResponse<TxStatusDto> txStatus(@PathVariable String txHash) {
        return ApiResponse.ok(settlementService.txStatus(txHash));
    }
}
