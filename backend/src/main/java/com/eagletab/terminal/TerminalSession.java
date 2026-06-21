package com.eagletab.terminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import com.pty4j.PtyProcess;

// 建立單一 PTY 連線的資料容器 TerminalSession。它只負責持有 WebSocket、PTY、寫入 shell 與銷毀程序
public class TerminalSession {
    
    private final WebSocketSession webSocketSession;
    private final PtyProcess ptyProcess;

    public TerminalSession(WebSocketSession webSocketSession, PtyProcess ptyProcess) {
        this.webSocketSession = new ConcurrentWebSocketSessionDecorator(webSocketSession, 5_000, 512 * 1024);
        this.ptyProcess = ptyProcess;
    }
    
     public String getId() {
        return webSocketSession.getId();
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public PtyProcess getPtyProcess() {
        return ptyProcess;
    }

    public void write(String data) throws IOException {
        ptyProcess.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
        ptyProcess.getOutputStream().flush();
    }

    public void destroy() {
        ptyProcess.destroyForcibly();
    }
}
