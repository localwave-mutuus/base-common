# sample-api Local Secrets

This directory contains non-secret examples only.

Runtime local secrets are imported from:

```text
${user.home}/.mutuus/sample-api/local.yml
```

Create that file by decrypting a SOPS + age encrypted source outside normal builds:

```powershell
sops --decrypt samples/sample-api/secrets/local.sops.yml.example > $HOME/.mutuus/sample-api/local.yml
```

Do not commit decrypted `*.yml` files or age private keys.
