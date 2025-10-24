package com.bqsummer.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;

/**
 * 实例ID生成器
 * 用于生成全局唯一的实例标识符，格式为 hostname:pid
 * 主要用于任务抢占场景中标识任务所有权
 * 
 * @author boboa-boot
 * @since 2025-10-24
 */
public class InstanceIdGenerator {
    
    private static volatile String cachedInstanceId;
    
    /**
     * 获取当前实例的唯一标识符
     * 生成规则：
     * 1. 优先使用 hostname:pid 格式（例如：server-01:12345）
     * 2. 如果获取失败，降级使用 UUID（例如：uuid:a1b2c3d4...）
     * 实例ID会被缓存，多次调用返回相同值
     * 
     * @return 实例ID字符串，格式为 hostname:pid 或 uuid:xxx
     */
    public static String getInstanceId() {
        if (cachedInstanceId != null) {
            return cachedInstanceId;
        }
        
        synchronized (InstanceIdGenerator.class) {
            if (cachedInstanceId != null) {
                return cachedInstanceId;
            }
            
            try {
                // 获取主机名
                String hostname = InetAddress.getLocalHost().getHostName();
                
                // 获取进程ID (从RuntimeMXBean的name中提取，格式通常为 "pid@hostname")
                String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
                String pid = runtimeName.split("@")[0];
                
                cachedInstanceId = hostname + ":" + pid;
            } catch (Exception e) {
                // 降级策略：使用UUID作为实例ID
                cachedInstanceId = "uuid:" + UUID.randomUUID().toString();
            }
            
            return cachedInstanceId;
        }
    }
    
    /**
     * 重置缓存的实例ID（仅用于测试）
     */
    static void resetInstanceId() {
        cachedInstanceId = null;
    }
}
