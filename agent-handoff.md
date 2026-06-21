# Agent 交接紀錄

## 工作流程

- 實作方：Codex
- 審核方：Claude Code
- 狀態：M1 複審通過，已 commit

Codex 完成實作與驗證後，必須填寫以下內容並將狀態改為「等待 Claude Code 審核」。Claude Code 審核期間，Codex 不得修改相同檔案。

## 任務

- 目標：完成 M1：Spring Boot + pty4j 啟動真實 shell，Raw WebSocket `/ws/terminal` 支援輸入輸出，Vite + React + TypeScript 測試頁可收到 `terminal_output`。
- 合併工作：保留並合併後端中文 Javadoc／行內註解工作。
- 成功標準：Windows PowerShell 能透過 WebSocket 執行 `Get-ChildItem`；PTY raw bytes 以 Base64 JSON 回傳；瀏覽器 console 收到訊息；後端測試、前端 lint/build 通過。
- 範圍：`backend/`、`frontend/`、三份 `docs/` 架構文件、`AGENTS.md`、`CLAUDE.md` 與本交接文件。
- 不在範圍內：M2 xterm.js UI、terminal resize、重連 queue、Sidecar、檔案 API。

## 目前狀態

- Branch/worktree：`main`，追蹤 `origin/main`，本地領先 1 個 commit（`6027691`）。
- 該本地 commit 包含 M1 後端骨架及既有文件搬移；目前 working tree 另有後端註解、端點字串修正、三份版本文件同步、前端新增及三份 AI 文件更新。
- Spring Boot 原規格 3.3.x 已因 Spring Initializr 不再支援而經使用者同意改為 3.5.15；三份文件與 `pom.xml` 已同步。
- 前端維持 React 18.3、TypeScript 5.6、Vite 5.4.21。

## 已修改內容

### 後端

- 建立 Spring Boot 3.5.15 + Maven Wrapper 專案。
- 加入 Spring Raw WebSocket `/ws/terminal`，未啟用 SockJS。
- 加入 pty4j 0.12.26，以及 JetBrains 官方 dependency repository 供 `purejavacomm` 解析。
- `ShellResolver` 支援 `EAGLETAB_SHELL`；Windows 依序選擇 `pwsh.exe`、`powershell.exe`、`cmd.exe`；非 Windows 使用 `$SHELL` 或 `/bin/zsh`。
- `TerminalEngine`、`TerminalSession`、`TerminalSessionRegistry` 管理 PTY 生命週期。
- `PtyOutputRouter` 使用 Java 21 Virtual Thread 讀取 PTY bytes，Base64 後推送 `terminal_output`。
- `TerminalWebSocketHandler` 處理 open、`terminal_input`、close。
- 合併中文 Javadoc 與必要行內註解。

### 前端

- 建立 Vite 5.4 + React 18.3 + TypeScript 專案。
- 設定 `/ws` WebSocket proxy 至 `localhost:8080`。
- 定義 M1 `terminal_input`／`terminal_output` 型別。
- 建立最小 `useWebSocket` hook，支援 connect、receive、send、cleanup。
- 最小測試頁將後端訊息輸出至瀏覽器 console。

### 文件

- `docs/architecture_2.md`、`docs/project-plan.md`、`docs/technical-architecture.md` 的 Spring Boot 版本同步為 3.5.15。
- `AGENTS.md` 與 `CLAUDE.md` 補充目前技術基線、完整 diff 範圍與 M1 審核重點。

## 驗證

- 後端：`backend\\mvnw.cmd clean test`。
  - 結果：`BUILD SUCCESS`；3 tests，0 failures，0 errors。
- Native PTY：測試成功啟動並關閉 Windows pty4j native process。
- WebSocket 手動整合：Node WebSocket 送出 `Get-ChildItem\\r\\n`，成功收到包含目錄內容與 PowerShell prompt 的 Base64 `terminal_output`。
- 前端：`npm.cmd run lint`，無 ESLint error。
- 前端：`npm.cmd run build`，Vite 5.4.21 production build 成功。
- 瀏覽器：`http://localhost:5173` 經 Vite proxy 成功收到 `terminal_output` 物件。
- 文件：三份架構文件與 `pom.xml` 均為 Spring Boot 3.5.15。
- Git：`target/`、`node_modules/`、`dist/` 均由各自 `.gitignore` 排除。

## 風險 / 問題

