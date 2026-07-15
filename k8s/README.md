# Kubernetes Deployment (MicroK8s)

Deploys the URL shortener to a remote MicroK8s cluster over SSH. No registry is
required: images are built locally with Docker and streamed into the remote
node's containerd.

## Files

| File | Purpose |
|------|---------|
| `00-namespace.yml` | `urlshortener` namespace |
| `10-urlshortener.yml` | Server deployment + `urlshortener-admin` (9090) and `urlshortener-redirect` (8081) services |
| `20-admin-ui.yml` | Admin UI deployment + `urlshortener-ui` (8080) service |
| `30-ingress-local.yml` | Plain-HTTP catch-all ingress for local use (`http://localhost/`) |
| `30-ingress-letsencrypt.yml` | Ingress + cert-manager ClusterIssuer for a public domain |
| `build.sh` | Build both images locally for the remote node's architecture |
| `upload.sh` | Side-load the built images into the remote MicroK8s |
| `deploy.sh` | Apply the manifests remotely and roll out |
| `build-and-deploy.sh` | Full pipeline: build → upload → deploy |
| `undeploy.sh` | Tear everything down (delete the namespace) |
| `env/*.env.example` | Template for a per-target (domain/host) settings file |

## Prerequisites

Local machine:

- Docker to build the images
- SSH access to the target machine (e.g. `ubuntu@my-vps`)

Remote machine — install MicroK8s and enable the required addons:

```bash
snap install microk8s --classic --channel=1.35/stable
microk8s status --wait-ready
sudo usermod -aG microk8s $USER
newgrp microk8s
microk8s enable ingress cert-manager hostpath-storage
```

(No container registry is needed — images are side-loaded straight into
containerd by `upload.sh`.)

## Deploy

Simplest path — one command builds, uploads and deploys:

```bash
./k8s/build-and-deploy.sh ubuntu@my-vps local          # plain HTTP (http://<node>/)

DOMAIN=short.example.com ACME_EMAIL=me@example.com \
  ./k8s/build-and-deploy.sh ubuntu@my-vps letsencrypt   # Let's Encrypt cert
```

Or run the steps yourself (e.g. to redeploy without rebuilding):

```bash
# 1. Build the images locally, 2. import them into the remote containerd
./k8s/build.sh ubuntu@my-vps
./k8s/upload.sh ubuntu@my-vps

# 3. Apply manifests and roll out
./k8s/deploy.sh ubuntu@my-vps local
DOMAIN=short.example.com ACME_EMAIL=me@example.com ./k8s/deploy.sh ubuntu@my-vps letsencrypt
```

`deploy.sh` always restarts both deployments so freshly imported `:latest`
images are picked up, then waits for the rollout and prints the resulting
pods, services and ingress. The deployments carry readiness probes, so the
rollout only reports complete once the containers are actually accepting
connections.

To remove everything again:

```bash
./k8s/undeploy.sh ubuntu@my-vps   # deletes the namespace (and the data volume)
```

## Configuration

The server is configured via environment variables (all optional; the
defaults suit a plain localhost run):

| Variable | Default | Purpose |
|----------|---------|---------|
| `REDIRECT_SERVER_HOST` | `localhost` | Bind address of the redirect listener |
| `REDIRECT_SERVER_PORT` | `8081` | Port of the redirect listener |
| `ADMIN_SERVER_HOST` | `localhost` | Bind address of the admin API |
| `ADMIN_SERVER_PORT` | `9090` | Port of the admin API |

The Docker image sets both hosts to `0.0.0.0` (container traffic arrives over
the pod/bridge network). To expose the servers to other machines in a
bare-metal setup, set the host variables to `0.0.0.0` explicitly.

## Local mode

Local mode needs no extra configuration: the ingress rule has no hostname
(catch-all) and serves plain HTTP on port 80 — no certificate, no TLS secret,
no `/etc/hosts` entry. From a browser on the cluster node:

- Admin UI: `http://localhost/management`
- Redirects: `http://localhost/<short-code>`

From another machine, use the node's IP instead of `localhost`. The short links
shown in the UI come from `SHORTCODE_BASE_URL`, which defaults to
`http://localhost/`. To make them point at the node so they work from other
machines, set it when deploying — no file edit needed:

```bash
SHORTCODE_BASE_URL=http://<node-ip>/ ./k8s/deploy.sh ubuntu@my-vps local
```

The UI WAR is deployed at the `/management` context path inside Jetty, matching
the ingress prefix — no path rewriting is involved.

## Let's Encrypt mode

