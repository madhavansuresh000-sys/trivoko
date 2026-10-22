# Flash-sale load test, start to finish (Phase 7).  Run from the project root:
#     powershell -File load-tests\run-flash-load.ps1
# Needs: Docker Desktop with the trivoko-mysql container running, Java 25, the grafana/k6 Docker image.
# It uses a throw-away database "trivoko_load" (never your dev data) and a second copy of the app, in Docker, on port 8090.
param([string]$Shape = 'burst')   # burst = 5,000 at the same instant; spread = 1,000 per second for 5 seconds; ceiling = the burst at /api/health
$ErrorActionPreference = 'Continue'   # mysql prints a harmless password warning on stderr
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$results = Join-Path $root 'load-tests\results'
New-Item -ItemType Directory -Force $results | Out-Null

# passwords come from .env (never printed)
$env_ = @{}
Get-Content "$root\.env" | Where-Object { $_ -match '^\s*[A-Z_]+=' } | ForEach-Object { $k, $v = $_ -split '=', 2; $env_[$k.Trim()] = $v.Trim() }

function Sql($db, $statement) {
    docker exec trivoko-mysql mysql -uroot "-p$($env_['MYSQL_ROOT_PASSWORD'])" -N -B $db -e $statement 2>$null
}

Write-Host '1/7 fresh database trivoko_load'
Sql '' "DROP DATABASE IF EXISTS trivoko_load; CREATE DATABASE trivoko_load; GRANT ALL ON trivoko_load.* TO '$($env_['MYSQL_USER'])'@'%'; FLUSH PRIVILEGES;" | Out-Null

Write-Host '2/7 build the app and its Docker image'
Push-Location "$root\backend"
.\mvnw.cmd -q -DskipTests package
docker build -q -t trivoko-backend . | Out-Null
Pop-Location

Write-Host '3/7 start the app in Docker on port 8090 (Flyway builds the tables)'
docker rm -f trivoko-load-app 2>$null | Out-Null
docker run -d --name trivoko-load-app --network trivoko_default -p 8090:8080 `
    -e SPRING_DATASOURCE_URL=jdbc:mysql://trivoko-mysql:3306/trivoko_load `
    -e MYSQL_USER=$($env_['MYSQL_USER']) -e MYSQL_PASSWORD=$($env_['MYSQL_PASSWORD']) `
    -e TZ=Asia/Kolkata -e MAIL_ENABLED=false -e APP_FLASH_RATE_LIMIT_PER_IP=1000000 trivoko-backend | Out-Null
$log = "$results\app-load.log"
try {
    $up = $false
    for ($i = 0; $i -lt 90 -and -not $up; $i++) {
        Start-Sleep 2
        try { $up = (Invoke-WebRequest http://localhost:8090/api/health -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200 } catch { }
    }
    if (-not $up) { throw "the app did not start - see $log" }

    Write-Host '4/7 seed 5,000 customers + one deal of 100 units'
    Get-Content "$root\load-tests\seed-flash-load.sql" -Raw | docker exec -i trivoko-mysql mysql -uroot "-p$($env_['MYSQL_ROOT_PASSWORD'])" trivoko_load 2>$null
    $item = (Sql 'trivoko_load' 'SELECT MAX(id) FROM flash_sale_items').Trim()
    (Sql 'trivoko_load' "SELECT id, email FROM users WHERE email LIKE 'load%@example.com' ORDER BY id") |
        ForEach-Object { ($_ -split "`t") -join ',' } | Set-Content "$results\load-users.csv" -Encoding ascii

    Write-Host "5/7 k6: 5,000 customers claim deal #$item ($Shape)"
    # k6 runs in a container on the same Docker network, so 5,000 connections go straight to the app
    # (Windows' own port forwarding refuses a burst that big - that would test Windows, not the app)
    $k6Shape = if ($Shape -eq 'spread') { 'spread' } else { 'burst' }
    $k6Target = if ($Shape -eq 'ceiling') { 'health' } else { 'claim' }
    docker run --rm --network trivoko_default --ulimit nofile=65535:65535 -v "${root}\load-tests:/work" -w /work `
        grafana/k6 run -e BASE=http://trivoko-load-app:8080 -e ITEM=$item -e SHAPE=$k6Shape -e TARGET=$k6Target -e OUT=/work/results/flash-claim-$Shape-summary.json --out json=/work/results/flash-claim-$Shape-raw.json flash-claim.js
    $k6Exit = $LASTEXITCODE

    Write-Host '6/7 what the database says'
    $sold = Sql 'trivoko_load' "SELECT sold FROM flash_sale_items WHERE id = $item"
    $rows = Sql 'trivoko_load' "SELECT COUNT(*) FROM flash_sale_claims WHERE item_id = $item"
    $people = Sql 'trivoko_load' "SELECT COUNT(DISTINCT user_id) FROM flash_sale_claims WHERE item_id = $item"
    Write-Host "    sold=$sold  claim rows=$rows  different people=$people   (expected 100 / 100 / 100)"
    "sold=$sold claim_rows=$rows different_people=$people k6_exit=$k6Exit" | Set-Content "$results\flash-claim-$Shape-db-check.txt"
}
finally {
    Write-Host '7/7 stop the app and remove the throw-away database'
    docker logs trivoko-load-app *> $log
    docker rm -f trivoko-load-app 2>$null | Out-Null
    Sql '' 'DROP DATABASE IF EXISTS trivoko_load' | Out-Null
}
