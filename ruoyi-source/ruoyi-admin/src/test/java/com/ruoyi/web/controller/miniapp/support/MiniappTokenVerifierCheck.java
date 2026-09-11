package com.ruoyi.web.controller.miniapp.support;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 无第三方测试依赖的协议回归检查，通过 main 执行。 */
public class MiniappTokenVerifierCheck
{
    private static final byte[] KEY = new byte[32];
    private static final long NOW = 1800000000L;
    public static void main(String[] args) throws Exception
    {
        new SecureRandom().nextBytes(KEY);
        String key = Base64.getEncoder().encodeToString(KEY);
        String issued = MiniappTokenVerifier.issue("wx_ham", key, NOW);
        if (!"wx_ham".equals(MiniappTokenVerifier.verify(issued, key, NOW + 120, 120)))
            throw new AssertionError("签发凭证无法验证");
        if (issued.equals(MiniappTokenVerifier.issue("wx_ham", key, NOW)))
            throw new AssertionError("签发凭证未使用随机 IV");
        try { MiniappTokenVerifier.verify(issued, key, NOW + 121, 120); throw new AssertionError("签发凭证未过期"); }
        catch (IllegalArgumentException expected) { }
        for (long offset : new long[] {0, -120, 120})
        {
            String actual = MiniappTokenVerifier.verify(encrypt("wx_ham\n" + (NOW + offset)), key, NOW, 120);
            if (!"wx_ham".equals(actual)) throw new AssertionError("标识解密失败");
        }
        for (String raw : new String[] {"wx_ham\n" + (NOW - 121), "wx_ham\n" + (NOW + 121),
                "wx_ham\n202609111048", "wx_ham\nabc", "bad id\n" + NOW, "wx_ham\n" + NOW + "\nextra"})
            reject(encrypt(raw), key);
        reject(null, key);
        reject("v1.invalid.invalid", key);
        String token = encrypt("wx_ham\n" + NOW);
        String[] parts = token.split("\\.");
        byte[] altered = Base64.getUrlDecoder().decode(parts[2]);
        altered[0] ^= 1;
        reject(parts[0] + "." + parts[1] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(altered), key);
        try { MiniappTokenVerifier.verify(token, "", NOW, 120); throw new AssertionError("空密钥被接受"); }
        catch (IllegalStateException expected) { }
        if (MiniappTokenVerifier.expirySeconds(null) != -1 || MiniappTokenVerifier.expirySeconds(" ") != -1
                || MiniappTokenVerifier.expirySeconds("5") != 300) throw new AssertionError("分钟配置解析失败");
        for (String invalid : new String[] {"0", "-1", "abc", "1.5", "9999999999999"})
        {
            try { MiniappTokenVerifier.expirySeconds(invalid); throw new AssertionError("非法有效期被接受"); }
            catch (IllegalStateException expected) { }
        }
        MiniappTokenVerifier.verify(issued, key, NOW + 1000000, -1);
        MiniappTokenVerifier.verify(issued, key, NOW + 300, 300);
        System.out.println("PASS: configurable minutes, no expiry, invalid config, expiry boundaries, tampering, missing token/key");
    }

    private static void reject(String token, String key)
    {
        try { MiniappTokenVerifier.verify(token, key, NOW, 120); throw new AssertionError("无效凭证被接受"); }
        catch (IllegalArgumentException expected) { }
    }

    private static String encrypt(String plaintext) throws Exception
    {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD("GET /api/repeater/list".getBytes(StandardCharsets.UTF_8));
        return "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(iv) + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }
}
