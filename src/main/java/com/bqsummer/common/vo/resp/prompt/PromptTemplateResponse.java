package com.bqsummer.common.vo.resp.prompt;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Prompt 妯℃澘鍝嶅簲 VO
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
public class PromptTemplateResponse {

    /**
     * 妯℃澘ID
     */
    private Long id;

    /**
     * 瑙掕壊ID
     */
    private Long charId;

    /**
     * 妯℃澘鎻忚堪
     */
    private String description;

    /**
     * 閫傜敤妯″瀷浠ｇ爜
     */
    private String modelCode;

    /**
     * 妯℃澘璇█
     */
    private String lang;

    /**
     * 妯℃澘鍐呭
     */
    private String content;

    /**
     * 妯℃澘鍙傛暟缁撴瀯璇存槑
     */
    private Map<String, Object> paramSchema;

    /**
     * 鐗堟湰鍙?
     */
    private Integer version;

    /**
     * 鏄惁鏈€鏂扮増鏈?
     */
    private Boolean isLatest;

    /**
     * 鏄惁绋冲畾鐗堟湰
     */

    /**
     * 鐘舵€?
     */
    private Integer status;

    /**
     * 鐏板害绛栫暐
     */
    private Integer grayStrategy;

    /**
     * 鐏板害姣斾緥
     */
    private Integer grayRatio;

    /**
     * 鐏板害鐢ㄦ埛鐧藉悕鍗?
     */
    private List<Long> grayUserList;

    /**
     * 妯℃澘浼樺厛绾?
     */
    private Integer priority;

    /**
     * 鎵╁睍鍖归厤鏉′欢
     */
    private Map<String, Object> tags;

    private List<Long> kbEntryIds;

    /**
     * 鍚庡鐞嗛厤缃?
     */
    private Long postProcessPipelineId;

    private Map<String, Object> postProcessConfig;

    /**
     * 鍒涘缓浜?
     */
    private String createdBy;

    /**
     * 鏇存柊浜?
     */
    private String updatedBy;

    /**
     * 鍒涘缓鏃堕棿
     */
    private LocalDateTime createdAt;

    /**
     * 鏇存柊鏃堕棿
     */
    private LocalDateTime updatedAt;
}



