# EagleTab

以瀏覽器為介面的個人開發工具。核心是讓你在瀏覽器裡操作**真實 shell**，並在右側 Sidecar 面板自動預覽 AI CLI 工具（`claude` / `codex` / `aider`）輸出的檔案、localhost URL 與 git diff——不需點任何按鈕。

> 參考 AT.field 概念，用 Java + TypeScript 實作，同時作為作品集。單人自用，v1 不做多租戶、沙盒、持久化。

---

## 運作原理

AI CLI 在平台原生 terminal 裡正常執行，後端額外監看 stdout：偵測到值得預覽的內容（檔案路徑、`localhost` URL、`diff --git`），就即時推送給前端 Sidecar 自動渲染。

```
┌─────────────────────────────────────────────────┐
│                  瀏覽器（前端）                    │
│  ┌──────────┬──────────────────┬──────────────┐  │
│  │ Navigator│   TerminalView    │   Sidecar    │  │
│  │  220px   │   1fr / xterm.js  │    420px     │  │
│  └──────────┴──────────────────┴──────────────┘  │
└───────────────────────┬───────────────────────────┘
                        │ WebSocket /ws/terminal
┌───────────────────────┴───────────────────────────┐
│                Spring Boot 後端                     │
│  PtyOutputRouter ─┬─ raw forward → terminal_output │
│  (pty4j 真實 PTY)  └─ AnsiStrip → Detection →       │
│                      PatternMatcher → sidecar_suggestion
└───────────────────────┬───────────────────────────┘
                        │
                 平台原生 OS shell
```

## 技術棧

| | |
|---|---|
| **後端** | Java 21、Spring Boot 3.5.15、pty4j 0.12.26、Raw WebSocket（不啟用 SockJS） |
| **前端** | React 18.3、TypeScript 5、Vite 5.4、@xterm/xterm 5.5、Zustand |
| **Sidecar** | react-markdown、react-pdf、diff2html、`<iframe sandbox>` |

## 開發環境

**後端**（監聽 `http://localhost:8080`，WebSocket `ws://localhost:8080/ws/terminal`）

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

**前端**（監聽 `http://localhost:5173`，Vite proxy 轉發 `/ws` 與 `/api`）

```bash
cd frontend
npm install
npm run dev
```

### Shell 選擇

1. 環境變數 `EAGLETAB_SHELL` 覆寫（絕對路徑）
2. Windows 依序：`pwsh.exe` → `powershell.exe` → `cmd.exe`
3. macOS：`$SHELL`，未設定時 `/bin/zsh`

## WebSocket 協定

同一條連線，用 `type` 區分：

```typescript
// 前端 → 後端
{ type: "terminal_input",  data: string }               // UTF-8 鍵盤輸入
{ type: "terminal_resize", cols: number, rows: number }

// 後端 → 前端
{ type: "terminal_output",    data: string }            // PTY raw bytes 的 Base64
{ type: "sidecar_suggestion", kind: "file"|"url"|"diff", payload: string }
```

`terminal_output.data` 用 Base64 是因為 PTY 輸出含不合法 UTF-8 與控制字元，JSON text frame 無法直接安全傳輸。

## 里程碑

| 里程碑 | 目標 | 狀態 |
|---|---|---|
| M1 | 後端可連 shell，前後端 WebSocket 連通 | **完成** |
| M2 | 前端 xterm.js 終端機可用 | 未開始 |
| M3 | 三欄版面骨架 | 未開始 |
| M4 | 輸出偵測（檔案 / URL）上線 | 未開始 |
| M5 | Sidecar 自動渲染各 Viewer | 未開始 |
| M6 | git diff viewer，MVP 完整 | 未開始 |

## 文件

| 檔案 | 說明 |
|---|---|
| [docs/technical-architecture.md](docs/technical-architecture.md) | 完整技術架構規格（唯一真實來源） |
| [docs/project-plan.md](docs/project-plan.md) | 開發計劃與里程碑驗收條件 |
| [docs/architecture_2.md](docs/architecture_2.md) | 高階需求與原始決策摘要 |
| [AGENTS.md](AGENTS.md) / [CLAUDE.md](CLAUDE.md) | 雙 Agent 協作規則 |
</content>
</invoke>
