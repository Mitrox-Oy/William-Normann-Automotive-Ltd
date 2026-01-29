/**
 * Quick test script to verify owner login endpoint
 * 
 * Usage:
 *   node scripts/test-owner-login.js owner@example.com password123
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080';

async function testOwnerLogin(email, password) {
  try {
    console.log(`Testing owner login for: ${email}...\n`);
    
    const response = await fetch(`${API_BASE_URL}/api/auth/owner/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: email,
        password: password,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      console.error('❌ Owner login failed:');
      console.error(`   Status: ${response.status}`);
      console.error(`   Message: ${data.message || JSON.stringify(data)}`);
      process.exit(1);
    }

    console.log('✅ Owner login successful!');
    console.log(`   Email: ${data.username}`);
    console.log(`   Role: ${data.role}`);
    console.log(`   ID: ${data.id}`);
    console.log(`   Token: ${data.token ? data.token.substring(0, 20) + '...' : 'N/A'}`);
    console.log('\nYou can now use these credentials in the frontend login page.');
  } catch (error) {
    console.error('❌ Error testing owner login:');
    console.error(`   ${error.message}`);
    process.exit(1);
  }
}

const args = process.argv.slice(2);

if (args.length < 2) {
  console.log('Usage: node scripts/test-owner-login.js <email> <password>');
  console.log('\nExample:');
  console.log('  node scripts/test-owner-login.js owner@example.com password123');
  process.exit(1);
}

testOwnerLogin(args[0], args[1]);

