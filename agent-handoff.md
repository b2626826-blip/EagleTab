# Agent 交接紀錄

## 工作流程

- 實作方：Codex
- 審核方：Claude Code
- 狀態：等待 Codex 實作

Codex 完成實作與驗證後，必須填寫以下內容並將狀態改為「等待 Claude Code 審核」。Claude Code 審核期間，Codex 不得修改相同檔案。

## 任務

- 目標：以 `technical-architecture.md` 為主要規格，同步舊架構文件與開發計劃。
- 成功標準：Java 版本、Raw WebSocket、`terminal_resize`、Sidecar viewer 範圍與驗證方式在三份文件中一致；file payload 傳輸方式已決定並寫入規格。
- 範圍：`architecture_2.md`、`project-plan.md`、`technical-architecture.md` 與本交接文件。
- 不在範圍內：建立前後端程式碼、初始化 Git。

## 目前狀態

- Branch/worktree：`main`，追蹤 `origin/main`。
- Dirty files：`agent-handoff.md`（Claude Code 審核結果與 Codex 複查紀錄）。
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

- 已決定：file payload 傳輸採用 (a) 方案——v1 補 `GET /api/files?path=` 端點，規格已寫入 `technical-architecture.md` 5.5。
- 剩餘風險：path traversal 防護必須在 M5 實作時全部到位（`Path.normalize()` + `startsWith(root)` + 副檔名白名單 + symlink 確認），見 5.5 安全限制表。

## 給 Claude Code 的審核請求

請 review 本次修改檔案，依 `AGENTS.md` 的 Review 標準回報 findings；預設不要修改文件。

- 審核重點：確認三份規格對 Java 21、Raw WebSocket、v1 viewer 範圍與六個里程碑描述一致。
- 不要碰：不要建立程式碼或擴充 file payload 協定；僅回報 findings。

---

## Claude Code 審核結果

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

## Codex 複查結果

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
