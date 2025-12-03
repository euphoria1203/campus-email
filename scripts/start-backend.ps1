param(
    [string]$Profile = "dev"
)

$solutionRoot = Resolve-Path "$PSScriptRoot\.."
Push-Location $solutionRoot

try {
    Write-Host "[CampusMail] Starting Spring Boot backend with profile '$Profile'..." -ForegroundColor Cyan
    mvn spring-boot:run -Dspring-boot.run.profiles=$Profile
}
finally {
    Pop-Location
}
