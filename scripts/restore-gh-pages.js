const fs = require('fs');
const path = require('path');

const apiDir = path.join(process.cwd(), 'app', 'api');
const tempApiDir = path.join(process.cwd(), 'api.backup');

// Restore API directory after build
if (fs.existsSync(tempApiDir)) {
  if (fs.existsSync(apiDir)) {
    fs.rmSync(apiDir, { recursive: true, force: true });
  }
  fs.renameSync(tempApiDir, apiDir);
  console.log('✅ Restored API routes');
}

