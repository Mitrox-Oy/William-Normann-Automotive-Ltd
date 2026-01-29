/**
 * Script to create owner accounts
 * 
 * This script:
 * 1. Creates a user account via the register endpoint (as CUSTOMER)
 * 2. Updates the role to OWNER in the database
 * 
 * Usage:
 *   node scripts/create-owner-user.js owner@example.com password123
 *   node scripts/create-owner-user.js owner@example.com password123 "Owner" "Name"
 */

const { execSync } = require('child_process');
const API_BASE_URL = process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080';

async function createOwnerUser(email, password, firstName = 'Store', lastName = 'Owner') {
  try {
    console.log(`Creating owner account: ${email}...`);
    
    // Step 1: Create user account (will be created as CUSTOMER)
    console.log('\nStep 1: Creating user account...');
    const registerResponse = await fetch(`${API_BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
        role: 'CUSTOMER', // Backend ignores this and always creates CUSTOMER
      }),
    });

    if (!registerResponse.ok) {
      const errorData = await registerResponse.json();
      // If user already exists, that's okay - we'll just update the role
      if (errorData.message?.includes('already taken')) {
        console.log('   User already exists, will update role to OWNER');
      } else {
        throw new Error(`Registration failed: ${errorData.message || registerResponse.statusText}`);
      }
    } else {
      const userData = await registerResponse.json();
      console.log(`   ✅ User account created (ID: ${userData.id})`);
    }

    // Step 2: Update role to OWNER in database
    console.log('\nStep 2: Updating role to OWNER...');
    const updateCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "UPDATE users SET role = 'OWNER' WHERE username = '${email}';"`;
    
    try {
      const result = execSync(updateCommand, { encoding: 'utf-8' });
      console.log('   ✅ Role updated to OWNER');
      
      // Verify the update
      const verifyCommand = `docker exec -i william-normann-shop-db psql -U postgres -d william_normann_shop -c "SELECT username, role FROM users WHERE username = '${email}';"`;
      const verifyResult = execSync(verifyCommand, { encoding: 'utf-8' });
      console.log('\n   Verification:');
      console.log(verifyResult);
      
    } catch (dbError) {
      console.error('   ❌ Failed to update role in database:');
      console.error(`   ${dbError.message}`);
      throw dbError;
    }

    console.log('\n✅ Owner account created successfully!');
    console.log(`   Email: ${email}`);
    console.log(`   Password: ${password}`);
    console.log(`   Role: OWNER`);
    console.log('\nYou can now login with these credentials.');
    console.log('Note: Use the owner login endpoint (/api/auth/owner/login) if your frontend supports it.');
    
  } catch (error) {
    console.error('\n❌ Error creating owner account:');
    console.error(`   ${error.message}`);
    console.error('\nMake sure:');
    console.error('   1. Backend is running on', API_BASE_URL);
    console.error('   2. Database container is running');
    console.error('   3. Docker is accessible');
    process.exit(1);
  }
}

// Parse command line arguments
const args = process.argv.slice(2);

if (args.length < 2) {
  console.log('Usage: node scripts/create-owner-user.js <email> <password> [firstName] [lastName]');
  console.log('\nExamples:');
  console.log('  node scripts/create-owner-user.js owner@example.com password123');
  console.log('  node scripts/create-owner-user.js owner@example.com password123 "Store" "Owner"');
  process.exit(1);
}

const [email, password, firstName = 'Store', lastName = 'Owner'] = args;

createOwnerUser(email, password, firstName, lastName);

