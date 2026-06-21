# EagleTab — 技術架構文件 v1.0

> 個人開發工具，參考 AT.field 概念，以 Java + TypeScript 實作。
> 本文件由後端架構師 × 前端開發者協作規劃，整合為單一可執行規格。

---

## 一、專案定位與邊界

| 項目 | v1 決定 |
|---|---|
| 主要用途 | 自用開發工具 + 作品集展示 |
| 使用者規模 | 單人，不做多租戶 |
| 終端機 | 平台原生 shell（Windows：PowerShell；macOS：使用者 shell，可執行系統指令） |
| AI 整合 | CLI 委派——使用者直接在 terminal 跑 `claude` / `codex` / `aider` |
| 安全模型 | 不做 Docker 沙盒（v1），不做 SSH/Serial |
| 持久化 | 不做，記憶體存即可，重開頁面 session 斷掉可接受 |

**v1 明確不做**：多租戶、Docker 沙盒、SSH/Serial、AI Agent 自動執行、Session 持久化/歷史紀錄

---

## 二、技術棧

### 後端
| 技術 | 版本 | 用途 |
|---|---|---|
| Java | 21 | 主語言（Virtual Threads） |
| Spring Boot | 3.3.x | Web 框架 |
| Spring WebSocket | 隨 Boot | Raw WebSocket 端點 |
| pty4j | 0.12.26 | 真實 pseudo-terminal（JetBrains） |
| JNA | 5.x（傳遞） | pty4j native bridge |
| Jackson | 2.x（隨 Boot） | JSON 序列化 |

### 前端
| 技術 | 版本 | 用途 |
|---|---|---|
| React | 18.3 | UI 框架 |
| TypeScript | 5.5 | 型別安全 |
| Vite | 5.4 | 建構工具 |
| @xterm/xterm | 5.5 | 終端機渲染 |
| @xterm/addon-fit | 0.10 | 自動 resize |
| Zustand | 4.5 | 輕量狀態管理 |
| react-markdown + remark-gfm | 9.x / 4.x | Markdown 渲染 |
| diff2html | 3.4 | Git diff 渲染 |
| react-pdf + pdfjs-dist | 7.x / 4.x | PDF 預覽 |

---

## 三、整體架構

```
┌──────────────────────────────────────────────────────────────┐
│                        瀏覽器（前端）                          │
│                                                              │
│  ┌───────────┐   ┌────────────────────┐   ┌──────────────┐  │
│  │ Navigator │   │   TerminalView      │   │   Sidecar    │  │
│  │ 左側 220px│   │   中間 1fr          │   │  右側 420px  │  │
│  │ 檔案清單  │   │   xterm.js          │   │  動態 Viewer │  │
│  │（v1假資料）│   │                    │   │              │  │
│  └───────────┘   └────────────────────┘   └──────────────┘  │
└──────────────────────────┬───────────────────────────────────┘
                           │ WebSocket ws://localhost:8080/ws/terminal
                           │ JSON text frames
┌──────────────────────────┴───────────────────────────────────┐
│                     Spring Boot 後端                          │
│                                                              │
│  TerminalWebSocketHandler                                    │
│         │                                                    │
│  TerminalEngine (pty4j)                                      │
│         │                                                    │
│         ├─── fork 1 ──→ forwardRaw() ──→ 前端 terminal_output│
│         │                                                    │
│         └─── fork 2 ──→ AnsiStripper                        │
│                              │                               │
│                         OutputDetectionEngine                │
│                              │                               │
│                         PatternMatcher (regex)               │
│                              │                               │
│                         SidecarNotifier ──→ 前端 sidecar_suggestion
└──────────────────────────────────────────────────────────────┘
                           │
                    平台原生 OS shell process
```

**核心概念**：AI CLI 在 terminal 裡正常執行，後端額外監看 stdout，偵測到值得預覽的內容就即時推送給前端 Sidecar 自動渲染。

---

## 四、WebSocket 訊息協定

同一條 WebSocket，用 `type` 欄位區分：

```typescript
// ── 前端 → 後端 ──────────────────────────────────
{ type: "terminal_input",  data: string }           // 鍵盤輸入，UTF-8
{ type: "terminal_resize", cols: number, rows: number } // xterm resize 事件

// ── 後端 → 前端 ──────────────────────────────────
{ type: "terminal_output",    data: string }        // Base64 raw bytes → xterm.write(Uint8Array)
{ type: "sidecar_suggestion", kind: "file" | "url" | "diff", payload: string }
```

