package com.ruoyi.web.controller.miniapp;

import java.util.List;
import java.time.Instant;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import com.ruoyi.web.controller.miniapp.support.MiniappTokenVerifier;
import org.springframework.web.bind.annotation.GetMapping;
import com.ruoyi.system.domain.MiniappRepeater;
import com.ruoyi.system.service.IMiniappRepeaterService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.MiniappUser;
import com.ruoyi.system.service.IMiniappUserService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.utils.MiniappLogUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 小程序登录验证接口(给小程序调用,匿名访问)
 *
 * 对外接口: POST /api/login/verify
 * 参数: miniappId, openid, wxId, nickname(可选)
 * 逻辑: 验证小程序标识 -> 查/增用户 -> 返回是否会员
 *
 * @author ruoyi
 */
@Api(tags = "小程序 API")
@RestController
@RequestMapping("/api")
public class MiniappApiController
{
    @Autowired
    private IMiniappUserService miniappUserService;

    @Autowired
    private ISysConfigService configService;

    @Value("${miniapp.api.aes-key:}")
    private String apiAesKey;

    /**
     * 用户验证接口
     * 确认数据是否存在,不存在则增加,存在则查询返回
     */
    @Anonymous
    @ApiOperation("用户验证")
    @PostMapping("/login/verify")
    public AjaxResult verify(@RequestBody Map<String, String> params)
    {
        long startTime = System.currentTimeMillis();
        String miniappId = params.get("miniappId");
        String openid = params.get("openid");
        String wxId = params.get("wxId");
        String nickname = params.get("nickname");
        String reqUrl = "/api/login/verify";
        String ip = params.getOrDefault("ip", "");

        String details = "{\"miniappId\":\"" + miniappId + "\",\"openid\":\"" + openid + "\",\"wxId\":\"" + wxId + "\"}";

        String validIds = configService.selectConfigByKey("miniapp.valid.ids");
        if (StringUtils.isEmpty(validIds))
        {
            MiniappLogUtil.log("接口日志", "小程序用户验证", "拒绝", details, miniappId, openid, reqUrl, System.currentTimeMillis() - startTime, ip);
            return AjaxResult.error("小程序标识未配置,拒绝访问");
        }

        boolean isValid = false;
        for (String id : validIds.split(","))
        {
            if (id.trim().equals(miniappId))
            {
                isValid = true;
                break;
            }
        }
        if (!isValid)
        {
            MiniappLogUtil.log("接口日志", "小程序用户验证", "拒绝", details, miniappId, openid, reqUrl, System.currentTimeMillis() - startTime, ip);
            return AjaxResult.error("小程序标识不在白名单中,拒绝访问");
        }

        if (StringUtils.isEmpty(openid) || StringUtils.isEmpty(wxId))
        {
            MiniappLogUtil.log("接口日志", "小程序用户验证", "失败", details, miniappId, openid, reqUrl, System.currentTimeMillis() - startTime, ip);
            return AjaxResult.error("openid和wxId不能为空");
        }

        MiniappUser user = miniappUserService.selectByMiniappAndOpenid(miniappId, openid);
        if (user == null)
        {
            user = new MiniappUser();
            user.setMiniappId(miniappId);
            user.setOpenid(openid);
            user.setWxId(wxId);
            user.setNickname(nickname);
            user.setIsMember(0);
            user.setStatus("0");
            miniappUserService.insertMiniappUser(user);
            MiniappLogUtil.log("接口日志", "小程序用户验证", "成功", details, miniappId, openid, reqUrl, System.currentTimeMillis() - startTime, ip);
            AjaxResult ajax = AjaxResult.success("用户验证成功(新增)");
            ajax.put("userId", user.getUserId());
            ajax.put("isMember", 0);
            ajax.put("memberExpire", null);
            return ajax;
        }
        else
        {
            if ("1".equals(user.getStatus()))
            {
                MiniappLogUtil.log("接口日志", "小程序用户验证", "拒绝", details, miniappId, openid, reqUrl, System.currentTimeMillis() - startTime, ip);
                return AjaxResult.error("用户已停用,拒绝访问");
            }
            MiniappLogUtil.log("接口日志", "小程序用户验证", "成功", details, miniappId, openid, reqUrl, System.currentTimeMillis() - startTime, ip);
            AjaxResult ajax = AjaxResult.success("用户验证成功");
            ajax.put("userId", user.getUserId());
            ajax.put("isMember", user.getIsMember());
            ajax.put("memberExpire", user.getMemberExpire());
            return ajax;
        }
    }

    @Autowired
    private IMiniappRepeaterService miniappRepeaterService;

    @Anonymous
    @ApiOperation("中继台列表（AES-GCM 请求凭证）")
    @GetMapping("/repeater/list")
    public ResponseEntity<AjaxResult> repeaterList(
            @RequestParam MultiValueMap<String, String> params)
    {
        if (params.containsKey("token") && params.get("token").size() != 1)
        {
            return ResponseEntity.badRequest().body(AjaxResult.error(400, "token 不允许重复传入"));
        }
        String token = params.getFirst("token");
        // 跳过后台 JWT，使用独立的小程序凭证；不能移除此处的校验。
        final String miniappId;
        try
        {
            miniappId = MiniappTokenVerifier.verify(token, apiAesKey, Instant.now().getEpochSecond());
        }
        catch (IllegalStateException e)
        {
            return ResponseEntity.status(503).body(AjaxResult.error(503, e.getMessage()));
        }
        catch (IllegalArgumentException e)
        {
            return ResponseEntity.status(401).body(AjaxResult.error(401, e.getMessage()));
        }
        String validIds = configService.selectConfigByKey("miniapp.valid.ids");
        if (validIds == null || !Arrays.stream(validIds.split(",")).map(String::trim).anyMatch(miniappId::equals))
        {
            return ResponseEntity.status(403).body(AjaxResult.error(403, "小程序标识未授权"));
        }
        MiniappRepeater repeater = new MiniappRepeater();
        try
        {
            for (String name : params.keySet())
            {
                if (!Arrays.asList("token", "province", "city", "repeaterName", "callSign", "operationMode").contains(name)
                        || params.get(name).size() != 1)
                    throw new IllegalArgumentException("存在未知或重复查询参数");
            }
            repeater.setProvince(queryText(params, "province", 30));
            repeater.setCity(queryText(params, "city", 30));
            repeater.setRepeaterName(queryText(params, "repeaterName", 100));
            repeater.setCallSign(queryText(params, "callSign", 50));
            repeater.setOperationMode(queryText(params, "operationMode", 16));
            if (repeater.getCity() != null && repeater.getProvince() == null)
                throw new IllegalArgumentException("传入城市时必须同时提供省份");
            if (repeater.getOperationMode() != null
                    && !Arrays.asList("ANALOG", "DIGITAL", "MIXED").contains(repeater.getOperationMode()))
                throw new IllegalArgumentException("operationMode 必须为 ANALOG、DIGITAL 或 MIXED");
        }
        catch (IllegalArgumentException e)
        {
            return ResponseEntity.badRequest().body(AjaxResult.error(400, e.getMessage()));
        }
        repeater.setStatus("0");
        repeater.setIsPublic("0");
        List<MiniappRepeater> list = miniappRepeaterService.selectMiniappRepeaterList(repeater);
        return ResponseEntity.ok(AjaxResult.success(list));
    }

    private String queryText(MultiValueMap<String, String> params, String name, int maxLength)
    {
        String value = params.getFirst(name);
        if (value == null) return null;
        if (value.length() > maxLength || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(name + " 长度超限或包含控制字符");
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
