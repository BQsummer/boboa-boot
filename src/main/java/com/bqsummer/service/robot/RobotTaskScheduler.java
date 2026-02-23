package com.bqsummer.service.robot;

import com.bqsummer.common.dto.robot.ActionType;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.configuration.Configs;
import com.bqsummer.configuration.RobotTaskConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * 机器人任务调度器
 * 
 * 职责:
 * 1. 维护内存中的 DelayQueue，管理未来10分钟内的待执行任务
 * 2. 消费 DelayQueue 中到期的任务，调用 RobotTaskExecutor 执行
 * 3. 提供任务加载接口供 RobotTaskLoaderJob 调用
 * 4. 队列容量限制：防止内存队列无限增长导致OOM
 * 5. 按动作类型限制并发执行数：避免某类任务过度消耗资源
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotTaskScheduler {
    
    private final RobotTaskExecutor taskExecutor;
    private final RobotTaskConfiguration config;
    private final Configs configs;
    
    /**
     * 内存延迟队列，存储未来10分钟内的任务
     */
    private final DelayQueue<RobotTaskWrapper> taskQueue = new DelayQueue<>();
    
    /**
     * 已加载任务的ID集合，防止重复加载
     */
    private final Set<Long> loadedTaskIds = ConcurrentHashMap.newKeySet();

    /**
     * 因队列满而拒绝加载的任务数统计
     */
    private final AtomicLong rejectedTaskCount = new AtomicLong(0);
    
    /**
     * 动作类型 -> 并发控制信号量
     * 用于限制每种动作类型的并发执行数
     */
    private final Map<String, Semaphore> concurrencySemaphores = new ConcurrentHashMap<>();
    
    /**
     * 动作类型 -> 并发限制配置值
     * 用于存储当前的并发限制，支持动态修改和查询
     */
    private final Map<String, Integer> actionConcurrencyConfig = new ConcurrentHashMap<>();
    
    /**
     * 动作类型 -> 因并发满而延迟重试的任务数统计
     */
    private final Map<String, AtomicLong> delayedRetryCount = new ConcurrentHashMap<>();
    
    /**
     * 消费者线程池
     */
    private ExecutorService consumerExecutor;
    
    /**
     * 消费者运行标志
     */
    private volatile boolean running = false;
    
    /**
     * 应用启动时初始化
     */
    @PostConstruct
    public void startConsumer() {
        
        // 初始化并发控制信号量
        initConcurrencySemaphores();
        
        // 启动消费线程
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
     * 根据动作类型获取对应的并发配置值
     *
     * @param actionType 动作类型枚举
     * @return 该动作类型的并发上限
     */
    private int getConcurrencyConfigValue(ActionType actionType) {
        return switch (actionType) {
            case SEND_MESSAGE -> config.getConcurrencySendMessage();
            case SEND_VOICE -> config.getConcurrencySendVoice();
            case SEND_NOTIFICATION -> config.getConcurrencySendNotification();
        };
    }
    
    /**
     * 初始化并发控制信号量
     * 为每种动作类型创建对应的信号量，用于限制并发执行数
     * 动态遍历 ActionType 枚举，自动适配新增的动作类型
     */
    private void initConcurrencySemaphores() {
        // 遍历 ActionType 枚举，初始化信号量和配置映射
        int totalConcurrency = 0;

        for (ActionType actionType : ActionType.values()) {
            String actionTypeName = actionType.name();
            int concurrency = getConcurrencyConfigValue(actionType);

            concurrencySemaphores.put(actionTypeName, new Semaphore(concurrency));
            actionConcurrencyConfig.put(actionTypeName, concurrency);  // 存储配置值
            delayedRetryCount.put(actionTypeName, new AtomicLong(0));
            totalConcurrency += concurrency;
            
            log.info("初始化并发控制信号量 - {}: {}", actionTypeName, concurrency);
        }
        
        log.info("并发控制信号量初始化完成 - 总并发上限: {}", totalConcurrency);
        
        // 验证并发配置合理性
        int maxPoolSize = config.getExecutorMaxPoolSize();
        if (totalConcurrency > maxPoolSize) {
            log.warn("并发上限总和 ({}) 超过线程池容量 ({})，可能导致部分槽位无法使用",
                     totalConcurrency, maxPoolSize);
        }
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
     * 消费任务线程逻辑（带并发控制）
     */
    private void consumeTasks() {
        log.info("DelayQueue 消费线程已启动");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞等待到期任务（最多等待1秒）
                RobotTaskWrapper wrapper = taskQueue.poll(1, TimeUnit.SECONDS);
                
                if (wrapper != null) {
                    RobotTask task = wrapper.getTask();
                    String actionType = task.getActionType();
                    
                    log.debug("从队列中取出到期任务: taskId={}, actionType={}, scheduledAt={}", 
                             task.getId(), actionType, task.getScheduledAt());
                    
                    // 获取该动作类型的并发控制信号量
                    Semaphore semaphore = concurrencySemaphores.get(actionType);
                    
                    if (semaphore == null) {
                        // 未配置的动作类型，直接执行（不限制并发）
                        log.warn("未配置动作类型 {} 的并发控制，直接执行", actionType);

                        // 异步执行，并在完成后从已加载集合中移除
                        taskExecutor.executeAsync(task).whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("任务执行异常: taskId={}, actionType={}",
                                         task.getId(), actionType, ex);
                            } else {
                                log.debug("任务执行完成: taskId={}, actionType={}",
                                         task.getId(), actionType);
                            }
                            // 任务执行完成后才从已加载集合中移除，防止重复加载
                            loadedTaskIds.remove(task.getId());
                        });
                    } else {
                        // 尝试获取并发槽位（非阻塞）
                        boolean acquired = semaphore.tryAcquire();
                        
                        if (acquired) {
                            // 成功获取槽位，提交执行
                            log.debug("获取并发槽位成功: taskId={}, actionType={}, 剩余槽位={}", 
                                    task.getId(), actionType, semaphore.availablePermits());
                            
                            // 异步执行，并在完成后释放信号量
                            taskExecutor.executeAsync(task).whenComplete((result, ex) -> {
                                // 无论成功还是失败，都释放信号量
                                semaphore.release();
                                if (ex != null) {
                                    log.error("任务执行异常: taskId={}, actionType={}", 
                                             task.getId(), actionType, ex);
                                } else {
                                    log.debug("任务执行完成，释放并发槽位: taskId={}, actionType={}, 剩余槽位={}", 
                                             task.getId(), actionType, semaphore.availablePermits());
                                }
                            });
                            
                            // 从已加载集合中移除
                            loadedTaskIds.remove(task.getId());
                        } else {
                            // bug 3条未处理就有可能走这个分支，多线程有问题
                            // 并发已满，延迟1秒后重试
                            log.info("并发槽位已满，延迟重试: taskId={}, actionType={}", task.getId(), actionType);
                            
                            // 更新任务的计划执行时间（延迟1秒）
                            task.setScheduledAt(task.getScheduledAt().plusSeconds(configs.getConcurrentDelay()));
                            
                            // 重新包装并放回队列
                            RobotTaskWrapper retryWrapper = new RobotTaskWrapper(task);
                            taskQueue.offer(retryWrapper);
                            
                            // 统计延迟重试次数
                            delayedRetryCount.computeIfAbsent(actionType, k -> new AtomicLong(0))
                                             .incrementAndGet();
                        }
                    }
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
     * 加载任务到内存队列（带队列容量检查）
     * 
     * 使用 synchronized 保证线程安全，防止多线程并发导致队列超限
     * 
     * @param tasks 待加载的任务列表
     * @return 实际加载的任务数
     */
    public synchronized int loadTasks(List<RobotTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        
        // 检查队列容量：当前大小 + 待加载数量是否超过上限
        int currentSize = taskQueue.size();
        int availableCapacity = configs.getQueueSize() - currentSize;
        
        // 如果队列已满，拒绝所有任务
        if (availableCapacity <= 0) {
            int rejected = tasks.size();
            rejectedTaskCount.addAndGet(rejected);
            log.warn("队列已满，拒绝加载 {} 个任务 - 当前队列大小: {}, 队列容量上限: {}", 
                     rejected, currentSize, configs.getQueueSize());
            return 0;
        }
        
        // 计算实际可加载的任务数（部分加载逻辑）
        int tasksToLoad = Math.min(tasks.size(), availableCapacity);
        int tasksToReject = tasks.size() - tasksToLoad;
        
        // 如果有任务将被拒绝，记录警告日志
        if (tasksToReject > 0) {
            rejectedTaskCount.addAndGet(tasksToReject);
            log.warn("队列容量不足，拒绝加载 {} 个任务 - 待加载: {}, 可用容量: {}, 当前队列: {}/{}", 
                     tasksToReject, tasks.size(), availableCapacity, currentSize, configs.getQueueSize());
        }
        
        // 加载任务到队列
        int loaded = 0;
        for (int i = 0; i < tasksToLoad; i++) {
            RobotTask task = tasks.get(i);
            
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
            
            // 队列使用率检查：超过 80% 时记录警告
            double usageRate = (double) taskQueue.size() / configs.getQueueSize();
            if (usageRate > 0.80) {
                log.warn("队列使用率较高: {}/{} {}%，建议检查任务执行速度或调整队列容量",
                         taskQueue.size(), configs.getQueueSize(), usageRate * 100);
            }
        }
        
        return loaded;
    }
    
    /**
     * 获取因队列满而拒绝加载的任务数（用于监控）
     * 
     * @return 累计拒绝的任务数
     */
    public long getRejectedTaskCount() {
        return rejectedTaskCount.get();
    }
    
    /**
     * 获取队列使用率（0.0 ~ 1.0）
     * 
     * @return 队列使用率（当前大小 / 容量上限）
     */
    public double getQueueUsageRate() {
        return (double) taskQueue.size() / configs.getQueueSize();
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
    
    /**
     * 获取指定动作类型的可用并发槽位数
     * 
     * @param actionType 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
     * @return 可用槽位数，如果未配置该类型返回 -1
     */
    public int getConcurrencyAvailable(String actionType) {
        Semaphore semaphore = concurrencySemaphores.get(actionType);
        return semaphore != null ? semaphore.availablePermits() : -1;
    }
    
    /**
     * 获取指定动作类型的并发使用率（0.0 ~ 1.0）
     * 
     * @param actionType 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
     * @return 并发使用率，如果未配置该类型返回 -1.0
     */
    public double getConcurrencyUsageRate(String actionType) {
        Semaphore semaphore = concurrencySemaphores.get(actionType);
        if (semaphore == null) {
            return -1.0;
        }
        
        // 计算使用率：(总容量 - 可用槽位) / 总容量
        int totalPermits = getConcurrencyLimit(actionType);
        int available = semaphore.availablePermits();
        return (double) (totalPermits - available) / totalPermits;
    }
    
    /**
     * 获取指定动作类型的并发上限
     * 
     * @param actionType 动作类型
     * @return 并发上限，如果未配置返回 0
     */
    public int getConcurrencyLimit(String actionType) {
        return actionConcurrencyConfig.getOrDefault(actionType, 0);
    }
    
    /**
     * 动态修改指定动作类型的并发限制
     * 
     * 通过调整 Semaphore 的许可数实现动态修改：
     * - 增加限制：直接释放差值数量的许可 (release)
     * - 减少限制：尝试获取差值数量的许可 (tryAcquire)，不会中断正在执行的任务
     * 
     * @param actionType 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
     * @param newLimit 新的并发限制值（必须大于 0）
     * @throws IllegalArgumentException 如果动作类型未配置或新限制非法
     */
    public synchronized void updateConcurrencyLimit(String actionType, int newLimit) {
        if (newLimit <= 0) {
            throw new IllegalArgumentException("并发限制必须大于 0，实际值: " + newLimit);
        }
        
        Semaphore semaphore = concurrencySemaphores.get(actionType);
        if (semaphore == null) {
            // 如果动作类型不存在，创建新的信号量和配置
            semaphore = new Semaphore(newLimit);
            concurrencySemaphores.put(actionType, semaphore);
            actionConcurrencyConfig.put(actionType, newLimit);
            delayedRetryCount.put(actionType, new AtomicLong(0));
            log.info("创建新的并发控制信号量: actionType={}, limit={}", actionType, newLimit);
            return;
        }
        
        int oldLimit = getConcurrencyLimit(actionType);
        int diff = newLimit - oldLimit;
        
        if (diff == 0) {
            // 无需修改
            log.debug("并发限制未变化，无需修改: actionType={}, limit={}", actionType, newLimit);
            return;
        }

        // 必须立即调整 Semaphore 许可数，因为：
        // 1. consumeTasks() 方法实时使用 semaphore.tryAcquire() 判断是否可执行
        // 2. 只更新 actionConcurrencyConfig 不会影响已创建的 Semaphore 对象
        // 3. 需要确保并发限制即时生效，而不是等到下次重启或重新初始化
        if (diff > 0) {
            // 增加许可：直接释放差值数量的许可
            semaphore.release(diff);
            log.debug("增加并发限制: actionType={}, oldLimit={}, newLimit={}, diff=+{}", 
                     actionType, oldLimit, newLimit, diff);
        } else {
            // 减少许可：尝试获取差值数量的许可（非阻塞）
            // 注意：如果当前可用许可不足，只会获取实际可用的数量
            // 这确保了正在执行的任务不会被中断
            int acquired = semaphore.tryAcquire(-diff) ? -diff : semaphore.drainPermits();
            log.debug("减少并发限制: actionType={}, oldLimit={}, newLimit={}, diff={}, acquired={}", 
                     actionType, oldLimit, newLimit, diff, acquired);
        }
        
        // 更新配置映射
        actionConcurrencyConfig.put(actionType, newLimit);
        
        log.info("并发限制修改成功: actionType={}, oldLimit={}, newLimit={}, availablePermits={}", 
                actionType, oldLimit, newLimit, semaphore.availablePermits());
    }
    
    /**
     * 获取线程池最大容量
     * 
     * @return 线程池最大线程数
     */
    public int getMaxPoolSize() {
        return config.getExecutorMaxPoolSize();
    }
    
    /**
     * 获取内存队列容量上限
     * 
     * @return 队列容量上限
     */
    public int getMaxQueueSize() {
        return configs.getQueueSize();
    }
    
    /**
     * 获取指定动作类型因并发满而延迟重试的任务数
     * 
     * @param actionType 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
     * @return 延迟重试次数
     */
    public long getDelayedRetryCount(String actionType) {
        AtomicLong count = delayedRetryCount.get(actionType);
        return count != null ? count.get() : 0;
    }
}
