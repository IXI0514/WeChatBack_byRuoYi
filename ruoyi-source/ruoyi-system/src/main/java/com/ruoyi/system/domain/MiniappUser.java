package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 小程序用户表 miniapp_user
 *
 * @author ruoyi
 */
public class MiniappUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Excel(name = "用户ID", cellType = ColumnType.NUMERIC)
    private Long userId;

    /** 小程序标识 */
    @Excel(name = "小程序标识")
    private String miniappId;

    /** 用户openid */
    @Excel(name = "用户openid")
    private String openid;

    /** 微信id(union_id,方便管理) */
    @Excel(name = "微信id")
    private String wxId;

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickname;

    /** 是否会员 0否 1是 */
    @Excel(name = "是否会员", readConverterExp = "0=否,1=是")
    private Integer isMember;

    /** 会员到期时间 */
    @Excel(name = "会员到期时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date memberExpire;

    /** 状态 0正常 1停用 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
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

    public String getWxId()
    {
        return wxId;
    }

    public void setWxId(String wxId)
    {
        this.wxId = wxId;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public Integer getIsMember()
    {
        return isMember;
    }

    public void setIsMember(Integer isMember)
    {
        this.isMember = isMember;
    }

    public Date getMemberExpire()
    {
        return memberExpire;
    }

    public void setMemberExpire(Date memberExpire)
    {
        this.memberExpire = memberExpire;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("miniappId", getMiniappId())
            .append("openid", getOpenid())
            .append("wxId", getWxId())
            .append("nickname", getNickname())
            .append("isMember", getIsMember())
            .append("memberExpire", getMemberExpire())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
