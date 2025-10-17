package com.bqsummer.service.robot;

import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.configuration.RobotTaskConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.*;

/**
 * 机器人任务调度器
 * 
 * 职责:
 * 1. 维护内存中的 DelayQueue，管理未来10分钟内的待执行任务
 * 2. 消费 DelayQueue 中到期的任务，调用 RobotTaskExecutor 执行
 * 3. 提供任务加载接口供 RobotTaskLoaderJob 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotTaskScheduler {
    
    private final RobotTaskExecutor taskExecutor;
    private final RobotTaskConfiguration config;
    
    /**
     * 内存延迟队列，存储未来10分钟内的任务
     */
    private final DelayQueue<RobotTaskWrapper> taskQueue = new DelayQueue<>();
    
    /**
     * 已加载任务的ID集合，防止重复加载
     */
    private final Set<Long> loadedTaskIds = ConcurrentHashMap.newKeySet();
    
    /**
     * 消费者线程池
     */
    private ExecutorService consumerExecutor;
    
    /**
     * 消费者运行标志
     */
    private volatile boolean running = false;
    
    /**
     * 应用启动时初始化消费线程
     */
    @PostConstruct
    public void startConsumer() {
        log.info("启动 RobotTaskScheduler 消费线程");
        
        consumerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "robot-task-consumer");
            thread.setDaemon(true);
            return thread;
        });
        
        running = true;
        consumerExecutor.submit(this::consumeTasks);
    }
    
    /**
     * 应用关闭时停止消费线程
     */
    @PreDestroy
    public void stopConsumer() {
        log.info("停止 RobotTaskScheduler 消费线程");
        running = false;
        
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
            try {
                if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("消费线程未在10秒内停止");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待消费线程停止时被中断", e);
            }
        }
    }
    
    /**
     * 消费任务线程逻辑
     */
    private void consumeTasks() {
        log.info("DelayQueue 消费线程已启动");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞等待到期任务（最多等待1秒）
                RobotTaskWrapper wrapper = taskQueue.poll(1, TimeUnit.SECONDS);
                
                if (wrapper != null) {
                    RobotTask task = wrapper.getTask();
                    log.debug("从队列中取出到期任务: taskId={}, scheduledAt={}", 
                             task.getId(), task.getScheduledAt());
                    
                    // 提交给执行器异步执行
                    taskExecutor.executeAsync(task);
                    
                    // 从已加载集合中移除
                    loadedTaskIds.remove(task.getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("消费线程被中断，准备退出");
                break;
            } catch (Exception e) {
                log.error("消费任务时发生异常", e);
                // 继续运行，不因单个任务失败而停止整个消费线程
            }
        }
        
        log.info("DelayQueue 消费线程已停止");
    }
    
    /**
     * 加载任务到内存队列
     * 
     * @param tasks 待加载的任务列表
     * @return 实际加载的任务数
     */
    public int loadTasks(java.util.List<RobotTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        
        int loaded = 0;
        for (RobotTask task : tasks) {
            // 防止重复加载
            if (loadedTaskIds.contains(task.getId())) {
                log.debug("任务已加载，跳过: taskId={}", task.getId());
                continue;
            }
            
            RobotTaskWrapper wrapper = new RobotTaskWrapper(task);
            taskQueue.offer(wrapper);
            loadedTaskIds.add(task.getId());
            loaded++;
        }
        
        if (loaded > 0) {
            log.info("加载 {} 个任务到内存队列，当前队列大小: {}", loaded, taskQueue.size());
        }
        
        return loaded;
    }
    
    /**
     * 获取当前队列大小（用于监控）
     */
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    /**
     * 获取已加载任务数（用于监控）
     */
    public int getLoadedTaskCount() {
        return loadedTaskIds.size();
    }
}
