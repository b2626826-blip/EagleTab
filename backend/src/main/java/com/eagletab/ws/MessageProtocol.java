package com.eagletab.ws;

/** 集中定義前端與後端交換的 WebSocket JSON 訊息格式。 */
public final class MessageProtocol {

    /** 協定類別只提供資料型別，不允許建立實例。 */
    private MessageProtocol() {
    }

    /** 前端送往後端的訊息，type 表示操作種類，data 或尺寸欄位保存操作內容。 */
    public record ClientMessage(String type, String data, Integer cols, Integer rows) {
    }

    /** 後端送往前端的終端機輸出；data 為 Base64 編碼的原始 PTY 位元組。 */
    public record TerminalOutput(String type, String data) {

        /** 建立固定為 terminal_output 類型的輸出訊息。 */
        public TerminalOutput(String data) {
            this("terminal_output", data);
        }
    }
}
