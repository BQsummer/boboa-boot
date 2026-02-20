package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.prompt.KbEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KbEntryMapper extends BaseMapper<KbEntry> {
}
