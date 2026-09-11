package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/** 小程序用户提交、等待后台审核的中继台资料。 */
public class MiniappRepeaterSubmission extends BaseEntity
{
    private Long submissionId;
    private String miniappId;
    private String repeaterName;
    private String nameNormalized;
    private String callSign;
    private String province;
    private String city;
    private String operationMode;
    private BigDecimal uplinkFrequencyMhz;
    private BigDecimal downlinkFrequencyMhz;
    private String radioConfig;
    private String remark;
    /** 0待审核、1已通过、2已驳回。 */
    private String reviewStatus;
    private String reviewRemark;
    private String reviewer;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;
    private Long repeaterId;
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long value) { submissionId = value; }
    public String getMiniappId() { return miniappId; }
    public void setMiniappId(String value) { miniappId = value; }
    public String getRepeaterName() { return repeaterName; }
    public void setRepeaterName(String value) { repeaterName = value; }
    public String getNameNormalized() { return nameNormalized; }
    public void setNameNormalized(String value) { nameNormalized = value; }
    public String getCallSign() { return callSign; }
    public void setCallSign(String value) { callSign = value; }
    public String getProvince() { return province; }
    public void setProvince(String value) { province = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getOperationMode() { return operationMode; }
    public void setOperationMode(String value) { operationMode = value; }
    public BigDecimal getUplinkFrequencyMhz() { return uplinkFrequencyMhz; }
    public void setUplinkFrequencyMhz(BigDecimal value) { uplinkFrequencyMhz = value; }
    public BigDecimal getDownlinkFrequencyMhz() { return downlinkFrequencyMhz; }
    public void setDownlinkFrequencyMhz(BigDecimal value) { downlinkFrequencyMhz = value; }
    public String getRadioConfig() { return radioConfig; }
    public void setRadioConfig(String value) { radioConfig = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String value) { reviewStatus = value; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String value) { reviewRemark = value; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String value) { reviewer = value; }
    public Date getReviewTime() { return reviewTime; }
    public void setReviewTime(Date value) { reviewTime = value; }
    public Long getRepeaterId() { return repeaterId; }
    public void setRepeaterId(Long value) { repeaterId = value; }
}
