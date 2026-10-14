$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot

$variables = [ordered]@{
    DB_URL = 'jdbc:h2:mem:demo_segura;MODE=PostgreSQL;DB_CLOSE_DELAY=-1'
    DB_USERNAME = 'sa'
    DB_PASSWORD = ''
    SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.h2.Driver'
    SPRING_JPA_DATABASE_PLATFORM = 'org.hibernate.dialect.H2Dialect'
    SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'
    APP_DATOS_DEMO = 'true'
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
    Write-Host 'Demo local en http://127.0.0.1:8084. Pulsa Ctrl+C para detenerla.'
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
