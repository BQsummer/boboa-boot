package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.ai.ModelRequestLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型请求日志 Mapper 接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Mapper
public interface ModelRequestLogMapper extends BaseMapper<ModelRequestLog> {
}
