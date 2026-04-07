# WARNING: FOR DEMO/DEVELOPMENT USE ONLY — NOT FOR PRODUCTION.
# Security is disabled. Data is unencrypted and unauthenticated.
#
# Starts OpenSearch via Docker Compose for use as a vector store.
# Security plugin is disabled — HTTP only, no authentication for connections.
# An admin password is still required by OpenSearch 2.12+ at startup.
#
# Usage (Windows — PowerShell 5.1+ or PowerShell 7+):
#   NOTE: always prefix with .\ — PowerShell does not run scripts from the
#         current directory without it.
#
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
#   .\start.ps1

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$EnvFile   = Join-Path $ScriptDir ".env"

# ── Load .env ─────────────────────────────────────────────────────────────────

function Read-Env {
    if (-not (Test-Path $EnvFile)) { return }
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match "^\s*#" -or $_ -match "^\s*$") { return }
        if ($_ -match "^([^=]+)=(.*)$") {
            $key   = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Only set if not already in environment
            if (-not [System.Environment]::GetEnvironmentVariable($key, "Process")) {
                [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            }
        }
    }
}

function Write-Env {
    @"
OPENSEARCH_PASSWORD=$env:OPENSEARCH_PASSWORD
OPENSEARCH_PORT=$env:OPENSEARCH_PORT
"@ | Set-Content -Path $EnvFile -Encoding UTF8
    Write-Host "Settings saved to $EnvFile"

    $gitignoreFile = Join-Path $ScriptDir ".gitignore"
    if (-not (Test-Path $gitignoreFile)) {
        ".env" | Set-Content -Path $gitignoreFile -Encoding UTF8
    } elseif (-not (Select-String -Path $gitignoreFile -Pattern "^\.env$" -Quiet)) {
        "`n.env" | Add-Content -Path $gitignoreFile -Encoding UTF8
    }
}


function Test-PortFree($port) {
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $port)
        $listener.Start()
        $listener.Stop()
        return $true   # port is free
    } catch {
        return $false  # port is already bound
    }
}

function Find-FreePort($startPort) {
    $port = $startPort
    while (-not (Test-PortFree $port)) {
        Write-Host "Port $port is in use, trying $($port + 1)..."
        $port++
    }
    return $port
}

$env:OPENSEARCH_PASSWORD = $null
$env:OPENSEARCH_PORT     = $null
Read-Env

# ── Disclaimer ────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  This setup is for DEMO/DEVELOPMENT purposes only."
Write-Host "  It is not supported in any production environment."
Write-Host ""
$consent = Read-Host "  If you are fully aware and agree, type 'ok' to continue"
if ($consent -ne "ok") {
    Write-Host "Aborted."
    exit 0
}
Write-Host ""

# ── Admin password (required by OpenSearch 2.12+ at startup) ──────────────────

$changed = $false

function Test-PasswordStrength($pw) {
    if ($pw.Length -lt 12)                 { return "Password must be at least 12 characters long." }
    if ($pw -notmatch '[A-Z]')             { return "Password must contain at least one uppercase letter." }
    if ($pw -notmatch '[a-z]')             { return "Password must contain at least one lowercase letter." }
    if ($pw -notmatch '[0-9]')             { return "Password must contain at least one digit." }
    if ($pw -notmatch '[^A-Za-z0-9]')      { return "Password must contain at least one special character." }
    return $null
}

if (-not $env:OPENSEARCH_PASSWORD) {
    Write-Host "  OpenSearch requires a strong password (scored by zxcvbn)."
    Write-Host "  Min 12 chars, upper+lower+digit+special. Avoid common words/patterns."
    Write-Host "  Test strength: https://lowe.github.io/tryzxcvbn"
    while ($true) {
        $secure = Read-Host "Admin password (startup only, not used for connections)" -AsSecureString
        $env:OPENSEARCH_PASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
        $err = Test-PasswordStrength $env:OPENSEARCH_PASSWORD
        if (-not $err) { break }
        Write-Host "  Invalid: $err" -ForegroundColor Yellow
    }
    $changed = $true
}

# ── Port ───────────────────────────────────────────────────────────────────────

$startPort = if ($env:OPENSEARCH_PORT) { [int]$env:OPENSEARCH_PORT } else { 19600 }
$resolvedPort = Find-FreePort $startPort
if ($resolvedPort -ne $startPort) {
    Write-Host "Using port $resolvedPort instead."
    $changed = $true
}
$env:OPENSEARCH_PORT = "$resolvedPort"

if ($changed) { Write-Env }

# ── Docker check ──────────────────────────────────────────────────────────────

try { docker version | Out-Null }
catch {
    Write-Error "docker is not installed or not on PATH."
    exit 1
}

# ── Docker Compose ────────────────────────────────────────────────────────────

Set-Location $ScriptDir

# Detect compose command
$composeCmd = $null
try { docker compose version | Out-Null; $composeCmd = @("docker","compose") } catch {}
if (-not $composeCmd) {
    try { docker-compose version | Out-Null; $composeCmd = @("docker-compose") } catch {}
}
if (-not $composeCmd) {
    Write-Error "Neither 'docker compose' plugin nor 'docker-compose' is available."
    exit 1
}

Write-Host "Starting OpenSearch..."
& $composeCmd[0] ($composeCmd[1..($composeCmd.Count-1)] + @("up","-d"))

# ── Wait until healthy ────────────────────────────────────────────────────────

Write-Host -NoNewline "Waiting for OpenSearch to be ready"
$maxRetries = 30
$retry = 0
$ready = $false

while ($retry -le $maxRetries) {
    try {
        docker exec smart-workflow-opensearch `
            curl -fs `
            "http://localhost:9200/_cluster/health" 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    } catch {}
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
    $retry++
}

Write-Host ""

if (-not $ready) {
    Write-Error "OpenSearch did not become ready after $($maxRetries * 2) seconds.`nCheck logs: docker logs smart-workflow-opensearch"
    exit 1
}

Write-Host " ready."

# ── Summary ───────────────────────────────────────────────────────────────────

$url = "http://localhost:$($env:OPENSEARCH_PORT)"
Write-Host ""
Write-Host "==========================================================="
Write-Host "  OpenSearch Vector Store"
Write-Host "-----------------------------------------------------------"
Write-Host "  URL      : $url"
Write-Host "-----------------------------------------------------------"
Write-Host "  Set these Ivy variables:"
Write-Host "    AI.RAG.OpenSearch.Url               = $url"
Write-Host "    AI.RAG.OpenSearch.ApiKey             = (leave blank)"
Write-Host "    AI.RAG.OpenSearch.UserName           = (leave blank)"
Write-Host "    AI.RAG.OpenSearch.Password           = (leave blank)"
Write-Host "    AI.RAG.OpenSearch.DefaultCollection  = (optional, defaults to 'default-axon-ivy-vector-store')"
Write-Host "==========================================================="
Write-Host ""
Write-Host "Stop:   docker compose stop"
Write-Host "Logs:   docker compose logs -f"
Write-Host "Reset:  docker compose down -v   # also deletes index data"
