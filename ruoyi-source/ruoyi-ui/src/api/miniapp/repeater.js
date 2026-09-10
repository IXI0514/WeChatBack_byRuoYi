import request from '@/utils/request'

export function listRepeater(query) { return request({ url: '/miniapp/repeater/list', method: 'get', params: query }) }
export function getRepeater(id) { return request({ url: '/miniapp/repeater/' + id, method: 'get' }) }
export function addRepeater(data) { return request({ url: '/miniapp/repeater', method: 'post', data }) }
export function updateRepeater(data) { return request({ url: '/miniapp/repeater', method: 'put', data }) }
export function delRepeater(id) { return request({ url: '/miniapp/repeater/' + id, method: 'delete' }) }
