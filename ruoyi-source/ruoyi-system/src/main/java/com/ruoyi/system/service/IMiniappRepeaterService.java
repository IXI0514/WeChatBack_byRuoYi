package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.MiniappRepeater;

public interface IMiniappRepeaterService
{
    List<MiniappRepeater> selectMiniappRepeaterList(MiniappRepeater repeater);
    MiniappRepeater selectMiniappRepeaterById(Long repeaterId);
    int insertMiniappRepeater(MiniappRepeater repeater);
    int updateMiniappRepeater(MiniappRepeater repeater);
    int deleteMiniappRepeaterByIds(Long[] repeaterIds);
}
