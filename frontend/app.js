const API_BASE = "http://localhost:8080";

const healthBtn = document.getElementById("healthBtn");
const dashboardBtn = document.getElementById("dashboardBtn");
const addUserBtn = document.getElementById("addUserBtn");
const listUsersBtn = document.getElementById("listUsersBtn");
const addTaskBtn = document.getElementById("addTaskBtn");
const listTasksBtn = document.getElementById("listTasksBtn");
const updateTaskStatusBtn = document.getElementById("updateTaskStatusBtn");

const healthResult = document.getElementById("healthResult");
const dashboardResult = document.getElementById("dashboardResult");
const usersResult = document.getElementById("usersResult");
const tasksResult = document.getElementById("tasksResult");

const userNameInput = document.getElementById("userNameInput");
const userRoleInput = document.getElementById("userRoleInput");
const taskTitleInput = document.getElementById("taskTitleInput");
const taskOwnerIdInput = document.getElementById("taskOwnerIdInput");
const taskIdInput = document.getElementById("taskIdInput");
const taskStatusInput = document.getElementById("taskStatusInput");

async function requestJson(url, method = "GET") {
  const resp = await fetch(url, { method });
  if (!resp.ok) {
    let message = `HTTP ${resp.status}`;
    try {
      const data = await resp.json();
      if (data.error) {
        message = data.error;
      }
    } catch (_) {
      // Ignore JSON parse failure and use HTTP status
    }
    throw new Error(message);
  }
  return resp.json();
}

healthBtn.addEventListener("click", async () => {
  healthResult.textContent = "请求中...";
  try {
    const data = await requestJson(`${API_BASE}/api/health`);
    healthResult.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    healthResult.textContent = `请求失败: ${err.message}`;
  }
});

dashboardBtn.addEventListener("click", async () => {
  dashboardResult.textContent = "请求中...";
  try {
    const data = await requestJson(`${API_BASE}/api/dashboard`);
    dashboardResult.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    dashboardResult.textContent = `请求失败: ${err.message}`;
  }
});

listUsersBtn.addEventListener("click", async () => {
  usersResult.textContent = "请求中...";
  try {
    const data = await requestJson(`${API_BASE}/api/users`);
    usersResult.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    usersResult.textContent = `请求失败: ${err.message}`;
  }
});

addUserBtn.addEventListener("click", async () => {
  const name = encodeURIComponent(userNameInput.value.trim());
  const role = encodeURIComponent(userRoleInput.value.trim() || "member");
  usersResult.textContent = "请求中...";
  try {
    await requestJson(`${API_BASE}/api/users?name=${name}&role=${role}`, "POST");
    const list = await requestJson(`${API_BASE}/api/users`);
    usersResult.textContent = JSON.stringify(list, null, 2);
    userNameInput.value = "";
    userRoleInput.value = "";
  } catch (err) {
    usersResult.textContent = `请求失败: ${err.message}`;
  }
});

listTasksBtn.addEventListener("click", async () => {
  tasksResult.textContent = "请求中...";
  try {
    const data = await requestJson(`${API_BASE}/api/tasks`);
    tasksResult.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    tasksResult.textContent = `请求失败: ${err.message}`;
  }
});

addTaskBtn.addEventListener("click", async () => {
  const title = encodeURIComponent(taskTitleInput.value.trim());
  const ownerId = encodeURIComponent(taskOwnerIdInput.value.trim());
  tasksResult.textContent = "请求中...";
  try {
    await requestJson(`${API_BASE}/api/tasks?title=${title}&ownerId=${ownerId}`, "POST");
    const list = await requestJson(`${API_BASE}/api/tasks`);
    tasksResult.textContent = JSON.stringify(list, null, 2);
    taskTitleInput.value = "";
  } catch (err) {
    tasksResult.textContent = `请求失败: ${err.message}`;
  }
});

updateTaskStatusBtn.addEventListener("click", async () => {
  const id = encodeURIComponent(taskIdInput.value.trim());
  const status = encodeURIComponent(taskStatusInput.value.trim());
  tasksResult.textContent = "请求中...";
  try {
    await requestJson(`${API_BASE}/api/tasks/status?id=${id}&status=${status}`, "POST");
    const list = await requestJson(`${API_BASE}/api/tasks`);
    tasksResult.textContent = JSON.stringify(list, null, 2);
  } catch (err) {
    tasksResult.textContent = `请求失败: ${err.message}`;
  }
});
