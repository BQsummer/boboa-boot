package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.invite.InviteUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface InviteUsageMapper extends BaseMapper<InviteUsage> {

    @Select("SELECT COUNT(1) FROM invite_usage WHERE client_ip = #{ip} AND used_at >= #{since}")
    int countByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(1) FROM invite_usage WHERE invitee_user_id = #{userId}")
    int countByInvitee(@Param("userId") Long userId);
}

