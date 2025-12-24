<template>
  <div class="layout-container">
    <el-container class="main-container">
      <el-aside width="220px" class="main-aside">
        
        <div class="brand-area">
          <div class="logo-text">产业知识</div>
          <div class="sub-text">智能问答工具</div>
        </div>

        <el-menu 
          :default-active="$route.path" 
          router 
          class="side-menu"
          background-color="#001529" 
          text-color="#b0b8bf" 
          active-text-color="#fff"
        >
          <el-menu-item index="/chat">
            <el-icon><ChatLineRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Files /></el-icon>
            <span>知识库管理</span>
          </el-menu-item>
          <el-menu-item v-if="userRole === 'admin'" index="/admin">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>

        <div class="aside-footer">
          <div class="user-card">
            <el-avatar :size="32" icon="UserFilled" class="user-avatar" />
            <div class="user-info">
              <div class="u-name" :title="username">{{ username }}</div>
              <div class="u-role">{{ roleDisplayName }}</div>
            </div>
            
            <el-dropdown trigger="click" @command="handleCommand">
              <el-icon class="setting-icon"><Tools /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided style="color: #f56c6c;">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-aside>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
// ... Script 部分完全不需要修改，保持原样 ...
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { ChatLineRound, Files, Setting, Tools } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const username = ref('');
const userRole = ref('');

const initUserInfo = () => {
  username.value = localStorage.getItem('username') || '';
  const role = localStorage.getItem('role');
  userRole.value = role ? role.trim() : '';
};

const roleDisplayName = computed(() => {
  if (userRole.value === 'admin') return '系统管理员';
  if (userRole.value === 'researcher') return '研究员';
  return '普通用户'; 
});

onMounted(() => { initUserInfo(); });
watch(() => route.path, () => { initUserInfo(); });

const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', { 
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' 
    }).then(() => { localStorage.clear(); router.push('/login'); });
  } else if (command === 'profile') { router.push('/profile'); }
};
</script>

<style scoped>
.layout-container { height: 100vh; width: 100vw; overflow: hidden; }
.main-container { height: 100%; }

/* 左侧导航栏：统一深色系 #001529 */
.main-aside {
  background-color: #001529; 
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 移除阴影，让它和 Chat 的侧边栏看起来像一个整体，或者只保留轻微分割线 */
  border-right: 1px solid rgba(255,255,255,0.05); 
  z-index: 10;
}

.brand-area {
  height: 60px; /* 稍微调低一点高度，更精致 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #001529; /* 与背景同色，融为一体 */
  color: #fff;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.logo-text { font-size: 18px; font-weight: 600; letter-spacing: 1px; color: #fff; }
.sub-text { font-size: 12px; color: #697b8c; margin-top: 2px; }

.side-menu {
  flex: 1;
  border-right: none;
  padding-top: 10px;
}
/* 选中菜单项的高亮样式 */
:deep(.el-menu-item.is-active) {
  background-color: #409EFF !important; /* 品牌蓝背景 */
  color: #fff !important;
}
:deep(.el-menu-item:hover) {
  background-color: rgba(255,255,255,0.05) !important;
}

/* 底部用户信息区域 */
.aside-footer {
  height: 70px;
  background-color: #001529;
  border-top: 1px solid rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  flex-shrink: 0;
}

.user-card {
  width: 100%;
  display: flex;
  align-items: center;
  background: rgba(255,255,255,0.03); /* 极淡的背景 */
  padding: 8px 10px;
  border-radius: 6px;
  transition: background 0.3s;
  cursor: default;
}
.user-card:hover { background: rgba(255,255,255,0.08); }

.user-avatar { background-color: #409EFF; flex-shrink: 0; font-size: 14px; }

.user-info {
  margin-left: 10px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.u-name { color: #e6f7ff; font-size: 14px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.u-role { color: #8c9fa9; font-size: 12px; margin-top: 2px; }

.setting-icon { color: #8c9fa9; cursor: pointer; font-size: 16px; margin-left: 5px; transition: color 0.3s; }
.setting-icon:hover { color: #409EFF; }

.app-main { padding: 0; height: 100%; overflow: hidden; background-color: #f5f7fa; }
</style>