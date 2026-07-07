# sample-api Local Secrets

This directory contains non-secret examples only.

Runtime local secrets are imported from:

```text
${user.home}/.mutuus/sample-api/local.yml
```

Create that file outside the repository:

```powershell
New-Item -ItemType Directory -Force $HOME/.mutuus/sample-api
Copy-Item samples/sample-api/secrets/local.example.yml $HOME/.mutuus/sample-api/local.yml
```

Replace placeholders in the copied file with real local values. Do not commit runtime `*.yml` secret files.
