package com.eagletab.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import com.eagletab.ws.MessageProtocol.TerminalOutput;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PtyOutputRouter {

    private static final Logger logger = LoggerFactory.getLogger(PtyOutputRouter.class);

    private final ObjectMapper oggObjectMapper;

    public PtyOutputRouter(ObjectMapper objectMapper) {
        this.oggObjectMapper = objectMapper;
    }

    public void startReading(TerminalSession session) {
        Thread.ofVirtual().name("pty-reader-" + session.getId()).start(() -> forwardOutput(session));
    }
    
    private void forwardOutput(TerminalSession session) {
        
        byte[] buffer = new byte[4096];

        try (InputStream output = session.getPtyProcess().getInputStream()){
            
            int bytesRead;

            while ((bytesRead = output.read(buffer)) != -1) {
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                String base64 = Base64.getEncoder().encodeToString(chunk);
                String json = oggObjectMapper.writeValueAsString( new TerminalOutput(base64));
            
                session.getWebSocketSession().sendMessage( new TextMessage(json));
            }

        } catch (IOException exception ) {

            if (session.getWebSocketSession().isOpen()) {
                logger.warn( "系統無法輸出", session.getId(), exception);
        
            }
        }
    }
}
