package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.MiniappRepeaterSubmission;

public interface IMiniappRepeaterSubmissionService
{
    int submit(MiniappRepeaterSubmission submission);
    List<MiniappRepeaterSubmission> selectList(MiniappRepeaterSubmission submission);
    MiniappRepeaterSubmission selectById(Long submissionId);
    int review(MiniappRepeaterSubmission submission);
}
