package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 接口日志表 miniapp_api_log
 *
 * @author ruoyi
 */
public class MiniappApiLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @Excel(name = "日志ID", cellType = ColumnType.NUMERIC)
    private Long logId;

    /** 日志类型 */
    @Excel(name = "日志类型")
    private String type;

    /** 接口名称 */
    @Excel(name = "接口名称")
    private String des;

    /** 请求结果 */
    @Excel(name = "请求结果")
    private String result;

    /** 接口入参记录(JSON) */
    private String details;

    /** 小程序标识 */
    @Excel(name = "小程序标识")
    private String miniappId;

    /** 用户openid */
    @Excel(name = "用户openid")
    private String openid;

    /** 请求URL */
    @Excel(name = "请求URL")
    private String reqUrl;

    /** 响应耗时(毫秒) */
    @Excel(name = "响应耗时(ms)", cellType = ColumnType.NUMERIC)
    private Long respTime;

    /** 请求IP */
    @Excel(name = "请求IP")
    private String ip;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getDes()
    {
        return des;
    }

    public void setDes(String des)
    {
        this.des = des;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    public String getMiniappId()
    {
        return miniappId;
    }

    public void setMiniappId(String miniappId)
    {
        this.miniappId = miniappId;
    }

    public String getOpenid()
    {
        return openid;
    }

    public void setOpenid(String openid)
    {
        this.openid = openid;
    }

    public String getReqUrl()
    {
        return reqUrl;
    }

    public void setReqUrl(String reqUrl)
    {
        this.reqUrl = reqUrl;
    }

    public Long getRespTime()
    {
        return respTime;
    }

    public void setRespTime(Long respTime)
    {
        this.respTime = respTime;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("type", getType())
            .append("des", getDes())
            .append("result", getResult())
            .append("miniappId", getMiniappId())
            .append("openid", getOpenid())
            .append("reqUrl", getReqUrl())
            .append("respTime", getRespTime())
            .append("ip", getIp())
            .append("createTime", getCreateTime())
            .toString();
    }
}
