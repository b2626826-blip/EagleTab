package com.eagletab.terminal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

/** 負責建立真實 PTY、管理終端機工作階段，並啟動輸出轉送。 */
@Component
public class TerminalEngine {

    /** 依目前平台決定要啟動的 Shell。 */
    private final ShellResolver shellResolver;
    /** 保存 WebSocket ID 與終端機工作階段的對應關係。 */
    private final TerminalSessionRegistry registry;
    /** 將 PTY 輸出非同步傳送到 WebSocket。 */
    private final PtyOutputRouter outputRouter;

    /** 注入建立與管理終端機工作階段所需的元件。 */
    public TerminalEngine(
            ShellResolver shellResolver,
            TerminalSessionRegistry registry,
            PtyOutputRouter outputRouter) {
        this.shellResolver = shellResolver;
        this.registry = registry;
        this.outputRouter = outputRouter;
    }

    /**
     * 為新 WebSocket 連線建立 PTY，註冊工作階段並開始轉送 Shell 輸出。
     */
    public TerminalSession createSession(WebSocketSession webSocketSession)
            throws IOException {
        // 保留主機環境變數，並宣告前端終端機支援的色彩能力。
        Map<String, String> environment = new HashMap<>(System.getenv());
        environment.put("TERM", "xterm-256color");
        environment.put("COLORTERM", "truecolor");

        // 建立具固定初始尺寸的互動式 PTY，並將錯誤輸出合併到標準輸出。
        PtyProcess ptyProcess = new PtyProcessBuilder(shellResolver.resolve())
                .setEnvironment(environment)
                .setInitialColumns(220)
                .setInitialRows(50)
                .setRedirectErrorStream(true)
                .start();

        TerminalSession session = new TerminalSession(webSocketSession, ptyProcess);

        registry.register(session);
        outputRouter.startReading(session);
        return session;
    }

    /** 從 registry 移除指定工作階段，並終止其底層 PTY 程序。 */
    public void closeSession(String sessionId) {
        TerminalSession session = registry.remove(sessionId);
        if (session != null) {
            session.destroy();
        }
    }
}
