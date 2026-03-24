# Installation

## Requirements

- Java 17 or later
- No database setup required — uses embedded SQLite

## Option 1: JAR (Recommended)

Download `utem-core-0.9.1.jar` from [GitHub Releases](https://github.com/shossain786/utem-core/releases).

```bash
java -jar utem-core-0.9.1.jar
```

The server starts on port 8080. Data is stored in `utem.db` in the current directory.

### Windows — start.bat

For Windows users, download `start.bat` alongside the JAR:

```
utem-core-0.9.1.jar
start.bat
```

Double-click `start.bat` — it starts the server and opens the browser automatically.

## Option 2: Docker

```bash
docker run -d \
  -p 8080:8080 \
  -v utem-data:/app/data \
  --name utem \
  sddmhossain/utem-core:latest
```

Open [http://localhost:8080](http://localhost:8080).

To persist data across container restarts, the volume mounts `/app/data` where SQLite stores `utem.db`.

### Docker Compose

```yaml
version: '3.8'
services:
  utem:
    image: sddmhossain/utem-core:0.9.1
    ports:
      - "8080:8080"
    volumes:
      - utem-data:/app/data
    environment:
      - UTEM_SECURITY_ENABLED=true
      - UTEM_ADMIN_PASSWORD=changeme
    restart: unless-stopped

volumes:
  utem-data:
```

## Option 3: Build from Source

```bash
git clone https://github.com/shossain786/utem-core.git
cd utem-core
mvn package -DskipTests
java -jar target/utem-core-0.9.1.jar
```

## Verify

Once started, visit [http://localhost:8080](http://localhost:8080). You should see the UTEM dashboard.

Check the server is healthy:

```bash
curl http://localhost:8080/utem/runs
```

Expected: `{"content":[],"totalElements":0,...}`
