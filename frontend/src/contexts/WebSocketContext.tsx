import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import websocketService, { type ConnectionStatus } from '@/services/websocket';

interface WebSocketContextValue {
  status: ConnectionStatus;
  reconnectCount: number;
}

const WebSocketContext = createContext<WebSocketContextValue>({
  status: 'disconnected',
  reconnectCount: 0,
});

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<ConnectionStatus>(websocketService.status);
  const [reconnectCount, setReconnectCount] = useState(websocketService.reconnectCount);

  useEffect(() => {
    websocketService.connect();

    const removeStatusListener = websocketService.onStatusChange(setStatus);
    const removeCountListener = websocketService.onReconnectCountChange(setReconnectCount);

    return () => {
      removeStatusListener();
      removeCountListener();
      websocketService.disconnect();
    };
  }, []);

  return (
    <WebSocketContext.Provider value={{ status, reconnectCount }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketContext(): WebSocketContextValue {
  return useContext(WebSocketContext);
}
