const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_zaviro_1";
const ADMIN_EMAIL = "moaztariq27@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function serverTimestamp() {
  return new Date(Date.now() - 1000);
}


// --- 1. UNAUTHENTICATED ACCESS REJECTION ---

test("Unauthenticated user: cannot read customer profiles or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("customers").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("orders").doc("order_1").get());
  await assertFails(unauthDb.collection("settings").doc("admin_security").get());
});

// --- 2. CUSTOMER PROFILE ISOLATION & VALIDATION ---

test("Authenticated user: can create and read their own customer profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp(aliceDb);
  await assertSucceeds(
    aliceDb.collection("customers").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Khan",
      phone: "0300-1234567",
      email: "alice@example.com",
      address: "Gulberg III, Lahore",
      notes: "Call on arrival",
      profileImageUrl: "",
      authProvider: "google",
      isAuthenticated: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("customers").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read or write another user's customer profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = serverTimestamp(db);
    await db.collection("customers").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Bob Malik",
      phone: "0321-7654321",
      email: "bob@example.com",
      address: "DHA Phase 5, Lahore",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("customers").doc(BOB_UID).get());
  const ts = serverTimestamp(aliceDb);
  await assertFails(
    aliceDb.collection("customers").doc(BOB_UID).update({
      name: "Hacked Name",
      updatedAt: ts,
    })
  );
});

test("Authenticated user: shadow field injection on customer profile fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp(aliceDb);
  await assertFails(
    aliceDb.collection("customers").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Khan",
      createdAt: ts,
      updatedAt: ts,
      isVerifiedAdmin: true,
    })
  );
});

// --- 3. ORDER CREATION, QUERY ALIGNMENT & TERMINAL STATE LOCKING ---

test("Authenticated user: can create their own RECEIVED order and query with customerUid filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp(aliceDb);
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_alice_1").set({
      id: "order_alice_1",
      orderNumber: "#ZAV-1001",
      customerUid: ALICE_UID,
      customerName: "Alice Khan",
      phone: "0300-1234567",
      customerEmail: "alice@example.com",
      deliveryAddress: "Gulberg III, Lahore",
      orderNotes: "Extra napkins",
      subtotal: 650,
      deliveryFee: 150,
      total: 800,
      status: "RECEIVED",
      paymentMethod: "Cash on Delivery (COD)",
      items: [
        {
          id: "item_1",
          name: "Creamy Chicken Pasta",
          quantity: 1,
          unitPrice: 650,
          extrasSummary: "",
          specialInstructions: "",
        },
      ],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("orders").where("customerUid", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails order list query without customerUid filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = serverTimestamp(db);
    await db.collection("orders").doc("order_bob_1").set({
      id: "order_bob_1",
      orderNumber: "#ZAV-2002",
      customerUid: BOB_UID,
      customerName: "Bob",
      phone: "0321-1112223",
      deliveryAddress: "Model Town, Lahore",
      subtotal: 500,
      deliveryFee: 150,
      total: 650,
      status: "RECEIVED",
      paymentMethod: "Cash on Delivery (COD)",
      items: [{ id: "1", name: "Wings", quantity: 1, unitPrice: 500 }],
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("orders").get());
});

test("Authenticated user: cannot spoof customerUid or total on order creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp(aliceDb);
  // Spoofed customerUid
  await assertFails(
    aliceDb.collection("orders").doc("order_spoof_1").set({
      id: "order_spoof_1",
      orderNumber: "#ZAV-9999",
      customerUid: BOB_UID,
      customerName: "Alice",
      phone: "0300-1234567",
      deliveryAddress: "Gulberg, Lahore",
      subtotal: 500,
      deliveryFee: 150,
      total: 650,
      status: "RECEIVED",
      paymentMethod: "Cash on Delivery (COD)",
      items: [{ id: "1", name: "Wrap", quantity: 1, unitPrice: 500 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Mismatched total != subtotal + deliveryFee
  await assertFails(
    aliceDb.collection("orders").doc("order_spoof_2").set({
      id: "order_spoof_2",
      orderNumber: "#ZAV-9998",
      customerUid: ALICE_UID,
      customerName: "Alice",
      phone: "0300-1234567",
      deliveryAddress: "Gulberg, Lahore",
      subtotal: 500,
      deliveryFee: 150,
      total: 10,
      status: "RECEIVED",
      paymentMethod: "Cash on Delivery (COD)",
      items: [{ id: "1", name: "Wrap", quantity: 1, unitPrice: 500 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Terminal state locking: customer cannot modify a DELIVERED order", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = serverTimestamp(db);
    await db.collection("orders").doc("order_delivered_1").set({
      id: "order_delivered_1",
      orderNumber: "#ZAV-3003",
      customerUid: ALICE_UID,
      customerName: "Alice",
      phone: "0300-1234567",
      deliveryAddress: "Gulberg, Lahore",
      subtotal: 500,
      deliveryFee: 150,
      total: 650,
      status: "DELIVERED",
      paymentMethod: "Cash on Delivery (COD)",
      items: [{ id: "1", name: "Wrap", quantity: 1, unitPrice: 500 }],
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp(aliceDb);
  await assertFails(
    aliceDb.collection("orders").doc("order_delivered_1").update({
      status: "CANCELLED",
      updatedAt: ts,
    })
  );
});

// --- 4. ADMIN RBAC & EMAIL SPOOFING PROTECTION ---

test("Email spoofing guard: unverified admin email cannot write products", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  const ts = serverTimestamp(spoofedAdminDb);

  await assertFails(
    spoofedAdminDb.collection("products").doc("prod_1").set({
      id: "prod_1",
      name: "Creamy Pasta",
      category: "Pasta",
      price: 550,
      isAvailable: true,
      isDeleted: false,
      updatedAt: ts,
    })
  );
});

test("Verified bootstrapped admin: can write products and customers can query non-deleted products", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const ts = serverTimestamp(adminDb);

  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").set({
      id: "prod_1",
      name: "Creamy Pasta",
      category: "Pasta",
      description: "Rich white sauce pasta",
      ingredients: "Pasta, Cream, Chicken",
      price: 550,
      isAvailable: true,
      isPopular: true,
      imageUrl: "",
      isDeleted: false,
      updatedAt: ts,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("products").where("isDeleted", "==", false).get()
  );
});
