package com.bqsummer.mapstruct.ai;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.AiModel;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiModelStructMapper {

    AiModelBo toBo(AiModel aiModel);
}
