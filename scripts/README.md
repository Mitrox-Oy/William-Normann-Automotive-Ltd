# Scripts

Helper scripts for development and testing.

## create-test-user.js

Creates test users via the backend registration API.

### Usage

```bash
# Create a customer user
node scripts/create-test-user.js customer@example.com password123

# Create an owner user
node scripts/create-test-user.js owner1@example.com password123 OWNER

# Create an admin user with custom name
node scripts/create-test-user.js admin@example.com password123 ADMIN "Admin" "User"
```

### Parameters

1. `email` - User email (required)
2. `password` - User password (required, min 6 characters)
3. `role` - User role: `CUSTOMER`, `OWNER`, or `ADMIN` (optional, defaults to `CUSTOMER`)
4. `firstName` - First name (optional, defaults to "Test")
5. `lastName` - Last name (optional, defaults to "User")

### Requirements

- Node.js installed
- Backend running on `http://localhost:8080` (or set `NEXT_PUBLIC_SHOP_API_BASE_URL`)
- Database accessible

### Examples

```bash
# Create test customer
node scripts/create-test-user.js customer@example.com password123

# Create test owner
node scripts/create-test-user.js owner1@example.com password123 OWNER

# Create test admin
node scripts/create-test-user.js admin@example.com password123 ADMIN
```

