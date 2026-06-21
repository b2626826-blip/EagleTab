package com.eagletab.ws;

public final class MessageProtocol {

    private MessageProtocol() {
    }

    public record ClientMessage(String type, String data) {
    }

    public record TerminalOutput(String type, String data) {

        public TerminalOutput(String data) {
            this("terminal_output", data);
        }
    }
}