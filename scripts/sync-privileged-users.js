/**
 * Sync privileged users directly in PostgreSQL (local or Heroku) using DATABASE_URL.
 *
 * This script:
 * - Creates/updates 1 ADMIN user and 2 OWNER users (email + password + names)
 * - Ensures they are active in owner_whitelist (required for /api/auth/owner/login)
 * - Deletes legacy owner@example.com (and basic dependent rows) if present
 *
 * Configuration (env vars):
 * - DATABASE_URL (required)
 * - WNA_ADMIN_EMAIL,  WNA_ADMIN_PASSWORD
 * - WNA_OWNER1_EMAIL, WNA_OWNER1_PASSWORD
 * - WNA_OWNER2_EMAIL, WNA_OWNER2_PASSWORD
 * Optional:
 * - WNA_ADMIN_FIRST_NAME, WNA_ADMIN_LAST_NAME
 * - WNA_OWNER1_FIRST_NAME, WNA_OWNER1_LAST_NAME
 * - WNA_OWNER2_FIRST_NAME, WNA_OWNER2_LAST_NAME
 * - WNA_REMOVE_EMAILS (comma-separated, default: owner@example.com)
 *
 * Usage:
 *   node scripts/sync-privileged-users.js
 */

const { Client } = require('pg');

function required(name, value) {
  if (!value) throw new Error(`Missing required env var: ${name}`);
  return value;
}

function splitEmails(value) {
  return String(value || '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);
}

async function ensurePgcrypto(client) {
  // Needed for bcrypt hashes via crypt(..., gen_salt('bf', ...))
  await client.query('CREATE EXTENSION IF NOT EXISTS pgcrypto');
}

async function upsertUser(client, { username, password, firstName, lastName, role }) {
  await client.query(
    `INSERT INTO users (
        username,
        password,
        first_name,
        last_name,
        role,
        account_non_expired,
        account_non_locked,
        credentials_non_expired,
        enabled,
        created_at,
        updated_at
     )
     VALUES (
        $1,
        crypt($2, gen_salt('bf', 10)),
        $3,
        $4,
        $5,
        true,
        true,
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
     )
     ON CONFLICT (username) DO UPDATE
       SET password = crypt($2, gen_salt('bf', 10)),
           first_name = EXCLUDED.first_name,
           last_name = EXCLUDED.last_name,
           role = EXCLUDED.role,
           account_non_expired = true,
           account_non_locked = true,
           credentials_non_expired = true,
           enabled = true,
           updated_at = CURRENT_TIMESTAMP`,
    [username, password, firstName || '', lastName || '', role]
  );
}

async function upsertWhitelist(client, { email, notes }) {
  await client.query(
    `INSERT INTO owner_whitelist (email, is_active, created_at, updated_at, created_by, notes)
     VALUES ($1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, $2, $3)
     ON CONFLICT (email) DO UPDATE
       SET is_active = true,
           updated_at = CURRENT_TIMESTAMP,
           notes = EXCLUDED.notes`,
    [email, 'sync-privileged-users', notes || null]
  );
}

async function deleteUserFully(client, username) {
  const idRes = await client.query('SELECT id FROM users WHERE username = $1', [username]);
  const userId = idRes.rows[0]?.id;
  if (userId == null) {
    // Still remove whitelist entry (if any)
    await client.query('DELETE FROM owner_whitelist WHERE email = $1', [username]).catch(() => {});
    return { deleted: false };
  }

  // Best-effort dependent deletes; ignore if tables don't exist in a given schema.
  await client.query('DELETE FROM wishlist WHERE user_id = $1', [userId]).catch(() => {});
  await client.query('DELETE FROM shipping_addresses WHERE user_id = $1', [userId]).catch(() => {});
  await client
    .query('DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = $1)', [userId])
    .catch(() => {});
  await client.query('DELETE FROM carts WHERE user_id = $1', [userId]).catch(() => {});
  await client
    .query('DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id = $1)', [userId])
    .catch(() => {});
  await client.query('DELETE FROM orders WHERE user_id = $1', [userId]).catch(() => {});

  await client.query('DELETE FROM users WHERE id = $1', [userId]);
  await client.query('DELETE FROM owner_whitelist WHERE email = $1', [username]).catch(() => {});
  return { deleted: true };
}

async function main() {
  const databaseUrl = required('DATABASE_URL', process.env.DATABASE_URL);

  const admin = {
    username: required('WNA_ADMIN_EMAIL', process.env.WNA_ADMIN_EMAIL),
    password: required('WNA_ADMIN_PASSWORD', process.env.WNA_ADMIN_PASSWORD),
    firstName: process.env.WNA_ADMIN_FIRST_NAME || 'Johannes',
    lastName: process.env.WNA_ADMIN_LAST_NAME || 'Hurmerinta',
    role: 'ADMIN',
    notes: 'Privileged admin account',
  };

  const owner1 = {
    username: required('WNA_OWNER1_EMAIL', process.env.WNA_OWNER1_EMAIL),
    password: required('WNA_OWNER1_PASSWORD', process.env.WNA_OWNER1_PASSWORD),
    firstName: process.env.WNA_OWNER1_FIRST_NAME || 'Owner',
    lastName: process.env.WNA_OWNER1_LAST_NAME || 'User',
    role: 'OWNER',
    notes: 'Privileged owner account',
  };

  const owner2 = {
    username: required('WNA_OWNER2_EMAIL', process.env.WNA_OWNER2_EMAIL),
    password: required('WNA_OWNER2_PASSWORD', process.env.WNA_OWNER2_PASSWORD),
    firstName: process.env.WNA_OWNER2_FIRST_NAME || 'Owner',
    lastName: process.env.WNA_OWNER2_LAST_NAME || 'User',
    role: 'OWNER',
    notes: 'Privileged owner account',
  };

  const removeEmails = splitEmails(process.env.WNA_REMOVE_EMAILS || 'owner@example.com');

  // Heroku-style managed Postgres typically requires SSL; local Docker Postgres typically does not.
  let ssl = false;
  try {
    const u = new URL(databaseUrl);
    const host = (u.hostname || '').toLowerCase();
    const isLocal = host === 'localhost' || host === '127.0.0.1' || host === 'postgres';
    const forceSsl = String(process.env.DATABASE_SSL || '').toLowerCase() === 'true';
    ssl = forceSsl || !isLocal;
  } catch {
    // If parsing fails, keep ssl=false; user can force via DATABASE_SSL=true.
  }

  const client = new Client({
    connectionString: databaseUrl,
    ssl: ssl ? { rejectUnauthorized: false } : false,
  });
  await client.connect();

  try {
    await client.query('BEGIN');
    await ensurePgcrypto(client);

    for (const u of [admin, owner1, owner2]) {
      await upsertUser(client, u);
      await upsertWhitelist(client, { email: u.username, notes: u.notes });
    }

    for (const email of removeEmails) {
      await deleteUserFully(client, email);
    }

    await client.query('COMMIT');

    const verify = await client.query(
      `SELECT username, role FROM users
       WHERE username = ANY($1::text[])
       ORDER BY username`,
      [[admin.username, owner1.username, owner2.username, ...removeEmails]]
    );

    console.log('Synced privileged users (username, role):');
    for (const row of verify.rows) console.log(`- ${row.username} (${row.role})`);
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    throw e;
  } finally {
    await client.end();
  }
}

main().catch(err => {
  console.error(err.message);
  process.exit(1);
});
