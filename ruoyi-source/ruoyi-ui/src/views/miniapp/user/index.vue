<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="小程序标识" prop="miniappId">
        <el-input v-model="queryParams.miniappId" placeholder="请输入" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="openid" prop="openid">
        <el-input v-model="queryParams.openid" placeholder="请输入" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="微信id" prop="wxId">
        <el-input v-model="queryParams.wxId" placeholder="请输入" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="请输入" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="是否会员" prop="isMember">
        <el-select v-model="queryParams.isMember" placeholder="请选择" clearable style="width: 100px">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 100px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['miniapp:user:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userList">
      <el-table-column label="用户ID" align="center" prop="userId" width="76" />
      <el-table-column label="小程序标识" align="center" prop="miniappId" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="openid" align="center" prop="openid" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="微信id" align="center" prop="wxId" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="昵称" align="center" prop="nickname" min-width="80" :show-overflow-tooltip="true" />
      <el-table-column label="是否会员" align="center" prop="isMember" width="84">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isMember === 1 ? 'success' : 'info'">{{ scope.row.isMember === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="会员到期" align="center" prop="memberExpire" width="155" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="76">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="155" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="72" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['miniapp:user:edit']">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="修改用户" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="editForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户ID">
          <span>{{ form.userId }}</span>
        </el-form-item>
        <el-form-item label="小程序标识">
          <span>{{ form.miniappId }}</span>
        </el-form-item>
        <el-form-item label="openid">
          <span>{{ form.openid }}</span>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="是否会员" prop="isMember">
          <el-radio-group v-model="form.isMember">
            <el-radio :label="0">否</el-radio>
            <el-radio :label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="会员到期" prop="memberExpire" v-show="form.isMember === 1">
          <el-date-picker v-model="form.memberExpire" type="datetime" placeholder="选择到期时间" value-format="yyyy-MM-dd HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getUser, listUser, updateUser } from "@/api/miniapp/user";

export default {
  name: "MiniappUser",
  data() {
    return {
      loading: true,
      total: 0,
      userList: [],
      showSearch: true,
      open: false,
      form: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        miniappId: undefined,
        openid: undefined,
        wxId: undefined,
        nickname: undefined,
        isMember: undefined,
        status: undefined
      },
      rules: {
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listUser(this.queryParams).then(response => {
        this.userList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleEdit(row) {
      getUser(row.userId).then(response => {
        this.form = response.data;
        this.open = true;
      });
    },
    submitForm() {
      this.$refs["editForm"].validate(valid => {
        if (valid) {
          updateUser(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.open = false;
            this.getList();
          });
        }
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {};
    },
    handleExport() {
      this.download('miniapp/user/export', { ...this.queryParams }, `miniapp_user_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>
