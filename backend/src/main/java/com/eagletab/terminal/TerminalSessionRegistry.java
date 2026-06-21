package com.eagletab.terminal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** 以 WebSocket session ID 保存目前作用中的終端機工作階段。 */
@Component
public class TerminalSessionRegistry {

    /** WebSocket 與 PTY 執行緒會同時存取，因此使用執行緒安全的 Map。 */
    private final Map<String, TerminalSession> sessions = new ConcurrentHashMap<>();

    /** 登記新建立的終端機工作階段。 */
    public void register(TerminalSession session) {
        sessions.put(session.getId(), session);
    }

    /** 依 WebSocket session ID 取得對應的終端機工作階段。 */
    public TerminalSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    /** 移除並回傳指定工作階段，讓呼叫端接續釋放 PTY。 */
        public TerminalSession remove(String sessionId) {
        return sessions.remove(sessionId);
    }

}
