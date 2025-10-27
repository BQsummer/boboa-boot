package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.ai.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型 Mapper 接口
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
}
