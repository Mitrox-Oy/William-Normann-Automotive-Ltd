/**
 * Script to delete a user from the database
 * 
 * Usage:
 *   node scripts/delete-user.js customer@example.com
 */

const { execSync } = require('child_process');

function deleteUser(email) {
  try {
    console.log(`Deleting user: ${email}...`);
    
    const command = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "DELETE FROM users WHERE username = '${email}';"`;
    
    const result = execSync(command, { encoding: 'utf-8' });
    console.log(result);
    console.log(`✅ User deleted successfully!`);
    console.log(`\nYou can now create a new user with:`);
    console.log(`   node scripts/create-test-user.js ${email} <password>`);
  } catch (error) {
    console.error('❌ Error deleting user:');
    console.error(`   ${error.message}`);
    console.error('\nMake sure:');
    console.error('   1. Docker is running');
    console.error('   2. Database container is running');
    console.error('   3. User email is correct');
    process.exit(1);
  }
}

const args = process.argv.slice(2);
if (args.length < 1) {
  console.log('Usage: node scripts/delete-user.js <email>');
  console.log('\nExample:');
  console.log('  node scripts/delete-user.js customer@example.com');
  process.exit(1);
}

deleteUser(args[0]);

