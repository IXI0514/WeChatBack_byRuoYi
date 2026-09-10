<template>
  <div class="welcome-page">
    <section class="welcome-card" aria-labelledby="welcome-title">
      <div class="welcome-copy">
        <p class="welcome-kicker">ARCHIVE MANAGEMENT</p>
        <h1 id="welcome-title">欢迎回来</h1>
        <p class="welcome-subtitle">管理后台</p>
        <p class="welcome-description">在这里处理档案、用户与系统配置。</p>
      </div>
      <div class="welcome-visual" aria-hidden="true">
        <img src="@/assets/images/login-archive-background.png" alt="">
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'Welcome'
}
</script>

<style lang="scss" scoped>
.welcome-page {
  min-height: calc(100vh - 84px);
  box-sizing: border-box;
  padding: 32px;
  background: #f4f6fb;
}

.welcome-card {
  position: relative;
  min-height: min(560px, calc(100vh - 148px));
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 72px clamp(38px, 7vw, 120px);
  border: 1px solid #e7ebf3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 16px 42px rgba(33, 47, 78, 0.08);
}

.welcome-copy {
  position: relative;
  z-index: 1;
  max-width: 420px;
}

.welcome-kicker {
  margin: 0 0 20px;
  color: #78839a;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.16em;
}

h1 {
  margin: 0;
  color: #17264a;
  font-size: clamp(42px, 5vw, 68px);
  font-weight: 600;
  letter-spacing: 0.04em;
  line-height: 1.12;
}

.welcome-subtitle {
  margin: 16px 0 0;
  color: #60708e;
  font-size: 22px;
  letter-spacing: 0.16em;
}

.welcome-description {
  margin: 36px 0 0;
  color: #8a94a8;
  font-size: 15px;
  line-height: 1.8;
}

.welcome-visual {
  position: absolute;
  top: 0;
  right: 0;
  width: min(56%, 850px);
  height: 100%;
  overflow: hidden;
  background: #1c2a50;
}

.welcome-visual::after {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(90deg, #fff 0%, rgba(255, 255, 255, 0.92) 3%, rgba(255, 255, 255, 0) 24%);
}

.welcome-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center;
}

@media (max-width: 900px) {
  .welcome-page {
    padding: 20px;
  }

  .welcome-card {
    min-height: 520px;
    align-items: flex-start;
    padding: 52px 34px;
  }

  .welcome-visual {
    top: auto;
    bottom: 0;
    width: 100%;
    height: 48%;
  }

  .welcome-visual::after {
    background: linear-gradient(180deg, #fff 0%, rgba(255, 255, 255, 0) 30%);
  }
}

@media (max-width: 560px) {
  .welcome-page {
    padding: 12px;
  }

  .welcome-card {
    min-height: calc(100vh - 108px);
    padding: 42px 26px;
    border-radius: 12px;
  }

  .welcome-kicker {
    margin-bottom: 14px;
    font-size: 10px;
  }

  .welcome-subtitle {
    font-size: 18px;
  }

  .welcome-description {
    margin-top: 24px;
    font-size: 14px;
  }
}
</style>
