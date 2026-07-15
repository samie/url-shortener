#!/usr/bin/env bash
# Full pipeline: build, upload, then deploy. Args are forwarded to each step
# (see build.sh / upload.sh / deploy.sh).
#   ./k8s/build-and-deploy.sh <ssh-host> [local|letsencrypt]
#   ./k8s/build-and-deploy.sh <env-file>
set -euo pipefail
d="$(dirname "$0")"
"$d/build.sh"  "$@"
"$d/upload.sh" "$@"
"$d/deploy.sh" "$@"
