# AGENTS.md

## 協作模式

本專案採用固定雙 Agent 流程：

1. Codex 負責實作與驗證。
2. Codex 完成後更新 `agent-handoff.md`。
3. Claude Code 依交接內容與目前 diff 進行審核。
4. Claude Code 預設只回報 findings，不直接修改程式；需要修正時交回 Codex。

同一時間只允許一個 Agent 修改同一批檔案。

## 專案規則

開始工作前：

- 明確說明假設。
- 需求有多種解讀時，先提出差異。
- 不確定且會影響方向時，先詢問。
- 先定義成功標準，再修改程式。
- 只改和任務直接相關的內容。

## 實作要求（Codex）

- 先檢查 repo 狀態與相關檔案。
- 使用能解決問題的最簡單方案。
- 不加入未要求的功能、設定或抽象化。
- 符合現有程式碼風格，不重構無關程式碼。
- 不覆蓋使用者或其他 Agent 的修改。
- 跑最小且直接相關的測試、lint 或 build。
- 完成後更新 `agent-handoff.md`，再交由 Claude Code 審核。

## 專案基線

- 後端位於 `backend/`：Java 21、Spring Boot 3.5.15、Maven Wrapper、pty4j 0.12.26。
- 前端位於 `frontend/`：React 18.3、TypeScript 5.6、Vite 5.4。
- M1 WebSocket 端點為 `/ws/terminal`，使用 Raw WebSocket，不啟用 SockJS。
- `terminal_input.data` 是 UTF-8 輸入；`terminal_output.data` 是 PTY raw bytes 的 Base64 字串。
- Windows shell fallback：`pwsh.exe`、`powershell.exe`、`cmd.exe`；`EAGLETAB_SHELL` 可覆寫。
- 後端驗證：在 `backend/` 執行 `.\mvnw.cmd clean test`。
- 前端驗證：在 `frontend/` 執行 `npm.cmd run lint` 與 `npm.cmd run build`。

版本、協定或路徑調整時，必須同步更新 `docs/` 內相關架構文件與 `agent-handoff.md`。

## 審核要求（Claude Code）

開始審核前，同時檢查：

- `origin/main..HEAD` 的本地提交。
- working tree 的 tracked diff。
- untracked files；不能只看 `git diff`，以免漏掉新模組。
- `agent-handoff.md` 列出的已跑與未跑驗證。

依序檢查：

1. 正確性問題。
2. 行為 regression。
3. 缺少測試。
4. 安全或資料處理風險。
5. 過度複雜的修改。
6. 不相關的變更。

審核時不要修改程式碼或與 Codex 同時操作相同檔案。若使用者明確要求 Claude Code 修正，才可轉為實作方。

審核回覆格式：

```text
Findings:
- [嚴重度] file:line 問題與影響

Open Questions:
- ...

Tests:
- 已跑：
- 未跑：
```

如果沒有明確問題，直接說「沒有發現明確 bug」，並列出剩餘風險與未執行的檢查。

