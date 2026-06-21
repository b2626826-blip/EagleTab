package com.eagletab.terminal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// session registry，讓 WebSocket 訊息能用 session ID 找到對應 PTY。
@Component
public class TerminalSessionRegistry {
    private final Map<String, TerminalSession> sessions = new ConcurrentHashMap<>();
    
    public void register(TerminalSession session) {
        sessions.put(session.getId(), session);
    }

    public TerminalSession get(String sessionId) {
        return sessions.get(sessionId);
    }

        public TerminalSession remove(String sessionId) {
        return sessions.remove(sessionId);
    }

}
