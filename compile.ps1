# Auto-detect Java and compile the Spring Boot project
Write-Host "🔍 Searching for Java installation..." -ForegroundColor Cyan

# Try to find Java
$javaPath = (Get-Command java -ErrorAction SilentlyContinue).Source

if ($javaPath) {
    Write-Host "✅ Found Java: $javaPath" -ForegroundColor Green
    
    # Extract JDK home from java.exe path
    # Typically: C:\Program Files\Java\jdk-17\bin\java.exe -> C:\Program Files\Java\jdk-17
    $javaBin = Split-Path $javaPath -Parent
    $javaHome = Split-Path $javaBin -Parent
    
    Write-Host "📁 Setting JAVA_HOME: $javaHome" -ForegroundColor Yellow
    $env:JAVA_HOME = $javaHome
    
    Write-Host "`n🔨 Compiling project..." -ForegroundColor Cyan
    Write-Host "This may take a few minutes..." -ForegroundColor Gray
    
    # Compile with Maven
    mvn clean compile
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ COMPILATION SUCCESSFUL!" -ForegroundColor Green
        Write-Host "`nYou can now run the application with:" -ForegroundColor Cyan
        Write-Host "  mvn spring-boot:run" -ForegroundColor White
        Write-Host "`nOr build the JAR with:" -ForegroundColor Cyan
        Write-Host "  mvn clean package" -ForegroundColor White
    } else {
        Write-Host "`n❌ Compilation failed. Check errors above." -ForegroundColor Red
    }
} else {
    Write-Host "❌ Java not found in PATH!" -ForegroundColor Red
    Write-Host "Please install JDK 17 or set JAVA_HOME manually" -ForegroundColor Yellow
    
    # Try common JDK locations
    $commonPaths = @(
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Java\jdk-11",
        "C:\Program Files\OpenJDK\jdk-17",
        "C:\Program Files\Eclipse Adoptium\jdk-17"
    )
    
    Write-Host "`n🔍 Checking common JDK locations:" -ForegroundColor Cyan
    foreach ($path in $commonPaths) {
        if (Test-Path $path) {
            Write-Host "  Found: $path" -ForegroundColor Green
            Write-Host "  Set it with: `$env:JAVA_HOME = '$path'" -ForegroundColor White
        }
    }
}

Write-Host "`n📊 Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
