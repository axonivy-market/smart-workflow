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

if (-not $env:OPENSEARCH_PASSWORD) {
    $env:OPENSEARCH_PASSWORD = Read-Host "OpenSearch admin password (startup only, not used for connections)"
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
