package com.ruoyi.web.controller.miniapp;

import java.util.List;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import com.ruoyi.web.controller.miniapp.support.MiniappTokenVerifier;
import org.springframework.web.bind.annotation.GetMapping;
import com.ruoyi.system.domain.MiniappRepeater;
import com.ruoyi.system.service.IMiniappRepeaterService;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.math.BigDecimal;
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
import com.ruoyi.system.domain.MiniappRepeaterSubmission;
import com.ruoyi.system.service.IMiniappRepeaterSubmissionService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.utils.MiniappLogUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private IMiniappUserService miniappUserService;

    @Autowired
    private ISysConfigService configService;

    @Value("${miniapp.api.aes-key:}")
    private String apiAesKey;

    @Autowired
    private IMiniappRepeaterSubmissionService miniappRepeaterSubmissionService;

    /** 仅凭白名单 ID 领取 token，不验证调用方身份；仅用于公开数据。 */
    @Anonymous
    @ApiOperation("获取小程序访问凭证")
    @PostMapping("/token")
    public ResponseEntity<AjaxResult> token(@RequestBody(required = false) com.fasterxml.jackson.databind.JsonNode params)
    {
        if (params == null || !params.isObject() || params.size() != 1
                || !params.hasNonNull("miniappId") || !params.get("miniappId").isTextual())
            return ResponseEntity.badRequest().body(AjaxResult.error(400, "仅接受字符串参数 miniappId"));
        String miniappId = params.get("miniappId").textValue();
        if (!miniappId.matches("[A-Za-z0-9_-]{1,64}"))
            return ResponseEntity.badRequest().body(AjaxResult.error(400, "小程序标识格式错误"));
        try
        {
            if (apiAesKey == null || java.util.Base64.getDecoder().decode(apiAesKey).length != 32)
                throw new IllegalArgumentException();
        }
        catch (IllegalArgumentException e)
        {
            return ResponseEntity.status(503).body(AjaxResult.error(503, "小程序 API AES 密钥未正确配置"));
        }
        String validIds = configService.selectConfigByKey("miniapp.valid.ids");
        if (validIds == null || !Arrays.stream(validIds.split(",")).map(String::trim).anyMatch(miniappId::equals))
            return ResponseEntity.status(403).body(AjaxResult.error(403, "小程序标识未授权"));
        try
        {
            long now = Instant.now().getEpochSecond();
            long expiry = tokenExpirySeconds();
            AjaxResult result = AjaxResult.success();
            result.put("token", MiniappTokenVerifier.issue(miniappId, apiAesKey, now));
            result.put("expiresIn", expiry);
            result.put("expiresAt", expiry < 0 ? null : now + expiry);
            return ResponseEntity.ok().header("Cache-Control", "no-store").body(result);
        }
        catch (IllegalStateException e)
        {
            return ResponseEntity.status(503).body(AjaxResult.error(503, e.getMessage()));
        }
    }

    @Anonymous
    @ApiOperation("提交中继台资料审核")
    @PostMapping("/repeater/submit")
    public ResponseEntity<AjaxResult> submitRepeater(@RequestBody(required = false) com.fasterxml.jackson.databind.JsonNode params)
    {
        if (params == null || !params.isObject())
            return ResponseEntity.badRequest().body(AjaxResult.error(400, "请求体必须是 JSON 对象"));
        Set<String> allowed = new HashSet<>(Arrays.asList("miniappId", "token", "repeaterName", "callSign", "province", "city", "operationMode", "uplinkFrequencyMhz", "downlinkFrequencyMhz", "radioConfig", "remark"));
        java.util.Iterator<String> names = params.fieldNames();
        while (names.hasNext()) if (!allowed.contains(names.next()))
            return ResponseEntity.badRequest().body(AjaxResult.error(400, "存在不允许提交的字段"));
        try
        {
            String miniappId = jsonText(params, "miniappId", 64, true);
            String token = jsonText(params, "token", 1024, true);
            ResponseEntity<AjaxResult> rejected = checkBusinessToken(token, miniappId);
            if (rejected != null) return rejected;
            MiniappRepeaterSubmission submission = new MiniappRepeaterSubmission();
            submission.setMiniappId(miniappId);
            submission.setRepeaterName(jsonText(params, "repeaterName", 100, true));
            submission.setCallSign(jsonText(params, "callSign", 50, false));
            submission.setProvince(jsonText(params, "province", 30, true));
            submission.setCity(jsonText(params, "city", 30, true));
            submission.setOperationMode(jsonText(params, "operationMode", 16, true));
            if (!Arrays.asList("ANALOG", "DIGITAL", "MIXED").contains(submission.getOperationMode()))
                throw new IllegalArgumentException("operationMode 必须为 ANALOG、DIGITAL 或 MIXED");
            submission.setUplinkFrequencyMhz(jsonDecimal(params, "uplinkFrequencyMhz"));
            submission.setDownlinkFrequencyMhz(jsonDecimal(params, "downlinkFrequencyMhz"));
            submission.setRadioConfig(jsonConfig(params.get("radioConfig")));
            submission.setRemark(jsonText(params, "remark", 500, false));
            miniappRepeaterSubmissionService.submit(submission);
            AjaxResult result = AjaxResult.success("已提交审核");
            result.put("submissionId", submission.getSubmissionId());
            result.put("reviewStatus", "0");
            return ResponseEntity.ok(result);
        }
        catch (com.ruoyi.common.exception.ServiceException e)
        {
            return ResponseEntity.status(409).body(AjaxResult.error(409, e.getMessage()));
        }
        catch (IllegalArgumentException e)
        {
            return ResponseEntity.badRequest().body(AjaxResult.error(400, e.getMessage()));
        }
    }

    /**
     * 用户验证接口
     * 确认数据是否存在,不存在则增加,存在则查询返回
     */
    @Anonymous
    @ApiOperation("用户验证")
    @PostMapping("/login/verify")
    public ResponseEntity<AjaxResult> verify(@RequestBody Map<String, String> params)
    {
        ResponseEntity<AjaxResult> rejected = checkBusinessToken(params.get("token"), params.get("miniappId"));
        if (rejected != null) return rejected;
        return ResponseEntity.ok(verifyUser(params));
    }

    private AjaxResult verifyUser(Map<String, String> params)
    {
        long startTime = System.currentTimeMillis();
        String miniappId = params.get("miniappId");
        String openid = params.get("openid");
        String wxId = params.get("wxId");
        String nickname = params.get("nickname");
        String reqUrl = "/api/login/verify";
        String ip = params.getOrDefault("ip", "");

        String details = requestDetails(params);

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
            if (StringUtils.isNotEmpty(nickname) && !nickname.equals(user.getNickname()))
            {
                // 小程序昵称可变化，只更新昵称，不能借登录验证覆盖后台维护的会员和状态字段。
                MiniappUser nicknameUpdate = new MiniappUser();
                nicknameUpdate.setUserId(user.getUserId());
                nicknameUpdate.setNickname(nickname);
                miniappUserService.updateMiniappUser(nicknameUpdate);
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
            miniappId = MiniappTokenVerifier.verify(token, apiAesKey, Instant.now().getEpochSecond(), tokenExpirySeconds());
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

    /**
     * 完整保留登录接口传入的字段，避免手工拼接 JSON 时遗漏 nickname、ip 或后续扩展字段。
     * 请求凭证只记录脱敏值，防止日志泄露可直接使用的小程序 token。
     */
    private String requestDetails(Map<String, String> params)
    {
        Map<String, String> details = new LinkedHashMap<>(params);
        if (details.containsKey("token"))
        {
            details.put("token", "***");
        }
        try
        {
            return OBJECT_MAPPER.writeValueAsString(details);
        }
        catch (JsonProcessingException e)
        {
            // 当前接口只接受字符串参数，理论上不会到达这里；保留合法 JSON 以确保日志写入不受影响。
            return "{\"logSerializeError\":true}";
        }
    }

    private String jsonText(com.fasterxml.jackson.databind.JsonNode params, String name, int maxLength, boolean required)
    {
        com.fasterxml.jackson.databind.JsonNode node = params.get(name);
        if (node == null || node.isNull())
        {
            if (required) throw new IllegalArgumentException(name + " 不能为空");
            return null;
        }
        if (!node.isTextual()) throw new IllegalArgumentException(name + " 必须为字符串");
        String value = node.textValue().trim();
        if ((required && value.isEmpty()) || value.length() > maxLength || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(name + " 为空、长度超限或包含控制字符");
        return value.isEmpty() ? null : value;
    }

    private BigDecimal jsonDecimal(com.fasterxml.jackson.databind.JsonNode params, String name)
    {
        com.fasterxml.jackson.databind.JsonNode node = params.get(name);
        if (node == null || !node.isNumber()) throw new IllegalArgumentException(name + " 必须为数字");
        BigDecimal value = node.decimalValue();
        if (value.scale() > 5 || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("9999.99999")) > 0)
            throw new IllegalArgumentException(name + " 范围或精度不正确");
        return value;
    }

    private String jsonConfig(com.fasterxml.jackson.databind.JsonNode node)
    {
        if (node == null || node.isNull()) return null;
        if (!node.isObject()) throw new IllegalArgumentException("radioConfig 必须为 JSON 对象");
        try
        {
            String value = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(node);
            if (value.length() > 8000) throw new IllegalArgumentException("radioConfig 过长");
            return value;
        }
        catch (com.fasterxml.jackson.core.JsonProcessingException e)
        {
            throw new IllegalArgumentException("radioConfig 格式错误");
        }
    }

    private long tokenExpirySeconds()
    {
        return MiniappTokenVerifier.expirySeconds(configService.selectConfigByKey("miniapp.token.expire.minutes"));
    }

    private ResponseEntity<AjaxResult> checkBusinessToken(String token, String requestedId)
    {
        try
        {
            String id = MiniappTokenVerifier.verify(token, apiAesKey, Instant.now().getEpochSecond(), tokenExpirySeconds());
            String ids = configService.selectConfigByKey("miniapp.valid.ids");
            if (!id.equals(requestedId) || ids == null
                    || !Arrays.stream(ids.split(",")).map(String::trim).anyMatch(id::equals))
                return ResponseEntity.status(403).body(AjaxResult.error(403, "小程序标识未授权或与凭证不一致"));
            return null;
        }
        catch (IllegalStateException e)
        {
            return ResponseEntity.status(503).body(AjaxResult.error(503, e.getMessage()));
        }
        catch (IllegalArgumentException e)
        {
            return ResponseEntity.status(401).body(AjaxResult.error(401, e.getMessage()));
        }
    }
}
