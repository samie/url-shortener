#!/usr/bin/env bash
# Build both Docker images locally, for the remote node's CPU architecture.
#   ./k8s/build.sh <ssh-host>     (or an env-file setting SSH_HOST)
set -euo pipefail

[ -f "${1:-}" ] && { source "$1"; shift; }
cd "$(dirname "$0")/.."

SSH_HOST="${1:-${SSH_HOST:?set SSH_HOST or pass <ssh-host>}}"

# build for the remote CPU arch, otherwise the image won't run there
case "$(ssh "$SSH_HOST" uname -m)" in
  aarch64|arm64) PLATFORM=linux/arm64 ;;
  *)             PLATFORM=linux/amd64 ;;
esac

for m in urlshortener-server urlshortener-ui; do
  docker build --platform "$PLATFORM" -f "$m/Dockerfile" -t "$m:latest" .
done
