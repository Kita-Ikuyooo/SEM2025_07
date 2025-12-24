<template>
  <div class="knowledge-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="handleBack" class="breadcrumb-link">全部知识库</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentFolder">{{ currentFolder.name }}</el-breadcrumb-item>
          </el-breadcrumb>

          <div class="header-actions">
            <template v-if="canManage">
              <el-button v-if="!currentFolder" type="success" @click="showAddDialog = true">
                新建文件夹
              </el-button>

              <el-upload
                v-else
                action="#"
                :http-request="handleUpload"
                :show-file-list="false"
                :before-upload="beforeUpload"
                accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              >
                <el-button type="primary">上传文档</el-button>
              </el-upload>
            </template>
            <el-tag v-else type="info" effect="plain">访客模式 (只读)</el-tag>
          </div>
        </div>
      </template>

      <div v-if="!currentFolder">
        <draggable 
          v-model="folderList" 
          item-key="id" 
          tag="div" 
          class="folder-grid"
          :disabled="!canManage"
          animation="200"
          @end="handleDragEnd"
        >
          <template #item="{ element }">
            <div class="folder-card" @click="handleEnterFolder(element)">
              <div class="folder-icon">
                <el-icon :size="60" color="#E6A23C"><FolderOpened /></el-icon>
              </div>
              <div class="folder-name">{{ element.name }}</div>
              
              <div class="folder-actions" v-if="canManage" @click.stop>
                <el-button link type="primary" size="small" @click="openRenameDialog(element)">
                   重命名
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteFolder(element.id)">
                   删除
                </el-button>
              </div>
            </div>
          </template>
        </draggable>
        
        <el-empty v-if="folderList.length === 0" description="暂无知识库文件夹，请先新建" />
      </div>

      <div v-else>
        <el-table :data="documentList" style="width: 100%" v-loading="loading">
          <el-table-column prop="docName" label="文件名" min-width="220" show-overflow-tooltip />
          <el-table-column prop="fileType" label="类型" width="100">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.fileType.toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120">
            <template #default="scope">
               {{ (scope.row.fileSize / 1024).toFixed(2) }} KB
            </template>
          </el-table-column>
          <el-table-column prop="uploadTime" label="上传时间" width="180" />
          
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleAction(scope.row, 'preview')">预览</el-button>
              <el-button link type="success" @click="handleAction(scope.row, 'download')">下载</el-button>
              <el-popconfirm v-if="canManage" title="确定要删除此文档吗？" @confirm="handleDeleteDoc(scope.row.docId)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" title="新建知识库文件夹" width="400px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="newFolderName" placeholder="请输入文件夹名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAddFolder">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRenameDialog" title="重命名文件夹" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="renameValue" placeholder="请输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { FolderOpened } from '@element-plus/icons-vue';
import request from '../utils/request';
import draggable from 'vuedraggable'; // 【引入】

const loading = ref(false);
const folderList = ref([]);       
const documentList = ref([]);     
const currentFolder = ref(null); 
const showAddDialog = ref(false);
const newFolderName = ref('');

// 重命名相关变量
const showRenameDialog = ref(false);
const renameValue = ref('');
const currentRenameId = ref('');

// 在 script setup 中
const canManage = computed(() => {
  const role = localStorage.getItem('role');
  // 逻辑：只要角色存在，且不是 viewer，就有文件管理权限
  // 这意味着 admin 和 researcher 都可以管理，符合你的要求
  return role && role !== 'viewer';
});

// 获取文件夹列表
const fetchFolders = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/docs/category/list');
    if (res.code === 200) {
      folderList.value = res.data;
    }
  } catch (error) { console.error(error); } 
  finally { loading.value = false; }
};

// ... 原有的 handleEnterFolder, handleBack, confirmAddFolder 保持不变 ...
const handleEnterFolder = async (folder: any) => {
  currentFolder.value = folder;
  loading.value = true;
  try {
    const res: any = await request.get(`/docs/list`, { params: { categoryId: folder.id } });
    if (res.code === 200) documentList.value = res.data;
  } catch (error) { ElMessage.error('获取文件列表失败'); } 
  finally { loading.value = false; }
};

