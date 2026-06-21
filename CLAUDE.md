@AGENTS.md

# CLAUDE.md

## Claude Code 角色

Claude Code 在本專案固定擔任 Codex 實作完成後的審核方。

開始審核前：

1. 讀取 `AGENTS.md`。
2. 讀取 `agent-handoff.md`。
3. 檢查目前 repo 狀態與 diff。
4. 確認 Codex 已停止修改本次範圍內的檔案。

本次 M1 審核必須涵蓋：

- `origin/main..HEAD`，因後端 M1 位於本地領先提交中。
- working tree 中的後端註解、端點修正、版本文件與 AI 協作文件。
- untracked 的 `frontend/`；不能只用 `git diff` 判斷範圍。

## 審核邊界

- 以 `agent-handoff.md` 記錄的目標、範圍與不在範圍內項目為準。
- 優先找出會造成錯誤、regression、資料風險或測試缺口的問題。
- 每項 finding 必須包含嚴重度、檔案位置、問題原因與實際影響。
- 不提出純風格偏好，除非它會影響正確性或維護安全。
- 預設只審核與回報，不直接修改檔案。
- 需要修正時，將具體 findings 交回 Codex 實作。

## M1 審核重點

- `/ws/terminal` 必須是 Raw WebSocket，且前後端協定一致。
- WebSocket open/close、PTY 建立/銷毀與 registry 對應不得造成 process leak 或 session race。
- `terminal_input` 必須寫入正確 PTY；`terminal_output` 必須保留 raw bytes 並以 Base64 傳輸。
- Shell fallback、`EAGLETAB_SHELL`、Windows 啟動參數需符合文件。
- Vite proxy、`useWebSocket` cleanup、未追蹤前端檔案與測試缺口都需納入審核。
- 確認中文註解描述實際行為，沒有掩蓋或改變執行邏輯。

## 完成條件

依 `AGENTS.md` 的 Review 格式輸出結果。沒有明確問題時，也要列出剩餘風險與未執行的驗證。

