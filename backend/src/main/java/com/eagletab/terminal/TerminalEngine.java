package com.eagletab.terminal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

// TerminalEngine負責啟動真實 PTY、設定環境並註冊 session
@Component
public class TerminalEngine {

    private final ShellResolver shellResolver;
    private final TerminalSessionRegistry registry;
    private final PtyOutputRouter outputRouter;

    public TerminalEngine(
            ShellResolver shellResolver,
            TerminalSessionRegistry registry,
            PtyOutputRouter outputRouter) {
        this.shellResolver = shellResolver;
        this.registry = registry;
        this.outputRouter = outputRouter;
    }

    public TerminalSession createSession(WebSocketSession webSocketSession)
            throws IOException {
        Map<String, String> environment = new HashMap<>(System.getenv());
        environment.put("TERM", "xterm-256color");
        environment.put("COLORTERM", "truecolor");

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

    public void closeSession(String sessionId) {
        TerminalSession session = registry.remove(sessionId);
        if (session != null) {
            session.destroy();
        }
    }
}