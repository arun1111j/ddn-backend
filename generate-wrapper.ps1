# Generate Web3j Java Wrapper from compiled Solidity contract
# Run this script from the backend root directory

$binFile = "target\contracts\src_main_resources_contracts_DocumentNotarization_sol_DocumentNotarization.bin"
$abiFile = "target\contracts\src_main_resources_contracts_DocumentNotarization_sol_DocumentNotarization.abi"
$outputDir = "src\main\java"
$packageName = "com.notarize.contracts"

Write-Host "Generating Web3j wrapper..." -ForegroundColor Green
Write-Host "  ABI: $abiFile" -ForegroundColor Cyan
Write-Host "  BIN: $binFile" -ForegroundColor Cyan  
Write-Host "  Output: $outputDir" -ForegroundColor Cyan
Write-Host "  Package: $packageName" -ForegroundColor Cyan

# Use npx to run web3j CLI
npx web3j generate solidity `
    -a="$abiFile" `
    -b="$binFile" `
    -o="$outputDir" `
    -p="$packageName"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nWrapper generated successfully!" -ForegroundColor Green
    Write-Host "Location: $outputDir\$($packageName.Replace('.', '\'))\DocumentNotarization.java" -ForegroundColor Yellow
} else {
    Write-Host "`nWrapper generation failed!" -ForegroundColor Red
    Write-Host "Trying alternative method with web3j-maven-plugin..." -ForegroundColor Yellow
    
    # Alternative: Call web3j command directly from web3j JAR in Maven cache
    Write-Host "Please install web3j CLI: npm install -g web3j" -ForegroundColor Cyan
}
