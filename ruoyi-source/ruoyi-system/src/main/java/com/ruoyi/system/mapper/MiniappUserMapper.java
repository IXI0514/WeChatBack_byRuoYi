package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.MiniappUser;

/**
 * 小程序用户 数据层
 *
 * @author ruoyi
 */
public interface MiniappUserMapper
{
    /**
     * 根据小程序标识和openid查询用户
     *
     * @param miniappId 小程序标识
     * @param openid 用户openid
     * @return 用户信息
     */
    public MiniappUser selectByMiniappAndOpenid(@org.apache.ibatis.annotations.Param("miniappId") String miniappId,
                                                @org.apache.ibatis.annotations.Param("openid") String openid);

    /**
     * 查询小程序用户列表
     *
     * @param miniappUser 用户信息
     * @return 用户集合
     */
    public List<MiniappUser> selectMiniappUserList(MiniappUser miniappUser);

    /**
     * 通过用户ID查询小程序用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public MiniappUser selectMiniappUserById(Long userId);

    /**
     * 新增小程序用户
     *
     * @param miniappUser 用户信息
     * @return 影响行数
     */
    public int insertMiniappUser(MiniappUser miniappUser);

    /**
     * 修改小程序用户
     *
     * @param miniappUser 用户信息
     * @return 影响行数
     */
    public int updateMiniappUser(MiniappUser miniappUser);

    /**
     * 修改会员状态
     *
     * @param miniappUser 用户信息
     * @return 影响行数
     */
    public int updateMemberStatus(MiniappUser miniappUser);
}
