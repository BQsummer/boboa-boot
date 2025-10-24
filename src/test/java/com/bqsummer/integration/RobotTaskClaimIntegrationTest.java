package com.bqsummer.integration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.mapper.robot.RobotTaskMapper;
import com.bqsummer.service.robot.RobotTaskExecutor;
import com.bqsummer.util.InstanceIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务声明式领取机制集成测试
 * <p>
 * 端到端测试任务抢占、所有权验证、失败重试等场景（用户故事1和用户故事3）
 * </p>
 */
@SpringBootTest
@DisplayName("任务声明式领取机制集成测试")
class RobotTaskClaimIntegrationTest extends BaseTest {

    @Autowired
    private RobotTaskMapper robotTaskMapper;

    @Autowired
    private RobotTaskExecutor robotTaskExecutor;

    /**
     * 创建测试任务
     */
    private RobotTask createTestTask() {
        RobotTask task = RobotTask.builder()
                .userId(1001L)
                .robotId(2001L)
                .taskType("IMMEDIATE")
                .actionType("SEND_MESSAGE")
                .actionPayload("{\"messageId\":123,\"content\":\"test\"}")
                .scheduledAt(LocalDateTime.now().minusSeconds(1))
                .status(TaskStatus.PENDING.name())
                .retryCount(0)
                .maxRetryCount(3)
                .build();
        
        robotTaskMapper.insert(task);
        return task;
    }

