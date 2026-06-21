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

/** 持續讀取 PTY 輸出，並將結果轉送到對應的 WebSocket 用戶端。 */
@Component
public class PtyOutputRouter {

    /** 記錄仍在連線時發生的 PTY 讀取或 WebSocket 傳送錯誤。 */
    private static final Logger logger = LoggerFactory.getLogger(PtyOutputRouter.class);

    /** 將終端機輸出訊息序列化成前端可解析的 JSON。 */
    private final ObjectMapper oggObjectMapper;

    /** 使用 Spring 共用的 JSON 設定建立輸出路由器。 */
    public PtyOutputRouter(ObjectMapper objectMapper) {
        this.oggObjectMapper = objectMapper;
    }

    /**
     * 為指定終端機啟動獨立虛擬執行緒，避免阻塞 WebSocket 處理執行緒。
     */
    public void startReading(TerminalSession session) {
        Thread.ofVirtual().name("pty-reader-" + session.getId()).start(() -> forwardOutput(session));
    }

    /** 讀取 PTY 的原始位元組，包裝成協定訊息後送往前端。 */
    private void forwardOutput(TerminalSession session) {
        // 每次最多讀取 4 KiB，兼顧互動回應速度與傳輸次數。
        byte[] buffer = new byte[4096];

        try (InputStream output = session.getPtyProcess().getInputStream()) {

            int bytesRead;

            while ((bytesRead = output.read(buffer)) != -1) {
                // 終端機輸出可能不是完整 UTF-8 字元；使用 Base64 可保留原始位元組。
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                String base64 = Base64.getEncoder().encodeToString(chunk);
                String json = oggObjectMapper.writeValueAsString(new TerminalOutput(base64));

                session.getWebSocketSession().sendMessage(new TextMessage(json));
            }

        } catch (IOException exception) {

            // WebSocket 已關閉通常代表正常離線，不需要再記錄警告。
            logger.warn(
                    "Failed to forward PTY output for session {}",
                    session.getId(),
                    exception);

        }
    }
}
