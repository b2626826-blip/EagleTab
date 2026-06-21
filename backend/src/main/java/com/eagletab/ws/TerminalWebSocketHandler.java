package com.eagletab.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.eagletab.terminal.TerminalEngine;


// Sping Raw WebSocket 文字訊息處理器，負責處理來自前端的 WebSocket 連接和消息
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {
     private final TerminalEngine terminalEngine;

    public TerminalWebSocketHandler(TerminalEngine terminalEngine) {
        this.terminalEngine = terminalEngine;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        terminalEngine.createSession(session);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        terminalEngine.closeSession(session.getId());
    }
}