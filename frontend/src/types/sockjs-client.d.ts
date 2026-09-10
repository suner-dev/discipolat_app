declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string | string[], _reserved?: any, options?: any);
    onopen: () => void;
    onclose: () => void;
    onmessage: (e: MessageEvent) => void;
    onerror: (e: Event) => void;
    send(data: string): void;
    close(): void;
    readyState: number;
  }
  export default SockJS;
}
