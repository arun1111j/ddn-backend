#!/bin/bash

# setup-env.sh - Environment setup script for Document Notarization App
echo "🚀 Setting up environment for Document Notarization..."

# Check if .env file already exists
if [ -f .env ]; then
    echo "⚠️  .env file already exists. Backing up as .env.backup..."
    cp .env .env.backup
fi

# Create .env file with default values for local development
cat > .env << 'EOL'
# ===== DATABASE CONFIGURATION =====
DB_HOST=localhost
DB_PORT=3306
DB_NAME=notarization_db
DB_USER=root
DB_PASSWORD=root

# ===== BLOCKCHAIN CONFIGURATION =====
# Local Ganache settings
BLOCKCHAIN_URL=http://localhost:8545

# Ganache default private key (from ganache-cli -d)
PRIVATE_KEY=0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d

# Contract automation
CONTRACT_ADDRESS=
AUTO_DEPLOY=true

# ===== IPFS CONFIGURATION =====
# Get these from https://app.pinata.cloud/developers
PINATA_API_KEY=your_pinata_api_key_here
PINATA_API_SECRET=your_pinata_secret_here

# ===== APPLICATION CONFIGURATION =====
CORS_ORIGINS=http://localhost:3000,http://localhost:5173

# ===== RENDER.COM SPECIFIC =====
# These will be set automatically in Render
# RENDER_EXTERNAL_URL=
# RENDER_GIT_BRANCH=
EOL

echo "✅ Environment file (.env) created successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Update the Pinata credentials in .env file"
echo "2. Run: docker-compose up -d"
echo "3. Check logs: docker-compose logs -f app"
echo "4. Verify health: curl http://localhost:8080/api/health"
echo ""
echo "🔗 Pinata Dashboard: https://app.pinata.cloud/developers"