const handleBack = () => { currentFolder.value = null; fetchFolders(); };

const confirmAddFolder = async () => {
  if (!newFolderName.value.trim()) return;
  try {
    const res: any = await request.post('/docs/category/add', { name: newFolderName.value });
    if (res.code === 200) {
      ElMessage.success('创建成功');
      showAddDialog.value = false;
      newFolderName.value = '';
      fetchFolders();
    }
  } catch (error) { ElMessage.error('创建失败'); }
};

// ... 原有 handleUpload, handleAction, handleDeleteDoc 保持不变 ...
const handleUpload = async (options: any) => {
  const { file } = options;
  const formData = new FormData();
  formData.append('file', file);
  formData.append('categoryId', currentFolder.value.id); 
  try {
    const res: any = await request.post('/docs/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
    if (res.code === 200) { ElMessage.success('上传成功'); handleEnterFolder(currentFolder.value); }
  } catch (error) { ElMessage.error('上传异常'); }
};

const handleAction = async (row: any, type: 'preview' | 'download') => {
  try {
    const res: any = await request.get(`/docs/url/${type}/${row.docId}`);
    if (res.code === 200) {
      if (type === 'preview') window.open(res.data, '_blank');
      else window.location.href = res.data;
    }
  } catch (error) { ElMessage.error('获取链接失败'); }
};

const handleDeleteDoc = async (docId: string) => {
  const res: any = await request.delete(`/docs/${docId}`);
  if (res.code === 200) { ElMessage.success('文档已删除'); handleEnterFolder(currentFolder.value); }
};

const handleDeleteFolder = (id: string) => {
  ElMessageBox.confirm('确定删除该文件夹吗？', '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/docs/category/${id}`);
    if (res.code === 200) { ElMessage.success('文件夹已删除'); fetchFolders(); }
  });
};

const beforeUpload = (file: any) => file.size / 1024 / 1024 < 50;

// 【新增】打开重命名弹窗
const openRenameDialog = (folder: any) => {
  currentRenameId.value = folder.id;
  renameValue.value = folder.name;
  showRenameDialog.value = true;
}

// 【新增】确认重命名
const confirmRename = async () => {
  if (!renameValue.value.trim()) return;
  try {
    const res: any = await request.post('/docs/category/rename', {
      id: currentRenameId.value,
      name: renameValue.value
    });
    if (res.code === 200) {
      ElMessage.success('重命名成功');
      showRenameDialog.value = false;
      fetchFolders(); // 刷新列表
    }
  } catch(e) { ElMessage.error('重命名失败'); }
}

// 【新增】拖拽结束事件
const handleDragEnd = async () => {
  // 提取当前列表的 ID 顺序
  const sortedIds = folderList.value.map((item: any) => item.id);
  
  try {
    // 发送给后端保存
    await request.post('/docs/category/reorder', sortedIds);
    // 不用提示，静默保存体验更好，或者轻微提示
    // ElMessage.success('排序已保存');
  } catch(e) {
    ElMessage.error('排序保存失败');
  }
}

onMounted(() => {
  fetchFolders();
});
</script>

<style scoped>
.knowledge-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.breadcrumb-link { cursor: pointer; color: #409EFF; font-weight: bold; }

/* 网格布局：因为 draggable 渲染为 div，所以直接给它应用 grid 样式 */
.folder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); /* 稍微加宽一点 */
  gap: 25px;
  padding: 20px 0;
}

.folder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 15px 15px 15px; /* 调整内边距 */
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer; /* 拖拽时手型 */
  transition: all 0.3s;
  background-color: #fff;
  position: relative;
}
.folder-card:hover {
  background-color: #f5f7fa;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px); /* 悬浮上浮效果 */
}

.folder-name {
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
  text-align: center;
  word-break: break-all;
  margin-bottom: 5px; /* 留出空间给按钮 */
  font-weight: 500;
}

/* 按钮区域样式 */
.folder-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 5px;
  opacity: 0; /* 默认隐藏 */
  transition: opacity 0.2s;
}
.folder-card:hover .folder-actions {
  opacity: 1; /* 悬浮显示 */
}
</style>