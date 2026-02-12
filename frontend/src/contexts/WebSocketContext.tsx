import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import websocketService, { type ConnectionStatus } from '@/services/websocket';

interface WebSocketContextValue {
  status: ConnectionStatus;
}

const WebSocketContext = createContext<WebSocketContextValue>({
  status: 'disconnected',
});

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<ConnectionStatus>(websocketService.status);

  useEffect(() => {
    websocketService.connect();

    const removeListener = websocketService.onStatusChange(setStatus);

    return () => {
      removeListener();
      websocketService.disconnect();
    };
  }, []);

  return (
    <WebSocketContext.Provider value={{ status }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketContext(): WebSocketContextValue {
  return useContext(WebSocketContext);
}
