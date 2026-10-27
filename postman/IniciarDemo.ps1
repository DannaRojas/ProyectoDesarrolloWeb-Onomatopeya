$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
$archivoEntorno = Join-Path $proyecto '.env'
$clave = $env:DB_PASSWORD
if (-not $clave -and (Test-Path -LiteralPath $archivoEntorno)) {
    $lineaClave = Get-Content -LiteralPath $archivoEntorno | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
    if ($lineaClave) {
        $clave = $lineaClave.Substring('DB_PASSWORD='.Length).Trim().Trim('"').Trim("'")
    }
}
if (-not $clave) {
    throw 'Configura DB_PASSWORD en el entorno o en .env para PostgreSQL local de pruebas.'
}
$claveDemo = [Convert]::ToBase64String(
    [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))

$variables = [ordered]@{
    DB_PASSWORD = $clave
    SPRING_DATASOURCE_URL = 'jdbc:postgresql://127.0.0.1:5433/demo_postman'
    SPRING_DATASOURCE_USERNAME = 'gestor_pruebas'
    SPRING_DATASOURCE_PASSWORD = $clave
    SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.postgresql.Driver'
    SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'
    APP_DATOS_DEMO = 'true'
    APP_DATOS_DEMO_CLAVE = $claveDemo
    SERVER_PORT = '8084'
}
$anteriores = @{}
foreach ($nombre in $variables.Keys) {
    $actual = Get-Item -Path "Env:$nombre" -ErrorAction SilentlyContinue
    $anteriores[$nombre] = if ($actual) { $actual.Value } else { $null }
    Set-Item -Path "Env:$nombre" -Value $variables[$nombre]
}

Push-Location -LiteralPath $proyecto
try {
    if (-not (Get-NetTCPConnection -LocalPort 5433 -State Listen -ErrorAction SilentlyContinue)) {
        & docker compose -p onoma-entrega2-check --profile pruebas up -d db_pruebas
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudo iniciar PostgreSQL de pruebas.'
        }
    }
    Write-Host 'Demo PostgreSQL aislada en http://127.0.0.1:8084. Pulsa Ctrl+C para detenerla.'
    Write-Host 'Usuario de demo: admin.uno@example.com'
    Write-Host "Contraseña de esta ejecución: $claveDemo"
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.useTestClasspath=true'
    if ($LASTEXITCODE -ne 0) {
        throw 'La demo no pudo iniciarse.'
    }
} finally {
    Pop-Location
    foreach ($nombre in $variables.Keys) {
        if ($null -eq $anteriores[$nombre]) {
            Remove-Item -Path "Env:$nombre" -ErrorAction SilentlyContinue
        } else {
            Set-Item -Path "Env:$nombre" -Value $anteriores[$nombre]
        }
    }
}
