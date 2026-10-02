$ErrorActionPreference = "Stop"

$Package = "com.omerfaruk.dongu"
$Apk = Join-Path $PSScriptRoot "Dongu-Migration-1.1.0.apk"
$Backup = Join-Path $PSScriptRoot "dongu-1.0.0-veri-yedegi.xml"
$Verify = Join-Path $PSScriptRoot "dongu-1.1.0-dogrulama.xml"

function Find-Adb {
    $items = @()
    $items += (Join-Path $PSScriptRoot "platform-tools\adb.exe")
    if ($env:ANDROID_HOME) { $items += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe") }
    if ($env:LOCALAPPDATA) { $items += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe") }
    $cmd = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($cmd) { $items += $cmd.Source }
    return $items | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
}

function Run-Adb {
    param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Args)
    & $script:Adb @Args
    if ($LASTEXITCODE -ne 0) {
        throw "ADB komutu basarisiz oldu: adb $($Args -join ' ')"
    }
}

Write-Host ""
Write-Host "=== DONGU 1.0 -> 1.1 KAYIPSIZ VERI TASIMA ===" -ForegroundColor Magenta
Write-Host "Bu arac eski veriyi once PC'ye yedekler; yedek dogrulanmadan uygulamayi KALDIRMAZ." -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $Apk)) {
    throw "Dongu-Migration-1.1.0.apk bu script ile ayni klasorde bulunamadi."
}

$script:Adb = Find-Adb
if (-not $script:Adb) {
    throw "adb.exe bulunamadi. Android SDK Platform Tools'u kur veya platform-tools klasorunu bu scriptin yanina koy."
}

Write-Host "ADB: $script:Adb"
Run-Adb start-server

$deviceLines = & $script:Adb devices
$devices = @($deviceLines | Where-Object { $_ -match "\tdevice$" })
if ($devices.Count -ne 1) {
    throw "Tam olarak 1 adet USB hata ayiklama yetkisi verilmis Android cihaz bagli olmali. Telefonda USB hata ayiklama iznini onayla."
}

Write-Host "[1/7] Eski Dongu uygulamasi kontrol ediliyor..."
& $script:Adb shell pm path $Package | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Eski Dongu uygulamasi telefonda bulunamadi." }

Write-Host "[2/7] Uygulama verisi telefondan okunuyor..."
& $script:Adb shell "run-as $Package cat shared_prefs/dongu_data.xml > /data/local/tmp/dongu_data.xml"
if ($LASTEXITCODE -ne 0) {
    throw "Eski uygulama verisi okunamadi. HICBIR SEY SILINMEDI."
}
Run-Adb pull /data/local/tmp/dongu_data.xml $Backup
Run-Adb shell rm -f /data/local/tmp/dongu_data.xml

if (-not (Test-Path $Backup) -or (Get-Item $Backup).Length -lt 20) {
    throw "Yedek dosyasi olusmadi veya bos. HICBIR SEY SILINMEDI."
}
$backupText = Get-Content -Raw -LiteralPath $Backup
if ($backupText -notmatch "<map") {
    throw "Yedek dosyasi gecersiz gorunuyor. HICBIR SEY SILINMEDI."
}
$beforeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Backup).Hash
Write-Host "Yedek guvende: $Backup" -ForegroundColor Green
Write-Host "SHA256: $beforeHash"

Write-Host "[3/7] Eski imzali 1.0 kaldiriliyor..."
Run-Adb uninstall $Package

Write-Host "[4/7] Kalici guncelleme anahtariyla imzali Dongu 1.1.0 kuruluyor..."
Run-Adb install $Apk

Write-Host "[5/7] Eski veri yeni kuruluma geri aktariliyor..."
Run-Adb push $Backup /data/local/tmp/dongu_data.xml
Run-Adb shell "run-as $Package mkdir -p shared_prefs"
Run-Adb shell "cat /data/local/tmp/dongu_data.xml | run-as $Package sh -c 'cat > shared_prefs/dongu_data.xml'"
Run-Adb shell "run-as $Package chmod 600 shared_prefs/dongu_data.xml"
Run-Adb shell rm -f /data/local/tmp/dongu_data.xml

Write-Host "[6/7] Aktarilan veri birebir dogrulaniyor..."
Run-Adb shell "run-as $Package cat shared_prefs/dongu_data.xml > /data/local/tmp/dongu_verify.xml"
Run-Adb pull /data/local/tmp/dongu_verify.xml $Verify
Run-Adb shell rm -f /data/local/tmp/dongu_verify.xml
$afterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Verify).Hash

if ($beforeHash -ne $afterHash) {
    throw "UYARI: Veri hash'i eslesmedi. PC'deki $Backup yedegi KORUNDU; uygulamayi kullanmadan once kontrol et."
}
Remove-Item -LiteralPath $Verify -Force -ErrorAction SilentlyContinue

Write-Host "[7/7] Dongu aciliyor..."
& $script:Adb shell pm grant $Package android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
& $script:Adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null

Write-Host ""
Write-Host "BASARILI: Dongu 1.1.0 kuruldu ve eski veriler SHA256 ile birebir dogrulandi." -ForegroundColor Green
Write-Host "Bundan sonraki ayni anahtarla imzali surumler mevcut uygulamanin USTUNE kurulabilir." -ForegroundColor Green
Write-Host "Guvenlik icin PC'deki veri yedegi silinmedi: $Backup" -ForegroundColor Yellow
Write-Host "Uygulamayi kontrol ettikten sonra bu XML dosyasini istersen silebilirsin; saglik/not verilerini icerebilir." -ForegroundColor Yellow
Write-Host ""
Read-Host "Kapatmak icin Enter'a bas"
