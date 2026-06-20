# EagleTab — 完整計劃書 v1.0

> **規格依據**：本計劃的技術版本、訊息協定與模組設計以 `technical-architecture.md` 為準；若內容不一致，應先更新本文件。

---

## 一、專案概覽

**EagleTab** 是一個以瀏覽器為介面的個人開發工具，核心功能是讓使用者在瀏覽器裡操作真實 shell，並在右側 Sidecar 面板自動預覽 AI CLI 工具（claude、codex、aider）輸出的檔案、URL 與 diff。

---

## 二、成功標準（v1 定義）

- [ ] 在瀏覽器裡能打 `ls`、`cd`、`vim`，行為與 iTerm 相同
- [ ] 在 terminal 跑 `claude` / `codex`，輸出的 `.md` 檔案路徑自動在 Sidecar 渲染 Markdown
- [ ] 在 terminal 執行 `python3 -m http.server`，Sidecar 自動嵌入 iframe 顯示 localhost 頁面
- [ ] 在 terminal 執行 `git diff`，Sidecar 自動顯示 diff viewer
- [ ] terminal 輸出圖片或 PDF 路徑時，Sidecar 自動顯示對應 viewer
- [ ] 三欄版面在視窗縮放時正常伸縮

---

## 三、MVP 開發路線（6 個里程碑）

每個里程碑均可獨立驗證，完成後才進入下一步。

---

### M1：後端可連 shell

**目標**：Spring Boot + pty4j 能開真實 shell，前後端 WebSocket 連通。

**後端工作**：
- Java 21 + Spring Boot 3.3.x 專案骨架（`EagleTabApplication`、`WebSocketConfig`）
- 使用 `/ws/terminal` Raw WebSocket 端點，不啟用 SockJS
- `TerminalWebSocketHandler`（onOpen/onMessage/onClose）
- `TerminalEngine` + `TerminalSession` + `TerminalSessionRegistry`
- `PtyOutputRouter`：只做 raw forward，不做偵測

**前端工作**：
- Vite + React + TS 專案骨架
- `useWebSocket` hook 基本版（connect、send、接收）
- 最小化 HTML 測試頁（無 UI，只 console.log）

**驗證**：
```bash
# 後端
websocat ws://localhost:8080/ws/terminal
# 送 { "type": "terminal_input", "data": "ls\n" }
# 收到 terminal_output 含 ls 結果的 Base64 字串

# 前端
console.log(msg) 顯示 { type: "terminal_output", data: "<base64>" }
```

**完成條件**：WebSocket 連線不斷，PTY 可正常輸入輸出。

---

### M2：前端終端機可用

**目標**：瀏覽器裡出現真實終端機，可正常操作。

**前端工作**：
- `useTerminal` hook（xterm.js 初始化、FitAddon、ResizeObserver）
- `TerminalView` 元件掛載到 DOM
- Base64 decode → `terminal.write(Uint8Array)`
- `terminal.onData` → `send({ type: "terminal_input", data })`

**後端工作**：
- `terminal_resize` 訊息處理（更新 PTY WinSize）

**驗證**：
```bash
# 瀏覽器開頁面
# 打 vim → 游標正常、方向鍵正常、顏色正常（ANSI 256 color）
# 視窗縮放 → 終端機自動適應，不出現截斷或偏移
```

**完成條件**：終端機行為與 iTerm 一致。

---

### M3：三欄版面骨架

**目標**：三欄 UI 版面完成，Sidecar 空白佔位。

**前端工作**：
- `App.tsx` CSS Grid（220px / 1fr / 420px）
- `Navigator` 元件（假資料 FileList）
- `Sidecar` 元件（空白 EmptyViewer）
- Zustand store 基本結構（`appStore.ts`）
- 全域 CSS 變數（主題色、欄寬）

**驗證**：
```
目視確認三欄各占正確寬度
視窗縮放時中間欄 1fr 正確伸縮，左右欄固定寬度
```

**完成條件**：版面穩定，無 layout shift。

---

### M4：輸出偵測上線

**目標**：後端能偵測 stdout 中的檔案路徑與 localhost URL，並推送 sidecar_suggestion。

**後端工作**：
- `AnsiStripper`（含單元測試）
- `OutputDetectionEngine`（行緩衝，MAX_BUFFER 8KB）
- `PatternMatcher`（FILE_ABS、FILE_REL、LOCALHOST_URL 三種 pattern）
- `DetectionResult` record
- `SidecarNotifier.sendSidecarSuggestion()`
- `PtyOutputRouter` 接通偵測分流
- Dedup LRU cache（per-session，capacity 100，TTL 5s）

**前端工作**：
- `useWebSocket` 解析 `sidecar_suggestion` → `store.setSidecarContent()`
- `Sidecar` 元件讀取 store，console.log 顯示收到的事件

