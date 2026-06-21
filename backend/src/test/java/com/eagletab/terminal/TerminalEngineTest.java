package com.eagletab.terminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.web.socket.WebSocketSession;

class TerminalEngineTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void startsAndClosesNativePty() throws Exception {
        WebSocketSession webSocketSession =
                mock(WebSocketSession.class);
        when(webSocketSession.getId()).thenReturn("test-session");

        TerminalSessionRegistry registry =
                new TerminalSessionRegistry();
        PtyOutputRouter outputRouter =
                mock(PtyOutputRouter.class);

        TerminalEngine engine = new TerminalEngine(
                new ShellResolver(),
                registry,
                outputRouter
        );

        try {
            TerminalSession session =
                    engine.createSession(webSocketSession);

            assertThat(session.getPtyProcess().isAlive()).isTrue();
            assertThat(registry.get("test-session")).isSameAs(session);
            verify(outputRouter).startReading(session);
        } finally {
            engine.closeSession("test-session");
        }

        assertThat(registry.get("test-session")).isNull();
    }
}