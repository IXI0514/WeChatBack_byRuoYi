package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.MiniappRepeaterSubmission;

public interface MiniappRepeaterSubmissionMapper
{
    int countRepeaterByNormalizedName(String nameNormalized);
    int countSubmissionByNormalizedName(String nameNormalized);
    int insertSubmission(MiniappRepeaterSubmission submission);
    List<MiniappRepeaterSubmission> selectSubmissionList(MiniappRepeaterSubmission submission);
    MiniappRepeaterSubmission selectSubmissionById(Long submissionId);
    int reviewSubmission(MiniappRepeaterSubmission submission);
}