**驗證**：
```bash
# 在 terminal 執行
echo /tmp/test.md
python3 -m http.server 8000

# 前端 console 應出現
{ type: "sidecar_suggestion", kind: "file", payload: "/tmp/test.md" }
{ type: "sidecar_suggestion", kind: "url",  payload: "http://localhost:8000" }
```

**完成條件**：偵測正確觸發，無重複事件（同一 URL 5 秒內只送一次）。

---

### M5：Sidecar 自動渲染

**目標**：Sidecar 收到事件後自動顯示對應 Viewer。

**前端工作**：
- `resolveViewer()` 選擇邏輯
- `MarkdownViewer`（react-markdown + remark-gfm + rehype-highlight）
- `ImageViewer`（原生 `<img>`）
- `PdfViewer`（react-pdf + pdfjs-dist）
- `IframeViewer`（`<iframe sandbox>`，只允許 localhost）
- `EmptyViewer`（佔位文字）
- `Sidecar` 接通 store，收到事件自動 mount 對應 viewer

**後端工作**：
- 依 `technical-architecture.md` 的 `sidecar_suggestion` 協定提供 file payload
- 限制 Sidecar 只能處理偵測規則允許的檔案類型

**驗證**：
```bash
# 在 terminal 輸出檔案路徑
echo "$PWD/README.md"
# Sidecar 自動顯示渲染後的 Markdown

echo "$PWD/screenshot.png"
echo "$PWD/spec.pdf"
# Sidecar 自動顯示圖片與 PDF

python3 -m http.server 3000
# Sidecar 自動顯示 localhost:3000 的 iframe
```

**完成條件**：Sidecar 自動切換，不需使用者點擊任何按鈕。

---

### M6：git diff viewer 完成

**目標**：git diff 偵測與渲染上線，MVP 完整。

**後端工作**：
- `PatternMatcher` 加入 `GIT_DIFF` pattern
- `OutputDetectionEngine` 加入 context window（Deque，容量 5 行）

**前端工作**：
- `DiffViewer`（diff2html `html()` + dangerouslySetInnerHTML）
  - v1 簡化：後端 payload 直接帶 raw diff string
- `Sidecar` 解析 kind=diff → DiffViewer

**後端工作（配合簡化）**：
- 偵測到 `diff --git` 後，累積後續 diff 輸出（最多 200 行）放入 payload

**驗證**：
```bash
# 在有 git repo 的目錄執行
git diff
# Sidecar 自動顯示 side-by-side diff viewer
```

**完成條件**：MVP 六項成功標準全部通過。

---

## 四、後續擴充路線（v2+，不在 v1 範圍）

| 功能 | 優先度 | 說明 |
|---|---|---|
| Navigator 接真實檔案系統 | 高 | 後端新增 `/api/files` REST endpoint |
| 多終端機分頁 | 中 | `TerminalTabs` + 多 session 管理 |
| Session 持久化 | 低 | 重開頁面保留 terminal 歷史 |
| Docker 沙盒隔離 | 低 | 公開展示時需要 |
| SSH / Serial 連線 | 低 | 多機器支援 |

---

## 五、里程碑時程估算

> 以每日 2-4 小時開發時間估算

| 里程碑 | 估算時間 | 累積 |
|---|---|---|
| M1：後端可連 shell | 1-2 天 | 第 1-2 天 |
| M2：前端終端機可用 | 1 天 | 第 2-3 天 |
| M3：三欄版面骨架 | 0.5 天 | 第 3-4 天 |
| M4：輸出偵測上線 | 2 天 | 第 5-6 天 |
| M5：Sidecar 自動渲染 | 1-2 天 | 第 7-8 天 |
| M6：git diff viewer | 1 天 | 第 9 天 |

**預計 MVP 完成時間**：約 9-10 個工作天

---

## 六、作品集亮點（Portfolio 視角）

技術含量最高、最值得強調的部分：

1. **即時 Stream Parsing**：PTY stdout 是連續 byte stream，邊讀邊解析、行緩衝、ANSI 去除、不阻塞 terminal 渲染。
2. **Output Detection Engine**：正規表達式模式比對在真實 CLI 輸出中的應用，含多種 edge case 處理（AI CLI 特有的 OSC 序列、進度列 `\r` 重繪、UTF-8 跨 chunk 截斷）。
3. **xterm.js + WebSocket 高頻資料流**：繞過 React render cycle（Zustand subscribe）直接寫入 xterm.js，避免 setState 的效能瓶頸。
4. **pty4j Virtual PTY**：讓 Web UI 控制真實 OS shell process 的完整實作。

---

## 七、檔案索引

| 文件 | 說明 |
|---|---|
| `architecture_2.md` | 原始架構討論文件（需求確認紀錄） |
| `technical-architecture.md` | 完整技術架構規格（本次產出） |
| `project-plan.md` | 開發計劃與里程碑（本文件） |
| `agent-handoff.md` | Codex ↔ Claude Code 交接紀錄 |
| `AGENTS.md` | 雙 Agent 協作規則 |
| `CLAUDE.md` | Claude Code 角色定義 |