> `terminal_output.data` 使用 Base64 的原因：PTY 輸出包含不合法 UTF-8 序列與控制字元，JSON text frame 需 Base64 才能安全傳輸。

---

## 五、後端架構細節

### 5.1 專案套件結構

```
com.eagletab
├── EagleTabApplication.java
├── config/
│   ├── WebSocketConfig.java          # 端點註冊，CORS origin 設定
│   └── AppConfig.java
├── files/
│   └── FileController.java           # GET /api/files?path=（安全限制見 5.5）
├── terminal/
│   ├── TerminalEngine.java           # pty4j 初始化、session 工廠
│   ├── TerminalSession.java          # 持有 PtyProcess + WebSocketSession
│   ├── TerminalSessionRegistry.java  # ConcurrentHashMap<sessionId, TerminalSession>
│   └── PtyOutputRouter.java          # stdout 分流（raw forward + detection）
├── detection/
│   ├── OutputDetectionEngine.java    # 行緩衝 + 逐行偵測
│   ├── AnsiStripper.java             # ANSI escape code 移除
│   ├── PatternMatcher.java           # 持有所有 Pattern，回傳 DetectionResult
│   └── DetectionResult.java          # record: kind + payload
└── ws/
    ├── TerminalWebSocketHandler.java  # WebSocketHandler 實作
    ├── MessageProtocol.java           # 進出訊息 record 定義
    └── SidecarNotifier.java           # 封裝推送邏輯，處理 thread-safety
```

### 5.2 Terminal Engine（pty4j 整合）

**Shell 選擇規則**：

1. 若設定 `EAGLETAB_SHELL`，使用其指定的 shell 絕對路徑。
2. Windows 依序尋找 `pwsh.exe`、`powershell.exe`、`cmd.exe`。
3. macOS 優先使用 `$SHELL`，未設定時使用 `/bin/zsh`。

shell 與啟動參數以字串陣列傳入 `PtyProcessBuilder`，不拼接成單一命令字串。Windows 使用 `-NoLogo` 啟動 PowerShell；macOS 使用 `-l` 啟動 login shell。Git Bash 可透過 `EAGLETAB_SHELL` 選用，但不是 Windows 預設依賴。

```java
// TerminalEngine.java 核心片段
public TerminalSession createSession(WebSocketSession wsSession) {
    Map<String, String> env = new HashMap<>(System.getenv());
    env.put("TERM", "xterm-256color");
    env.put("COLORTERM", "truecolor");

    String[] shellCommand = shellResolver.resolve();
    PtyProcess pty = new PtyProcessBuilder(shellCommand)
        .setEnvironment(env)
        .setInitialColumns(220)   // ponytail: 220 cols 避免 AI CLI 誤換行干擾偵測
        .setInitialRows(50)
        .setRedirectErrorStream(true)
        .start();

    TerminalSession session = new TerminalSession(wsSession, pty);
    registry.register(wsSession.getId(), session);
    router.startReading(session);
    return session;
}
```

**PTY 生命週期**：
```
WebSocket onOpen  → createSession() → PtyProcessBuilder.start() → 啟動 Virtual Thread 讀 stdout
WebSocket message → writeToPty(data)
WebSocket onClose → session.destroy() → pty.destroyForcibly() → registry.remove()
```

**stdout 分流（PtyOutputRouter）**，使用 Java 21 Virtual Thread：
```java
Thread.ofVirtual().name("pty-reader-" + session.getId()).start(() -> {
    byte[] buf = new byte[4096];
    InputStream stdout = session.getPty().getInputStream();
    int n;
    while ((n = stdout.read(buf)) != -1) {
        byte[] chunk = Arrays.copyOf(buf, n);
        notifier.sendRawOutput(session, chunk);                        // 分流 1：原始 bytes
        String plain = AnsiStripper.strip(new String(chunk, UTF_8));
        detectionEngine.feed(session.getId(), plain);                  // 分流 2：偵測
    }
});
```

### 5.3 Output Detection Engine

**流式解析策略**：行緩衝 + 安全閥

```
feed(chunk) 流程：
  lineBuffer.append(chunk)
  while buffer 含 '\n':
      取出完整行 → processLine(sessionId, line)
  若 buffer > 8KB 無換行 → 強制 flush 後清空（防記憶體堆積）
```

