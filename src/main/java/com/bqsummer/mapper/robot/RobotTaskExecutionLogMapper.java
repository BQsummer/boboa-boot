package com.bqsummer.mapper.robot;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.robot.RobotTaskExecutionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机器人任务执行日志 Mapper 接口
 */
@Mapper
public interface RobotTaskExecutionLogMapper extends BaseMapper<RobotTaskExecutionLog> {
}
