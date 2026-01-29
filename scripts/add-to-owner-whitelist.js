/**
 * Script to add an email to the owner whitelist
 * 
 * Owner accounts need to be in the whitelist to login via /api/auth/owner/login
 * 
 * Usage:
 *   node scripts/add-to-owner-whitelist.js owner@example.com
 */

const { execSync } = require('child_process');

function addToWhitelist(email) {
  try {
    console.log(`Adding ${email} to owner whitelist...`);
    
    // Check if whitelist table exists and get its structure
    const checkTableCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE '%whitelist%';"`;
    
    let tableName = 'owner_whitelist'; // Default table name
    
    try {
      const tableCheck = execSync(checkTableCommand, { encoding: 'utf-8' });
      if (!tableCheck.includes('owner_whitelist')) {
        console.log('   ⚠️  Owner whitelist table not found. Skipping whitelist addition.');
        console.log('   The owner account can still be created, but may need whitelist entry for login.');
        return;
      }
    } catch (error) {
      console.log('   ⚠️  Could not check for whitelist table. Skipping whitelist addition.');
      return;
    }
    
    // Check if email already exists in whitelist
    const checkCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "SELECT id, email, is_active FROM owner_whitelist WHERE email = '${email}';"`;
    
    try {
      const checkResult = execSync(checkCommand, { encoding: 'utf-8' });
      
      if (checkResult.includes(email)) {
        // Email exists, update to active
        console.log('   Email found in whitelist, activating...');
        const updateCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "UPDATE owner_whitelist SET is_active = true WHERE email = '${email}';"`;
        execSync(updateCommand, { encoding: 'utf-8' });
        console.log('   ✅ Whitelist entry activated');
      } else {
        // Email doesn't exist, insert new entry
        console.log('   Adding new whitelist entry...');
        const insertCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "INSERT INTO owner_whitelist (email, is_active, created_at) VALUES ('${email}', true, NOW()) ON CONFLICT (email) DO UPDATE SET is_active = true;"`;
        execSync(insertCommand, { encoding: 'utf-8' });
        console.log('   ✅ Added to whitelist');
      }
      
      // Verify
      const verifyCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "SELECT email, is_active FROM owner_whitelist WHERE email = '${email}';"`;
      const verifyResult = execSync(verifyCommand, { encoding: 'utf-8' });
      console.log('\n   Verification:');
      console.log(verifyResult);
      
    } catch (error) {
      console.error('   ❌ Failed to update whitelist:');
      console.error(`   ${error.message}`);
      throw error;
    }
    
  } catch (error) {
    console.error('❌ Error adding to whitelist:');
    console.error(`   ${error.message}`);
    console.error('\nMake sure:');
    console.error('   1. Database container is running');
    console.error('   2. Docker is accessible');
    console.error('   3. Whitelist table exists');
    process.exit(1);
  }
}

const args = process.argv.slice(2);

if (args.length < 1) {
  console.log('Usage: node scripts/add-to-owner-whitelist.js <email>');
  console.log('\nExample:');
  console.log('  node scripts/add-to-owner-whitelist.js owner@example.com');
  process.exit(1);
}

addToWhitelist(args[0]);

