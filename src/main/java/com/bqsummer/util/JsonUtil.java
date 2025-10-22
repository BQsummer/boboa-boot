package com.bqsummer.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 序列化/反序列化工具类
 * 基于 FastJson2 实现
 */
@Slf4j
public class JsonUtil {
    
    /**
     * 将对象序列化为JSON字符串
     *
     * @param object 待序列化的对象
     * @return JSON字符串，序列化失败返回null
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return JSON.toJSONString(object);
        } catch (JSONException e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串反序列化为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化的对象，失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return JSON.parseObject(json, clazz);
        } catch (JSONException e) {
            log.error("JSON反序列化失败: json={}, targetClass={}, error={}", 
                json, clazz.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 检查字符串是否是合法的JSON格式
     *
     * @param json 待检查的字符串
     * @return true表示合法，false表示不合法
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            JSON.parse(json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
