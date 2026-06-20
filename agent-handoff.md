# Agent 交接紀錄

## 工作流程

- 實作方：Codex
- 審核方：Claude Code
- 狀態：等待 Claude Code 審核

Codex 完成實作與驗證後，必須填寫以下內容並將狀態改為「等待 Claude Code 審核」。Claude Code 審核期間，Codex 不得修改相同檔案。

## 任務

- 目標：以 `technical-architecture.md` 為主要規格，同步舊架構文件與開發計劃。
- 成功標準：Java 版本、Raw WebSocket、`terminal_resize`、Sidecar viewer 範圍與驗證方式在三份文件中一致。
- 範圍：`architecture_2.md`、`project-plan.md`、`technical-architecture.md` 與本交接文件。
- 不在範圍內：建立前後端程式碼、初始化 Git、重新設計尚未定案的 file payload 傳輸格式。

## 目前狀態

- Branch/worktree：目前資料夾不是 Git repository，無 branch/worktree 與可用 diff。
- Dirty files：無法透過 Git 判定；本次修改檔案見下節。
- 重要決策：`technical-architecture.md` 是唯一主要技術規格；Raw WebSocket 不啟用 SockJS；圖片/PDF viewer 與 `terminal_resize` 屬於 v1。

## 已修改內容

- 修改檔案：`architecture_2.md`、`project-plan.md`、`technical-architecture.md`、`agent-handoff.md`
- 行為變更：僅文件變更。舊架構文件加入規格優先順序並同步 Java 21、訊息協定及 MVP 範圍；計劃書同步 v1 內容及可執行驗證命令；技術文件移除與 Raw WebSocket 矛盾的 `.withSockJS()`。

## 驗證

- 已跑命令：使用 `rg` 搜尋 `Java 17`、`SockJS`、`terminal_resize`、圖片/PDF v2、舊 Markdown 驗證命令與 diff payload 矛盾敘述。
- 結果：搜尋未發現上述舊描述；三份規格對已確認項目一致。
- 未跑檢查：build、test、lint。
- 未跑原因：目前只有 Markdown 文件，尚無程式碼或建置設定。

## 風險 / 問題

- 已知風險：資料夾不是 Git repository，Claude Code 無法依一般流程檢查 diff。
- 待確認問題：`sidecar_suggestion` 的 file payload 目前只定義為字串路徑，但瀏覽器如何安全讀取並顯示本機 Markdown、圖片與 PDF，技術文件尚未定義完整傳輸方式。

## 給 Claude Code 的審核請求

請 review 本次修改檔案，依 `AGENTS.md` 的 Review 標準回報 findings。此資料夾不是 Git repository，沒有可用 diff；預設不要修改文件。

- 審核重點：確認三份規格對 Java 21、Raw WebSocket、v1 viewer 範圍與六個里程碑描述一致。
- 不要碰：不要建立程式碼或擴充 file payload 協定；僅回報 findings。
