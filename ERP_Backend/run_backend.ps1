# Run ERP_Backend with environment variables from the backend directory.
$script_root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$script_root = (Resolve-Path -LiteralPath $script_root).Path
$exit_code = 0

Push-Location -LiteralPath $script_root
try {
    $env_file = Join-Path $script_root '.env'
    if (Test-Path -LiteralPath $env_file) {
        Get-Content -LiteralPath $env_file | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith('#') -and $line -match '^([^=]+)=(.*)$') {
                $name = $Matches[1].Trim()
                $value = $Matches[2].Trim()
                [System.Environment]::SetEnvironmentVariable($name, $value, [System.EnvironmentVariableTarget]::Process)
            }
        }
    }

    Write-Host "Starting ERP_Backend with Spring Boot..." -ForegroundColor Cyan
    & .\mvnw.cmd spring-boot:run
    $exit_code = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $exit_code
