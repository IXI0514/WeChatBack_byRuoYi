package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.MiniappRepeater;

public interface MiniappRepeaterMapper
{
    List<MiniappRepeater> selectMiniappRepeaterList(MiniappRepeater repeater);
    MiniappRepeater selectMiniappRepeaterById(Long repeaterId);
    int insertMiniappRepeater(MiniappRepeater repeater);
    int updateMiniappRepeater(MiniappRepeater repeater);
    int deleteMiniappRepeaterByIds(Long[] repeaterIds);
}
