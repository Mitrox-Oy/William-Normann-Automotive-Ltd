/**
 * Script to reset a user's password via direct database update
 * 
 * WARNING: This requires direct database access and should only be used for development.
 * 
 * Usage:
 *   node scripts/reset-user-password.js customer@example.com newpassword123
 */

const { execSync } = require('child_process');

function resetPassword(email, newPassword) {
  // Generate BCrypt hash using the backend container
  // We'll use a simple approach: call the backend to hash it, or use a known hash
  
  // For now, we'll delete and recreate the user instead
  console.log(`⚠️  This script will delete and recreate user: ${email}`);
  console.log(`   Use the create-test-user.js script instead for a safer approach.`);
  console.log(`\nTo reset password:`);
  console.log(`   1. Delete user: docker exec -it william-normann-shop-db psql -U postgres -d william_normann_shop -c "DELETE FROM users WHERE username = '${email}';"`);
  console.log(`   2. Create new user: node scripts/create-test-user.js ${email} ${newPassword}`);
  process.exit(1);
}

const args = process.argv.slice(2);
if (args.length < 2) {
  console.log('Usage: node scripts/reset-user-password.js <email> <newPassword>');
  console.log('\nRecommended: Use create-test-user.js to delete and recreate instead.');
  process.exit(1);
}

resetPassword(args[0], args[1]);

