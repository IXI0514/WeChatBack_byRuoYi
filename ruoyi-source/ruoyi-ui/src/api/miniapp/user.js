import request from '@/utils/request'

// 查询小程序用户列表
export function listUser(query) {
  return request({
    url: '/miniapp/user/list',
    method: 'get',
    params: query
  })
}

// 查询用户详情
export function getUser(userId) {
  return request({
    url: '/miniapp/user/' + userId,
    method: 'get'
  })
}

// 修改用户
export function updateUser(data) {
  return request({
    url: '/miniapp/user',
    method: 'put',
    data: data
  })
}

// 修改会员状态
export function updateMemberStatus(data) {
  return request({
    url: '/miniapp/user/member',
    method: 'put',
    data: data
  })
}