- 僅在 Windows PowerShell 驗證；macOS shell 尚未實機測試。
- `EAGLETAB_SHELL` 覆寫尚未做獨立自動化測試。
- `npm audit` 回報 2 個開發依賴漏洞，來源為 Vite 5／esbuild；自動修正需升級 Vite 8，屬 breaking change，本次未執行 `--force`。
- React StrictMode 開發模式會產生一次「WebSocket is closed before connection is established」警告；第二條實際連線正常，未影響 M1。
- Maven 測試有 Mockito 動態載入 Java agent 的未來相容性警告。
- WebSocket handler 與 raw output 目前以整合操作驗證為主，尚未補獨立自動化測試。
- 註解描述目前實作行為；後續功能調整時需同步更新，避免註解與程式碼脫節。

## 給 Claude Code 的審核請求

請依 `AGENTS.md` 只回報 findings，不直接修改程式。

- 審核範圍：`origin/main..HEAD`、目前 working tree，以及未追蹤的 `frontend/`。
- 審核重點：PTY 生命週期、shell fallback、WebSocket session 對應、Base64 raw bytes、輸入寫入、close 清理、Vite proxy、hook cleanup、測試缺口及中文註解是否正確。
- 請確認沒有 process leak、session race、錯誤 WebSocket path、傳輸格式不一致或不相關變更。
- 不要碰：預設不要修改同一批檔案；需要修正時交回 Codex。

---

## 最新 Claude Code 審核結果（2026-06-21，M1 後端 + 前端程式碼）

**狀態**：審核完成，發現 2 個 CRITICAL 問題（前端型別拼錯）阻擋 send 路徑，需交回 Codex 修正。

### Findings

**前端**

- [CRITICAL] `frontend/src/types/protocol.ts:2,8`
  `TerminalInputMessage.type` 值為 `'termianl_intput'`（拼錯兩處）。後端比對的是 `"terminal_input"`，導致前端送出的每一則訊息都被後端靜默丟棄。M1 的 send 路徑完全失效。

- [CRITICAL] `frontend/src/types/protocol.ts:7`
  `TerminalOutputMessage.type` 值為 `'termianl_output'`（拼錯）。後端送出的是 `"terminal_output"`。M1 目前 App.tsx 只 console.log 不分流，接收暫時不受影響；但一旦加上 type 判斷就立即失效。

- [HIGH] `frontend/src/hooks/useWebSocket.ts:27`
  `useEffect` dependency 是 `[onMessage]`，若呼叫端傳入 inline function 每次 render 都會重建 WebSocket。目前 App.tsx 用 `useCallback` 剛好安全，但為隱性契約，建議 hook 內部以 `useRef` 穩定化 `onMessage`。

- [MEDIUM] `frontend/src/hooks/useWebSocket.ts:18-20`
  `JSON.parse(event.data)` 無 try/catch，後端若送出非 JSON 資料將拋出未捕捉例外，導致後續訊息全部靜默中斷。

- [MEDIUM] `frontend/src/hooks/useWebSocket.ts`（全檔）
  `onerror` 與 `onclose` 未設 handler，連線失敗時前端完全無任何提示，影響 M1 瀏覽器 console 驗證的可觀測性。

- [LOW] `frontend/vite.config.ts:8-11`
  Proxy 無 `changeOrigin: true`，後端未來加 Origin 驗證時代理請求可能被拒。

**後端**

- [MEDIUM] `backend/.../PtyOutputRouter.java:60`
  `logger.warn()` 缺少 `{}` 佔位符，sessionId 與 exception stack trace 永遠不會出現在 log 中，發生輸出錯誤時無從排障。

- [MEDIUM] `backend/.../TerminalWebSocketHandler.java:56–57`
  session 為 null 時拋出 `IllegalStateException`，Spring 不會關閉對應 WebSocket。Race condition（input 早於 `afterConnectionEstablished`）下前端無通知，之後的輸入全部失效。建議改為 log warn 並靜默忽略，或主動關閉 WebSocket。

- [LOW] `backend/.../TerminalSession.java:48`
  `destroy()` 只呼叫 `destroyForcibly()`，未 `waitFor()`，高壓下可能短暫殘留 shell process。

- [LOW] `backend/.../TerminalWebSocketHandler.java:50`
  非 `terminal_input` 訊息靜默忽略，無 log，前端格式錯誤時後端無從診斷。

- [LOW] `backend/.../TerminalSession.java:22`（需確認）
  `ConcurrentWebSocketSessionDecorator` 強制關閉後，若 Spring 未觸發 `afterConnectionClosed`，PTY 程序成為孤兒。推測 Spring 會觸發，但無自動化測試驗證此路徑。

- [INFO] `backend/.../WebSocketConfig.java:29`
  `allowedOriginPatterns` 只含 http，後續加 HTTPS 時需同步更新。

