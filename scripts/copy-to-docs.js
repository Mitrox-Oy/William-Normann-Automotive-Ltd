const fs = require('fs');
const path = require('path');

const sourceDir = path.join(process.cwd(), 'out');
const destDir = path.join(process.cwd(), 'docs');

// Remove existing docs directory if it exists
if (fs.existsSync(destDir)) {
  fs.rmSync(destDir, { recursive: true, force: true });
}

// Copy out directory to docs
if (fs.existsSync(sourceDir)) {
  fs.cpSync(sourceDir, destDir, { recursive: true });
  console.log('✅ Successfully copied build output to /docs folder');
} else {
  console.error('❌ Build output not found. Please run "npm run build" first.');
  process.exit(1);
}

