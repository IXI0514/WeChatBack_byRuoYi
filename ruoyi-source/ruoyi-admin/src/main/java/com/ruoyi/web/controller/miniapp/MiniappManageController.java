package com.ruoyi.web.controller.miniapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.MiniappUser;
import com.ruoyi.system.service.IMiniappUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.ruoyi.system.domain.MiniappApiLog;
import com.ruoyi.system.service.IMiniappApiLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ruoyi.system.domain.MiniappRepeater;
import com.ruoyi.system.service.IMiniappRepeaterService;
import java.util.List;

/** 系统后台的小程序管理入口：用户、会员、接口日志和中继台。 */
@Api(tags = "后台-小程序管理")
@RestController
@RequestMapping("/system/miniapp")
public class MiniappManageController extends BaseController
{
    // 用户与会员管理
    @Autowired
    private IMiniappUserService miniappUserService;

    /**
     * 查询小程序用户列表
     */
    @ApiOperation("用户列表")
    @PreAuthorize("@ss.hasPermi('miniapp:user:list')")
    @GetMapping("/user/list")
    public TableDataInfo listUser(MiniappUser miniappUser)
    {
        startPage();
        List<MiniappUser> list = miniappUserService.selectMiniappUserList(miniappUser);
        return getDataTable(list);
    }

    /**
     * 获取用户详情
     */
    @ApiOperation("用户详情")
    @PreAuthorize("@ss.hasPermi('miniapp:user:list')")
    @GetMapping("/user/{userId}")
    public AjaxResult getInfoUser(@PathVariable Long userId)
    {
        return AjaxResult.success(miniappUserService.selectMiniappUserById(userId));
    }

    /**
     * 修改用户信息(含会员状态、会员到期时间)
     */
    @ApiOperation("修改用户")
    @Log(title = "小程序用户管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('miniapp:user:edit')")
    @PutMapping("/user")
    public AjaxResult editUser(@RequestBody MiniappUser miniappUser)
    {
        return toAjax(miniappUserService.updateMiniappUser(miniappUser));
    }

    /**
     * 修改会员状态(快捷修改是否会员+到期时间)
     */
    @ApiOperation("修改会员状态")
    @Log(title = "小程序会员状态", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('miniapp:user:edit')")
    @PutMapping("/user/member")
    public AjaxResult updateMemberStatus(@RequestBody MiniappUser miniappUser)
    {
        return toAjax(miniappUserService.updateMemberStatus(miniappUser));
    }

    // 接口日志管理
    @Autowired
    private IMiniappApiLogService miniappApiLogService;

    /**
     * 查询接口日志列表
     */
    @ApiOperation("日志列表")
    @PreAuthorize("@ss.hasPermi('miniapp:log:list')")
    @GetMapping("/log/list")
    public TableDataInfo listLog(MiniappApiLog miniappApiLog)
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
    @PreAuthorize("@ss.hasPermi('miniapp:log:remove')")
    @DeleteMapping("/log/{logIds}")
    public AjaxResult removeLog(@PathVariable Long[] logIds)
    {
        return toAjax(miniappApiLogService.deleteMiniappApiLogByIds(logIds));
    }

    /**
     * 清空接口日志
     */
    @ApiOperation("清空日志")
    @Log(title = "接口日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('miniapp:log:remove')")
    @DeleteMapping("/log/clean")
    public AjaxResult cleanLog()
    {
        return toAjax(miniappApiLogService.cleanMiniappApiLog());
    }

    // 中继台管理
    @Autowired
    private IMiniappRepeaterService miniappRepeaterService;

    @ApiOperation("中继台列表")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:list')")
    @GetMapping("/repeater/list")
    public TableDataInfo listRepeater(MiniappRepeater repeater)
    {
        startPage();
        return getDataTable(miniappRepeaterService.selectMiniappRepeaterList(repeater));
    }

    @ApiOperation("中继台详情")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:list')")
    @GetMapping("/repeater/{repeaterId}")
    public AjaxResult getInfoRepeater(@PathVariable Long repeaterId)
    {
        return AjaxResult.success(miniappRepeaterService.selectMiniappRepeaterById(repeaterId));
    }

    @ApiOperation("新增中继台")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:add')")
    @Log(title = "中继台管理", businessType = BusinessType.INSERT)
    @PostMapping("/repeater")
    public AjaxResult addRepeater(@RequestBody MiniappRepeater repeater)
    {
        repeater.setCreateBy(getUsername());
        return toAjax(miniappRepeaterService.insertMiniappRepeater(repeater));
    }

    @ApiOperation("修改中继台")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:edit')")
    @Log(title = "中继台管理", businessType = BusinessType.UPDATE)
    @PutMapping("/repeater")
    public AjaxResult editRepeater(@RequestBody MiniappRepeater repeater)
    {
        repeater.setUpdateBy(getUsername());
        return toAjax(miniappRepeaterService.updateMiniappRepeater(repeater));
    }

    @ApiOperation("删除中继台")
    @PreAuthorize("@ss.hasPermi('miniapp:repeater:remove')")
    @Log(title = "中继台管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/repeater/{repeaterIds}")
    public AjaxResult removeRepeater(@PathVariable Long[] repeaterIds)
    {
        return toAjax(miniappRepeaterService.deleteMiniappRepeaterByIds(repeaterIds));
    }


}
