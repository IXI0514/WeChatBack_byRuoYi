package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MiniappUser;
import com.ruoyi.system.mapper.MiniappUserMapper;
import com.ruoyi.system.service.IMiniappUserService;

/**
 * 小程序用户 服务实现
 *
 * @author ruoyi
 */
@Service
public class MiniappUserServiceImpl implements IMiniappUserService
{
    @Autowired
    private MiniappUserMapper miniappUserMapper;

    @Override
    public MiniappUser selectByMiniappAndOpenid(String miniappId, String openid)
    {
        return miniappUserMapper.selectByMiniappAndOpenid(miniappId, openid);
    }

    @Override
    public List<MiniappUser> selectMiniappUserList(MiniappUser miniappUser)
    {
        return miniappUserMapper.selectMiniappUserList(miniappUser);
    }

    @Override
    public MiniappUser selectMiniappUserById(Long userId)
    {
        return miniappUserMapper.selectMiniappUserById(userId);
    }

    @Override
    public int insertMiniappUser(MiniappUser miniappUser)
    {
        return miniappUserMapper.insertMiniappUser(miniappUser);
    }

    @Override
    public int updateMiniappUser(MiniappUser miniappUser)
    {
        return miniappUserMapper.updateMiniappUser(miniappUser);
    }

    @Override
    public int updateMemberStatus(MiniappUser miniappUser)
    {
        return miniappUserMapper.updateMemberStatus(miniappUser);
    }
}
