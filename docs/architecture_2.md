# EagleTab — 架構規劃文件

> 參考 AT.field 概念，用 Java + TypeScript 實作的個人開發工具（同時作為作品集）
>
> **文件定位**：本文件保留高階需求與原始決策摘要；技術版本、訊息協定、模組結構與實作細節一律以 `technical-architecture.md` 為準。

---

## 1. 專案目標與範疇

| 項目 | 決定 |
|---|---|
| 主要用途 | 自用開發工具，順便當作品集 |
| 使用者規模 | 單人使用，不做多租戶 |
| 終端機 | 真實 shell（可執行系統指令、跑程式） |
| AI 整合方式 | **CLI 委派模式**——不自己接 LLM API，讓使用者在 terminal 裡直接跑 `claude` / `codex` / `aider` 等既有 CLI 工具 |
| 安全模型 | 不做 Docker 沙盒隔離（v1 不需要，之後要公開展示再加） |
| 持久化 | 不做。記憶體存即可，重開頁面 session 斷掉沒關係 |

**v1 明確不做（避免過度工程）**：
- 多租戶權限系統
- Docker per-session 沙盒
- SSH / Serial 連線管理
- AI Agent 自動執行模式（AI 自己跑多步驟指令鏈）
- Session 持久化 / 歷史紀錄儲存

---

## 2. 技術棧

**後端**
- Java 21 / Spring Boot 3.3.x
- WebSocket（terminal I/O + 結構化訊息）
- `pty4j`（JetBrains 開源函式庫，開真實 pseudo-terminal）

**前端**
- TypeScript + React + Vite
- `xterm.js`（終端機渲染，本身就是 TS 寫的）
- Markdown 渲染、PDF.js、diff viewer（依需求逐步加）

---

## 3. 整體架構

```
┌─────────────────────────────────────────────────┐
│                   瀏覽器（前端）                    │
│  ┌──────────┬──────────────────┬──────────────┐  │
│  │ Navigator│   TerminalView    │   Sidecar    │  │
│  │ (左側)   │   (中間, xterm.js)│  (右側預覽)   │  │
│  └──────────┴──────────────────┴──────────────┘  │
└───────────────────────┬───────────────────────────┘
                         │ WebSocket
┌───────────────────────┴───────────────────────────┐
│              Spring Boot 後端                       │
│  ┌─────────────────┐   ┌─────────────────────────┐ │
│  │ Terminal Engine  │   │ Output Detection Engine │ │
│  │ (pty4j 真實 PTY) │──▶│ (regex 比對 stdout)      │ │
│  └─────────────────┘   └─────────────────────────┘ │
│              │                      │                │
│         真實 shell process     偵測結果（檔案/URL/diff）│
└──────────────────────────────────────────────────────┘
```

核心邏輯：**AI CLI 在平台原生 terminal 裡正常執行，後端額外監看輸出內容，偵測到值得預覽的東西就通知前端在 Sidecar 開啟。**

---

## 4. 核心模組

### 4.1 Terminal Engine（後端）
- 用 `pty4j` 開一個平台原生 shell process
- shell 解析順序：`EAGLETAB_SHELL` 覆寫；Windows 依序使用 `pwsh.exe`、`powershell.exe`、`cmd.exe`；macOS 使用 `$SHELL`，未設定時使用 `/bin/zsh`
- Git Bash 是 Windows 的可選覆寫，不是必要依賴
- 一個 WebSocket session ↔ 一個 PTY process
- stdin 從前端來，stdout/stderr 往兩個方向送：
  1. 原樣轉發給前端 xterm.js（保證 terminal 本身行為完全正常）
  2. 額外送一份去除 ANSI escape code 的純文字版本給 Output Detection Engine

### 4.2 Output Detection Engine（後端，取代原本規劃的「AI Service」）
- 對純文字版輸出做 regex 比對，偵測：
  - 檔案路徑（絕對/相對路徑 + 常見副檔名：`.md` `.png` `.jpg` `.pdf` `.html` `.json` `.diff`）
  - `localhost` / `127.0.0.1` URL
  - Git diff 區塊（`diff --git` 開頭，或 `git diff` 指令後的輸出）
- 偵測到後透過 WebSocket 送 `sidecar_suggestion` 事件給前端
- **這是整個專案技術含量最高、最值得在作品集強調的部分**（即時 stream parsing、ANSI escape 處理、pattern matching）

### 4.3 前端三欄式 UI
- `Navigator`：檔案/連線/session 清單（v1 先做固定假資料即可）
- `TerminalView`：包 xterm.js，純粹顯示終端機
- `Sidecar`：依 `sidecar_suggestion` 事件動態渲染對應 viewer

---

## 5. WebSocket 訊息協定

同一條 WebSocket，用 `type` 欄位區分用途：

```typescript
// 前端 → 後端
{ type: "terminal_input", data: string }
{ type: "terminal_resize", cols: number, rows: number }

// 後端 → 前端
{ type: "terminal_output", data: string }              // Base64 raw bytes，解碼後灌給 xterm.js
{ type: "sidecar_suggestion", kind: "file" | "url" | "diff", payload: string }
```

---

## 6. Sidecar Viewer 對應規則

| 偵測類型 | Viewer |
|---|---|
| `.md` 檔案路徑 | Markdown 渲染器 |
| `.png` / `.jpg` 檔案路徑 | 圖片顯示 |
| `.pdf` 檔案路徑 | PDF.js 嵌入 |
| `localhost` URL | iframe 嵌入 |
| git diff 區塊 | diff viewer（如 diff2html） |

---

## 7. MVP 開發步驟（每步可驗證）

1. **基礎終端機**：Spring Boot + pty4j 開平台原生 shell，前端 xterm.js 顯示
   → 驗證：Windows 能執行 `Get-ChildItem`、`cd`；macOS 能執行 `ls`、`cd`
2. **訊息協定與可用終端機**：把 terminal I/O 與 resize 包成結構化 WebSocket 訊息，接通 xterm.js
   → 驗證：可操作已安裝的全螢幕互動式 CLI、方向鍵與 ANSI 色彩，視窗縮放後 PTY 尺寸正確
3. **三欄式骨架**：Navigator（假資料）+ TerminalView + Sidecar（空白佔位）
   → 驗證：版面排版正確，resize 正常
4. **輸出偵測（先做兩種）**：檔案路徑 + localhost URL 偵測，送出 `sidecar_suggestion`
   → 驗證：Windows 以 PowerShell 輸出 `Join-Path (Get-Location) 'report.md'` 並執行 `py -m http.server`；macOS 輸出 `$PWD/report.md` 並執行 `python3 -m http.server`，兩者都能正確收到事件
5. **Sidecar 渲染**：Markdown、圖片、PDF、localhost iframe viewer，收到事件自動開啟
   → 驗證：上一步偵測到的東西真的能在右側看到內容
6. **Git diff viewer**：偵測並渲染 raw diff，完成 v1
   → 驗證：在 Git repository 執行 `git diff`，Sidecar 顯示 side-by-side diff

**v2+ 後續擴充**：多終端機分頁、Navigator 接真實檔案系統、Session 持久化、Docker 沙盒、SSH / Serial

---

## 8. 待確認事項

> **已確認**：Sidecar 偵測到內容後**自動開啟**最新一筆（不跳通知、不需使用者按按鈕）。
> **已確認**：專案名稱為 **EagleTab**。
