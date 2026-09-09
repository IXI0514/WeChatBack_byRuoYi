package com.ruoyi.web.controller.miniapp;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.MiniappApiLog;
import com.ruoyi.system.service.IMiniappApiLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 接口日志查看(后台管理,需登录)
 *
 * @author ruoyi
 */
@Api(tags = "接口日志")
@RestController
@RequestMapping("/miniapp/log")
public class MiniappApiLogController extends BaseController
{
    @Autowired
    private IMiniappApiLogService miniappApiLogService;

    /**
     * 查询接口日志列表
     */
    @ApiOperation("日志列表")
    @GetMapping("/list")
    public TableDataInfo list(MiniappApiLog miniappApiLog)
    {
        startPage();
        List<MiniappApiLog> list = miniappApiLogService.selectMiniappApiLogList(miniappApiLog);
        return getDataTable(list);
    }

    /**
     * 删除接口日志
     */
    @ApiOperation("删除日志")
    @Log(title = "接口日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(miniappApiLogService.deleteMiniappApiLogByIds(logIds));
    }

    /**
     * 清空接口日志
     */
    @ApiOperation("清空日志")
    @Log(title = "接口日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        return toAjax(miniappApiLogService.cleanMiniappApiLog());
    }
}
