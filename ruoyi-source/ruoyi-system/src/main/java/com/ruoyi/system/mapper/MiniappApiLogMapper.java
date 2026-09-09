package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.MiniappApiLog;

/**
 * 接口日志 数据层
 *
 * @author ruoyi
 */
public interface MiniappApiLogMapper
{
    /**
     * 查询接口日志列表
     *
     * @param miniappApiLog 日志信息
     * @return 日志集合
     */
    public List<MiniappApiLog> selectMiniappApiLogList(MiniappApiLog miniappApiLog);

    /**
     * 新增接口日志
     *
     * @param miniappApiLog 日志信息
     * @return 影响行数
     */
    public int insertMiniappApiLog(MiniappApiLog miniappApiLog);

    /**
     * 通过日志ID删除接口日志
     *
     * @param logIds 日志ID集合
     * @return 影响行数
     */
    public int deleteMiniappApiLogByIds(Long[] logIds);

    /**
     * 清空接口日志
     *
     * @return 影响行数
     */
    public int cleanMiniappApiLog();
}
