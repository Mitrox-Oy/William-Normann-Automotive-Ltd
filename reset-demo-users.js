/**
 * Update/Reset demo user passwords in Heroku PostgreSQL
 * Run with: node reset-demo-users.js
 */

const { Client } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL || 
  'postgres://uafgpajadi2ig6:pf3f26a77fca31fb9aeca683b8b2881cf797ab9f01123cc24e003aeb5c6afdb4c@cd76nbak7haqep.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d2lc3pph809bpt';

async function resetDemoUsers() {
  const client = new Client({
    connectionString: DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  });

  try {
    console.log('\n========================================');
    console.log('Resetting Demo User Passwords');
    console.log('========================================\n');

    await client.connect();
    console.log('✅ Connected to database\n');

    // Password is 'password' hashed with BCrypt (strength 10)
    const hashedPassword = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

    // Update customer user
    console.log('Updating customer@example.com...');
    const customerResult = await client.query(`
      UPDATE users 
      SET password = $1, updated_at = CURRENT_TIMESTAMP
      WHERE username = $2
      RETURNING id, username, role
    `, [hashedPassword, 'customer@example.com']);
    
    if (customerResult.rowCount > 0) {
      console.log('✅ Customer password updated');
    } else {
      console.log('⚠️  Customer user not found, creating...');
      await client.query(`
        INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      `, ['customer@example.com', hashedPassword, 'Demo', 'Customer', 'CUSTOMER']);
      console.log('✅ Customer user created');
    }

    // Update owner user
    console.log('\nUpdating owner@example.com...');
    const ownerResult = await client.query(`
      UPDATE users 
      SET password = $1, role = $2, updated_at = CURRENT_TIMESTAMP
      WHERE username = $3
      RETURNING id, username, role
    `, [hashedPassword, 'OWNER', 'owner@example.com']);
    
    if (ownerResult.rowCount > 0) {
      console.log('✅ Owner password updated and role set to OWNER');
    } else {
      console.log('⚠️  Owner user not found, creating...');
      await client.query(`
        INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      `, ['owner@example.com', hashedPassword, 'Store', 'Owner', 'OWNER']);
      console.log('✅ Owner user created');
    }

    // Verify users
    console.log('\nVerifying users...');
    const result = await client.query(`
      SELECT id, username, first_name, last_name, role, created_at, updated_at
      FROM users
      WHERE username IN ('customer@example.com', 'owner@example.com')
      ORDER BY role
    `);

    console.log('\n📋 Current users:');
    console.log('='.repeat(80));
    result.rows.forEach(user => {
      console.log(`${user.role.padEnd(10)} | ${user.username.padEnd(25)} | ${user.first_name} ${user.last_name}`);
      console.log(`  ID: ${user.id} | Updated: ${user.updated_at.toISOString()}`);
    });

    console.log('\n========================================');
    console.log('✅ Demo Users Updated Successfully!');
    console.log('========================================');
    console.log('\nYou can now login with:');
    console.log('  Customer: customer@example.com / password');
    console.log('  Owner:    owner@example.com / password');
    console.log('');

  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error('Full error:', error);
  } finally {
    await client.end();
  }
}

resetDemoUsers();
