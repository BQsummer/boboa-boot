package com.bqsummer.mapper.robot;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.robot.RobotTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机器人任务 Mapper 接口
 */
@Mapper
public interface RobotTaskMapper extends BaseMapper<RobotTask> {
}
