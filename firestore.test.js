const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_store_1";
const BOB_UID = "bob_store_2";
const DATABASE_ID = "ai-studio-android-modastoc-f43d0de1-cb34-4a0a-bdc7-1eb6db156a27";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function getDb(context) {
  return context.firestore({ databaseId: DATABASE_ID });
}

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

function validGarmentPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 5000);
  return {
    userId: uid,
    barcode: "7501055312345",
    name: "Camisa Lino Clasica",
    category: "CAMISAS",
    size: "M",
    color: "Blanco",
    costPrice: 180.0,
    salePrice: 399.0,
    stock: 12,
    lowStockThreshold: 3,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validSalePayload(uid, garmentId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    garmentId,
    garmentName: "Camisa Lino Clasica",
    barcode: "7501055312345",
    size: "M",
    quantity: 2,
    unitPrice: 399.0,
    totalAmount: 798.0,
    paymentMethod: "EFECTIVO",
    createdAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write garments or sales", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("garments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sales").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("garments").doc("g_1").set(validGarmentPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, read, partially update, and delete garments", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const garmentRef = aliceDb.collection("users").doc(ALICE_UID).collection("garments").doc("garment_1");

  await assertSucceeds(garmentRef.set(validGarmentPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("garments").get());

  // Partial stock update
  await assertSucceeds(
    garmentRef.update({
      stock: 10,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full details edit
  await assertSucceeds(
    garmentRef.update({
      name: "Camisa Lino Premium",
      salePrice: 449.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(garmentRef.delete());
});

test("Rejects negative stock, ghost fields, @DocumentId id field, and immutable field mutation", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const garments = aliceDb.collection("users").doc(ALICE_UID).collection("garments");

  // Negative stock rejected
  await assertFails(garments.doc("g_neg").set(validGarmentPayload(ALICE_UID, { stock: -1 })));

  // Ghost field rejected
  await assertFails(garments.doc("g_ghost").set(validGarmentPayload(ALICE_UID, { isVerified: true })));

  // @DocumentId 'id' inside payload rejected
  await assertFails(garments.doc("g_id").set(validGarmentPayload(ALICE_UID, { id: "g_id" })));

  // Future timestamp rejected
  await assertFails(
    garments.doc("g_future").set(validGarmentPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Create valid, then try mutating createdAt or userId
  await assertSucceeds(garments.doc("g_ok").set(validGarmentPayload(ALICE_UID)));
  await assertFails(
    garments.doc("g_ok").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    garments.doc("g_ok").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's garments or sales", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = getDb(context);
    await adminDb.collection("users").doc(BOB_UID).collection("garments").doc("bob_g1").set(
      validGarmentPayload(BOB_UID)
    );
    await adminDb.collection("users").doc(BOB_UID).collection("sales").doc("bob_s1").set(
      validSalePayload(BOB_UID, "bob_g1")
    );
  });

  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("garments").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("garments").doc("bob_g1").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("garments").doc("bob_g1").update({
      stock: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("sales").get());
});

test("Sales rules enforce existing garment, valid payment method, immutability, and allow void/delete", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const garmentRef = aliceDb.collection("users").doc(ALICE_UID).collection("garments").doc("g_sale_1");
  const saleRef = aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1");

  // Sale fails if referenced garment does not exist
  await assertFails(saleRef.set(validSalePayload(ALICE_UID, "nonexistent_g")));

  // Create garment first
  await assertSucceeds(garmentRef.set(validGarmentPayload(ALICE_UID)));

  // Sale with invalid payment method fails
  await assertFails(
    saleRef.set(validSalePayload(ALICE_UID, "g_sale_1", { paymentMethod: "CRYPTO" }))
  );

  // Valid sale with optional notes omitted succeeds
  await assertSucceeds(saleRef.set(validSalePayload(ALICE_UID, "g_sale_1")));

  // Valid sale with optional notes string succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_2").set(
      validSalePayload(ALICE_UID, "g_sale_1", { notes: "Cliente frecuente" })
    )
  );

  // Sales are immutable (updates rejected)
  await assertFails(saleRef.update({ quantity: 5 }));

  // Owner can list and delete/void a sale
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("sales").get());
  await assertSucceeds(saleRef.delete());
});
