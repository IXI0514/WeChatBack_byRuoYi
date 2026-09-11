package com.ruoyi.web.controller.miniapp.support;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.controller.miniapp.MiniappApiController;
import org.springframework.http.ResponseEntity;
import com.ruoyi.common.core.domain.AjaxResult;

/** 直接调用入口的回归检查；不连接数据库，不使用真实密钥。 */
public class MiniappTokenEndpointCheck
{
    public static void main(String[] args) throws Exception
    {
        MiniappApiController controller = new MiniappApiController();
        byte[] randomKey = new byte[32];
        new java.security.SecureRandom().nextBytes(randomKey);
        String key = Base64.getEncoder().encodeToString(randomKey);
        set(controller, "apiAesKey", key);
        set(controller, "configService", Proxy.newProxyInstance(ISysConfigService.class.getClassLoader(),
                new Class<?>[] { ISysConfigService.class }, (proxy, method, values) ->
                    "miniapp.valid.ids".equals(values[0]) ? "wx_ham" : ""));
        ObjectNode request = new ObjectMapper().createObjectNode();
        request.put("miniappId", "wx_ham");
        ResponseEntity<AjaxResult> response = controller.token(request);
        check(response, 200);
        String token = (String) response.getBody().get("token");
        if (!"wx_ham".equals(MiniappTokenVerifier.verify(token, key, Instant.now().getEpochSecond(), -1)))
            throw new AssertionError("AES 密钥签发的 token 校验失败");
        if (!Long.valueOf(-1).equals(response.getBody().get("expiresIn")) || response.getBody().get("expiresAt") != null)
            throw new AssertionError("有效期错误");
        request.put("clientSecret", "incorrect");
        check(controller.token(request), 400);
        request.remove("clientSecret");
        request.put("miniappId", "");
        check(controller.token(request), 400);
        request.put("miniappId", 123);
        check(controller.token(request), 400);
        request.put("miniappId", "not_allowed");
        check(controller.token(request), 403);
        request.put("miniappId", "wx_ham");
        set(controller, "apiAesKey", "invalid");
        check(controller.token(request), 503);
        check(controller.token(null), 400);
        set(controller, "apiAesKey", key);
        set(controller, "configService", Proxy.newProxyInstance(ISysConfigService.class.getClassLoader(),
                new Class<?>[] { ISysConfigService.class }, (proxy, method, values) ->
                    "miniapp.valid.ids".equals(values[0]) ? "wx_ham" : "5"));
        response = controller.token(request);
        check(response, 200);
        if (!Long.valueOf(300).equals(response.getBody().get("expiresIn"))) throw new AssertionError("5分钟签发失败");
        check(controller.verify(new java.util.HashMap<String, String>()), 401);
        java.util.Map<String, String> login = new java.util.HashMap<>();
        login.put("miniappId", "other");
        login.put("token", token);
        check(controller.verify(login), 403);
        login.put("miniappId", "wx_ham");
        login.put("token", MiniappTokenVerifier.issue("wx_ham", key, Instant.now().getEpochSecond() - 301));
        check(controller.verify(login), 401);
        org.springframework.util.LinkedMultiValueMap<String, String> query = new org.springframework.util.LinkedMultiValueMap<>();
        query.add("token", login.get("token"));
        check(controller.repeaterList(query), 401);
        System.out.println("PASS: issuance default unlimited / 5 minutes, input validation, login and list expiration, ID mismatch");
    }

    private static void set(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void check(ResponseEntity<AjaxResult> response, int status)
    {
        if (response.getStatusCodeValue() != status || !Integer.valueOf(status).equals(response.getBody().get("code")))
            throw new AssertionError("预期状态码 " + status);
    }
}
