import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final List<User> USERS = new ArrayList<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static final AtomicInteger USER_ID_SEQ = new AtomicInteger(3);
    private static final AtomicInteger TASK_ID_SEQ = new AtomicInteger(3);

    public static void main(String[] args) throws IOException {
        seedData();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/tasks", new TasksHandler());
        server.createContext("/api/tasks/status", new TaskStatusHandler());
        server.setExecutor(null);

        System.out.println("Backend started on http://localhost:8080");
        server.start();
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String body = "{\"status\":\"ok\",\"time\":\"" + Instant.now().toString() + "\"}";
            sendJson(exchange, 200, body);
        }
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            int todo = 0;
            int doing = 0;
            int done = 0;

            synchronized (TASKS) {
                for (Task task : TASKS) {
                    if ("todo".equals(task.status)) {
                        todo++;
                    } else if ("doing".equals(task.status)) {
                        doing++;
                    } else if ("done".equals(task.status)) {
                        done++;
                    }
                }
            }

            String body = "{"
                    + "\"generatedAt\":\"" + Instant.now() + "\","
                    + "\"users\":" + USERS.size() + ","
                    + "\"tasks\":" + TASKS.size() + ","
                    + "\"status\":{"
                    + "\"todo\":" + todo + ","
                    + "\"doing\":" + doing + ","
                    + "\"done\":" + done
                    + "}"
                    + "}";
            sendJson(exchange, 200, body);
        }
    }

    static class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, usersJson());
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI uri = exchange.getRequestURI();
                Map<String, String> query = parseQuery(uri.getRawQuery());
                String name = query.getOrDefault("name", "").trim();
                String role = query.getOrDefault("role", "member").trim();
                if (name.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"name is required\"}");
                    return;
                }
                if (role.isEmpty()) {
                    role = "member";
                }
                User user = new User(USER_ID_SEQ.getAndIncrement(), name, role);
                synchronized (USERS) {
                    USERS.add(user);
                }
                sendJson(exchange, 201, "{\"success\":true,\"user\":" + user.toJson() + "}");
                return;
            }

            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
        }
    }

    static class TasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, tasksJson());
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI uri = exchange.getRequestURI();
                Map<String, String> query = parseQuery(uri.getRawQuery());
                String title = query.getOrDefault("title", "").trim();
                String ownerIdRaw = query.getOrDefault("ownerId", "").trim();
                if (title.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"title is required\"}");
                    return;
                }
                if (ownerIdRaw.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"ownerId is required\"}");
                    return;
                }

                int ownerId;
                try {
                    ownerId = Integer.parseInt(ownerIdRaw);
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, "{\"error\":\"ownerId must be number\"}");
                    return;
                }

                User owner = findUserById(ownerId);
                if (owner == null) {
                    sendJson(exchange, 404, "{\"error\":\"owner not found\"}");
                    return;
                }

                Task task = new Task(TASK_ID_SEQ.getAndIncrement(), title, owner.id, owner.name, "todo");
                synchronized (TASKS) {
                    TASKS.add(task);
                }
                sendJson(exchange, 201, "{\"success\":true,\"task\":" + task.toJson() + "}");
                return;
            }

            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
        }
    }

    static class TaskStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            URI uri = exchange.getRequestURI();
            Map<String, String> query = parseQuery(uri.getRawQuery());
            String idRaw = query.getOrDefault("id", "").trim();
            String status = query.getOrDefault("status", "").trim();
            if (idRaw.isEmpty() || status.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"id and status are required\"}");
                return;
            }
            if (!isValidTaskStatus(status)) {
                sendJson(exchange, 400, "{\"error\":\"status must be todo/doing/done\"}");
                return;
            }

            int taskId;
            try {
                taskId = Integer.parseInt(idRaw);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"id must be number\"}");
                return;
            }

            Task updated = null;
            synchronized (TASKS) {
                for (Task task : TASKS) {
                    if (task.id == taskId) {
                        task.status = status;
                        updated = task;
                        break;
                    }
                }
            }
            if (updated == null) {
                sendJson(exchange, 404, "{\"error\":\"task not found\"}");
                return;
            }
            sendJson(exchange, 200, "{\"success\":true,\"task\":" + updated.toJson() + "}");
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String value = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String urlDecode(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void seedData() {
        USERS.add(new User(1, "Alice", "PM"));
        USERS.add(new User(2, "Bob", "Engineer"));

        TASKS.add(new Task(1, "Define MVP scope", 1, "Alice", "doing"));
        TASKS.add(new Task(2, "Build demo API", 2, "Bob", "todo"));
    }

    private static User findUserById(int id) {
        synchronized (USERS) {
            for (User user : USERS) {
                if (user.id == id) {
                    return user;
                }
            }
        }
        return null;
    }

    private static boolean isValidTaskStatus(String status) {
        return "todo".equals(status) || "doing".equals(status) || "done".equals(status);
    }

    private static String usersJson() {
        List<String> items = new ArrayList<>();
        synchronized (USERS) {
            for (User user : USERS) {
                items.add(user.toJson());
            }
        }
        return "{\"items\":[" + String.join(",", items) + "]}";
    }

    private static String tasksJson() {
        List<String> items = new ArrayList<>();
        synchronized (TASKS) {
            for (Task task : TASKS) {
                items.add(task.toJson());
            }
        }
        return "{\"items\":[" + String.join(",", items) + "]}";
    }

    static class User {
        final int id;
        final String name;
        final String role;

        User(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }

        String toJson() {
            return "{"
                    + "\"id\":" + id + ","
                    + "\"name\":\"" + escapeJson(name) + "\","
                    + "\"role\":\"" + escapeJson(role) + "\""
                    + "}";
        }
    }

    static class Task {
        final int id;
        final String title;
        final int ownerId;
        final String ownerName;
        String status;

        Task(int id, String title, int ownerId, String ownerName, String status) {
            this.id = id;
            this.title = title;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.status = status;
        }

        String toJson() {
            return "{"
                    + "\"id\":" + id + ","
                    + "\"title\":\"" + escapeJson(title) + "\","
                    + "\"ownerId\":" + ownerId + ","
                    + "\"ownerName\":\"" + escapeJson(ownerName) + "\","
                    + "\"status\":\"" + escapeJson(status) + "\""
                    + "}";
        }
    }
}
