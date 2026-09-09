package com.ruoyi.web.controller.miniapp;

import java.util.List;
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

/**
 * 小程序用户管理(后台管理,需登录)
 *
 * @author ruoyi
 */
@Api(tags = "小程序用户管理")
@RestController
@RequestMapping("/miniapp/user")
public class MiniappUserController extends BaseController
{
    @Autowired
    private IMiniappUserService miniappUserService;

    /**
     * 查询小程序用户列表
     */
    @ApiOperation("用户列表")
    @GetMapping("/list")
    public TableDataInfo list(MiniappUser miniappUser)
    {
        startPage();
        List<MiniappUser> list = miniappUserService.selectMiniappUserList(miniappUser);
        return getDataTable(list);
    }

    /**
     * 获取用户详情
     */
    @ApiOperation("用户详情")
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId)
    {
        return AjaxResult.success(miniappUserService.selectMiniappUserById(userId));
    }

    /**
     * 修改用户信息(含会员状态、会员到期时间)
     */
    @ApiOperation("修改用户")
    @Log(title = "小程序用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MiniappUser miniappUser)
    {
        return toAjax(miniappUserService.updateMiniappUser(miniappUser));
    }

    /**
     * 修改会员状态(快捷修改是否会员+到期时间)
     */
    @ApiOperation("修改会员状态")
    @Log(title = "小程序会员状态", businessType = BusinessType.UPDATE)
    @PutMapping("/member")
    public AjaxResult updateMemberStatus(@RequestBody MiniappUser miniappUser)
    {
        return toAjax(miniappUserService.updateMemberStatus(miniappUser));
    }
}
