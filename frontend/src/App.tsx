import { useCallback } from 'react'
import { useWebSocket } from './hooks/useWebSocket'
import type { ServerMessage } from './types/protocol'

function App() {
  const handleMessage = useCallback((message: ServerMessage) => {
    console.log(message)
  }, [])

  useWebSocket({ onMessage: handleMessage })

  return (
    <main>
      <p>EagleTab M1 WebSocket test page</p>
      <p>Open the browser console to inspect terminal_output messages.</p>
    </main>
  )
}

export default App