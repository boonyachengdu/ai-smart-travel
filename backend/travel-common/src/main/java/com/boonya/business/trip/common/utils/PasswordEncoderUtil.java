package com.boonya.business.trip.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密工具类（基于 BCrypt）
 * 使用方式：
 *   注册/修改密码时：PasswordEncoderUtil.encode("123456")
 *   登录校验时：PasswordEncoderUtil.matches("明文", "数据库密文")
 */
public class PasswordEncoderUtil {

    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密明文密码
     * @param rawPassword 明文密码
     * @return BCrypt 加密后的密文
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return encoder.encode(rawPassword);
    }

    /**
     * 校验密码是否匹配
     * @param rawPassword   用户输入的明文密码
     * @param encodedPassword 数据库存储的加密密码
     * @return true=匹配，false=不匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 获取 PasswordEncoder 实例（供 Spring Security 配置使用）
     */
    public static PasswordEncoder getEncoder() {
        return encoder;
    }

    /**
     * 升级旧密码（可选，当需要从旧加密方式迁移到 BCrypt 时使用）
     * @param rawPassword 明文
     * @param encodedPassword 旧的加密密码
     * @return 如果需要升级，返回新的 BCrypt 密码；否则返回 null
     */
    public static String upgradeEncoding(String rawPassword, String encodedPassword) {
        if (encoder.upgradeEncoding(encodedPassword)) {
            return encode(rawPassword);
        }
        return null;
    }

    public static void main(String[] args) {
        String pwd = PasswordEncoderUtil.encode("123456");
        System.out.println(pwd);
    }
}