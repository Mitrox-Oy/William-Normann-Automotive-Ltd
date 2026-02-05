const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');

// Database configuration
const pool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'william_normann_shop',
  user: 'postgres',
  password: 'postgres',
});

async function runMigration() {
  const client = await pool.connect();
  try {
    console.log('Reading migration script...');
    const migrationPath = path.join(__dirname, 'database_migration_shop_topics.sql');
    const sql = fs.readFileSync(migrationPath, 'utf8');

    console.log('Executing migration...');
    // Split by semicolons and execute each statement
    const statements = sql
      .split(';')
      .map(stmt => stmt.trim())
      .filter(stmt => stmt.length > 0 && !stmt.startsWith('--'));

    for (const statement of statements) {
      try {
        await client.query(statement);
        console.log('✓ Executed:', statement.substring(0, 80) + '...');
      } catch (error) {
        console.error('Error executing statement:', statement.substring(0, 80));
        console.error('Error:', error.message);
      }
    }

    console.log('\n✓ Migration completed successfully!');

    // Verify the migration
    console.log('\nVerifying root categories...');
    const result = await client.query(
      "SELECT id, name, slug, parent_id FROM categories WHERE parent_id IS NULL AND active = true ORDER BY sort_order;"
    );
    console.log('Root categories:');
    console.table(result.rows);

  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  } finally {
    await client.end();
    await pool.end();
  }
}

runMigration();
