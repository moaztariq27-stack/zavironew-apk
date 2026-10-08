# ZAVIRO Pakistan — Firestore Security Specification & Red Team Audit

## 1. Data Invariants

1. **Default-Deny Catch-All**: Any path not explicitly matched is unconditionally denied (`allow read, write: if false;`).
2. **Customer & User Profile Isolation (`/customers/{userId}`, `/users/{userId}`)**:
   - Contain PII (`name`, `phone`, `email`, `address`).
   - Only the authenticated owner (`request.auth.uid == userId`) or a verified admin (`isAdmin()`) can `get`, `list` (where `resource.data.uid == request.auth.uid`), `create`, `update`, or `delete`.
   - `uid` and `createdAt` are immutable on `update`.
   - `createdAt` and `updatedAt` must be valid server timestamps (`<= request.time`).
3. **Order Isolation & Terminal State Locking (`/orders/{orderId}`)**:
   - Contains customer PII and financial totals.
   - `create` requires `request.auth != null`, `incoming().customerUid == request.auth.uid`, `incoming().status == 'RECEIVED'`, valid item list (`size() >= 1 && size() <= 50`), non-negative integer totals (`total == subtotal + deliveryFee`), and valid server timestamps (`createdAt <= request.time`, `updatedAt <= request.time`).
   - `get` and `list` require `request.auth != null` and `(resource.data.customerUid == request.auth.uid || isAdmin())`.
   - `update` enforces terminal state locking: once `existing().status in ['DELIVERED', 'CANCELLED']`, non-admin updates are rejected. A customer can only cancel their own `RECEIVED` order (`status` -> `'CANCELLED'`) without mutating totals, items, or ownership. Admins can update order status (`status`, `updatedAt`).
4. **Catalog Integrity (`/products/{productId}`, `/deals/{dealId}`, `/extras/{extraId}`)**:
   - `list` requires `resource.data.isDeleted == false || isAdmin()` (prevents scraping soft-deleted items).
   - `create`, `update`, `delete` require `isAdmin()` and strict schema validation (`isValidProduct`, `isValidDeal`, `isValidExtra`).
5. **Store Settings & Admin Security (`/settings/{settingId}`)**:
   - `/settings/general` and `/settings/branding` allow `get` for storefront display, `list` is denied, and writes require `isAdmin()` plus schema validation.
   - `/settings/admin_security` denies `read` to non-admins, forbids plaintext `adminPassword`, and requires `isAdmin()` on writes.

## 2. The "Dirty Dozen" Adversarial Payloads

1. **Unauthenticated PII Read**: Unauthenticated client attempts `get(/customers/alice_123)` -> **REJECTED**.
2. **Cross-User Profile Read**: Authenticated `bob_456` attempts `get(/customers/alice_123)` -> **REJECTED**.
3. **Unfiltered Profile/Order List Scraping**: Authenticated `alice_123` executes `collection("orders").get()` without `.where("customerUid", "==", "alice_123")` -> **REJECTED** by `resource.data.customerUid == request.auth.uid`.
4. **Order Identity Spoofing**: Authenticated `mallory_789` creates an order with `customerUid: "alice_123"` -> **REJECTED**.
5. **Order Status Shortcutting on Create**: Customer creates an order directly with `status: "DELIVERED"` -> **REJECTED** (must start in `'RECEIVED'`).
6. **Order Price Manipulation on Update**: Customer updates their `RECEIVED` order with `total: 0` -> **REJECTED** by `affectedKeys().hasOnly(['status', 'orderNotes', 'updatedAt'])`.
7. **Terminal State Re-opening**: Customer attempts to update an order whose `existing().status == 'DELIVERED'` back to `'CANCELLED'` -> **REJECTED** by terminal state lock.
8. **Shadow Field Injection ("Ghost Field")**: Customer updates `/customers/alice_123` with an extra field `isAdmin: true` -> **REJECTED** by `keys().hasOnly(...)` and `affectedKeys().hasOnly(...)`.
9. **Future Timestamp Spoofing**: Client submits `createdAt` set 1 hour in the future (`> request.time`) -> **REJECTED** by `createdAt <= request.time`.
10. **Immutable Field Mutation**: Customer updates `/customers/alice_123` changing `createdAt` or `uid` -> **REJECTED** by `incoming().createdAt == existing().createdAt && incoming().uid == existing().uid`.
11. **Unverified Admin Email Spoofing**: Attacker authenticates with `email: "moaztariq27@gmail.com"` but `email_verified: false` and attempts to write `/products/prod_1` -> **REJECTED** by `request.auth.token.email_verified == true`.
12. **Catalog Poisoning by Regular User**: Authenticated non-admin customer attempts to create or update `/products/prod_1` or `/settings/general` -> **REJECTED** by `isAdmin()`.
