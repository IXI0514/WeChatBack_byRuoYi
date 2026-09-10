<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" v-show="showSearch">
      <el-form-item label="省份"><el-select v-model="queryParams.province" filterable clearable placeholder="请选择省份" @change="handleQueryProvinceChange"><el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" /></el-select></el-form-item>
      <el-form-item label="城市"><el-select v-model="queryParams.city" filterable clearable placeholder="请选择城市" :disabled="!queryParams.province" @change="setQueryCity"><el-option v-for="city in queryCityOptions" :key="city" :label="city" :value="city" /></el-select></el-form-item>
      <el-form-item label="名称/呼号"><el-input v-model="queryParams.repeaterName" clearable placeholder="请输入名称" @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="类型"><el-select v-model="queryParams.operationMode" clearable placeholder="请选择"><el-option label="模拟" value="ANALOG" /><el-option label="数字" value="DIGITAL" /><el-option label="混合" value="MIXED" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.status" clearable placeholder="请选择"><el-option label="正常" value="0" /><el-option label="维护" value="1" /><el-option label="停用" value="2" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button v-hasPermi="['miniapp:repeater:add']" type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="repeaterList">
      <el-table-column label="名称" prop="repeaterName" min-width="150" show-overflow-tooltip />
      <el-table-column label="呼号" prop="callSign" width="110" show-overflow-tooltip />
      <el-table-column label="地区" min-width="120"><template slot-scope="scope">{{ scope.row.province }} {{ scope.row.city }}</template></el-table-column>
      <el-table-column label="类型" prop="operationMode" width="82"><template slot-scope="scope">{{ modeText(scope.row.operationMode) }}</template></el-table-column>
      <el-table-column label="上行 MHz" prop="uplinkFrequencyMhz" width="112" />
      <el-table-column label="下行 MHz" prop="downlinkFrequencyMhz" width="112" />
      <el-table-column label="公开" prop="isPublic" width="70"><template slot-scope="scope"><el-tag :type="scope.row.isPublic === '0' ? 'success' : 'info'">{{ scope.row.isPublic === '0' ? '是' : '否' }}</el-tag></template></el-table-column>
      <el-table-column label="状态" width="76"><template slot-scope="scope"><el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="核验时间" prop="lastVerifiedAt" width="155" />
      <el-table-column label="操作" width="150" class-name="small-padding fixed-width"><template slot-scope="scope">
        <el-button v-hasPermi="['miniapp:repeater:edit']" type="text" size="mini" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
        <el-button v-hasPermi="['miniapp:repeater:edit']" type="text" size="mini" icon="el-icon-circle-close" @click="handleDisable(scope.row)" v-if="scope.row.status !== '2'">停用</el-button>
        <el-button v-hasPermi="['miniapp:repeater:remove']" type="text" size="mini" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
      </template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="title" :visible.sync="open" width="820px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="115px">
        <el-row><el-col :span="12"><el-form-item label="中继台名称" prop="repeaterName"><el-input v-model="form.repeaterName" /></el-form-item></el-col><el-col :span="12"><el-form-item label="呼号"><el-input v-model="form.callSign" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="省份" prop="province"><el-select v-model="form.province" filterable allow-create default-first-option placeholder="请选择省份" style="width:100%" @change="handleFormProvinceChange"><el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="城市" prop="city"><el-select v-model="form.city" filterable allow-create default-first-option placeholder="请先选择省份" :disabled="!form.province" style="width:100%" @change="setFormCity"><el-option v-for="city in formCityOptions" :key="city" :label="city" :value="city" /></el-select></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="类型" prop="operationMode"><el-select v-model="form.operationMode" style="width:100%"><el-option label="模拟" value="ANALOG" /><el-option label="数字" value="DIGITAL" /><el-option label="混合" value="MIXED" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="最后核验"><el-date-picker v-model="form.lastVerifiedAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="上行频率 MHz" prop="uplinkFrequencyMhz"><el-input-number v-model="form.uplinkFrequencyMhz" :precision="5" :step="0.0125" controls-position="right" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="下行频率 MHz" prop="downlinkFrequencyMhz"><el-input-number v-model="form.downlinkFrequencyMhz" :precision="5" :step="0.0125" controls-position="right" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio label="0">正常</el-radio><el-radio label="1">维护</el-radio><el-radio label="2">停用</el-radio></el-radio-group></el-form-item></el-col><el-col :span="12"><el-form-item label="小程序公开"><el-radio-group v-model="form.isPublic"><el-radio label="0">公开</el-radio><el-radio label="1">不公开</el-radio></el-radio-group></el-form-item></el-col></el-row>
        <el-divider content-position="left">射频配置</el-divider>
        <el-row><el-col :span="8"><el-form-item label="频段"><el-select v-model="detailConfig.rf.band" filterable allow-create style="width:100%"><el-option label="VHF" value="VHF" /><el-option label="UHF" value="UHF" /><el-option label="HF" value="HF" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="带宽 kHz"><el-input-number v-model="detailConfig.rf.bandwidthKhz" :min="0" :precision="3" :step="0.5" controls-position="right" style="width:100%" /></el-form-item></el-col><el-col :span="8"><el-form-item label="发射功率 W"><el-input-number v-model="detailConfig.rf.powerW" :min="0" :precision="1" :step="1" controls-position="right" style="width:100%" /></el-form-item></el-col></el-row>
        <template v-if="isAnalog">
          <el-divider content-position="left">模拟制式配置</el-divider>
          <el-row><el-col :span="8"><el-form-item label="调制方式"><el-select v-model="detailConfig.analog.modulation" style="width:100%"><el-option label="FM" value="FM" /><el-option label="AM" value="AM" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="接收静噪类型"><el-select v-model="detailConfig.analog.rxToneType" style="width:100%"><el-option label="无" value="NONE" /><el-option label="CTCSS" value="CTCSS" /><el-option label="DCS" value="DCS" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="接收亚音/DCS"><el-input v-model="detailConfig.analog.rxToneValue" :disabled="detailConfig.analog.rxToneType === 'NONE'" placeholder="如 88.5 或 D023N" /></el-form-item></el-col></el-row>
          <el-row><el-col :span="8"><el-form-item label="发射静噪类型"><el-select v-model="detailConfig.analog.txToneType" style="width:100%"><el-option label="无" value="NONE" /><el-option label="CTCSS" value="CTCSS" /><el-option label="DCS" value="DCS" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="发射亚音/DCS"><el-input v-model="detailConfig.analog.txToneValue" :disabled="detailConfig.analog.txToneType === 'NONE'" placeholder="如 88.5 或 D023N" /></el-form-item></el-col><el-col :span="8"><el-form-item label="尾音延时 ms"><el-input-number v-model="detailConfig.analog.hangTimeMs" :min="0" :precision="0" :step="100" controls-position="right" style="width:100%" /></el-form-item></el-col></el-row>
        </template>
        <template v-if="isDigital">
          <el-divider content-position="left">数字制式配置</el-divider>
          <el-row><el-col :span="8"><el-form-item label="数字协议"><el-select v-model="detailConfig.digital.protocol" filterable allow-create style="width:100%"><el-option label="DMR" value="DMR" /><el-option label="D-STAR" value="D-STAR" /><el-option label="C4FM" value="C4FM" /><el-option label="P25" value="P25" /><el-option label="NXDN" value="NXDN" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="DMR 色码"><el-input-number v-model="detailConfig.digital.colorCode" :min="0" :max="15" :precision="0" controls-position="right" style="width:100%" /></el-form-item></el-col><el-col :span="8"><el-form-item label="网络名称"><el-input v-model="detailConfig.digital.network" placeholder="选填" /></el-form-item></el-col></el-row>
          <el-row><el-col :span="12"><el-form-item label="时隙 1 通话组"><el-input v-model="detailConfig.digital.slot1Talkgroups" placeholder="多个通话组用英文逗号分隔" /></el-form-item></el-col><el-col :span="12"><el-form-item label="时隙 2 通话组"><el-input v-model="detailConfig.digital.slot2Talkgroups" placeholder="多个通话组用英文逗号分隔" /></el-form-item></el-col></el-row>
        </template>
        <el-alert title="页面会在保存时自动生成详细配置 JSON；请勿填写控制口令、DTMF 码、密钥或设备管理地址。" type="info" :closable="false" class="config-tip" />
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="open=false">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listRepeater, getRepeater, addRepeater, updateRepeater, delRepeater } from '@/api/miniapp/repeater'
import { provinceCities, provinceOptions } from '@/utils/regionOptions'
export default {
  name: 'MiniappRepeater',
  data() { return { loading: true, total: 0, repeaterList: [], showSearch: true, open: false, title: '', form: {}, provinceOptions, detailConfig: { rf: { band: '', bandwidthKhz: 12.5, powerW: undefined }, analog: { modulation: 'FM', rxToneType: 'NONE', rxToneValue: '', txToneType: 'NONE', txToneValue: '', hangTimeMs: undefined }, digital: { protocol: 'DMR', colorCode: undefined, network: '', slot1Talkgroups: '', slot2Talkgroups: '' } }, configExtras: {}, queryParams: { pageNum: 1, pageSize: 10, province: undefined, city: undefined, repeaterName: undefined, operationMode: undefined, status: undefined }, rules: { repeaterName: [{ required: true, message: '名称不能为空', trigger: 'blur' }], province: [{ required: true, message: '省份不能为空', trigger: 'blur' }], city: [{ required: true, message: '城市不能为空', trigger: 'blur' }], operationMode: [{ required: true, message: '请选择类型', trigger: 'change' }], uplinkFrequencyMhz: [{ required: true, message: '请填写上行频率', trigger: 'blur' }], downlinkFrequencyMhz: [{ required: true, message: '请填写下行频率', trigger: 'blur' }] } } },
  computed: {
    isAnalog() { return ['ANALOG', 'MIXED'].includes(this.form.operationMode) },
    isDigital() { return ['DIGITAL', 'MIXED'].includes(this.form.operationMode) },
    queryCityOptions() { return provinceCities[this.queryParams.province] || [] },
    formCityOptions() { return provinceCities[this.form.province] || [] }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listRepeater(this.queryParams).then(r => { this.repeaterList = r.rows; this.total = r.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    handleQueryProvinceChange() { this.$set(this.queryParams, 'city', undefined) },
    setQueryCity(value) { this.$set(this.queryParams, 'city', value) },
    handleFormProvinceChange() { this.$set(this.form, 'city', undefined) },
    setFormCity(value) { this.$set(this.form, 'city', value) },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    emptyDetailConfig() { return { rf: { band: '', bandwidthKhz: 12.5, powerW: undefined }, analog: { modulation: 'FM', rxToneType: 'NONE', rxToneValue: '', txToneType: 'NONE', txToneValue: '', hangTimeMs: undefined }, digital: { protocol: 'DMR', colorCode: undefined, network: '', slot1Talkgroups: '', slot2Talkgroups: '' } } },
    reset() { this.form = { province: undefined, city: undefined, status: '0', isPublic: '0', operationMode: 'ANALOG' }; this.detailConfig = this.emptyDetailConfig(); this.configExtras = {} },
    handleAdd() { this.reset(); this.title = '新增中继台'; this.open = true },
    parseRadioConfig(raw) { try { return typeof raw === 'string' ? JSON.parse(raw || '{}') : (raw || {}) } catch (e) { return {} } },
    toneValue(tone) { if (!tone) return ''; return tone.ctcss_hz !== undefined ? String(tone.ctcss_hz) : (tone.dcs_code || '') },
    talkgroups(slots, slot) { const current = (slots || []).find(item => Number(item.slot) === slot); return current && current.talkgroups ? current.talkgroups.join(',') : '' },
    hydrateDetailConfig(raw) { const config = this.parseRadioConfig(raw); const detail = this.emptyDetailConfig(); const rf = config.rf || {}; const analog = config.analog || {}; const digital = config.digital || {}; detail.rf = { band: rf.band || '', bandwidthKhz: rf.bandwidth_khz, powerW: rf.power_w }; detail.analog = { modulation: analog.modulation || 'FM', rxToneType: (analog.rx_tone && analog.rx_tone.type) || 'NONE', rxToneValue: this.toneValue(analog.rx_tone), txToneType: (analog.tx_tone && analog.tx_tone.type) || 'NONE', txToneValue: this.toneValue(analog.tx_tone), hangTimeMs: analog.hang_time_ms }; detail.digital = { protocol: digital.protocol || 'DMR', colorCode: digital.color_code, network: digital.network || '', slot1Talkgroups: this.talkgroups(digital.slots, 1), slot2Talkgroups: this.talkgroups(digital.slots, 2) }; delete config.rf; delete config.analog; delete config.digital; this.configExtras = config; this.detailConfig = detail },
    handleEdit(row) { getRepeater(row.repeaterId).then(r => { this.form = r.data; this.hydrateDetailConfig(r.data.radioConfig); this.title = '编辑中继台'; this.open = true }) },
    compact(value) { if (Array.isArray(value)) return value.map(item => this.compact(item)).filter(item => item !== undefined); if (value && typeof value === 'object') { return Object.keys(value).reduce((result, key) => { const compacted = this.compact(value[key]); if (compacted !== undefined) result[key] = compacted; return result }, {}) } return value === '' || value === null || value === undefined ? undefined : value },
    buildTone(type, value) { if (!type || type === 'NONE') return { type: 'NONE' }; const tone = { type }; if (value !== '' && value !== null && value !== undefined) tone[type === 'CTCSS' ? 'ctcss_hz' : 'dcs_code'] = type === 'CTCSS' && !isNaN(Number(value)) ? Number(value) : value; return tone },
    parseTalkgroups(value) { return (value || '').split(/[,，\s]+/).filter(Boolean).map(item => /^\d+$/.test(item) ? Number(item) : item) },
    buildRadioConfig() { const config = JSON.parse(JSON.stringify(this.configExtras || {})); config.rf = this.compact({ band: this.detailConfig.rf.band, bandwidth_khz: this.detailConfig.rf.bandwidthKhz, power_w: this.detailConfig.rf.powerW }); if (this.isAnalog) config.analog = this.compact({ modulation: this.detailConfig.analog.modulation, rx_tone: this.buildTone(this.detailConfig.analog.rxToneType, this.detailConfig.analog.rxToneValue), tx_tone: this.buildTone(this.detailConfig.analog.txToneType, this.detailConfig.analog.txToneValue), hang_time_ms: this.detailConfig.analog.hangTimeMs }); if (this.isDigital) config.digital = this.compact({ protocol: this.detailConfig.digital.protocol, color_code: this.detailConfig.digital.colorCode, network: this.detailConfig.digital.network, slots: [{ slot: 1, talkgroups: this.parseTalkgroups(this.detailConfig.digital.slot1Talkgroups) }, { slot: 2, talkgroups: this.parseTalkgroups(this.detailConfig.digital.slot2Talkgroups) }] }); return JSON.stringify(this.compact(config)) },
    submitForm() { this.$refs.form.validate(valid => { if (!valid) return; const payload = { ...this.form, radioConfig: this.buildRadioConfig() }; const api = payload.repeaterId ? updateRepeater : addRepeater; api(payload).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.getList() }) }) },
    handleDisable(row) { this.$modal.confirm('确认停用“' + row.repeaterName + '”吗？').then(() => updateRepeater({ ...row, status: '2' })).then(() => { this.$modal.msgSuccess('已停用'); this.getList() }).catch(() => {}) },
    handleDelete(row) { this.$modal.confirm('确认删除“' + row.repeaterName + '”吗？').then(() => delRepeater(row.repeaterId)).then(() => { this.$modal.msgSuccess('删除成功'); this.getList() }).catch(() => {}) },
    modeText(v) { return { ANALOG: '模拟', DIGITAL: '数字', MIXED: '混合' }[v] || v },
    statusText(v) { return { '0': '正常', '1': '维护', '2': '停用' }[v] || v },
    statusType(v) { return { '0': 'success', '1': 'warning', '2': 'danger' }[v] || 'info' }
  }
}
</script>
<style scoped>
.config-tip { margin: 4px 0 18px 0; }
</style>
