const fs = require('fs');
const path = require('path');

const apiDir = path.join(process.cwd(), 'app', 'api');
const tempApiDir = path.join(process.cwd(), 'api.backup');

// Move API directory temporarily for static export (outside app directory)
if (fs.existsSync(apiDir)) {
  if (fs.existsSync(tempApiDir)) {
    fs.rmSync(tempApiDir, { recursive: true, force: true });
  }
  fs.renameSync(apiDir, tempApiDir);
  console.log('✅ Temporarily moved API routes for static export');
}

