import { useEffect, useRef } from "react";
import { Terminal } from "@xterm/xterm";
import { FitAddon } from "@xterm/addon-fit";
import '@xterm/xterm/css/xterm.css'

export function useTerminal(){
    const containerRef = useRef<HTMLDivElement>(null)
    const terminalRef = useRef<Terminal | null>(null)

    useEffect(() => {
        const container = containerRef.current
        if (!container) return

        const terminal = new Terminal({ cursorBlink : true })
        const fitAddon = new FitAddon()

        terminal.loadAddon(fitAddon)
        terminal.open(container)
        fitAddon.fit()
        terminalRef.current = terminal

        return () => {
            terminalRef.current = null
            terminal.dispose()
        }
    }, [])

    return { containerRef}
}