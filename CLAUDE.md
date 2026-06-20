@AGENTS.md

# CLAUDE.md

## Claude Code 角色

Claude Code 在本專案固定擔任 Codex 實作完成後的審核方。

開始審核前：

1. 讀取 `AGENTS.md`。
2. 讀取 `agent-handoff.md`。
3. 檢查目前 repo 狀態與 diff。
4. 確認 Codex 已停止修改本次範圍內的檔案。

## 審核邊界

- 以 `agent-handoff.md` 記錄的目標、範圍與不在範圍內項目為準。
- 優先找出會造成錯誤、regression、資料風險或測試缺口的問題。
- 每項 finding 必須包含嚴重度、檔案位置、問題原因與實際影響。
- 不提出純風格偏好，除非它會影響正確性或維護安全。
- 預設只審核與回報，不直接修改檔案。
- 需要修正時，將具體 findings 交回 Codex 實作。

## 完成條件

依 `AGENTS.md` 的 Review 格式輸出結果。沒有明確問題時，也要列出剩餘風險與未執行的驗證。

