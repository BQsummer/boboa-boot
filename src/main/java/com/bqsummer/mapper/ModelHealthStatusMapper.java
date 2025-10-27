package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.ai.ModelHealthStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型健康状态 Mapper 接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Mapper
public interface ModelHealthStatusMapper extends BaseMapper<ModelHealthStatus> {
}
