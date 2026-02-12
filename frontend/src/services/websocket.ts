import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';
type StatusListener = (status: ConnectionStatus) => void;
type MessageHandler<T> = (message: T) => void;

class WebSocketService {
  private client: Client;
  private statusListeners = new Set<StatusListener>();
  private _status: ConnectionStatus = 'disconnected';

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.setStatus('connected');
      },
      onDisconnect: () => {
        this.setStatus('disconnected');
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'], frame.body);
        this.setStatus('error');
      },
      onWebSocketClose: () => {
        this.setStatus('disconnected');
      },
    });
  }

  get status(): ConnectionStatus {
    return this._status;
  }

  private setStatus(status: ConnectionStatus) {
    this._status = status;
    this.statusListeners.forEach((listener) => listener(status));
  }

  connect(): void {
    if (this.client.active) return;
    this.setStatus('connecting');
    this.client.activate();
  }

  disconnect(): void {
    if (!this.client.active) return;
    this.client.deactivate();
  }

  onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    return () => {
      this.statusListeners.delete(listener);
    };
  }

  /**
   * Subscribe when connected, or queue for after connection.
   * Re-subscribes automatically on reconnect.
   * Returns an unsubscribe function.
   */
  subscribeSafe<T>(destination: string, handler: MessageHandler<T>): () => void {
    let subscription: StompSubscription | null = null;
    let unsubscribedEarly = false;

    const doSubscribe = () => {
      if (unsubscribedEarly || !this.client.connected) return;
      subscription = this.client.subscribe(destination, (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body) as T;
          handler(parsed);
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e);
        }
      });
    };

    if (this.client.connected) {
      doSubscribe();
    }

    const removeListener = this.onStatusChange((status) => {
      if (status === 'connected') {
        subscription?.unsubscribe();
        doSubscribe();
      }
    });

    return () => {
      unsubscribedEarly = true;
      subscription?.unsubscribe();
      removeListener();
    };
  }
}

const websocketService = new WebSocketService();
export default websocketService;
