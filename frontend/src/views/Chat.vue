<template>
  <div class="chat-layout">
    
    <div class="main-content">
      <div class="chat-header">
        <div class="header-left">
          <span class="session-title">{{ currentSessionTitle }}</span>
        </div>
        
        <div class="header-right">
          <el-popconfirm title="确定删除当前这段对话吗？" @confirm="handleDeleteCurrentSession">
            <template #reference>
              <el-button type="danger" link class="delete-current-btn">
                <el-icon><Delete /></el-icon> 删除当前对话
              </el-button>
            </template>
          </el-popconfirm>

          <el-button link @click="isCollapsed = !isCollapsed" class="toggle-btn" title="切换历史记录栏">
             <el-icon size="20" v-if="isCollapsed"><Expand /></el-icon>
             <el-icon size="20" v-else><Fold /></el-icon>
          </el-button>
        </div>
      </div>

      <div class="message-area" ref="msgRef">
        <div v-for="(item, index) in msgList" :key="index" :class="['message-row', item.role === 'user' ? 'row-right' : 'row-left']">
          <el-avatar :icon="item.role === 'user' ? UserFilled : Service" :size="36" 
            :style="{ backgroundColor: item.role === 'user' ? '#409EFF' : '#10a37f' }" 
          />
          <div class="message-content">
            <div class="bubble">{{ item.content }}</div>
            <div v-if="item.role === 'ai' && item.citations && item.citations.length > 0" class="citations">
              <div class="citation-title">引用来源:</div>
              <el-tag v-for="(cite, cIndex) in item.citations" :key="cIndex" size="small" type="info" class="citation-tag" @click="viewDoc(cite)">
                {{ typeof cite === 'string' ? cite : (cite.docName || '相关文档') }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="input-wrapper">
        <div class="input-box">
          <el-input
            v-model="inputQuery"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="请输入您的问题，按 Enter 发送..."
            resize="none"
            class="custom-textarea"
            @keydown.enter.prevent="handleEnter"
          />
          <el-button type="primary" class="send-btn" @click="handleSend" :loading="loading" :disabled="!inputQuery.trim()">
            <el-icon v-if="!loading"><Promotion /></el-icon>
            <span style="margin-left: 5px">发送</span>
          </el-button>
        </div>
        <div class="footer-tip">内容由 AI 生成，请注意甄别。</div>
      </div>
    </div>

    <div class="sidebar" :class="{ 'sidebar-collapsed': isCollapsed }">
      <div class="sidebar-header" v-show="!isCollapsed">
        <el-button class="new-chat-btn" @click="createNewSession">
          <el-icon style="margin-right: 5px"><Plus /></el-icon> 新建对话
        </el-button>
      </div>

      <div class="history-list-wrapper" v-show="!isCollapsed">
        <div class="list-title">最近记录 ({{ sessionList.length }}/15)</div>
        <div class="history-scroll-area">
          <div 
            v-for="item in sessionList" 
            :key="item.id" 
            class="history-item"
            :class="{ active: currentSessionId === item.id }"
            @click="switchSession(item.id)"
          >
            <div class="item-title">
              <el-icon><ChatDotSquare /></el-icon>
              <span class="text">{{ item.title }}</span>
            </div>
            <el-icon class="delete-icon" @click.stop="deleteSession(item.id)"><Delete /></el-icon>
          </div>
        </div>
      </div>

      <div class="sidebar-footer" v-show="!isCollapsed">
         <el-popconfirm title="确定清空所有历史记录吗？" @confirm="clearAllSessions">
            <template #reference>
              <div class="clear-all-btn">
                <el-icon><Delete /></el-icon>
                <span>清空所有记录</span>
              </div>
            </template>
         </el-popconfirm>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import { UserFilled, Service, Plus, Delete, ChatDotSquare, Expand, Fold, Promotion } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

// --- 类型定义 ---
interface Message { role: 'user' | 'ai', content: string, citations?: any[] }
interface SessionItem { id: string, title: string, timestamp: number }

// --- 状态变量 ---
const isCollapsed = ref(false)
const sessionList = ref<SessionItem[]>([])
const currentSessionId = ref('')
const inputQuery = ref('')
const loading = ref(false)
const msgRef = ref<HTMLElement | null>(null)

const defaultMsg: Message = { role: 'ai', content: '您好！我是产业知识助手，有什么可以帮您？' };
const msgList = ref<Message[]>([ defaultMsg ])

const currentSessionTitle = computed(() => {
  const session = sessionList.value.find(s => s.id === currentSessionId.value)
  return session ? session.title : '新对话'
})

// --- 初始化 ---
onMounted(() => {
  loadSessionList();
  if (sessionList.value.length > 0) {
    switchSession(sessionList.value[0].id);
  } else {
    createNewSession();
  }
})

// --- 逻辑方法 ---
const loadSessionList = () => {
  const json = localStorage.getItem('chat_sessions_v2');
  if (json) sessionList.value = JSON.parse(json);
}

const saveSessionList = () => {
  if (sessionList.value.length > 15) {
    const removed = sessionList.value.pop();
    if (removed) localStorage.removeItem(`chat_record_${removed.id}`);
  }
  localStorage.setItem('chat_sessions_v2', JSON.stringify(sessionList.value));
}

const switchSession = (id: string) => {
  currentSessionId.value = id;
  const record = localStorage.getItem(`chat_record_${id}`);
  msgList.value = record ? JSON.parse(record) : [ defaultMsg ];
  scrollToBottom();
}

const createNewSession = () => {
  currentSessionId.value = 'session_' + Date.now();
  msgList.value = [ defaultMsg ];
  scrollToBottom();
}

const deleteSession = (id: string) => {
  sessionList.value = sessionList.value.filter(s => s.id !== id);
  saveSessionList();
  localStorage.removeItem(`chat_record_${id}`);
  if (currentSessionId.value === id) {
    if (sessionList.value.length > 0) switchSession(sessionList.value[0].id);
    else createNewSession();
  }
}

const handleDeleteCurrentSession = () => {
  deleteSession(currentSessionId.value);
  ElMessage.success('当前对话已删除');
}

const clearAllSessions = () => {
  sessionList.value.forEach(s => localStorage.removeItem(`chat_record_${s.id}`));
  sessionList.value = [];
  localStorage.removeItem('chat_sessions_v2');
  createNewSession();
  ElMessage.success('已清空所有历史记录');
}

const handleEnter = (e: KeyboardEvent) => {
  if (!e.ctrlKey && !e.shiftKey) {
    handleSend();
  }
}

const handleSend = async () => {
  const content = inputQuery.value.trim()
  if (!content) return

  let isNewSession = false;
  const existingIndex = sessionList.value.findIndex(s => s.id === currentSessionId.value);
  if (existingIndex === -1) {
    isNewSession = true;
    const title = content.length > 10 ? content.substring(0, 10) + '...' : content;
    sessionList.value.unshift({ id: currentSessionId.value, title, timestamp: Date.now() });
  } else {
    const session = sessionList.value.splice(existingIndex, 1)[0];
    session.timestamp = Date.now();
    sessionList.value.unshift(session);
  }
  saveSessionList();

  msgList.value.push({ role: 'user', content })
  inputQuery.value = ''
  scrollToBottom()
  loading.value = true
  localStorage.setItem(`chat_record_${currentSessionId.value}`, JSON.stringify(msgList.value));

  try {
    const res: any = await request.post('/chat/completions', {
      question: content,
      sessionId: isNewSession ? '' : currentSessionId.value
    })

    if (res.code === 200) {
      msgList.value.push({
        role: 'ai',
        content: res.data.answer || '暂无回复',
        citations: res.data.citations || [] 
      })
    } else {
      msgList.value.push({ role: 'ai', content: '系统响应异常: ' + res.msg })
    }
  } catch (error) {
    console.error(error)
    msgList.value.push({ role: 'ai', content: '服务请求失败' })
  } finally {
    loading.value = false
    scrollToBottom()
    localStorage.setItem(`chat_record_${currentSessionId.value}`, JSON.stringify(msgList.value));
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
  })
}

