package com.eagletab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.eagletab.ws.TerminalWebSocketHandler;

/** 設定終端機使用的原生 WebSocket 端點與允許的前端來源。 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
        
    /** 處理終端機連線生命週期的 WebSocket handler。 */
    private final TerminalWebSocketHandler terminalWebSocketHandler;

    /** 由 Spring 注入終端機 WebSocket handler。 */
    public WebSocketConfig(TerminalWebSocketHandler terminalWebSocketHandler) {
        this.terminalWebSocketHandler = terminalWebSocketHandler;
    }

    /**
     * 將 handler 註冊為終端機端點，並只接受本機開發伺服器的跨來源連線。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
       registry.addHandler(terminalWebSocketHandler, "/ws/terminal")
        .setAllowedOriginPatterns("http://localhost:*");
    }
}
