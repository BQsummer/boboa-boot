package com.bqsummer.common.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话总结JSON结构
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryJson {
    
    /**
     * 对话主题列表
     */
    private List<Topic> topics;
    
    /**
     * 关键要点列表
     */
    private List<String> keyPoints;
    
    /**
     * 用户情绪状态
     * 可选值: calm, excited, frustrated, confused, sad
     */
    private String userEmotion;
    
    /**
     * 需要延续到下次对话的上下文
     */
    private String contextCarryOver;
    
    /**
     * 主题信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Topic {
        /**
         * 主题名称
         */
        private String name;
        
        /**
         * 主题简短描述
         */
        private String summary;
    }
}
