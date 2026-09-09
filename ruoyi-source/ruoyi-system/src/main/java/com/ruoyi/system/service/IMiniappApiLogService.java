package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.MiniappApiLog;

/**
 * 接口日志 服务层
 *
 * @author ruoyi
 */
public interface IMiniappApiLogService
{
    /**
     * 查询接口日志列表
     */
    public List<MiniappApiLog> selectMiniappApiLogList(MiniappApiLog miniappApiLog);

    /**
     * 新增接口日志
     */
    public int insertMiniappApiLog(MiniappApiLog miniappApiLog);

    /**
     * 删除接口日志
     */
    public int deleteMiniappApiLogByIds(Long[] logIds);

    /**
     * 清空接口日志
     */
    public int cleanMiniappApiLog();
}
