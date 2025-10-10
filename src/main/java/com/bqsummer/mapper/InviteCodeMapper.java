package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.invite.InviteCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InviteCodeMapper extends BaseMapper<InviteCode> {

    @Select("SELECT id, code, code_hash AS codeHash, creator_user_id AS creatorUserId, max_uses AS maxUses, used_count AS usedCount, status, expire_at AS expireAt, remark, created_time AS createdTime, updated_time AS updatedTime FROM invite_codes WHERE code = #{code} LIMIT 1")
    InviteCode findByCode(@Param("code") String code);

    @Update("UPDATE invite_codes SET used_count = used_count + 1, status = CASE WHEN used_count + 1 >= max_uses THEN 'USED' ELSE status END WHERE id = #{id} AND status = 'ACTIVE' AND (expire_at IS NULL OR expire_at > NOW()) AND used_count < max_uses")
    int tryConsumeOnce(@Param("id") Long id);

    @Update("UPDATE invite_codes SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT COUNT(1) FROM invite_codes WHERE creator_user_id = #{userId}")
    int countByCreator(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(used_count),0) FROM invite_codes WHERE creator_user_id = #{userId}")
    Integer sumUsedByCreator(@Param("userId") Long userId);
}
