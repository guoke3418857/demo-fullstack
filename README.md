# Demo Fullstack Project (JS + Java)

这是一个非常小的前后端 demo 工程，用于测试代码生成 PRD 文档。
当前版本是“接近真实业务”的最小任务看板系统（用户 + 任务 + 统计）。

## 目录结构

```text
demo-fullstack
├── backend
│   └── src
│       └── Main.java
└── frontend
    ├── app.js
    ├── index.html
    └── style.css
```

## 1) 启动后端 (Java)

要求：JDK 17+（JDK 11 也通常可用）

```bash
cd backend
mkdir -p out
javac -d out src/Main.java
java -cp out Main
```

后端默认地址：`http://localhost:8080`

可用接口：
- `GET /api/health`
- `GET /api/dashboard` 查看统计
- `GET /api/users` 查询用户列表
- `POST /api/users?name=Charlie&role=Designer` 新增用户
- `GET /api/tasks` 查询任务列表
- `POST /api/tasks?title=Write%20PRD&ownerId=1` 新增任务
- `POST /api/tasks/status?id=1&status=doing` 更新任务状态（todo/doing/done）

## 2) 启动前端 (JS)

方式 A：直接双击打开 `frontend/index.html`（部分浏览器对本地文件有跨域限制，不推荐）

方式 B（推荐）：用任意静态服务器启动

```bash
cd frontend
python3 -m http.server 5500
```

打开：`http://localhost:5500`

## 3) 联调说明

前端会请求后端：
- `http://localhost:8080/api/health`
- `http://localhost:8080/api/dashboard`
- `http://localhost:8080/api/users`
- `http://localhost:8080/api/tasks`

后端已开启基础 CORS，便于本地联调。
