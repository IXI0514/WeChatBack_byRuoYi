package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MiniappRepeater;
import com.ruoyi.system.mapper.MiniappRepeaterMapper;
import com.ruoyi.system.service.IMiniappRepeaterService;

@Service
public class MiniappRepeaterServiceImpl implements IMiniappRepeaterService
{
    @Autowired
    private MiniappRepeaterMapper miniappRepeaterMapper;
    public List<MiniappRepeater> selectMiniappRepeaterList(MiniappRepeater repeater) { return miniappRepeaterMapper.selectMiniappRepeaterList(repeater); }
    public MiniappRepeater selectMiniappRepeaterById(Long repeaterId) { return miniappRepeaterMapper.selectMiniappRepeaterById(repeaterId); }
    public int insertMiniappRepeater(MiniappRepeater repeater) { repeater.setNameNormalized(MiniappRepeaterSubmissionServiceImpl.normalizeName(repeater.getRepeaterName())); return miniappRepeaterMapper.insertMiniappRepeater(repeater); }
    public int updateMiniappRepeater(MiniappRepeater repeater) { repeater.setNameNormalized(MiniappRepeaterSubmissionServiceImpl.normalizeName(repeater.getRepeaterName())); return miniappRepeaterMapper.updateMiniappRepeater(repeater); }
    public int deleteMiniappRepeaterByIds(Long[] repeaterIds) { return miniappRepeaterMapper.deleteMiniappRepeaterByIds(repeaterIds); }
}