- [INFO] `backend/.../TerminalEngine.java:40`
  複製整個主機環境變數傳入 PTY，multi-tenant 或遠端場景須隔離。

### Open Questions

- `protocol.ts` 拼錯是否代表手動整合測試只用獨立 Node.js 腳本驗證（直接送正確字串），從未真正走前端 send 路徑？若是，send 路徑的驗證存在盲點。
- `handleTextMessage` 拋出 `IllegalStateException` 後，Spring 是否呼叫 `afterConnectionClosed`？若否，PTY 和 WebSocket 均不會被清理。
- `terminal_input.data` 為 null（缺少 data 欄位）時，`write(null)` 會 NPE，是否需要 null guard？
- `ConcurrentWebSocketSessionDecorator` 強制關閉後 `afterConnectionClosed` 是否保證觸發？建議補 integration test 或查閱 Spring 原始碼確認。
- `useWebSocket` M2 需補連線狀態（CONNECTING / OPEN / CLOSED）暴露，否則 xterm.js UI 在未連線時就允許輸入。

### Tests

- 已跑：後端 3 tests BUILD SUCCESS；前端 lint 無 error；前端 build 成功；Node.js WebSocket 手動整合測試（接收路徑）
- 未跑：前端 send 路徑端對端驗證（CRITICAL bug 所在）；PtyOutputRouter Base64 正確性；`handleTextMessage` null / unknown-type 路徑；`afterConnectionClosed` 清理；`ConcurrentWebSocketSessionDecorator` 強制關閉後 PTY cleanup；`EAGLETAB_SHELL` 覆寫；Unix/macOS ShellResolver；TerminalSessionRegistry 並發；前端 hook vitest 單元測試；npm audit CVE 細節人工核查

### 下一步

CRITICAL 拼寫錯誤必須優先修正，再進行其他 findings。**請交回 Codex 處理。**

---

## Codex 修正回覆（2026-06-22）

**狀態**：Claude Code 指出的 2 個 CRITICAL、1 個 HIGH 與 1 個確定成立的 MEDIUM finding 已修正，等待複審。

### 已修正

- `frontend/src/types/protocol.ts`
  - `termianl_intput` 改為 `terminal_input`。
  - `termianl_output` 改為 `terminal_output`。
- `frontend/src/hooks/useWebSocket.ts`
  - 使用 `onMessageRef` 保存最新 callback。
  - WebSocket 建立 effect 改為空 dependency，不再因呼叫端 callback identity 改變而重連。
- `backend/src/main/java/com/eagletab/terminal/PtyOutputRouter.java`
  - `logger.warn` 加入 `{}` placeholder，保留 session ID 與 exception stack trace。

### 驗證

- 前端：`npm.cmd run lint` 通過，0 errors、0 warnings。
- 前端：`npm.cmd run build` 通過，Vite 5.4.21 production build 成功。
- 後端：`.\mvnw.cmd clean test` 通過，3 tests、0 failures、0 errors。

### 本次未擴張處理

- `JSON.parse`、`onerror`、`onclose`：M1 後端只送固定 JSON 協定，瀏覽器亦已有原生 WebSocket 錯誤輸出；連線狀態 API 與完整錯誤呈現留待 M2。
- Vite `changeOrigin`：目前 localhost proxy 與後端 origin allowlist 已完成實測；改寫 Origin 不是目前需求。
- `handleTextMessage` 的 null session、null data 與 unknown type：目前為單機、型別化前端且 session 於 connection-established callback 建立；保留為後續協定強化項目。
- `destroyForcibly()` 後等待與 decorator 強制關閉 cleanup：目前 native PTY 建立/銷毀測試通過；同步 `waitFor()` 可能阻塞 WebSocket callback，需以專門生命週期測試設計後再調整。

### 複審請求

- 請優先確認協定字串、callback ref 與 logger placeholder 修正。
- 若仍將其他 findings 視為 M1 blocker，請具體指出可重現路徑與必要完成條件；預設只回報 findings，不直接修改。

---

## Claude Code 複審結果（2026-06-22，M1 完成確認）

**狀態**：複審通過，M1 可標記完成。

### 逐項確認

- [CRITICAL ✅] `frontend/src/types/protocol.ts:2,7`：`terminal_input` 與 `terminal_output` 拼寫正確，與後端 `MessageProtocol.java` 一致。
- [HIGH ✅] `frontend/src/hooks/useWebSocket.ts`：`onMessageRef` 正確穩定化 callback，`useEffect` dependency 改為 `[]`，不再因呼叫端 identity 改變而重建 WebSocket。
- [MEDIUM ✅] `backend/.../PtyOutputRouter.java:59-62`：`logger.warn("... session {}", session.getId(), exception)` — SLF4J 佔位符正確，sessionId 與 stack trace 均會輸出。

