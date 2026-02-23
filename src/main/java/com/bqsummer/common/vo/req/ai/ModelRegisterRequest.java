package com.bqsummer.common.vo.req.ai;

import com.bqsummer.common.dto.ai.ModelType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 模型注册请求 DTO
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class ModelRegisterRequest {
    
    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String name;
    
    /**
     * 模型版本
     */
    @NotBlank(message = "模型版本不能为空")
    private String version;
    
    /**
     * 接口类型
     */
    @NotBlank(message = "接口类型不能为空")
    private String apiKind;
    
    /**
     * 模型类型
     */
    @NotNull(message = "模型类型不能为空")
    private ModelType modelType;
    
    /**
     * API 端点 URL
     */
    @NotBlank(message = "API端点不能为空")
    private String apiEndpoint;
    
    /**
     * API 密钥
     */
    private String apiKey;
    
    /**
     * 上下文长度
     */
    private Integer contextLength;
    
    /**
     * 参数量
     */
    private String parameterCount;
    
    /**
     * 自定义标签
     */
    private List<String> tags;
    
    /**
     * 路由权重
     */
    private Integer weight;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
}
