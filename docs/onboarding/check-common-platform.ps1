param([string]$Settings = (Join-Path $PSScriptRoot 'settings.xml.template'))
$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath $Settings -PathType Leaf)) { throw 'settings 파일 없음' }
$Settings = (Resolve-Path -LiteralPath $Settings).Path
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven PATH 설정 필요' }
if (!$env:GS_MAVEN_READ_USER -or !$env:GS_MAVEN_READ_TOKEN) {
    throw 'GS_MAVEN_READ_USER / GS_MAVEN_READ_TOKEN 설정 필요'
}
$work = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
try {
    $pom = Join-Path $work 'pom.xml'
    '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>onboarding</groupId><artifactId>check</artifactId><version>1</version></project>' | Set-Content -LiteralPath $pom -Encoding UTF8
    & mvn -B -ntp -s $Settings -f $pom "-Dmaven.repo.local=$work/repository" `
        'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get' `
        '-Dartifact=ai.mutuus.common:common-platform:0.6.9:pom' '-Dtransitive=false' *> (Join-Path $work 'maven.log')
    if ($LASTEXITCODE -ne 0) { throw 'POM 받기 실패: troubleshooting.md 확인 (Maven 로그 비출력)' }
    $artifact = Join-Path $work 'repository/ai/mutuus/common/common-platform/0.6.9/common-platform-0.6.9.pom'
    if (!(Test-Path -LiteralPath $artifact) -or (Get-Item -LiteralPath $artifact).Length -eq 0) { throw 'POM 파일 없음' }
    Write-Output 'PASS: ai.mutuus.common:common-platform:0.6.9:pom 신규 임시 캐시에 받음 (빌드·서버 시험은 별도)'
} finally {
    Remove-Item -LiteralPath $work -Recurse -Force
}
