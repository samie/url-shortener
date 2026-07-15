#!/usr/bin/env bash
# Side-load the built images into the remote MicroK8s containerd (no registry).
# Run build.sh first.
#   ./k8s/upload.sh <ssh-host>     (or an env-file setting SSH_HOST)
set -euo pipefail

[ -f "${1:-}" ] && { source "$1"; shift; }
SSH_HOST="${1:-${SSH_HOST:?set SSH_HOST or pass <ssh-host>}}"

for m in urlshortener-server urlshortener-ui; do
  docker save "$m:latest" | gzip \
    | ssh "$SSH_HOST" 'f=$(mktemp); gunzip >"$f"; microk8s images import "$f"; rm "$f"'
done
