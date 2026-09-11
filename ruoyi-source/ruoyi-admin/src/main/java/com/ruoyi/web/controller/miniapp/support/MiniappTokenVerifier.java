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

    public static String verify(String token, String keyBase64, long nowSeconds)
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
            if (timestamp < nowSeconds - 120 || timestamp > nowSeconds + 120) throw new IllegalArgumentException();
            return fields[0];
        }
        catch (GeneralSecurityException | IllegalArgumentException e)
        {
            throw new IllegalArgumentException("小程序凭证无效或已过期");
        }
    }
}