**AnsiStripper**：
```java
private static final Pattern ANSI_PATTERN = Pattern.compile(
    "\\x1B(?:\\[[0-9;]*[A-Za-z]"              // CSI: ESC[...m cursor/color
    + "|\\][^\\x07\\x1B]*(?:\\x07|\\x1B\\\\)" // OSC: 超連結等
    + "|[\\x40-\\x5F])"                        // 雙字元 ESC 序列
);
```

**PatternMatcher regex 規則**：

| 偵測目標 | Pattern |
|---|---|
| Unix 絕對路徑（含副檔名） | `/(?:[\w.\-]+/)*[\w.\-]+\.(?:md\|json\|ts\|py\|png\|jpg\|pdf\|diff...)` |
| 相對路徑 | `\.{1,2}/(?:[\w.\-]+/)*[\w.\-]+\.（同上）` |
| localhost URL | `https?://(?:localhost\|127\.0\.0\.1)(?::\d{1,5})?(?:/\S*)?` |
| git diff 開頭行 | `^diff --git a/(.+) b/(.+)$` |

**偵測 dedup**：per-session LRU cache（容量 100，TTL 5 秒），相同 (kind, payload) 不重複推送。

### 5.4 WebSocket 端點

```java
// WebSocketConfig.java
registry.addHandler(handler, "/ws/terminal")
        .setAllowedOriginPatterns("http://localhost:*");

// SidecarNotifier — thread-safe 推送
WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(
    wsSession, 5000, 512 * 1024   // sendTimeLimit=5s, bufferSize=512KB
);
```

`WebSocketSession.sendMessage()` 非 thread-safe，使用 `ConcurrentWebSocketSessionDecorator` 解決 PTY reader thread 與 DetectionEngine 並發推送問題。

### 5.5 File Serving API

Sidecar viewer 需要取得本機檔案內容，後端提供一個最小 REST 端點：

```
GET /api/files?path=<絕對路徑>
```

**安全限制**（實作時必須全部到位）：

| 規則 | 做法 |
|---|---|
| 根目錄限制 | 只允許 `System.getProperty("user.home")` 以下的路徑 |
| 副檔名白名單 | `md, txt, png, jpg, jpeg, gif, webp, svg, pdf` |
| Path traversal 防護 | `Path.of(path).normalize()` 後確認仍在根目錄內（`startsWith`） |
| Symlink 防護 | `Files.readSymbolicLink` 後再做根目錄確認，或直接拒絕 symlink |

**回傳格式**：依副檔名設定正確 `Content-Type`，直接串流檔案位元組。

```java
// FileController.java 核心片段
@GetMapping("/api/files")
public ResponseEntity<Resource> serveFile(@RequestParam String path) throws IOException {
    Path target = Path.of(path).normalize().toAbsolutePath();
    Path root   = Path.of(System.getProperty("user.home")).toAbsolutePath();
    if (!target.startsWith(root)) return ResponseEntity.status(403).build();
    String ext = /* 取副檔名 */;
    if (!ALLOWED_EXTENSIONS.contains(ext)) return ResponseEntity.status(403).build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(mimeFor(ext)))
        .body(new FileSystemResource(target));
}
```

**前端 Viewer 取用方式**：

| Viewer | 做法 |
|---|---|
| MarkdownViewer | `fetch("/api/files?path=…")` → text → react-markdown |
| ImageViewer | `<img src="/api/files?path=…">` |
| PdfViewer | `<Document file="/api/files?path=…">` |

### 5.6 pom.xml 關鍵依賴

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.jetbrains.pty4j</groupId>
    <artifactId>pty4j</artifactId>
    <version>0.12.26</version>
</dependency>
<!-- JNA 由 pty4j 傳遞依賴，不需手動指定 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

---

## 六、前端架構細節

### 6.1 目錄結構

```
eagletab-frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx                     # 三欄 CSS Grid 根元件
│   ├── components/
│   │   ├── Navigator/
│   │   │   ├── Navigator.tsx       # 左側欄容器
│   │   │   └── FileList.tsx        # v1 固定假資料
│   │   ├── TerminalView/
│   │   │   └── TerminalView.tsx    # xterm.js 容器
│   │   └── Sidecar/
│   │       ├── Sidecar.tsx         # 右側欄，viewer 切換邏輯
│   │       └── viewers/
│   │           ├── MarkdownViewer.tsx
│   │           ├── ImageViewer.tsx
│   │           ├── PdfViewer.tsx
│   │           ├── IframeViewer.tsx
│   │           ├── DiffViewer.tsx
│   │           └── EmptyViewer.tsx
│   ├── hooks/
│   │   ├── useWebSocket.ts         # 連線管理、訊息分派、指數退避重連
│   │   └── useTerminal.ts          # xterm.js 初始化、FitAddon、store 訂閱
│   ├── store/
│   │   └── appStore.ts             # Zustand store
│   └── types/
│       └── protocol.ts             # WebSocket 訊息型別
├── vite.config.ts
└── package.json
```

