import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';
type StatusListener = (status: ConnectionStatus) => void;
type ReconnectCountListener = (count: number) => void;
type MessageHandler<T> = (message: T) => void;

const BASE_DELAY_MS = 1_000;
const MAX_DELAY_MS = 30_000;

class WebSocketService {
  private client: Client;
  private statusListeners = new Set<StatusListener>();
  private reconnectCountListeners = new Set<ReconnectCountListener>();
  private _status: ConnectionStatus = 'disconnected';
  private _reconnectCount = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 0, // disabled — we manage reconnect with exponential backoff
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this._reconnectCount = 0;
        this.notifyReconnectCount();
        this.setStatus('connected');
      },
      onDisconnect: () => {
        this.setStatus('disconnected');
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message'], frame.body);
        this.setStatus('error');
      },
      onWebSocketClose: () => {
        this.setStatus('disconnected');
        this.scheduleReconnect();
      },
    });
  }

  get status(): ConnectionStatus {
    return this._status;
  }

  get reconnectCount(): number {
    return this._reconnectCount;
  }

  private setStatus(status: ConnectionStatus) {
    this._status = status;
    this.statusListeners.forEach((listener) => listener(status));
  }

  private notifyReconnectCount() {
    this.reconnectCountListeners.forEach((l) => l(this._reconnectCount));
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer !== null) return; // already scheduled
    const delay = Math.min(BASE_DELAY_MS * Math.pow(2, this._reconnectCount), MAX_DELAY_MS);
    this._reconnectCount++;
    this.notifyReconnectCount();
    console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${this._reconnectCount})`);
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (!this.client.active) {
        this.setStatus('connecting');
        this.client.activate();
      }
    }, delay);
  }

  connect(): void {
    if (this.client.active) return;
    this.setStatus('connecting');
    this.client.activate();
  }

  disconnect(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (!this.client.active) return;
    this.client.deactivate();
  }

  onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    return () => {
      this.statusListeners.delete(listener);
    };
  }

  onReconnectCountChange(listener: ReconnectCountListener): () => void {
    this.reconnectCountListeners.add(listener);
    return () => {
      this.reconnectCountListeners.delete(listener);
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
