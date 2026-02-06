/**
 * Reset demo customer password directly in PostgreSQL
 * Run with: DATABASE_URL=... node reset-demo-users.js
 */

const { Client } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL;
if (!DATABASE_URL) {
  console.error('Missing DATABASE_URL (PostgreSQL connection string).');
  process.exit(1);
}

async function resetDemoUsers() {
  const client = new Client({ connectionString: DATABASE_URL, ssl: { rejectUnauthorized: false } });

  try {
    console.log('\n========================================');
    console.log('Resetting Demo Customer Password');
    console.log('========================================\n');

    await client.connect();
    console.log('Connected to database\n');

    // Password is 'password' hashed with BCrypt (strength 10)
    const hashedPassword = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

    console.log('Updating customer@example.com...');
    const customerResult = await client.query(`
      UPDATE users
      SET password = $1, updated_at = CURRENT_TIMESTAMP
      WHERE username = $2
      RETURNING id, username, role
    `, [hashedPassword, 'customer@example.com']);

    if (customerResult.rowCount > 0) {
      console.log('Customer password updated');
    } else {
      console.log('Customer user not found, creating...');
      await client.query(`
        INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      `, ['customer@example.com', hashedPassword, 'Demo', 'Customer', 'CUSTOMER']);
      console.log('Customer user created');
    }

    const result = await client.query(`
      SELECT id, username, first_name, last_name, role, created_at, updated_at
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
  } catch (error) {
    console.error('Error:', error.message);
  } finally {
    await client.end();
  }
}

resetDemoUsers();
