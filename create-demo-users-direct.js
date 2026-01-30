/**
 * Create demo users directly in Heroku PostgreSQL
 * Run with: node create-demo-users-direct.js
 */

const { Client } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL || 
  'postgres://uafgpajadi2ig6:pf3f26a77fca31fb9aeca683b8b2881cf797ab9f01123cc24e003aeb5c6afdb4c@cd76nbak7haqep.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d2lc3pph809bpt';

async function createDemoUsers() {
  const client = new Client({
    connectionString: DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  });

  try {
    console.log('\n========================================');
    console.log('Creating Demo Users in Heroku PostgreSQL');
    console.log('========================================\n');

    await client.connect();
    console.log('✅ Connected to database\n');

    // Password is 'password' hashed with BCrypt (strength 10)
    const hashedPassword = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

    // Create customer user
    console.log('Creating customer@example.com...');
    await client.query(`
      INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON CONFLICT (username) DO NOTHING
    `, ['customer@example.com', hashedPassword, 'Demo', 'Customer', 'CUSTOMER']);
    console.log('✅ Customer user created or already exists\n');

    // Create owner user
    console.log('Creating owner@example.com...');
    await client.query(`
      INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON CONFLICT (username) DO NOTHING
    `, ['owner@example.com', hashedPassword, 'Store', 'Owner', 'OWNER']);
    console.log('✅ Owner user created or already exists\n');

    // Verify users
    console.log('Verifying users...');
    const result = await client.query(`
      SELECT id, username, first_name, last_name, role, created_at
      FROM users
      WHERE username IN ('customer@example.com', 'owner@example.com')
      ORDER BY role
    `);

    console.log('\n📋 Created users:');
    console.log('================');
    result.rows.forEach(user => {
      console.log(`${user.role.padEnd(10)} | ${user.username.padEnd(25)} | ${user.first_name} ${user.last_name}`);
    });

    console.log('\n========================================');
    console.log('✅ Demo Users Created Successfully!');
    console.log('========================================');
    console.log('\nYou can now login with:');
    console.log('  Customer: customer@example.com / password');
    console.log('  Owner:    owner@example.com / password');
    console.log('');

  } catch (error) {
    console.error('❌ Error:', error.message);
    if (error.message.includes('does not exist')) {
      console.log('\n⚠️  The users table doesn\'t exist yet.');
      console.log('Run the database migrations first:');
      console.log('  heroku pg:psql -a william-normann < database_migration_phase1.sql');
    }
  } finally {
    await client.end();
  }
}

createDemoUsers();
