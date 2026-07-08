import type { RefObject } from 'react'

type TerminalViewProps = {
    containerRef : RefObject<HTMLDivElement>
}

export function TerminalView({ containerRef } : TerminalViewProps) {
    return <div ref = { containerRef } className='terminal-view' />
}