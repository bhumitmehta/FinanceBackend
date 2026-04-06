# ============================================================
#  Finance Backend – one-shot startup script
#  Run with:  .\start.ps1
#  Requires:  Java 17+, PostgreSQL 15/16/17, Maven (or uses the wrapper)
# ============================================================

$ErrorActionPreference = "Stop"

# ── Colours ──────────────────────────────────────────────────────────────────
function Write-Step  { param($msg) Write-Host "`n[STEP] $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "  [OK] $msg"   -ForegroundColor Green }
function Write-Fail  { param($msg) Write-Host " [ERR] $msg"   -ForegroundColor Red; exit 1 }
function Write-Info  { param($msg) Write-Host "      $msg"    -ForegroundColor Gray }

# ─────────────────────────────────────────────────────────────────────────────
# STEP 1 – Check Java
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Checking Java installation"

try {
    $javaVersion = (java -version 2>&1 | Select-String "version" | Select-Object -First 1).ToString()
    Write-Ok "Found: $javaVersion"
} catch {
    Write-Fail "Java not found. Install Java 17+ from https://adoptium.net and add it to PATH."
}

# ─────────────────────────────────────────────────────────────────────────────
# STEP 2 – Find psql
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Locating PostgreSQL (psql)"

$psqlCandidates = @(
    "psql",   # already on PATH
    "C:\Program Files\PostgreSQL\17\bin\psql.exe",
    "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files\PostgreSQL\15\bin\psql.exe",
    "C:\Program Files\PostgreSQL\14\bin\psql.exe"
)

$psql = $null
foreach ($candidate in $psqlCandidates) {
    if (Get-Command $candidate -ErrorAction SilentlyContinue) {
        $psql = $candidate
        break
    } elseif (Test-Path $candidate) {
        $psql = $candidate
        break
    }
}

if (-not $psql) {
    Write-Fail "psql not found. Install PostgreSQL 15+ from https://www.postgresql.org/download/windows/ and make sure the bin folder is in PATH."
}

Write-Ok "Found psql: $psql"

# ─────────────────────────────────────────────────────────────────────────────
# STEP 3 – Check PostgreSQL service is running
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Checking PostgreSQL service"

$pgService = Get-Service | Where-Object { $_.Name -like "postgresql*" } | Select-Object -First 1
if ($pgService) {
    if ($pgService.Status -ne "Running") {
        Write-Info "Service '$($pgService.Name)' is $($pgService.Status) — attempting to start it..."
        try {
            Start-Service $pgService.Name
            Start-Sleep -Seconds 3
            Write-Ok "Service started"
        } catch {
            Write-Fail "Could not start PostgreSQL service. Start it manually: Services → $($pgService.Name) → Start"
        }
    } else {
        Write-Ok "Service '$($pgService.Name)' is running"
    }
} else {
    Write-Info "No Windows service found — assuming PostgreSQL is running (e.g. manual start or WSL)."
    Write-Info "If the next step fails, make sure PostgreSQL is accepting connections on port 5432."
}

# ─────────────────────────────────────────────────────────────────────────────
# STEP 4 – Create DB user and database (idempotent)
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Setting up database (user=finance, db=financedb)"

$env:PGPASSWORD = "postgres"   # default superuser password — change if yours differs

# Check connection as superuser first
$testConn = & $psql -U postgres -d postgres -c "SELECT 1" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Info "Could not connect as 'postgres' superuser."
    Write-Info "If your superuser has a different name or password, edit the script and update the line:"
    Write-Info "   `$env:PGPASSWORD = `"postgres`"   and   -U postgres"
    Write-Fail "Cannot connect to PostgreSQL as superuser. DB setup skipped."
}

# Create role if missing
$createUser = @"
DO `$`$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'finance') THEN
    CREATE ROLE finance WITH LOGIN PASSWORD 'finance123';
    RAISE NOTICE 'Role finance created.';
  ELSE
    RAISE NOTICE 'Role finance already exists — skipping.';
  END IF;
END
`$`$;
"@
& $psql -U postgres -d postgres -c $createUser | Out-Null

# Create database if missing
$dbExists = & $psql -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='financedb';"
if ($dbExists -ne "1") {
    Write-Info "Creating database financedb..."
    & $psql -U postgres -d postgres -c "CREATE DATABASE financedb OWNER finance;" | Out-Null
    Write-Ok "Database financedb created"
} else {
    Write-Ok "Database financedb already exists"
}

# Grant privileges
& $psql -U postgres -d financedb -c "GRANT ALL PRIVILEGES ON DATABASE financedb TO finance;" | Out-Null
& $psql -U postgres -d financedb -c "GRANT ALL ON SCHEMA public TO finance;" | Out-Null
Write-Ok "Privileges granted to finance"

# Fix any pre-existing NOT NULL constraints on user_role_history (safe to run repeatedly)
$env:PGPASSWORD = "finance123"
$fixSchema = @"
DO \$\$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_role_history') THEN
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='user_role_history' AND column_name='previous_role' AND is_nullable='NO'
    ) THEN
      ALTER TABLE user_role_history ALTER COLUMN previous_role DROP NOT NULL;
      ALTER TABLE user_role_history ALTER COLUMN new_role      DROP NOT NULL;
      RAISE NOTICE 'Dropped NOT NULL from user_role_history columns.';
    END IF;
  END IF;
END
\$\$;
"@
& $psql -U finance -d financedb -c $fixSchema 2>&1 | Out-Null
Write-Ok "Schema compatibility check done"

# ─────────────────────────────────────────────────────────────────────────────
# STEP 5 – Verify port 8080 is free
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Checking port 8080"

$portInUse = netstat -ano | Select-String ":8080\s" | Select-Object -First 1
if ($portInUse) {
    $pid8080 = ($portInUse -split "\s+")[-1]
    $proc = Get-Process -Id $pid8080 -ErrorAction SilentlyContinue
    $procName = if ($proc) { $proc.Name } else { "unknown" }
    Write-Info "Port 8080 is in use by PID $pid8080 ($procName)."
    $answer = Read-Host "  Kill it and continue? (y/n)"
    if ($answer -eq "y") {
        Stop-Process -Id $pid8080 -Force
        Start-Sleep -Seconds 2
        Write-Ok "Killed PID $pid8080"
    } else {
        Write-Fail "Port 8080 still busy. Free it manually and re-run this script."
    }
} else {
    Write-Ok "Port 8080 is free"
}

# ─────────────────────────────────────────────────────────────────────────────
# STEP 6 – Build the project
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Building project (mvn clean package -DskipTests)"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $scriptDir

& .\mvnw.cmd clean package -DskipTests 2>&1 | Tee-Object -Variable buildOutput | Out-Null

$buildResult = $buildOutput | Select-String "BUILD SUCCESS|BUILD FAILURE" | Select-Object -Last 1
if ($buildResult -match "FAILURE") {
    Write-Host ($buildOutput | Select-Object -Last 30 | Out-String)
    Write-Fail "Maven build failed. See output above."
}
Write-Ok "Build successful"

# ─────────────────────────────────────────────────────────────────────────────
# STEP 7 – Start the application
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Starting Finance Backend on http://localhost:8080"
Write-Info "Press Ctrl+C to stop the server."
Write-Info ""
Write-Info "Swagger UI  → http://localhost:8080/swagger-ui/index.html"
Write-Info "Health      → http://localhost:8080/actuator/health"
Write-Info ""
Write-Info "Seeded accounts:"
Write-Info "  admin@finance.local  / Admin1234!"
Write-Info "  analyst@finance.local / Analyst1234!"
Write-Info "  viewer@finance.local  / Viewer1234!"
Write-Info ""

$env:PGPASSWORD = ""   # clear before handing off to Spring Boot
& .\mvnw.cmd spring-boot:run
