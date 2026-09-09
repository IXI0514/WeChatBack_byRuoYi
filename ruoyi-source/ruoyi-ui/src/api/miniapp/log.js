import request from '@/utils/request'

// 查询接口日志列表
export function listLog(query) {
  return request({
    url: '/miniapp/log/list',
    method: 'get',
    params: query
  })
}

// 删除接口日志
export function delLog(logIds) {
  return request({
    url: '/miniapp/log/' + logIds,
    method: 'delete'
  })
}

// 清空接口日志
export function cleanLog() {
  return request({
    url: '/miniapp/log/clean',
    method: 'delete'
  })
}
