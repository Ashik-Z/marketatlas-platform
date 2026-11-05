# Registers a baseline set of US-listed companies and fetches 2 years of daily history for new ones.
# Needs the API running on $Api and ADMIN_PASSWORD in .env. Safe to run again: existing assets are skipped.
param(
    [string]$Api = "http://localhost:8080",
    [string]$AdminUser = "admin",
    [int]$PauseSeconds = 10
)

$line = Get-Content (Join-Path $PSScriptRoot "..\.env") | Where-Object { $_ -match '^\s*ADMIN_PASSWORD=' } | Select-Object -First 1
if (-not $line) { throw "ADMIN_PASSWORD not found in .env" }
$pass = ($line -replace '^\s*ADMIN_PASSWORD=', '').Trim().Trim('"').Trim("'")
$token = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${AdminUser}:$pass"))
$auth = @{ Authorization = "Basic $token" }

$companies = @(
    @{ name = "Microsoft Corp."; country = "United States"; symbol = "MSFT" },
    @{ name = "Alphabet Inc.";   country = "United States"; symbol = "GOOGL" },
    @{ name = "Spotify Technology S.A."; country = "Sweden"; symbol = "SPOT" },
    @{ name = "HP Inc.";         country = "United States"; symbol = "HPQ" },
    @{ name = "Dell Technologies Inc."; country = "United States"; symbol = "DELL" }
)

function Send-Json($method, $path, $body) {
    Invoke-RestMethod -Method $method -Uri "$Api$path" -Headers $auth -ContentType "application/json" `
        -Body ($body | ConvertTo-Json) -TimeoutSec 120
}

$issuers = @(Invoke-RestMethod "$Api/api/issuers")
$created = @()

foreach ($c in $companies) {
    $existing = @(Invoke-RestMethod "$Api/api/assets?symbol=$($c.symbol)" | Where-Object { $_ })
    if ($existing.Count -gt 0) { Write-Host "skip   $($c.symbol) (already registered)"; continue }

    $issuer = $issuers | Where-Object { $_.name -eq $c.name } | Select-Object -First 1
    if (-not $issuer) {
        $issuer = Send-Json Post "/api/issuers" @{ name = $c.name; country = $c.country }
        $issuers += $issuer
    }
    $asset = Send-Json Post "/api/assets" @{
        name = $c.name; assetClass = "EQUITY"; issuerId = $issuer.id; symbol = $c.symbol; currency = "USD"
    }
    Write-Host "added  $($c.symbol) (asset $($asset.id))"
    $created += $c
}

$end = (Get-Date).ToString("yyyy-MM-dd")
$start = (Get-Date).AddYears(-2).ToString("yyyy-MM-dd")
$i = 0
foreach ($c in $created) {
    if ($i++ -gt 0) { Start-Sleep -Seconds $PauseSeconds }
    try {
        $bars = Invoke-RestMethod -Headers $auth -TimeoutSec 180 `
            -Uri "$Api/api/market-data/history?symbol=$($c.symbol)&startDate=$start&endDate=$end&interval=1day"
        Write-Host "fetched $($c.symbol): $(@($bars).Count) bars"
    } catch {
        Write-Warning "fetch failed for $($c.symbol): $($_.Exception.Message)"
    }
}
Write-Host "done."

