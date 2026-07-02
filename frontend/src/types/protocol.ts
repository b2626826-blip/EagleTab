export type TerminalInputMessage = {
    type: 'terminal_input'
    data: string
}

export type TerminalResizeMessage = {
    type: 'termianl_resize'
    cols: number
    rows: number
}

export type TerminalOutputMessage = {
    type: 'terminal_output'
    data: string
}

export type ClientMessage = TerminalInputMessage | TerminalResizeMessage
export type ServerMessage = TerminalOutputMessage