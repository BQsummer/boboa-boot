package com.bqsummer.model.adapter;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.service.ai.adapter.OpenAiAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenAiAdapterProviderCostTest {

    @Test
    void populateProviderAndCostPrefersOpenRouterHeaderAndReadsUsageCost() throws Exception {
        OpenAiAdapter adapter = new OpenAiAdapter();

        JSONObject responseJson = new JSONObject();
        responseJson.put("provider", "openrouter");
        JSONObject usage = new JSONObject();
        usage.put("cost", "0.000092");

        HttpHeaders headers = new HttpHeaders();
        headers.add("x-openrouter-provider", "openai");

        InferenceResponse response = new InferenceResponse();

        Method method = OpenAiAdapter.class.getDeclaredMethod(
                "populateProviderAndCost",
                InferenceResponse.class,
                JSONObject.class,
                JSONObject.class,
                HttpHeaders.class
        );
        method.setAccessible(true);
        method.invoke(adapter, response, responseJson, usage, headers);

        assertEquals("openai", response.getProvider());
        assertNotNull(response.getCost());
        assertEquals(0, new BigDecimal("0.000092").compareTo(response.getCost()));
    }
}