### 6.2 元件職責

| 元件 | 職責 | 關鍵 props/state |
|---|---|---|
| `App` | 三欄 Grid 骨架，持有 WebSocket hook | 讀 store.connectionState |
| `Navigator` | 左側欄，v1 假資料 | `onNavigate?(path) => void` |
| `TerminalView` | 掛載 xterm.js，不負責資料流 | `onInput: (data) => void` |
| `Sidecar` | 依 store.sidecarContent 自動切換 viewer | 讀 store.sidecarContent |
| 各 Viewer | 純顯示，接 content/url/diff 等 prop | 無副作用，declarative |

### 6.3 狀態管理（Zustand）

選擇 Zustand 而非 Context 的原因：`terminal_output` 是高頻事件（每個字元觸發），Context value 變更會 re-render 所有 consumer，Zustand 的 `subscribe()` 可完全繞過 React render cycle。

```typescript
// appStore.ts
interface AppState {
  connectionState: 'connecting' | 'connected' | 'disconnected' | 'error';
  pendingOutput: string | null;   // 高頻，useTerminal 用 subscribe 消費，不進 render
  sidecarContent: SidecarContent | null;
  // actions...
}
export const useAppStore = create<AppState>()(subscribeWithSelector(...));
```

### 6.4 useWebSocket hook

```
職責：
  建立 WebSocket → 監聽 open/message/close/error
  message 解析後依 type 分派到 store
  send(msg) 含 pending queue（斷線時暫存）
  指數退避自動重連（1s → 2s → 4s → ... 上限 30s）
  元件 unmount 時清除
```

### 6.5 useTerminal hook

```
職責：
  初始化 xterm.js Terminal + FitAddon + WebLinksAddon
  ResizeObserver 監聽容器尺寸變化，呼叫 fitAddon.fit()
  subscribe store.pendingOutput → terminal.write()（繞過 React render）
  terminal.onData → 呼叫 options.onData（送往 WebSocket）
```

### 6.6 Sidecar Viewer 選擇邏輯

```typescript
function resolveViewer(content: SidecarContent | null): ViewerType {
  if (!content) return 'empty';
  if (content.kind === 'url') return 'iframe';
  if (content.kind === 'diff') return 'diff';
  const ext = content.payload.split('.').pop()?.toLowerCase();
  if (ext === 'md') return 'markdown';
  if (['png','jpg','jpeg','gif','webp','svg'].includes(ext ?? '')) return 'image';
  if (ext === 'pdf') return 'pdf';
  return 'empty';
}
```

| 偵測類型 | Viewer | 技術 |
|---|---|---|
| `.md` | MarkdownViewer | react-markdown + remark-gfm + rehype-highlight |
| `.png/.jpg/.gif` | ImageViewer | 原生 `<img>` |
| `.pdf` | PdfViewer | react-pdf（pdfjs-dist worker） |
| `localhost` URL | IframeViewer | `<iframe sandbox="allow-scripts allow-same-origin">` |
| git diff | DiffViewer | diff2html `html()` + dangerouslySetInnerHTML |

### 6.7 package.json 關鍵依賴

```json
{
  "dependencies": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "@xterm/xterm": "^5.5.0",
    "@xterm/addon-fit": "^0.10.0",
    "@xterm/addon-web-links": "^0.11.0",
    "zustand": "^4.5.0",
    "react-markdown": "^9.0.0",
    "remark-gfm": "^4.0.0",
    "rehype-highlight": "^7.0.0",
    "highlight.js": "^11.10.0",
    "react-pdf": "^7.7.0",
    "pdfjs-dist": "^4.4.0",
    "diff2html": "^3.4.48"
  },
  "devDependencies": {
    "typescript": "^5.5.0",
    "vite": "^5.4.0",
    "@vitejs/plugin-react": "^4.3.0"
  }
}
```

---

## 七、完整資料流

