/**
 * List all products in Heroku PostgreSQL
 * Run with: node list-products.js
 */

const { Client } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL || 
  'postgres://uafgpajadi2ig6:pf3f26a77fca31fb9aeca683b8b2881cf797ab9f01123cc24e003aeb5c6afdb4c@cd76nbak7haqep.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d2lc3pph809bpt';

async function listProducts() {
  const client = new Client({
    connectionString: DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  });

  try {
    console.log('\n========================================');
    console.log('Products in Heroku Database');
    console.log('========================================\n');

    await client.connect();
    console.log('✅ Connected to database\n');

    // Check if products table exists
    const tableCheck = await client.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'products'
      );
    `);

    if (!tableCheck.rows[0].exists) {
      console.log('⚠️  Products table does not exist yet.');
      console.log('You need to create products through the admin panel first.');
      return;
    }

    // Check columns in products table
    const columnsResult = await client.query(`
      SELECT column_name, data_type 
      FROM information_schema.columns 
      WHERE table_name = 'products'
      ORDER BY ordinal_position
    `);
    
    console.log('📋 Products table columns:');
    columnsResult.rows.forEach(col => {
      console.log(`  - ${col.column_name} (${col.data_type})`);
    });
    console.log('');

    // List all products
    const result = await client.query(`
      SELECT *
      FROM products
      ORDER BY id DESC
      LIMIT 20
    `);

    if (result.rows.length === 0) {
      console.log('📋 No products found in database.');
      console.log('\nTo create products:');
      console.log('1. Login as owner: https://william-normann.netlify.app/login');
      console.log('2. Go to Admin → Products');
      console.log('3. Create new products');
    } else {
      console.log(`📋 Found ${result.rows.length} products:\n`);
      console.log('='.repeat(100));
      result.rows.forEach(product => {
        console.log(`ID: ${product.id}`);
        console.log(`Name: ${product.name}`);
        console.log(`SKU: ${product.sku || 'NULL'}`);
        console.log(`Price: $${product.price || 'NULL'}`);
        console.log(`Stock: ${product.stock_level || 0}`);
        console.log(`Active: ${product.active}`);
        console.log(`Created: ${product.created_date}`);
        console.log('-'.repeat(100));
      });

      console.log('\n💡 To view a product, use:');
      if (result.rows[0].sku) {
        console.log(`   https://william-normann.netlify.app/shop/${result.rows[0].sku}`);
      } else {
        console.log(`   Products need SKU values to be viewable`);
      }
    }

  } catch (error) {
    console.error('❌ Error:', error.message);
  } finally {
    await client.end();
  }
}

listProducts();
