package com.eagletab.terminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import com.pty4j.PtyProcess;

/** 封裝單一 WebSocket 連線及其 PTY，提供輸入、輸出與關閉所需操作。 */
public class TerminalSession {

    /** 使用可安全序列化傳送的 decorator 包裝原始 WebSocket 連線。 */
    private final WebSocketSession webSocketSession;
    /** 此連線專用的作業系統虛擬終端機程序。 */
    private final PtyProcess ptyProcess;

    /** 建立 WebSocket 與 PTY 的一對一工作階段。 */
    public TerminalSession(WebSocketSession webSocketSession, PtyProcess ptyProcess) {
        // 限制單次傳送等待時間與緩衝區，避免慢速用戶端無限占用記憶體。
        this.webSocketSession = new ConcurrentWebSocketSessionDecorator(webSocketSession, 5_000, 512 * 1024);
        this.ptyProcess = ptyProcess;
    }

    /** 回傳 WebSocket ID，作為整個終端機工作階段的識別碼。 */
     public String getId() {
        return webSocketSession.getId();
    }

    /** 回傳用於傳送終端機輸出的 WebSocket 連線。 */
    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    /** 回傳底層 PTY，供輸出路由器讀取 Shell 輸出。 */
    public PtyProcess getPtyProcess() {
        return ptyProcess;
    }

    /** 將前端輸入以 UTF-8 寫入 Shell，並立即送出緩衝內容。 */
    public void write(String data) throws IOException {
        ptyProcess.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
        ptyProcess.getOutputStream().flush();
    }

    /** 強制終止工作階段所持有的 PTY 程序。 */
    public void destroy() {
        ptyProcess.destroyForcibly();
    }
}