const viewDoc = (doc: any) => console.log(doc)
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background-color: #f5f7fa;
}

/* ================= 侧边栏 (右侧) ================= */
.sidebar {
  width: 260px;
  background-color: #002140; 
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
  border-left: 1px solid rgba(0,0,0,0.1); /* 左边框 */
}

.sidebar-collapsed { width: 0; overflow: hidden; }

.sidebar-header { padding: 15px 12px; flex-shrink: 0; }
.new-chat-btn {
  width: 100%;
  background-color: #409EFF;
  border: none;
  color: #fff;
  justify-content: flex-start;
  padding: 18px 15px;
  border-radius: 6px;
  font-weight: 500;
  transition: opacity 0.2s;
}
.new-chat-btn:hover { background-color: #66b1ff; }

.history-list-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.list-title { font-size: 12px; color: #8c9fa9; padding: 12px 15px; flex-shrink: 0; font-weight: 600; }
.history-scroll-area { flex: 1; overflow-y: auto; padding: 0 10px; }
.history-scroll-area::-webkit-scrollbar { width: 4px; }
.history-scroll-area::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 2px; }

.history-item {
  display: flex; align-items: center; padding: 12px; margin-bottom: 4px;
  border-radius: 6px; cursor: pointer; color: #d1d9e0; font-size: 14px;
  transition: all 0.2s;
}
.history-item:hover { background-color: rgba(255,255,255,0.08); color: #fff; }
.history-item.active { background-color: #1890ff; color: #fff; box-shadow: 0 2px 6px rgba(0,0,0,0.2); }

.item-title { flex: 1; display: flex; align-items: center; overflow: hidden; white-space: nowrap; }
.item-title .text { margin-left: 8px; overflow: hidden; text-overflow: ellipsis; }
.delete-icon { display: none; color: #a6adb4; }
.history-item:hover .delete-icon { display: block; }
.delete-icon:hover { color: #ff7875; }

.sidebar-footer {
  border-top: 1px solid rgba(255,255,255,0.1);
  padding: 15px;
  flex-shrink: 0;
}
.clear-all-btn { display: flex; align-items: center; color: #d1d9e0; cursor: pointer; font-size: 14px; transition: color 0.2s; }
.clear-all-btn:hover { color: #ff7875; }
.clear-all-btn span { margin-left: 8px; }

/* ================= 主内容 (左侧) ================= */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  background-color: #f5f7fa; /* 浅灰色背景 */
}

.chat-header {
  height: 60px;
  border-bottom: 1px solid #e5e7eb;
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; font-weight: 600; color: #374151; font-size: 15px; }
.header-right { display: flex; align-items: center; gap: 15px; }
.toggle-btn { color: #606266; padding: 0 8px; }
.toggle-btn:hover { color: #409EFF; }
.delete-current-btn { margin-right: 0 !important; }

/* 消息区域 */
.message-area { 
  flex: 1; 
  overflow-y: auto; 
  padding: 20px 20px 40px 20px; 
  scroll-behavior: smooth; 
}

.message-row { 
  display: flex; 
  margin-bottom: 25px; 
  width: 100%; 
}

/* AI 对齐 */
.row-left { 
  justify-content: flex-start; 
}
/* 用户 对齐 */
.row-right { 
  justify-content: flex-start; 
  flex-direction: row-reverse; 
}

.message-content { 
  max-width: 75%; 
  margin: 0 15px; 
  display: flex; 
  flex-direction: column; 
}
.row-right .message-content { align-items: flex-end; }

/* 气泡样式 */
.bubble { 
  padding: 12px 16px; 
  border-radius: 8px; 
  font-size: 15px; 
  line-height: 1.6; 
  white-space: pre-wrap; 
  word-break: break-word; 
  box-shadow: 0 1px 2px rgba(0,0,0,0.05); 
}

/* AI: 白底灰边 */
.row-left .bubble { 
  background-color: #fff; 
  color: #1f2937; 
  border-top-left-radius: 0; 
  border: 1px solid #eaecf0; 
}

/* 用户: 蓝底白字 */
.row-right .bubble { 
  background-color: #409EFF; 
  color: #fff; 
  border-top-right-radius: 0; 
  border: none;
}

.citations { 
  margin-top: 8px; 
  background: rgba(0,0,0,0.03); 
  padding: 8px; 
  border-radius: 6px; 
  font-size: 12px; 
}
.citation-tag { margin: 2px 4px 2px 0; cursor: pointer; }

/* 输入区域 */
.input-wrapper {
  padding: 20px;
  flex-shrink: 0;
  background-image: linear-gradient(180deg, rgba(255,255,255,0) 0%, #f5f7fa 20%);
}
.input-box {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  display: flex;
  align-items: flex-end;
  padding: 10px;
  transition: border 0.2s, box-shadow 0.2s;
}
.input-box:focus-within { border-color: #409EFF; box-shadow: 0 4px 16px rgba(64,158,255,0.15); }

:deep(.custom-textarea .el-textarea__inner) {
  box-shadow: none !important; border: none !important; resize: none; padding: 0; max-height: 150px; overflow-y: auto; color: #333;
}

.send-btn { 
  height: 38px; padding: 0 20px; border-radius: 8px; margin-left: 10px; min-width: 90px;
  display: flex; justify-content: center; align-items: center; font-weight: 500;
  box-shadow: 0 2px 6px rgba(64,158,255,0.3);
}

.footer-tip { text-align: center; font-size: 12px; color: #9ca3af; margin-top: 10px; }
</style>