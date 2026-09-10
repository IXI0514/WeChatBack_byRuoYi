<template>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="login-heading">
        <h1>账号登录</h1>
        <p>请输入账号信息以继续访问管理后台</p>
      </div>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item v-if="captchaEnabled" prop="code" class="captcha-row">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <button type="button" class="login-code" aria-label="刷新验证码" @click="getCode">
          <img :src="codeUrl" alt="验证码，点击刷新" class="login-code-img" />
        </button>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" class="remember-me">记住密码</el-checkbox>
      <el-form-item class="login-submit">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登录</span>
          <span v-else>正在登录...</span>
        </el-button>
        <div v-if="register" class="register-link">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? "" : username,
        password: password === undefined ? "" : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
        code: "",
        uuid: this.loginForm.uuid
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-height: 100vh;
  box-sizing: border-box;
  padding: 48px clamp(32px, 10vw, 180px) 48px 32px;
  background: #0d213d url("../assets/images/login-archive-background.png") center center / cover no-repeat;
}

.login-form {
  width: 420px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.48);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 56px rgba(1, 13, 31, 0.3);
  padding: 40px 38px 22px;
  z-index: 1;

  .el-form-item {
    margin-bottom: 20px;
  }

  .el-input {
    height: 46px;
  }
}

.login-form ::v-deep .el-input__inner {
  height: 46px;
  border: 1px solid #d9e0ea;
  border-radius: 8px;
  color: #1a2c44;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus {
    border-color: #315f92;
    box-shadow: 0 0 0 3px rgba(49, 95, 146, 0.12);
  }
}

.login-form {
  .input-icon {
    height: 46px;
    width: 16px;
    margin-left: 4px;
    color: #698099;
  }
}

.login-heading {
  margin-bottom: 34px;

  h1 {
    margin: 0 0 10px;
    color: #142942;
    font-size: 26px;
    font-weight: 600;
    letter-spacing: 1px;
    line-height: 1.25;
  }

  p {
    margin: 0;
    color: #76879a;
    font-size: 14px;
    line-height: 1.6;
  }
}

.captcha-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;

  .el-input {
    flex: 1;
  }
}

.login-code {
  flex: 0 0 116px;
  height: 46px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #d9e0ea;
  border-radius: 8px;
  background: #f4f7fa;
  cursor: pointer;

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.remember-me {
  margin: 0 0 24px;
  width: 100%;
  color: #68798d;
  font-size: 14px;
}

.login-submit {
  margin-bottom: 0 !important;

  .el-button {
    width: 100%;
    height: 46px;
    border: 0;
    border-radius: 8px;
    background: #234f81;
    box-shadow: 0 8px 16px rgba(35, 79, 129, 0.2);
    font-size: 15px;
    font-weight: 500;
    letter-spacing: 2px;

    &:hover,
    &:focus {
      background: #183e69;
    }
  }
}

.register-link {
  margin-top: 18px;
  text-align: right;
}

@media (max-width: 768px) {
  .login {
    justify-content: center;
    padding: 24px;
    background-position: 28% center;
  }

  .login-form {
    width: min(420px, 100%);
    padding: 34px 26px 20px;
  }
}
</style>
