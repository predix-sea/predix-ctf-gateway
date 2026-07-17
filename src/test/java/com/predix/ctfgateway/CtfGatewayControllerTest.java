package com.predix.ctfgateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CtfGatewayControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void submitTrade_isIdempotentByTradeId() throws Exception {
        String body = """
                {"tradeId":"t-1","tradeCode":"TR1","price":"0.55","quantity":"10","marketId":"m1","outcomeId":"yes"}
                """;

        String firstJson = mockMvc.perform(post("/api/v1/ctf/submit-trade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OK")))
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.data.txHash", startsWith("0x")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstHash = objectMapper.readTree(firstJson).path("data").path("txHash").asText();

        String secondJson = mockMvc.perform(post("/api/v1/ctf/submit-trade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondHash = objectMapper.readTree(secondJson).path("data").path("txHash").asText();
        assertEquals(firstHash, secondHash);
    }

    @Test
    void cancelAndQueryTx() throws Exception {
        String cancelBody = "{\"orderId\":\"ord-9\"}";
        String resp = mockMvc.perform(post("/api/v1/ctf/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(resp);
        String txHash = root.path("data").path("txHash").asText();

        mockMvc.perform(get("/api/v1/ctf/tx/" + txHash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }
}
