!/usr/bin/env bash
# Usage: ./blog.sh start|stop|restart|status
set -euo pipefail

APP_DIR=/home/ec2-user/simple-blog
JAR="$APP_DIR/simple-blog-0.0.1-SNAPSHOT.jar"
ENV_FILE="$APP_DIR/blog.env"
PID_FILE="$APP_DIR/blog.pid"
LOG_FILE="$APP_DIR/blog.log"

running() { [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; }

start() {
  if running; then echo "already running (pid $(cat "$PID_FILE"))"; return; fi
  [[ -f "$ENV_FILE" ]] || { echo "missing $ENV_FILE" >&2; exit 1; }
  set -a; source "$ENV_FILE"; set +a          # export the secrets to the java process only
  mkdir -p "$MEDIA_DIR"
  nohup java -Xmx512m -jar "$JAR" --spring.profiles.active=prod \
    >> "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  echo "started (pid $!)"
}

stop() {
  if ! running; then echo "not running"; rm -f "$PID_FILE"; return; fi
  kill "$(cat "$PID_FILE")"                    # SIGTERM: graceful shutdown
  for _ in {1..30}; do running || break; sleep 1; done
  running && kill -9 "$(cat "$PID_FILE")"
  rm -f "$PID_FILE"; echo "stopped"
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) running && echo "running (pid $(cat "$PID_FILE"))" || echo "not running" ;;
  *) echo "usage: $0 start|stop|restart|status" >&2; exit 1 ;;
esac
