# Docker

## Quick Run

```bash
docker run -d \
  -p 8080:8080 \
  -v utem-data:/app/data \
  --name utem \
  sddmhossain/utem-core:latest
```

## With Authentication

```bash
docker run -d \
  -p 8080:8080 \
  -v utem-data:/app/data \
  -e UTEM_SECURITY_ENABLED=true \
  -e UTEM_ADMIN_PASSWORD=changeme \
  --name utem \
  sddmhossain/utem-core:latest
```

## Docker Compose

```yaml
version: '3.8'
services:
  utem:
    image: sddmhossain/utem-core:0.9.2
    ports:
      - "8080:8080"
    volumes:
      - utem-data:/app/data
      - ./application.properties:/app/application.properties
    environment:
      - UTEM_SECURITY_ENABLED=true
      - UTEM_ADMIN_PASSWORD=changeme
    restart: unless-stopped

volumes:
  utem-data:
```

## Persisting Data

All data is stored in `/app/data/utem.db` inside the container. Always mount a volume:

```bash
-v utem-data:/app/data       # named volume (recommended)
-v /host/path:/app/data      # bind mount
```

## Custom Configuration

Mount your own `application.properties`:

```bash
-v ./application.properties:/app/application.properties
```

## Tags

| Tag | Description |
|---|---|
| `latest` | Latest stable release |
| `0.9.2` | Specific version |

All tags: [Docker Hub](https://hub.docker.com/r/sddmhossain/utem-core/tags)

## Reverse Proxy (nginx)

```nginx
server {
    listen 80;
    server_name utem.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

::: warning WebSocket
The `Upgrade` and `Connection` headers are required for the real-time dashboard (WebSocket/SockJS).
:::
