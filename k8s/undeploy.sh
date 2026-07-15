#!/usr/bin/env bash
# Delete everything: the namespace and its data volume. Irreversible.
#   ./k8s/undeploy.sh <ssh-host>     (or an env-file setting SSH_HOST)
set -euo pipefail

[ -f "${1:-}" ] && { source "$1"; shift; }
SSH_HOST="${1:-${SSH_HOST:?set SSH_HOST or pass <ssh-host>}}"

read -rp "Delete namespace urlshortener on $SSH_HOST (data is lost)? [y/N] " r
[ "$r" = y ] || [ "$r" = Y ] || { echo "Aborted."; exit 0; }

ssh "$SSH_HOST" "microk8s kubectl delete namespace urlshortener --ignore-not-found"
