package com.bqsummer.model.dto;

import com.bqsummer.model.entity.ModelType;
import lombok.Data;

/**
 * 模型查询请求 DTO
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class ModelQueryRequest {
    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
    
    /**
     * 过滤：提供商
     */
    private String provider;
    
    /**
     * 过滤：模型类型
     */
    private ModelType modelType;
    
    /**
     * 过滤：启用状态
     */
    private Boolean enabled;
}
