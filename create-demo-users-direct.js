/**
 * Create demo customer user directly in PostgreSQL
 * Run with: DATABASE_URL=... node create-demo-users-direct.js
 */

const { Client } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL;
if (!DATABASE_URL) {
  console.error('Missing DATABASE_URL (PostgreSQL connection string).');
  process.exit(1);
}

async function createDemoUsers() {
  const client = new Client({ connectionString: DATABASE_URL, ssl: { rejectUnauthorized: false } });

  try {
    console.log('\n========================================');
    console.log('Creating Demo Customer in PostgreSQL');
    console.log('========================================\n');

    await client.connect();
    console.log('Connected to database\n');

    // Password is 'password' hashed with BCrypt (strength 10)
    const hashedPassword = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

    console.log('Creating customer@example.com...');
    await client.query(`
      INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON CONFLICT (username) DO NOTHING
    `, ['customer@example.com', hashedPassword, 'Demo', 'Customer', 'CUSTOMER']);

    const result = await client.query(`
      SELECT id, username, first_name, last_name, role, created_at
      FROM users
      WHERE username IN ('customer@example.com')
      ORDER BY role
    `);

    console.log('\nCurrent demo users:');
    console.log('===================');
    for (const user of result.rows) {
      console.log(`${user.role.padEnd(10)} | ${user.username.padEnd(25)} | ${user.first_name} ${user.last_name}`);
    }

    console.log('\nYou can now login with:');
    console.log('  Customer: customer@example.com / password');
    console.log('');
    console.log('Note: Privileged OWNER/ADMIN accounts are managed separately.');
    console.log('  Use: node scripts/sync-privileged-users.js');
  } catch (error) {
    console.error('Error:', error.message);
  } finally {
    await client.end();
  }
}

createDemoUsers();
