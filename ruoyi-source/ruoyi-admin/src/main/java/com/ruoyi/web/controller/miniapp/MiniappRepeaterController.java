package com.ruoyi.web.controller.miniapp;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.MiniappRepeater;
import com.ruoyi.system.service.IMiniappRepeaterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "小程序中继台")
@RestController
@RequestMapping("/miniapp/repeater")
public class MiniappRepeaterController extends BaseController
{
    @Autowired
    private IMiniappRepeaterService miniappRepeaterService;

    @ApiOperation("中继台列表")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:list')")
    @GetMapping("/list")
    public TableDataInfo list(MiniappRepeater repeater)
    {
        startPage();
        return getDataTable(miniappRepeaterService.selectMiniappRepeaterList(repeater));
    }

    @ApiOperation("中继台详情")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:list')")
    @GetMapping("/{repeaterId}")
    public AjaxResult getInfo(@PathVariable Long repeaterId)
    {
        return AjaxResult.success(miniappRepeaterService.selectMiniappRepeaterById(repeaterId));
    }

    @ApiOperation("新增中继台")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:add')")
    @Log(title = "中继台管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MiniappRepeater repeater)
    {
        repeater.setCreateBy(getUsername());
        return toAjax(miniappRepeaterService.insertMiniappRepeater(repeater));
    }

    @ApiOperation("修改中继台")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:edit')")
    @Log(title = "中继台管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MiniappRepeater repeater)
    {
        repeater.setUpdateBy(getUsername());
        return toAjax(miniappRepeaterService.updateMiniappRepeater(repeater));
    }

    @ApiOperation("删除中继台")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:remove')")
    @Log(title = "中继台管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{repeaterIds}")
    public AjaxResult remove(@PathVariable Long[] repeaterIds)
    {
        return toAjax(miniappRepeaterService.deleteMiniappRepeaterByIds(repeaterIds));
    }

    @Anonymous
    @ApiOperation("小程序公开中继台列表")
    @GetMapping("/public/list")
    public AjaxResult publicList(MiniappRepeater repeater)
    {
        repeater.setStatus("0");
        repeater.setIsPublic("0");
        List<MiniappRepeater> list = miniappRepeaterService.selectMiniappRepeaterList(repeater);
        return AjaxResult.success(list);
    }
}
