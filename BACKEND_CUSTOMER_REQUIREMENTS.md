# BACKEND REQUIREMENTS - Customer Orders

## Problem
API endpoint `/api/Transaksi` requires `id_user` as a **required field**, but customer orders (from mobile app) don't have user accounts. Only staff (Owner, Manajer, Kasir) have user accounts for desktop platform.

## Solution
Backend needs to create a **GUEST/DUMMY USER** with fixed ID for customer orders.

### SQL Query to Create Guest User
```sql
-- Insert guest user with ID=0 for customer orders
INSERT INTO [User] (id_user, username, password, email, role, status, id_karyawan)
VALUES (0, 'guest_customer', 'no_password', 'customer@guest.com', 3, 0, NULL);

-- OR if auto-increment doesn't allow ID=0, use ID=1
INSERT INTO [User] (username, password, email, role, status, id_karyawan)
VALUES ('guest_customer', 'no_password', 'customer@guest.com', 3, 0, NULL);
-- Then note the generated ID (e.g., ID=1) and update mobile app CustomerApiService.kt
```

### Mobile App Usage
**File:** `CustomerApiService.kt`
```kotlin
// Line ~253
put("id_user", request.idUser ?: 0)  // Use 0 for customer/guest orders
```

**File:** `CustomerApiService.kt` (createTransaksiFromCart)
```kotlin
// Line ~354
val request = CreateTransaksiRequest(
    namaKonsumen = customerName,
    idUser = 0, // Use guest user ID (0 or whatever ID from backend)
    idMeja = mejaId,
    // ...
)
```

## Customer Flow
1. Customer opens app → **NO LOGIN** required
2. Customer chooses Dine In / Take Away
3. Customer enters name
4. Customer browses menu → adds to cart
5. Customer checkout → **creates Transaksi with id_user=0 (guest user)**
6. Customer name stored in `transaksi.nama_konsumen` field

## Alternative Solution (Better)
**Recommended:** Modify backend API to make `id_user` **NULLABLE** (optional) in Transaksi table.

```csharp
// In Transaksi model class
public int? id_user { get; set; } // Make nullable

// In CreateTransaksiRequest validation
[Required] becomes [Optional] for id_user
```

This way, customer orders can have `id_user = null` and staff orders have actual user IDs.

## Current Status
- Mobile app sends `id_user=0` for customer orders
- Backend needs guest user with ID=0 OR modify schema to make id_user nullable
- Customer data is stored in `nama_konsumen` field (NOT in User table)

## Testing
1. Create guest user in backend with ID=0
2. Try creating order from mobile app
3. Check if transaksi is created successfully with id_user=0

Date: 2026-02-19

