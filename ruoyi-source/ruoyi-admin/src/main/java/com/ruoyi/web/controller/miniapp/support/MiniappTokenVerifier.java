package com.ruoyi.web.controller.miniapp.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-GCM 请求凭证校验。此凭证只标识客户端，不代表用户登录。 */
public final class MiniappTokenVerifier
{
    private MiniappTokenVerifier() { }

    /** -1 表示未设置有效期；非法配置不能静默变为永久有效。 */
    public static long expirySeconds(String minutes)
    {
        if (minutes == null || minutes.trim().isEmpty()) return -1;
        try
        {
            String value = minutes.trim();
            if (!value.matches("[0-9]{1,9}")) throw new IllegalArgumentException();
            long count = Long.parseLong(value);
            if (count <= 0) throw new IllegalArgumentException();
            return Math.multiplyExact(count, 60L);
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("miniapp.token.expire.minutes 必须为正整数分钟或留空");
        }
    }

    public static String issue(String miniappId, String keyBase64, long nowSeconds)
    {
        if (miniappId == null || !miniappId.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("小程序标识格式错误");
        try
        {
            byte[] key = Base64.getDecoder().decode(keyBase64);
            if (key.length != 32) throw new IllegalArgumentException();
            byte[] iv = new byte[12];
            new java.security.SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD("GET /api/repeater/list".getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal((miniappId + "\n" + nowSeconds).getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return "v1." + encoder.encodeToString(iv) + "." + encoder.encodeToString(encrypted);
        }
        catch (GeneralSecurityException | RuntimeException e)
        {
            throw new IllegalStateException("小程序 API 密钥未配置或凭证生成失败");
        }
    }

    public static String verify(String token, String keyBase64, long nowSeconds, long expirySeconds)
    {
        final byte[] key;
        try
        {
            key = Base64.getDecoder().decode(keyBase64);
            if (key.length != 32) throw new IllegalArgumentException();
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("小程序 API 密钥未配置或格式错误");
        }
        try
        {
            if (token == null || token.length() > 1024) throw new IllegalArgumentException();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException();
            byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[2]);
            if (iv.length != 12 || ciphertext.length < 17) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD("GET /api/repeater/list".getBytes(StandardCharsets.UTF_8));
            String[] fields = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8).split("\n", -1);
            if (fields.length != 2 || !fields[0].matches("[A-Za-z0-9_-]{1,64}")
                    || !fields[1].matches("[0-9]{1,12}")) throw new IllegalArgumentException();
            long timestamp = Long.parseLong(fields[1]);
            // 即使不过期，也拒绝超出 120 秒时钟偏差的未来签发时间。
            if (timestamp > nowSeconds + 120
                    || (expirySeconds >= 0 && timestamp < nowSeconds - expirySeconds))
                throw new IllegalArgumentException();
            return fields[0];
        }
        catch (GeneralSecurityException | IllegalArgumentException e)
        {
            throw new IllegalArgumentException("小程序凭证无效或已过期");
        }
    }
}
