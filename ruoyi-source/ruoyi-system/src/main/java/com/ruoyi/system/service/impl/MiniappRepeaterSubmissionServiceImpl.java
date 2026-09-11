package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.MiniappRepeaterSubmission;
import com.ruoyi.system.domain.MiniappRepeater;
import com.ruoyi.system.mapper.MiniappRepeaterSubmissionMapper;
import com.ruoyi.system.service.IMiniappRepeaterSubmissionService;
import com.ruoyi.system.service.IMiniappRepeaterService;

@Service
public class MiniappRepeaterSubmissionServiceImpl implements IMiniappRepeaterSubmissionService
{
    @Autowired
    private MiniappRepeaterSubmissionMapper mapper;
    @Autowired
    private IMiniappRepeaterService repeaterService;

    /** 名称去重规则：忽略首尾、内部空白及英文大小写。 */
    public static String normalizeName(String name)
    {
        return name == null ? "" : name.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    @Override
    public int submit(MiniappRepeaterSubmission submission)
    {
        String normalized = normalizeName(submission.getRepeaterName());
        if (normalized.isEmpty()) throw new ServiceException("中继台名称不能为空");
        submission.setNameNormalized(normalized);
        if (mapper.countRepeaterByNormalizedName(normalized) > 0 || mapper.countSubmissionByNormalizedName(normalized) > 0)
            throw new ServiceException("该中继台名称已存在或已提交审核，不能重复提交");
        try { return mapper.insertSubmission(submission); }
        catch (DuplicateKeyException e) { throw new ServiceException("该中继台名称已提交审核，不能重复提交"); }
    }
    @Override public List<MiniappRepeaterSubmission> selectList(MiniappRepeaterSubmission s) { return mapper.selectSubmissionList(s); }
    @Override public MiniappRepeaterSubmission selectById(Long id) { return mapper.selectSubmissionById(id); }
    @Override @Transactional
    public int review(MiniappRepeaterSubmission submission)
    {
        MiniappRepeaterSubmission current = mapper.selectSubmissionById(submission.getSubmissionId());
        if (current == null) throw new ServiceException("审核提交不存在");
        if (!"0".equals(current.getReviewStatus())) throw new ServiceException("该提交已审核，不能重复审核");
        if (!"1".equals(submission.getReviewStatus()) && !"2".equals(submission.getReviewStatus()))
            throw new ServiceException("审核状态只能为通过或驳回");
        if ("1".equals(submission.getReviewStatus()))
        {
            if (mapper.countRepeaterByNormalizedName(current.getNameNormalized()) > 0)
                throw new ServiceException("已存在同名中继台，不能通过审核");
            MiniappRepeater repeater = new MiniappRepeater();
            repeater.setRepeaterName(current.getRepeaterName());
            repeater.setNameNormalized(current.getNameNormalized());
            repeater.setCallSign(current.getCallSign());
            repeater.setProvince(current.getProvince());
            repeater.setCity(current.getCity());
            repeater.setOperationMode(current.getOperationMode());
            repeater.setUplinkFrequencyMhz(current.getUplinkFrequencyMhz());
            repeater.setDownlinkFrequencyMhz(current.getDownlinkFrequencyMhz());
            repeater.setRadioConfig(current.getRadioConfig());
            repeater.setRemark(current.getRemark());
            repeater.setStatus("0");
            repeater.setIsPublic("0");
            repeater.setCreateBy(submission.getReviewer());
            repeaterService.insertMiniappRepeater(repeater);
            submission.setRepeaterId(repeater.getRepeaterId());
        }
        return mapper.reviewSubmission(submission);
    }
}