```
使用者鍵盤輸入
  → xterm.js onData
  → useTerminal → options.onData(data)
  → useWebSocket send({ type: 'terminal_input', data })
  → WebSocket 傳至後端
  → TerminalWebSocketHandler → session.writeToPty(bytes)
  → OS PTY → shell process (stdin)

shell process (stdout)
  → OS PTY → TerminalSession.pty.getInputStream()
  → PtyOutputRouter（Virtual Thread 讀取）
  ├─ 分流 1：SidecarNotifier.sendRawOutput() → { type: "terminal_output", data: Base64 }
  │           → WebSocket → useWebSocket 解析
  │           → store.appendTerminalOutput()
  │           → useTerminal subscribe → terminal.write(Uint8Array)  ← 不觸發 React render
  └─ 分流 2：AnsiStripper.strip() → OutputDetectionEngine.feed()
             → PatternMatcher.match() → DetectionResult
             → SidecarNotifier.sendSidecarSuggestion() → { type: "sidecar_suggestion", ... }
             → WebSocket → useWebSocket 解析
             → store.setSidecarContent()
             → Sidecar.tsx re-render → resolveViewer() → 自動渲染對應 Viewer
```

---

## 八、已知風險與對策

### 後端風險

| 風險 | 影響 | 對策 |
|---|---|---|
| OSC 序列跨 chunk 截斷 | ANSI strip 殘留、URL 提取失敗 | AnsiStripper 在行緩衝後的完整行執行，不對 chunk 執行 |
| AI CLI 進度列 `\r` 重繪 | 行緩衝累積大量假行，干擾偵測 | 行切割邏輯同時認 `\r`，偵測端做 dedup |
| multi-byte UTF-8 跨 chunk 截斷 | replacement character 出現 | 使用 `CharsetDecoder` 累積至完整行再解碼 |
| WebSocketSession 非 thread-safe | 並發 send 造成 IllegalStateException | 用 `ConcurrentWebSocketSessionDecorator` 包裝 |
| pty4j native lib 找不到 | UnsatisfiedLinkError，無法開 PTY | 確認 jar 包含目前 Windows/macOS 架構對應的 native library，或設 `-Dpty4j.preferred.native.folder` |
| /api/files path traversal | 讀取根目錄外檔案 | `Path.normalize()` + `startsWith(root)` + 副檔名白名單，見 5.5 |
| PTY cols 太小（80） | AI CLI 誤換行，干擾檔案路徑偵測 | 初始 220 cols，前端啟動後立即送 resize |
| process orphan（Spring 異常停止） | PTY child process 殘留 | `@PreDestroy` + JVM shutdown hook 呼叫 `destroyAll()` |

### 前端風險

| 風險 | 影響 | 對策 |
|---|---|---|
| FitAddon 在 DOM 未佈局時呼叫 | 取得 0x0 尺寸 | `requestAnimationFrame(() => fitAddon.fit())` |
| @xterm/xterm v5 vs 舊 xterm v4 API 差異 | import 路徑錯誤、CSS 路徑錯誤 | 全部使用 `@xterm/*` 系列，CSS: `@xterm/xterm/css/xterm.css` |
| iframe 被 X-Frame-Options 擋掉 | localhost 服務無法預覽 | v1 接受，只支援無 X-Frame-Options 的自起服務 |
| PDF.js worker 未設定 | PDF 無法載入 | main.tsx 一次性設定 `GlobalWorkerOptions.workerSrc` |
| Sidecar viewer 快速切換閃爍 | 視覺體驗差 | v1 接受，v2 加 CSS transition 或 key 管理 |
| 重連期間輸入遺失 | session 斷線 | v1 接受（session 重開本來就斷），UI 顯示 connectionState |

---

## 九、開發環境設置

### 後端啟動

Windows PowerShell：
```powershell
cd eagletab-backend
.\mvnw.cmd spring-boot:run
# 後端監聽 http://localhost:8080
# WebSocket 端點：ws://localhost:8080/ws/terminal
```

macOS：
```bash
cd eagletab-backend
./mvnw spring-boot:run
# 後端監聽 http://localhost:8080
# WebSocket 端點：ws://localhost:8080/ws/terminal
```

### 前端啟動
```bash
cd eagletab-frontend
npm install
npm run dev
# 前端監聽 http://localhost:5173
```

### vite.config.ts 代理設定（避免 CORS）
```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/ws': { target: 'ws://localhost:8080', ws: true },
      '/api': { target: 'http://localhost:8080' },
    },
  },
});
```

---
