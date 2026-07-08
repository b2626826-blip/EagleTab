export type TerminalInputMessage = {
    type: 'terminal_input'
    data: string
}

export type TerminalOutputMessage = {
    type: 'terminal_output'
    data: string
}

export type ClientMessage = TerminalInputMessage
export type ServerMessage = TerminalOutputMessage
