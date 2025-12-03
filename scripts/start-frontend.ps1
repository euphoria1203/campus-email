param(
    [int]$Port = 5173
)

$solutionRoot = Resolve-Path "$PSScriptRoot\.."
$frontendPath = Join-Path $solutionRoot "frontend"
Push-Location $frontendPath

try {
    Write-Host "[CampusMail] Starting frontend dev server on port $Port..." -ForegroundColor Cyan
    npm install
    npm run dev -- --port $Port
}
finally {
    Pop-Location
}
