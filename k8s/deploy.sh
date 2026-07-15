#!/usr/bin/env bash
# Apply the manifests to a MicroK8s node and roll out.
#   ./k8s/deploy.sh <ssh-host> [local|letsencrypt]
#   ./k8s/deploy.sh <env-file>          # a file setting SSH_HOST/MODE/DOMAIN/...
# letsencrypt needs DOMAIN (and usually ACME_EMAIL); SHORTCODE_BASE_URL optional.
set -euo pipefail

[ -f "${1:-}" ] && { source "$1"; shift; }
cd "$(dirname "$0")"

SSH_HOST="${1:-${SSH_HOST:?set SSH_HOST or pass <ssh-host>}}"
MODE="${2:-${MODE:-local}}"

if [ "$MODE" = letsencrypt ]; then
  : "${DOMAIN:?set DOMAIN for letsencrypt mode}"
  INGRESS=30-ingress-letsencrypt.yml
  SHORTCODE_BASE_URL="${SHORTCODE_BASE_URL:-https://$DOMAIN/}"
else
  INGRESS=30-ingress-local.yml
  SHORTCODE_BASE_URL="${SHORTCODE_BASE_URL:-http://localhost/}"
fi

apply() {
  sed -e "s|__DOMAIN__|${DOMAIN:-}|g" \
      -e "s|__ACME_EMAIL__|${ACME_EMAIL:-}|g" \
      -e "s|__SHORTCODE_BASE_URL__|$SHORTCODE_BASE_URL|g" "$1" \
    | ssh "$SSH_HOST" "microk8s kubectl apply -f -"
}

# ingress before the deployments: it carries the ConfigMap the UI needs
for f in 00-namespace.yml "$INGRESS" 10-urlshortener.yml 20-admin-ui.yml; do apply "$f"; done

# force a rollout so the freshly imported :latest images are picked up
for d in server ui; do ssh "$SSH_HOST" "microk8s kubectl rollout restart deploy/urlshortener-$d -n urlshortener"; done
for d in server ui; do ssh "$SSH_HOST" "microk8s kubectl rollout status  deploy/urlshortener-$d -n urlshortener"; done
ssh "$SSH_HOST" "microk8s kubectl get pods,svc,ingress -n urlshortener"
