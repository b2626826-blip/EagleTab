package com.eagletab.terminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.web.socket.WebSocketSession;

/** 驗證 TerminalEngine 與 Windows 原生 PTY 的核心生命週期。 */
class TerminalEngineTest {

    /** 確認 PTY 可啟動、工作階段會註冊、輸出會轉送，且關閉後完成移除。 */
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
            // 建立真正的 PTY，確認各元件的整合行為而非只驗證 mock 呼叫。
            TerminalSession session =
                    engine.createSession(webSocketSession);

            assertThat(session.getPtyProcess().isAlive()).isTrue();
            assertThat(registry.get("test-session")).isSameAs(session);
            verify(outputRouter).startReading(session);
        } finally {
            // 即使 assertion 失敗也要終止 PTY，避免測試程序殘留。
            engine.closeSession("test-session");
        }

        assertThat(registry.get("test-session")).isNull();
    }
}
