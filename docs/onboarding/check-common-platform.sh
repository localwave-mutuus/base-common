#!/usr/bin/env bash
# Resolve the published POM in isolation; never print credentials or Maven logs.
set -euo pipefail
settings="${1:-$(cd -- "$(dirname -- "$0")" && pwd)/settings.xml.template}"
[[ -f "$settings" ]] || { echo 'settings 파일 없음' >&2; exit 2; }
command -v mvn >/dev/null || { echo 'Maven PATH 설정 필요' >&2; exit 2; }
[[ -n "${GS_MAVEN_READ_USER:-}" && -n "${GS_MAVEN_READ_TOKEN:-}" ]] || {
  echo 'GS_MAVEN_READ_USER / GS_MAVEN_READ_TOKEN 설정 필요' >&2; exit 2;
}
settings="$(cd -- "$(dirname -- "$settings")" && pwd)/$(basename -- "$settings")"
work="$(mktemp -d)"
trap 'rm -rf -- "$work"' EXIT
cat > "$work/pom.xml" <<'POM'
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>onboarding</groupId><artifactId>check</artifactId><version>1</version></project>
POM
if ! mvn -B -ntp -s "$settings" -f "$work/pom.xml" "-Dmaven.repo.local=$work/repository" \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
  -Dartifact=ai.mutuus.common:common-platform:0.6.9:pom -Dtransitive=false \
  >"$work/maven.log" 2>&1; then
  echo 'FAIL: POM 받기 실패. troubleshooting.md에서 권한·네트워크·Maven 설정 확인 (자격 보호를 위해 로그 비출력).' >&2
  exit 1
fi
pom="$work/repository/ai/mutuus/common/common-platform/0.6.9/common-platform-0.6.9.pom"
[[ -s "$pom" ]] || { echo 'FAIL: POM 파일 없음' >&2; exit 1; }
echo 'PASS: ai.mutuus.common:common-platform:0.6.9:pom 신규 임시 캐시에 받음 (빌드·서버 시험은 별도)'
