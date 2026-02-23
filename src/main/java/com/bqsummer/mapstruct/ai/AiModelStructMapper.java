package com.bqsummer.mapstruct.ai;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.AiModel;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiModelStructMapper {

    @Mapping(target = "weight", ignore = true)
    @Mapping(target = "routingParams", ignore = true)
    AiModelBo toBo(AiModel aiModel);
}
