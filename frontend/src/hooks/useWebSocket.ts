import { useCallback, useEffect, useRef } from 'react'
import type { ClientMessage, ServerMessage } from '../types/protocol'

type UseWebSocketOptions = {
    onMessage: (message: ServerMessage) => void
}

export function useWebSocket({ onMessage }: UseWebSocketOptions) {
    const sockRef = useRef<WebSocket | null>(null)

    const onMessageRef = useRef(onMessage)

    useEffect(() => {
        onMessageRef.current = onMessage
    }, [onMessage])
    
    useEffect(() => {
        const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
        const url = `${protocol}://${window.location.host}/ws/terminal`
        const socket = new WebSocket(url)

        sockRef.current = socket



        socket.onmessage = (event) => {
            const message = JSON.parse(event.data) as ServerMessage
            onMessageRef.current(message)
        }

        return () => {
            socket.close()
            sockRef.current = null
        }
    }, [])

    const send = useCallback((message: ClientMessage) => {
        if (sockRef.current?.readyState !== WebSocket.OPEN) {
            return false
        }

        sockRef.current.send(JSON.stringify(message))
        return true
    }, [])

    return { send }
}
