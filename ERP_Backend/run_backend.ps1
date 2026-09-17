# Script to run ERP_Backend with environment variables
if (Test-Path .env) {
    Get-Content .env | Where-Object { $_ -match '^[^#=]+=.+$' } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), [System.EnvironmentVariableTarget]::Process)
    }
}

Write-Host "Starting ERP_Backend with Spring Boot..." -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run
