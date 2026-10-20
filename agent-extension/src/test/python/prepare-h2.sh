#!/usr/bin/env bash
set -euo pipefail
# Run from repository root. Synthetic local TLS material remains in ignored target/.
python3 -m pip install --quiet --target agent-extension/target/h2-python -r agent-extension/src/test/python/requirements.txt
mkdir -p agent-extension/target/harness
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout agent-extension/target/harness/h2-key.pem \
  -out agent-extension/target/harness/h2-cert.pem -days 1 -subj /CN=localhost \
  -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' >/dev/null 2>&1
