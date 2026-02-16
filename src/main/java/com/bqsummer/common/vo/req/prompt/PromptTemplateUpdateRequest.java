package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Prompt 妯℃澘鏇存柊璇锋眰 VO
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
public class PromptTemplateUpdateRequest {

    /**
     * 妯℃澘鎻忚堪
     */
    @Size(max = 255, message = "鎻忚堪闀垮害涓嶈兘瓒呰繃255瀛楃")
    private String description;

    /**
     * 閫傜敤妯″瀷浠ｇ爜
     */
    @Size(max = 64, message = "妯″瀷浠ｇ爜闀垮害涓嶈兘瓒呰繃64瀛楃")
    private String modelCode;

    /**
     * 妯℃澘璇█
     */
    @Size(max = 16, message = "璇█浠ｇ爜闀垮害涓嶈兘瓒呰繃16瀛楃")
    private String lang;

    /**
     * 妯℃澘鍐呭锛圔eetl 妯℃澘璇硶锛?
     */
    private String content;

    /**
     * 妯℃澘鍙傛暟缁撴瀯璇存槑锛圝SON Schema锛?
     */
    private Map<String, Object> paramSchema;

    /**
     * 鐘舵€侊細0=鑽夌锛?=鍚敤锛?=鍋滅敤
     */
    @Min(value = 0, message = "鐘舵€佸€间笉鍚堟硶")
    @Max(value = 2, message = "鐘舵€佸€间笉鍚堟硶")
    private Integer status;

    /**
     * 鏄惁绋冲畾鐗堟湰
     */

    /**
     * 鐏板害绛栫暐锛?=鏃犵伆搴︼紝1=鎸夋瘮渚嬶紝2=鎸夌敤鎴风櫧鍚嶅崟
     */
    @Min(value = 0, message = "鐏板害绛栫暐鍊间笉鍚堟硶")
    @Max(value = 2, message = "鐏板害绛栫暐鍊间笉鍚堟硶")
    private Integer grayStrategy;

    /**
     * 鐏板害姣斾緥锛?-100
     */
    @Min(value = 0, message = "鐏板害姣斾緥蹇呴』鍦?-100涔嬮棿")
    @Max(value = 100, message = "鐏板害姣斾緥蹇呴』鍦?-100涔嬮棿")
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

    /**
     * 鍚庡鐞嗛厤缃紙JSON锛夛紝鏀寔杩囨护鏍囩銆佹鍒欐浛鎹㈢瓑瑙勫垯
     */
    private Long postProcessPipelineId;

    private Map<String, Object> postProcessConfig;
}