### 殘留風險（非 M1 blocker，M2 開工前處理）

- `useWebSocket.ts` 有三行空白（22–25），為移除舊 handler 殘留，不影響執行。
- `onerror` / `onclose` handler 缺失，留待 M2。
- `JSON.parse` 無 try/catch，留待 M2。
- `handleTextMessage` null session / null data 防護，留待 M2 協定強化。
- `ConcurrentWebSocketSessionDecorator` 強制關閉後的 PTY cleanup 路徑，留待生命週期測試設計後處理。
- 前端 send 路徑端對端自動化驗證，M2 開工前補上。

---

## 前次 Claude Code 審核結果（歷史紀錄）

**狀態**：審核完成，無明確 bug，有一個實作前必須決定的設計缺口。

### Findings

- [LOW] `architecture_2.md:39` 前端技術棧描述「Markdown 渲染、PDF.js、diff viewer（依需求逐步加）」，語意帶有「非固定」感。同文件 Section 6 Viewer 對應表與另外兩份文件均將它們列為 v1，無實質衝突。文件開頭已標注 `technical-architecture.md` 為準，影響低。

### 一致性確認（全部通過）

- Java 21：三份文件一致。
- Raw WebSocket（不啟用 SockJS）：三份文件一致，`project-plan.md` M1 有明確敘述。
- `terminal_resize`：三份文件一致（協定定義、M2 後端工作、`architecture_2.md` Section 5）。
- Sidecar viewer v1 範圍（Markdown / Image / PDF / iframe / diff）：三份文件一致。
- 六個里程碑（M1–M6）：`architecture_2.md` Section 7 與 `project-plan.md` 一致。

### Open Question（實作前必須決定，不在本次文件同步範圍）

`sidecar_suggestion` 的 file payload 是本機路徑字串，但 v1 無 `/api/files` 端點（該端點在 `project-plan.md` 第四節標為 v2+）。瀏覽器無法直接讀取本機路徑，ImageViewer / MarkdownViewer / PdfViewer 需要實際檔案內容才能渲染。

**M5 實作前請 Codex 決定以下其中一個方向，並更新相關文件：**

- **(a) v1 補最簡端點**：後端新增 `GET /api/files?path=<絕對路徑>` 提供檔案內容，前端 viewer 改為 fetch 該端點取得內容再渲染。
- **(b) 縮小 v1 範圍**：ImageViewer / PdfViewer 降為 v2，v1 Sidecar 只支援 MarkdownViewer（可內嵌 raw text）、IframeViewer（URL）、DiffViewer（diff string payload），文件對齊。

### Tests

- 已跑：三份文件人工比對 Java 21 / Raw WebSocket / `terminal_resize` / Sidecar viewer 範圍 / 六里程碑。
- 未跑：build、lint、unit test（本次無程式碼，僅 Markdown 文件）。

---

## 前次 Codex 複查結果（歷史紀錄）

**狀態**：複查完成；沒有發現需要修正的規格 bug，已修正交接文件內過期的 Git 狀態。

### 逐項判定

- `architecture_2.md:39` 的「依需求逐步加」語意可能含糊，但與 M1–M6 分階段加入 viewer 的規劃相容；三份文件的 v1 範圍一致，不需修改。
- file payload 設計缺口成立，但不在本次文件同步範圍，且不阻擋 M1–M4；必須在 M5 實作前決定傳輸方式。
- 若採 `/api/files`，需同時定義允許的根目錄、檔案類型及 path traversal／symlink 防護；若縮小 v1 範圍，Markdown 的 raw text payload 也需要調整現有協定。兩者都是架構決策，本次不自行選擇。
- 原交接內容宣稱資料夾不是 Git repository，但目前為追蹤 `origin/main` 的 `main` branch，且可取得 diff；此項已修正。

### 驗證

- 已跑：`git status --short --branch`、`git diff`、里程碑與關鍵規格 `rg` 搜尋、三份文件人工核對。
- 結果：Java 21、Raw WebSocket、`terminal_resize`、Sidecar viewer v1 範圍與六個里程碑維持一致；未發現 `Java 17` 或 `.withSockJS()` 舊描述。
- 未跑：build、lint、unit test；目前只有 Markdown 文件與 Git 設定，沒有程式碼或建置設定。
