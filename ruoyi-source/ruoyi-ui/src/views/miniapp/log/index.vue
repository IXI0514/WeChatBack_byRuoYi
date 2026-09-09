<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="日志类型" prop="type">
        <el-input v-model="queryParams.type" placeholder="接口日志" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="接口名称" prop="des">
        <el-input v-model="queryParams.des" placeholder="请输入" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="请求结果" prop="result">
        <el-select v-model="queryParams.result" placeholder="请选择" clearable style="width: 100px">
          <el-option label="成功" value="成功" />
          <el-option label="失败" value="失败" />
          <el-option label="拒绝" value="拒绝" />
        </el-select>
      </el-form-item>
      <el-form-item label="小程序标识" prop="miniappId">
        <el-input v-model="queryParams.miniappId" placeholder="请输入" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="openid" prop="openid">
        <el-input v-model="queryParams.openid" placeholder="请输入" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="请求IP" prop="ip">
        <el-input v-model="queryParams.ip" placeholder="请输入" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker v-model="dateRange" style="width: 240px" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" @click="handleClean" v-hasPermi="['miniapp:log:remove']">清空</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['miniapp:log:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="日志ID" align="center" prop="logId" width="76" />
      <el-table-column label="类型" align="center" prop="type" width="88" :show-overflow-tooltip="true" />
      <el-table-column label="接口名称" align="center" prop="des" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="结果" align="center" prop="result" width="70">
        <template slot-scope="scope">
          <el-tag :type="resultTagType(scope.row.result)">{{ scope.row.result }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入参" align="center" width="62">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewDetails(scope.row)">查看</el-button>
        </template>
      </el-table-column>
      <el-table-column label="小程序标识" align="center" prop="miniappId" width="105" :show-overflow-tooltip="true" />
      <el-table-column label="openid" align="center" prop="openid" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="请求URL" align="center" prop="reqUrl" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="耗时(ms)" align="center" prop="respTime" width="72" />
      <el-table-column label="IP" align="center" prop="ip" width="105" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="155" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="72" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['miniapp:log:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="入参详情" :visible.sync="detailsOpen" width="700px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="接口名称">
          <span>{{ detailsRow.des }}</span>
        </el-form-item>
        <el-form-item label="请求结果">
          <el-tag :type="resultTagType(detailsRow.result)">{{ detailsRow.result }}</el-tag>
        </el-form-item>
        <el-form-item label="请求URL">
          <span>{{ detailsRow.reqUrl }}</span>
        </el-form-item>
        <el-form-item label="响应耗时">
          <span>{{ detailsRow.respTime }} ms</span>
        </el-form-item>
        <el-form-item label="请求IP">
          <span>{{ detailsRow.ip }}</span>
        </el-form-item>
        <el-form-item label="入参记录">
          <el-input type="textarea" :value="formatDetails(detailsRow.details)" :rows="10" readonly />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import { listLog, delLog, cleanLog } from "@/api/miniapp/log";

export default {
  name: "MiniappApiLog",
  data() {
    return {
      loading: true,
      total: 0,
      logList: [],
      showSearch: true,
      multiple: true,
      ids: [],
      dateRange: [],
      detailsOpen: false,
      detailsRow: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        type: undefined,
        des: undefined,
        result: undefined,
        miniappId: undefined,
        openid: undefined,
        ip: undefined
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listLog(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.logList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.logId);
      this.multiple = !selection.length;
    },
    handleViewDetails(row) {
      this.detailsRow = row;
      this.detailsOpen = true;
    },
    formatDetails(details) {
      if (!details) return "";
      try {
        return JSON.stringify(JSON.parse(details), null, 2);
      } catch (e) {
        return details;
      }
    },
    resultTagType(result) {
      if (result === "成功") return "success";
      if (result === "失败") return "danger";
      if (result === "拒绝") return "warning";
      return "info";
    },
    handleDelete(row) {
      const logIds = row.logId || this.ids;
      this.$modal.confirm('是否确认删除日志编号为"' + logIds + '"的数据项？').then(function () {
        return delLog(logIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleClean() {
      this.$modal.confirm('是否确认清空所有接口日志？').then(function () {
        return cleanLog();
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("清空成功");
      }).catch(() => {});
    }
  }
};
</script>
