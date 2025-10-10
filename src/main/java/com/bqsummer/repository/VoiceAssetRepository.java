package com.bqsummer.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.im.VoiceAsset;
import com.bqsummer.mapper.VoiceAssetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VoiceAssetRepository {

    @Autowired
    private VoiceAssetMapper mapper;

    public VoiceAsset save(VoiceAsset v) {
        mapper.insert(v);
        return v;
    }

    public VoiceAsset findById(Long id) {
        return mapper.selectById(id);
    }

    public List<VoiceAsset> listByUser(Long userId, int limit, Long beforeId) {
        QueryWrapper<VoiceAsset> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (beforeId != null && beforeId > 0) {
            qw.lt("id", beforeId);
        }
        qw.orderByDesc("id").last("LIMIT " + Math.max(1, Math.min(100, limit)));
        return mapper.selectList(qw);
    }
}

