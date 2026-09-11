package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class MiniappRepeater extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long repeaterId;
    private String repeaterName;
    private String callSign;
    private String province;
    private String city;
    private String operationMode;
    private BigDecimal uplinkFrequencyMhz;
    private BigDecimal downlinkFrequencyMhz;
    private String status;
    private String isPublic;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastVerifiedAt;
    private String radioConfig;
    private String nameNormalized;

    public Long getRepeaterId() { return repeaterId; }
    public void setRepeaterId(Long repeaterId) { this.repeaterId = repeaterId; }
    public String getRepeaterName() { return repeaterName; }
    public void setRepeaterName(String repeaterName) { this.repeaterName = repeaterName; }
    public String getCallSign() { return callSign; }
    public void setCallSign(String callSign) { this.callSign = callSign; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getOperationMode() { return operationMode; }
    public void setOperationMode(String operationMode) { this.operationMode = operationMode; }
    public BigDecimal getUplinkFrequencyMhz() { return uplinkFrequencyMhz; }
    public void setUplinkFrequencyMhz(BigDecimal uplinkFrequencyMhz) { this.uplinkFrequencyMhz = uplinkFrequencyMhz; }
    public BigDecimal getDownlinkFrequencyMhz() { return downlinkFrequencyMhz; }
    public void setDownlinkFrequencyMhz(BigDecimal downlinkFrequencyMhz) { this.downlinkFrequencyMhz = downlinkFrequencyMhz; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIsPublic() { return isPublic; }
    public void setIsPublic(String isPublic) { this.isPublic = isPublic; }
    public Date getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(Date lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public String getRadioConfig() { return radioConfig; }
    public void setRadioConfig(String radioConfig) { this.radioConfig = radioConfig; }
    public String getNameNormalized() { return nameNormalized; }
    public void setNameNormalized(String nameNormalized) { this.nameNormalized = nameNormalized; }
}
