package com.eagletab.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.eagletab.terminal.TerminalEngine;
import com.eagletab.terminal.TerminalSession;
import com.eagletab.terminal.TerminalSessionRegistry;
import com.eagletab.ws.MessageProtocol.ClientMessage;
import com.fasterxml.jackson.databind.ObjectMapper;


/** 將 WebSocket 連線的建立與關閉事件轉交給終端機引擎處理。 */
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

      private final TerminalEngine terminalEngine;
    private final TerminalSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public TerminalWebSocketHandler(
            TerminalEngine terminalEngine,
            TerminalSessionRegistry registry,
            ObjectMapper objectMapper
    ) {
        this.terminalEngine = terminalEngine;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        terminalEngine.createSession(session);
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws Exception {
        ClientMessage clientMessage = objectMapper.readValue(
                message.getPayload(),
                ClientMessage.class
        );

        if (!"terminal_input".equals(clientMessage.type())) {
            return;
        }

        TerminalSession terminalSession = registry.get(session.getId());
        if (terminalSession == null) {
            throw new IllegalStateException("Terminal session not found");
        }

        terminalSession.write(clientMessage.data());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        terminalEngine.closeSession(session.getId());
    }
}
