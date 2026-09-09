package com.ruoyi.system.utils;

import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.MiniappApiLog;
import com.ruoyi.system.service.IMiniappApiLogService;

/**
 * 小程序接口日志工具类
 * 通过 SpringUtils 获取 bean,可在任意地方静态调用
 *
 * @author ruoyi
 */
public class MiniappLogUtil
{
    /**
     * 记录接口日志
     *
     * @param type      日志类型(接口日志)
     * @param des       接口名称
     * @param result    请求结果(成功/失败/拒绝)
     * @param details   入参记录(JSON)
     * @param miniappId 小程序标识
     * @param openid    用户openid
     * @param reqUrl    请求URL
     * @param respTime  响应耗时(ms)
     * @param ip        请求IP
     */
    public static void log(String type, String des, String result, String details,
                            String miniappId, String openid, String reqUrl,
                            Long respTime, String ip)
    {
        MiniappApiLog apiLog = new MiniappApiLog();
        apiLog.setType(type);
        apiLog.setDes(des);
        apiLog.setResult(result);
        apiLog.setDetails(details);
        apiLog.setMiniappId(miniappId);
        apiLog.setOpenid(openid);
        apiLog.setReqUrl(reqUrl);
        apiLog.setRespTime(respTime);
        apiLog.setIp(ip);
        SpringUtils.getBean(IMiniappApiLogService.class).insertMiniappApiLog(apiLog);
    }
}
