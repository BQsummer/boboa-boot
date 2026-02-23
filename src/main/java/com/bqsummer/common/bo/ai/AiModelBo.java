package com.bqsummer.common.bo.ai;

import com.bqsummer.common.dto.ai.ModelType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AiModelBo {

    private Long id;

    /**
     * 妯″瀷鍚嶇О锛屽 GPT-4
     */
    private String name;

    /**
     * 妯″瀷鐗堟湰锛屽 gpt-4-turbo
     */
    private String version;

    /**
     * 鎺ュ彛绫诲瀷锛歰penai/azure/qwen/gemini 绛?
     */
    private String apiKind;

    /**
     * Actual model provider persisted in ai_model.provider.
     */
    private String provider;

    /**
     * 妯″瀷绫诲瀷锛欳HAT/EMBEDDING/RERANKER
     */
    private ModelType modelType;

    /**
     * API 绔偣 URL
     */
    private String apiEndpoint;

    /**
     * API 瀵嗛挜锛圓ES-256 鍔犲瘑瀛樺偍锛?
     */
    private String apiKey;

    /**
     * 涓婁笅鏂囬暱搴︼紙token 鏁帮級锛屽 8192
     */
    private Integer contextLength;

    /**
     * 鍙傛暟閲忥紝濡?175B
     */
    private String parameterCount;

    /**
     * 鑷畾涔夋爣绛撅紝濡?["fast", "cheap"]
     */
    private List<String> tags;

    /**
     * 鏄惁鍚敤锛歵rue-鍚敤 false-绂佺敤
     */
    private Boolean enabled;

    /**
     * 鍒涘缓浜虹敤鎴稩D
     */
    private Long createdBy;

    /**
     * 鍒涘缓鏃堕棿
     */
    private LocalDateTime createdAt;

    /**
     * 鏈€鍚庢洿鏂颁汉鐢ㄦ埛ID
     */
    private Long updatedBy;

    /**
     * 鏇存柊鏃堕棿
     */
    private LocalDateTime updatedAt;

    private Integer weight;

    /**
     * 鏉ヨ嚜璺敱绛栫暐缁戝畾鐨勮繍琛屽弬鏁帮紙JSON锛夈€?
     */
    private Map<String, Object> routingParams;
}
