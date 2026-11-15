const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user cannot read or write user tasks, transactions, or services", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("services").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("leaderboard").where("isPublic", "==", true).get());
});

test("User profile: owner can create and read, cross-user blocked, shadow isPremium blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Thabiso",
      email: "thabiso@example.co.za",
      currencyCode: "ZAR",
      savingsTargetZar: 5000,
      currentStreak: 0,
      bestScore: 0,
      totalGamePoints: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Cross-user PII read blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update on isPremium blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isPremium: true,
      updatedAt: ts(),
    })
  );
});

test("Money transactions: owner can CRUD own ZAR transactions, cross-user denied, negative amount denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set({
      userId: ALICE_UID,
      title: "Checkers Groceries",
      amount: 450.5,
      type: "EXPENSE",
      category: "Groceries",
      dateIso: "2026-11-15",
      notes: "Weekly essentials",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Negative amount rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      title: "Invalid",
      amount: -10,
      type: "EXPENSE",
      category: "Groceries",
      dateIso: "2026-11-15",
      createdAt: ts(),
    })
  );

  // Cross-user access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").get());
});

test("Shopping items, planner tasks, reminders, and goals enforce owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("shopping_items").doc("s1").set({
      userId: ALICE_UID,
      name: "Maize Meal 5kg",
      quantity: 1,
      unit: "bag",
      estimatedPrice: 79.99,
      shopName: "Shoprite",
      category: "Groceries",
      purchased: false,
      isFrequent: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Pay City Power prepaid",
      priority: "HIGH",
      dueDateIso: "2026-11-15",
      dueTime: "09:00",
      category: "Bills",
      completed: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("r1").set({
      userId: ALICE_UID,
      title: "Rent Payment",
      category: "Rent",
      dueDateTimeIso: "2026-12-01T08:00",
      recurrence: "MONTHLY",
      amountZar: 6500,
      completed: false,
      notificationsEnabled: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("financial_goals").doc("g1").set({
      userId: ALICE_UID,
      title: "Emergency Fund",
      category: "Saving Money",
      targetValue: 10000,
      currentValue: 2500,
      unitLabel: "ZAR",
      deadlineIso: "2027-06-01",
      completed: false,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("shopping_items").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reminders").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("financial_goals").get());
});

test("Local services directory and genuine reviews enforce verification lock and query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Cannot self-verify on create
  await assertFails(
    aliceDb.collection("services").doc("srv_spoof").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      businessName: "Spoof Auto",
      category: "Mechanics",
      city: "Johannesburg",
      suburb: "Randburg",
      businessPhone: "011-555-0101",
      servicesOffered: "Engine diagnostics and brake service",
      isVerified: true,
      isPublished: true,
      ratingSum: 0,
      reviewCount: 0,
      createdAt: ts(),
    })
  );

  // Valid create with isVerified = false
  await assertSucceeds(
    aliceDb.collection("services").doc("srv1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice M.",
      businessName: "Randburg Reliable Auto",
      category: "Mechanics",
      city: "Johannesburg",
      suburb: "Randburg",
      businessPhone: "011-555-0101",
      servicesOffered: "Brake pads, major service, diagnostics",
      isVerified: false,
      isPublished: true,
      ratingSum: 0,
      reviewCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Unfiltered services list must fail (rules are not filters)
  await assertFails(bobDb.collection("services").get());

  // Filtered published services list succeeds
  await assertSucceeds(bobDb.collection("services").where("isPublished", "==", true).get());

  // Bob submits a genuine review
  await assertSucceeds(
    bobDb.collection("services").doc("srv1").collection("reviews").doc("rev1").set({
      serviceId: "srv1",
      authorId: BOB_UID,
      authorName: "Bob K.",
      rating: 5,
      comment: "Transparent quote and fixed my brakes same day.",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Filtered review query succeeds
  await assertSucceeds(
    bobDb.collection("services").doc("srv1").collection("reviews").where("serviceId", "==", "srv1").get()
  );

  // Alice cannot delete Bob's review
  await assertFails(
    aliceDb.collection("services").doc("srv1").collection("reviews").doc("rev1").delete()
  );
});

test("Leaderboard allows opt-in public query and prevents cross-user overwrite", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      totalPoints: 180,
      currentStreak: 3,
      bestScore: 95,
      isPublic: true,
      updatedAt: ts(),
    })
  );

  await assertFails(bobDb.collection("leaderboard").get());
  await assertSucceeds(bobDb.collection("leaderboard").where("isPublic", "==", true).get());
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalPoints: 9999,
      updatedAt: ts(),
    })
  );
});