You don't edit any YAML — pass your details as environment variables and
`deploy.sh` fills them into the manifest:

- `DOMAIN` → your real domain, must already resolve to the cluster
  (e.g. `short.example.com`). Used for the ingress host **and** the TLS cert.
- `ACME_EMAIL` → your email for Let's Encrypt / ACME registration.
- `SHORTCODE_BASE_URL` *(optional)* → the public base for short links shown in
  the UI. Defaults to `https://$DOMAIN/`, which is almost always what you want.

```bash
DOMAIN=short.example.com ACME_EMAIL=me@example.com \
  ./k8s/deploy.sh ubuntu@my-vps letsencrypt
```

`deploy.sh` needs `DOMAIN` set for `letsencrypt` mode (it stops otherwise).
Requires the `cert-manager` addon (see prerequisites) and ports 80/443
reachable from the internet for the HTTP-01 challenge.

### Reusing the scripts across domains/hosts

Rather than retyping the variables, keep one settings file per target and pass
it to any script:

```bash
cp k8s/env/prod.env.example k8s/env/prod.env
# edit k8s/env/prod.env: SSH_HOST, MODE, DOMAIN, ACME_EMAIL, (SHORTCODE_BASE_URL)
./k8s/build-and-deploy.sh k8s/env/prod.env
```

Add a `staging.env`, `customer-a.env`, etc. and switch targets just by changing
the file you pass — the manifests never change. A positional `<ssh-host>`/`<mode>`
after the file still overrides `SSH_HOST`/`MODE`. Real `*.env` files in
`k8s/env/` are git-ignored; only the `*.example` templates are tracked.

## Data persistence

The server stores URL mappings via EclipseStore in the `urlshortener-data`
PersistentVolumeClaim (provided by the `hostpath-storage` addon), mounted at
`/app/data`. Data survives pod restarts and redeploys. Notes:

- On a multi-node cluster the hostpath volume binds to one node, which pins
  the server pod there.
- The server deployment uses the `Recreate` strategy: EclipseStore is
  single-writer, so the old pod must stop before the new one starts. Expect a
  few seconds of downtime per deploy.

## Session affinity

The Vaadin UI keeps all user state (login, UI state) in the pod's HTTP
session, so scaling it beyond one replica needs sticky sessions. The manifests
configure this for **both** ingress controllers (see below):

- **ingress-nginx:** `nginx.ingress.kubernetes.io/affinity` annotations on the
  Ingress (`30-ingress-*.yml`).
- **Traefik:** `traefik.ingress.kubernetes.io/service.sticky.cookie`
  annotations on the `urlshortener-ui` Service (`20-admin-ui.yml`).

Each controller ignores the other's annotations, so both can coexist. A pod
restart still ends its sessions (users log in again). The redirect service is
stateless and needs no affinity — under nginx it inherits the Ingress-wide
sticky cookie anyway, which it simply ignores.

## Ingress controller compatibility

The `ingress` addon is **ingress-nginx** on a stock MicroK8s, but some clusters
front the workloads with **Traefik** instead — the MicroK8s version number does
not tell you which. The manifests are written to work on either:

- `ingressClassName: public` — both controllers register the `public` class.
- The `letsencrypt` Ingress keeps `rules[].host`. **This is required by
  ingress-nginx:** it only serves a TLS certificate for hostnames that appear
  in the rules. A host-less catch-all rule leaves the cert unattached and nginx
  falls back to its self-signed *"Kubernetes Ingress Controller Fake
  Certificate"* — even though cert-manager issued a valid cert. Traefik is
  happy with the host present too, so keep it.
- Session-affinity annotations for both controllers coexist (see above).

To check which controller a cluster runs:

```bash
ssh <host> "microk8s kubectl get pods -n ingress"
```

Verify the served TLS cert on the wire (not from a cached browser session):

```bash
echo | openssl s_client -connect <host>:443 -servername <host> 2>/dev/null \
  | openssl x509 -noout -issuer -subject
```

A real cert shows `issuer=...Let's Encrypt...`; the nginx fallback shows
`O=Acme Co` / "Fake Certificate".

## Troubleshooting

```bash
ssh ubuntu@my-vps microk8s kubectl get pods,svc,ingress -n urlshortener
ssh ubuntu@my-vps microk8s kubectl logs deployment/urlshortener-server -n urlshortener
ssh ubuntu@my-vps microk8s kubectl describe ingress urlshortener -n urlshortener
```

Note: the ingress manifests use `ingressClassName: public`, the class provided
by the MicroK8s `ingress` addon.
