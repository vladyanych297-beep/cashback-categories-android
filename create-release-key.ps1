$ErrorActionPreference = 'Stop'

$signingDir = Join-Path $env:USERPROFILE '.android'
$keyStore = Join-Path $signingDir 'esi-lenta-release.p12'
$passwordFile = Join-Path $signingDir 'esi-lenta-release.password'

if ((Test-Path -LiteralPath $keyStore) -or (Test-Path -LiteralPath $passwordFile)) {
    throw 'Release signing files already exist. Refusing to replace the release identity.'
}
New-Item -ItemType Directory -Path $signingDir -Force | Out-Null

$password = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
try {
    [System.Environment]::SetEnvironmentVariable('LENTA_RELEASE_SIGNING_PASSWORD', $password, 'Process')
    & keytool -genkeypair -v -storetype PKCS12 -keystore $keyStore -alias esi-lenta-release `
        -keyalg RSA -keysize 4096 -validity 10000 `
        -dname 'CN=ESI.Company, OU=Mobile, O=ESI.Company, C=RU' `
        -storepass:env LENTA_RELEASE_SIGNING_PASSWORD -keypass:env LENTA_RELEASE_SIGNING_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw 'Release key generation failed' }
    [System.IO.File]::WriteAllText($passwordFile, $password, [System.Text.UTF8Encoding]::new($false))
} finally {
    [System.Environment]::SetEnvironmentVariable('LENTA_RELEASE_SIGNING_PASSWORD', $null, 'Process')
}

$owner = "${env:USERDOMAIN}\${env:USERNAME}"
foreach ($path in @($keyStore, $passwordFile)) {
    & icacls $path /inheritance:r /grant:r "${owner}:(F)" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to protect signing file: $path" }
}

Write-Output "Release signing key created: $keyStore"
Write-Output "Password saved separately: $passwordFile"
Write-Output 'Back up both files securely. Future updates must use this same key.'
