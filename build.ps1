param([switch]$Release)

$ErrorActionPreference = 'Stop'

$projectDir = (Resolve-Path -LiteralPath $PSScriptRoot).Path
$sdkDir = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$toolsDir = Join-Path $sdkDir 'build-tools\36.1.0'
$androidJar = Join-Path $sdkDir 'platforms\android-36\android.jar'
$buildDir = Join-Path $projectDir 'build'
$classesDir = Join-Path $buildDir 'classes'
$dexDir = Join-Path $buildDir 'dex'
$signingDir = Join-Path $env:USERPROFILE '.android'
$debugKeyStore = Join-Path $signingDir 'debug.keystore'
$releaseKeyStore = Join-Path $signingDir 'esi-lenta-release.p12'
$releasePasswordFile = Join-Path $signingDir 'esi-lenta-release.password'
$releaseAlias = 'esi-lenta-release'
$manifestText = [System.IO.File]::ReadAllText((Join-Path $projectDir 'AndroidManifest.xml'), [System.Text.Encoding]::UTF8)
$versionMatch = [regex]::Match($manifestText, 'android:versionName="([^"]+)"')
if (-not $versionMatch.Success) { throw 'Missing Android versionName in manifest' }
$versionName = $versionMatch.Groups[1].Value

foreach ($required in @($androidJar, (Join-Path $toolsDir 'aapt2.exe'), (Join-Path $toolsDir 'd8.bat'))) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Missing build dependency: $required" }
}
if ($Release) {
    foreach ($required in @($releaseKeyStore, $releasePasswordFile)) {
        if (-not (Test-Path -LiteralPath $required)) { throw "Missing release signing file: $required" }
    }
} elseif (-not (Test-Path -LiteralPath $debugKeyStore)) {
    throw "Missing debug keystore: $debugKeyStore"
}

if (Test-Path -LiteralPath $buildDir) {
    $resolvedBuild = (Resolve-Path -LiteralPath $buildDir).Path
    $expectedBuild = [System.IO.Path]::GetFullPath((Join-Path $projectDir 'build'))
    if ($resolvedBuild -ne $expectedBuild -or -not $resolvedBuild.StartsWith($projectDir + [System.IO.Path]::DirectorySeparatorChar)) {
        throw 'Build directory is outside the project.'
    }
    Remove-Item -LiteralPath $resolvedBuild -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir, $dexDir -Force | Out-Null

$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectDir 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 --release 8 -classpath $androidJar -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Push-Location $projectDir
try { & (Join-Path $toolsDir 'aapt2.exe') compile --dir res -o 'build\resources.zip' }
finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw 'aapt2 resource compilation failed' }

& (Join-Path $toolsDir 'aapt2.exe') link --manifest (Join-Path $projectDir 'AndroidManifest.xml') -I $androidJar -R (Join-Path $buildDir 'resources.zip') -o (Join-Path $buildDir 'unsigned.apk')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 failed' }

$classFiles = @(Get-ChildItem -LiteralPath $classesDir -Recurse -Filter '*.class' | ForEach-Object FullName)
$d8Args = @('--lib', $androidJar, '--output', $dexDir)
if ($Release) { $d8Args = @('--release', '--min-api', '26') + $d8Args }
& (Join-Path $toolsDir 'd8.bat') @d8Args $classFiles
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Push-Location $dexDir
try { & jar uf (Join-Path $buildDir 'unsigned.apk') 'classes.dex' }
finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw 'Adding classes.dex failed' }

& (Join-Path $toolsDir 'zipalign.exe') -f 4 (Join-Path $buildDir 'unsigned.apk') (Join-Path $buildDir 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

if ($Release) {
    $distDir = Join-Path $env:USERPROFILE 'Yandex.Disk\Distrib'
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null
    $outputApk = Join-Path $distDir "lenta-cashback-$versionName.apk"
    try {
        $env:LENTA_RELEASE_SIGNING_PASSWORD = [System.IO.File]::ReadAllText($releasePasswordFile, [System.Text.Encoding]::UTF8).Trim()
        & (Join-Path $toolsDir 'apksigner.bat') sign --ks $releaseKeyStore --ks-type PKCS12 --ks-key-alias $releaseAlias --ks-pass 'env:LENTA_RELEASE_SIGNING_PASSWORD' --key-pass 'env:LENTA_RELEASE_SIGNING_PASSWORD' --out $outputApk (Join-Path $buildDir 'aligned.apk')
        if ($LASTEXITCODE -ne 0) { throw 'Release APK signing failed' }
    } finally {
        [System.Environment]::SetEnvironmentVariable('LENTA_RELEASE_SIGNING_PASSWORD', $null, 'Process')
    }
} else {
    $outputApk = Join-Path $buildDir 'lenta-cashback-debug.apk'
    & (Join-Path $toolsDir 'apksigner.bat') sign --ks $debugKeyStore --ks-pass 'pass:android' --key-pass 'pass:android' --out $outputApk (Join-Path $buildDir 'aligned.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Debug APK signing failed' }
}
& (Join-Path $toolsDir 'apksigner.bat') verify $outputApk
if ($LASTEXITCODE -ne 0) { throw 'APK verification failed' }

if ($Release) {
    $sha256 = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumPath = "$outputApk.sha256"
    "$sha256  $([System.IO.Path]::GetFileName($outputApk))" | Set-Content -LiteralPath $checksumPath -Encoding ascii
}

Write-Output $outputApk
