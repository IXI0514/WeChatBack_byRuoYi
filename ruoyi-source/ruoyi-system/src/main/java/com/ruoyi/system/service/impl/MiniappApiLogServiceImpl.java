package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MiniappApiLog;
import com.ruoyi.system.mapper.MiniappApiLogMapper;
import com.ruoyi.system.service.IMiniappApiLogService;

/**
 * 接口日志 服务实现
 *
 * @author ruoyi
 */
@Service
public class MiniappApiLogServiceImpl implements IMiniappApiLogService
{
    @Autowired
    private MiniappApiLogMapper miniappApiLogMapper;

    @Override
    public List<MiniappApiLog> selectMiniappApiLogList(MiniappApiLog miniappApiLog)
    {
        return miniappApiLogMapper.selectMiniappApiLogList(miniappApiLog);
    }

    @Override
    public int insertMiniappApiLog(MiniappApiLog miniappApiLog)
    {
        return miniappApiLogMapper.insertMiniappApiLog(miniappApiLog);
    }

    @Override
    public int deleteMiniappApiLogByIds(Long[] logIds)
    {
        return miniappApiLogMapper.deleteMiniappApiLogByIds(logIds);
    }

    @Override
    public int cleanMiniappApiLog()
    {
        return miniappApiLogMapper.cleanMiniappApiLog();
    }
}
