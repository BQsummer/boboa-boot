package com.bqsummer.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * 加密工具类
 * 使用 Spring Security Crypto 实现 AES-256 加密
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Component
public class EncryptionUtil {
    
    private final TextEncryptor encryptor;
    
    public EncryptionUtil(
            @Value("${model.encryption.password:default-encryption-password}") String password,
            @Value("${model.encryption.salt:2025102100000000}") String salt) {
        // 使用 Spring Security Crypto 创建 TextEncryptor
        // 注意：生产环境中应使用环境变量配置密码和盐值
        this.encryptor = Encryptors.text(password, salt);
    }
    
    /**
     * 加密文本
     * 
     * @param plainText 明文
     * @return 密文
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return encryptor.encrypt(plainText);
    }
    
    /**
     * 解密文本
     * 
     * @param encryptedText 密文
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        return encryptor.decrypt(encryptedText);
    }
}
