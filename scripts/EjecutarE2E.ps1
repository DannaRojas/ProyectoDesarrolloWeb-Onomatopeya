param(
    [Parameter(Mandatory = $true)]
    [string]$FrontendDir,
    [switch]$ConservarContenedores
)

$ErrorActionPreference = 'Stop'
$frontend = (Resolve-Path -LiteralPath $FrontendDir).Path
$proyecto = 'onomatopeya-e2e'
$archivosCompose = @(
    '-f', 'compose.yaml',
    '-f', 'compose.frontend.yaml',
    '-f', 'compose.e2e.yaml'
)

$variablesAnteriores = @{
    DB_PASSWORD = $env:DB_PASSWORD
    APP_JWT_SECRET = $env:APP_JWT_SECRET
    E2E_DEMO_PASSWORD = $env:E2E_DEMO_PASSWORD
    FRONTEND_DIR = $env:FRONTEND_DIR
}

function Restaurar-Variable([string]$nombre, [string]$valor) {
    if ($null -eq $valor) {
        Remove-Item "Env:$nombre" -ErrorAction SilentlyContinue
    } else {
        Set-Item "Env:$nombre" $valor
    }
}

function Esperar-Api {
    for ($intento = 1; $intento -le 60; $intento++) {
        try {
            $respuesta = Invoke-RestMethod -Uri 'http://127.0.0.1:4203/api/v3/api-docs' -TimeoutSec 3
            if ($respuesta.openapi) { return }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw 'La aplicación no respondió por medio del frontend en el tiempo esperado.'
}

$falloPrincipal = $null
$falloLimpieza = $null

try {
    docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop no está iniciado.' }

    $env:DB_PASSWORD = "E2E-db-$([guid]::NewGuid().ToString('N'))"
    $env:APP_JWT_SECRET = "E2E-jwt-$([guid]::NewGuid().ToString('N'))"
    $env:E2E_DEMO_PASSWORD = "E2E-$([guid]::NewGuid().ToString('N'))-Aa1!"
    $env:FRONTEND_DIR = $frontend

    docker compose --project-name $proyecto @archivosCompose down --volumes --remove-orphans
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible limpiar el entorno E2E anterior.' }

    docker compose --project-name $proyecto @archivosCompose up --build --detach
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible iniciar el entorno E2E.' }
    Esperar-Api

    Push-Location $frontend
    try {
        npm run e2e:real
        if ($LASTEXITCODE -ne 0) { throw 'Las pruebas E2E reales fallaron.' }
    } finally {
        Pop-Location
    }
} catch {
    $falloPrincipal = $_
} finally {
    if (-not $ConservarContenedores) {
        docker compose --project-name $proyecto @archivosCompose down --volumes --remove-orphans
        if ($LASTEXITCODE -ne 0) { $falloLimpieza = 'No fue posible limpiar completamente el entorno E2E.' }
    }
    foreach ($entrada in $variablesAnteriores.GetEnumerator()) {
        Restaurar-Variable $entrada.Key $entrada.Value
    }
}

if ($null -ne $falloPrincipal) { throw $falloPrincipal }
if ($null -ne $falloLimpieza) { throw $falloLimpieza }
