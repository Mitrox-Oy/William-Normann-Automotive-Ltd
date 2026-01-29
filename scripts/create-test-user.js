/**
 * Script to create test users via the backend API
 * 
 * Usage:
 *   node scripts/create-test-user.js customer@example.com password123
 *   node scripts/create-test-user.js owner@example.com password123 owner
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080';

async function createUser(email, password, role = 'CUSTOMER', firstName = 'Test', lastName = 'User') {
  try {
    console.log(`Creating user: ${email} (role: ${role})...`);
    
    const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
        role: role,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      console.error('❌ Failed to create user:');
      console.error(`   Status: ${response.status}`);
      console.error(`   Message: ${data.message || JSON.stringify(data)}`);
      process.exit(1);
    }

    console.log('✅ User created successfully!');
    console.log(`   Email: ${data.username}`);
    console.log(`   Role: ${data.role}`);
    console.log(`   ID: ${data.id}`);
    console.log('\nYou can now login with these credentials.');
  } catch (error) {
    console.error('❌ Error creating user:');
    console.error(`   ${error.message}`);
    console.error('\nMake sure:');
    console.error('   1. Backend is running on', API_BASE_URL);
    console.error('   2. Database is accessible');
    console.error('   3. CORS is configured correctly');
    process.exit(1);
  }
}

// Parse command line arguments
const args = process.argv.slice(2);

if (args.length < 2) {
  console.log('Usage: node scripts/create-test-user.js <email> <password> [role] [firstName] [lastName]');
  console.log('\nExamples:');
  console.log('  node scripts/create-test-user.js customer@example.com password123');
  console.log('  node scripts/create-test-user.js owner@example.com password123 OWNER');
  console.log('  node scripts/create-test-user.js admin@example.com password123 ADMIN "Admin" "User"');
  process.exit(1);
}

const [email, password, role = 'CUSTOMER', firstName = 'Test', lastName = 'User'] = args;

createUser(email, password, role, firstName, lastName);

