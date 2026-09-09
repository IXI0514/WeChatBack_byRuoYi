package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.MiniappUser;

/**
 * 小程序用户 服务层
 *
 * @author ruoyi
 */
public interface IMiniappUserService
{
    /**
     * 根据小程序标识和openid查询用户(存在则返回,不存在返回null)
     */
    public MiniappUser selectByMiniappAndOpenid(String miniappId, String openid);

    /**
     * 查询小程序用户列表
     */
    public List<MiniappUser> selectMiniappUserList(MiniappUser miniappUser);

    /**
     * 通过ID查询小程序用户
     */
    public MiniappUser selectMiniappUserById(Long userId);

    /**
     * 新增小程序用户
     */
    public int insertMiniappUser(MiniappUser miniappUser);

    /**
     * 修改小程序用户
     */
    public int updateMiniappUser(MiniappUser miniappUser);

    /**
     * 修改会员状态(是否会员/到期时间)
     */
    public int updateMemberStatus(MiniappUser miniappUser);
}