    @Test
    @Transactional
    @DisplayName("测试1：多实例并发领取 - 只有一个实例成功")
    void testConcurrentClaimOnlyOneSucceeds() throws InterruptedException {
        // Given: 创建一个PENDING任务
        RobotTask task = createTestTask();
        Long taskId = task.getId();
        
        // When: 模拟2个实例同时尝试领取
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int instanceIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待统一启动信号
                    
                    // 重新查询任务（模拟不同实例获取到同一任务）
                    RobotTask freshTask = robotTaskMapper.selectById(taskId);
                    
                    // 尝试领取任务
                    boolean acquired = robotTaskExecutor.tryAcquireTask(freshTask);
                    if (acquired) {
                        successCount.incrementAndGet();
                        System.out.println("实例" + instanceIndex + " 领取成功");
                    } else {
                        System.out.println("实例" + instanceIndex + " 领取失败");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // 统一启动所有线程
        startLatch.countDown();
        
        // 等待所有线程完成
        doneLatch.await();
        
        // Then: 验证只有1个实例成功领取
        assertEquals(1, successCount.get(), "只有一个实例能成功领取任务");
        
        // 验证数据库状态
        RobotTask updatedTask = robotTaskMapper.selectById(taskId);
        assertEquals(TaskStatus.RUNNING.name(), updatedTask.getStatus(), "任务状态应为RUNNING");
        assertNotNull(updatedTask.getLockedBy(), "locked_by应该被设置");
        assertNotNull(updatedTask.getStartedAt(), "started_at应该被设置");
    }

    @Test
    @Transactional
    @DisplayName("测试2：心跳更新不影响任务完成")
    void testHeartbeatUpdateDoesNotAffectTaskCompletion() {
        // Given: 创建并领取任务
        RobotTask task = createTestTask();
        boolean acquired = robotTaskExecutor.tryAcquireTask(task);
        assertTrue(acquired, "任务应该被成功领取");
        
        String originalLockedBy = task.getLockedBy();
        
        // When: 模拟心跳更新（并发写操作）
        UpdateWrapper<RobotTask> heartbeatUpdate = new UpdateWrapper<>();
        heartbeatUpdate.eq("id", task.getId())
                      .set("heartbeat_at", LocalDateTime.now());
        int heartbeatUpdated = robotTaskMapper.update(null, heartbeatUpdate);
        assertEquals(1, heartbeatUpdated, "心跳更新应该成功");
        
        // Then: 任务仍然可以被原实例完成
        LocalDateTime completedTime = LocalDateTime.now();
        robotTaskExecutor.updateTaskStatusToDone(task, completedTime);
        
        // 验证数据库状态
        RobotTask updatedTask = robotTaskMapper.selectById(task.getId());
        assertEquals(TaskStatus.DONE.name(), updatedTask.getStatus(), 
                "任务状态应为DONE（心跳更新不影响完成）");
        assertEquals(originalLockedBy, updatedTask.getLockedBy(), 
                "locked_by应保持不变");
        assertNotNull(updatedTask.getCompletedAt(), "completed_at应该被设置");
    }

    @Test
    @Transactional
    @DisplayName("测试3：非所有者更新任务失败")
    void testNonOwnerCannotUpdateTask() {
        // Given: 实例A领取任务
        RobotTask task = createTestTask();
        boolean acquired = robotTaskExecutor.tryAcquireTask(task);
        assertTrue(acquired, "任务应该被成功领取");
        
        String instanceA = task.getLockedBy();
        assertNotNull(instanceA, "实例A应该持有任务");
        
        // When: 模拟实例B尝试更新任务状态（手动修改locked_by模拟不同实例）
        String instanceB = "different-instance:9999";
        
        // 先验证locked_by确实不同
        assertNotEquals(instanceA, instanceB, "确保测试使用不同的实例ID");
        
        // 尝试用实例B的ID更新任务（模拟跨实例误操作）
        UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", task.getId())
                    .eq("locked_by", instanceB)  // 使用错误的实例ID
                    .set("status", TaskStatus.DONE.name())
                    .set("completed_at", LocalDateTime.now());
        
        int updated = robotTaskMapper.update(null, updateWrapper);
        
        // Then: 更新应该失败（affected rows = 0）
        assertEquals(0, updated, "非所有者不能更新任务状态");
        
        // 验证任务状态未变
        RobotTask unchangedTask = robotTaskMapper.selectById(task.getId());
        assertEquals(TaskStatus.RUNNING.name(), unchangedTask.getStatus(), 
                "任务状态应保持RUNNING（非所有者无法更新）");
        assertEquals(instanceA, unchangedTask.getLockedBy(), 
                "locked_by应保持原值");
    }

    @Test
    @Transactional
    @DisplayName("测试4：任务失败重试时清空locked_by")
    void testTaskFailureRetryReleasesOwnership() {
        // Given: 创建并领取任务
        RobotTask task = createTestTask();
        boolean acquired = robotTaskExecutor.tryAcquireTask(task);
        assertTrue(acquired, "任务应该被成功领取");
        
        String originalLockedBy = task.getLockedBy();
        assertNotNull(originalLockedBy, "任务应该有所有者");
        
        // When: 模拟任务执行失败，触发重试逻辑
        String errorMessage = "模拟LLM调用失败";
        robotTaskExecutor.handleTaskFailure(task, errorMessage);
        
        // Then: 验证locked_by被清空，状态变为PENDING
        RobotTask retriedTask = robotTaskMapper.selectById(task.getId());
        assertEquals(TaskStatus.PENDING.name(), retriedTask.getStatus(), 
                "任务状态应变回PENDING（允许重试）");
        assertNull(retriedTask.getLockedBy(), 
                "locked_by应该被清空（释放所有权）");
        assertEquals(1, retriedTask.getRetryCount(), 
                "retry_count应该增加到1");
        assertNotNull(retriedTask.getScheduledAt(), 
                "scheduled_at应该被设置为未来时间");
        assertEquals(errorMessage, retriedTask.getErrorMessage(), 
                "error_message应该记录失败原因");
        
        // 验证任务可以被其他实例重新领取
        RobotTask freshTask = robotTaskMapper.selectById(task.getId());
        boolean reacquired = robotTaskExecutor.tryAcquireTask(freshTask);
        assertTrue(reacquired, "任务应该可以被重新领取（locked_by已清空）");
    }

    @Test
    @Transactional
    @DisplayName("测试5：达到最大重试次数后任务标记为FAILED并清空locked_by")
    void testTaskFailedAfterMaxRetries() {
        // Given: 创建一个已达到最大重试次数的任务
        RobotTask task = createTestTask();
        task.setRetryCount(3); // 已重试3次
        task.setMaxRetryCount(3); // 最大重试3次
        robotTaskMapper.updateById(task);
        
        // 领取任务
        boolean acquired = robotTaskExecutor.tryAcquireTask(task);
        assertTrue(acquired, "任务应该被成功领取");
        
        String originalLockedBy = task.getLockedBy();
        assertNotNull(originalLockedBy, "任务应该有所有者");
        
        // When: 模拟任务执行失败（已达到最大重试次数）
        String errorMessage = "LLM服务持续不可用";
        robotTaskExecutor.handleTaskFailure(task, errorMessage);
        
        // Then: 验证任务被标记为FAILED，locked_by被清空
        RobotTask failedTask = robotTaskMapper.selectById(task.getId());
        assertEquals(TaskStatus.FAILED.name(), failedTask.getStatus(), 
                "任务状态应为FAILED（超过最大重试次数）");
        assertNull(failedTask.getLockedBy(), 
                "locked_by应该被清空");
        assertEquals(4, failedTask.getRetryCount(), 
                "retry_count应该为4（3次重试+1次初始尝试）");
        assertNotNull(failedTask.getCompletedAt(), 
                "completed_at应该被设置");
        assertEquals(errorMessage, failedTask.getErrorMessage(), 
                "error_message应该记录失败原因");
    }

    @Test
    @Transactional
    @DisplayName("测试6：InstanceIdGenerator生成的ID格式正确")
    void testInstanceIdGeneratorFormat() {
        // When: 生成实例ID
        String instanceId = InstanceIdGenerator.getInstanceId();
        
        // Then: 验证格式
        assertNotNull(instanceId, "实例ID不应为null");
        assertFalse(instanceId.isEmpty(), "实例ID不应为空字符串");
        
        // 验证格式为 hostname:pid 或 uuid:xxx
        assertTrue(instanceId.contains(":"), 
                "实例ID应包含冒号分隔符");
        
        String[] parts = instanceId.split(":");
        assertEquals(2, parts.length, 
                "实例ID应该是两部分（hostname:pid 或 uuid:xxx）");
        
        // 验证多次调用返回相同值（缓存机制）
        String instanceId2 = InstanceIdGenerator.getInstanceId();
        assertEquals(instanceId, instanceId2, 
                "多次调用应该返回相同的实例ID（缓存）");
    }

    @Test
    @Transactional
    @DisplayName("测试7：验证任务领取时设置的字段完整性")
    void testTaskClaimSetsAllRequiredFields() {
        // Given: 创建PENDING任务
        RobotTask task = createTestTask();
        Long taskId = task.getId();
        
        // 记录领取前的状态
        RobotTask beforeClaim = robotTaskMapper.selectById(taskId);
        assertEquals(TaskStatus.PENDING.name(), beforeClaim.getStatus());
        assertNull(beforeClaim.getLockedBy());
        assertNull(beforeClaim.getStartedAt());
        
        // When: 领取任务
        boolean acquired = robotTaskExecutor.tryAcquireTask(task);
        
        // Then: 验证所有必要字段都被正确设置
        assertTrue(acquired, "任务应该被成功领取");
        
        RobotTask afterClaim = robotTaskMapper.selectById(taskId);
        assertEquals(TaskStatus.RUNNING.name(), afterClaim.getStatus(), 
                "status应该变为RUNNING");
        assertNotNull(afterClaim.getLockedBy(), 
                "locked_by应该被设置");
        assertNotNull(afterClaim.getStartedAt(), 
                "started_at应该被设置");
        assertNotNull(afterClaim.getHeartbeatAt(), 
                "heartbeat_at应该被设置");
        
        // 验证本地task对象也被正确更新
        assertEquals(TaskStatus.RUNNING.name(), task.getStatus());
        assertEquals(afterClaim.getLockedBy(), task.getLockedBy());
        assertEquals(afterClaim.getStartedAt(), task.getStartedAt());
    }
}
