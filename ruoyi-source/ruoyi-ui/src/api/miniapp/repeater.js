import request from '@/utils/request'

export function listRepeater(query) { return request({ url: '/system/miniapp/repeater/list', method: 'get', params: query }) }
export function getRepeater(id) { return request({ url: '/system/miniapp/repeater/' + id, method: 'get' }) }
export function addRepeater(data) { return request({ url: '/system/miniapp/repeater', method: 'post', data }) }
export function updateRepeater(data) { return request({ url: '/system/miniapp/repeater', method: 'put', data }) }
export function delRepeater(id) { return request({ url: '/system/miniapp/repeater/' + id, method: 'delete' }) }
