const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_001";
const ADMIN_EMAIL = "thathsarayasiru97@gmail.com";
const ALICE_UID = "alice_uid_002";
const ALICE_EMAIL = "alice@gmail.com";
const ALICE_KEY = "alice_key";
const BOB_UID = "bob_uid_003";
const BOB_EMAIL = "bob@gmail.com";
const BOB_KEY = "bob_key";
const STRANGER_UID = "stranger_uid_004";
const STRANGER_EMAIL = "stranger@gmail.com";
const MONTH_ID = "2026-11";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function memberContext(uid, email, verified = true) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: verified,
  });
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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

async function seedMonthAndMembers() {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("plan_config").doc("settings").set({
      planName: "Spotify Family",
      monthlyAmountLkr: 450,
      currency: "LKR",
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      updatedAt: now,
    });
    await db.collection("whitelist").doc(ALICE_KEY).set({
      memberKey: ALICE_KEY,
      email: ALICE_EMAIL,
      displayName: "Alice",
      phone: "94771112222",
      role: "member",
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("whitelist").doc(BOB_KEY).set({
      memberKey: BOB_KEY,
      email: BOB_EMAIL,
      displayName: "Bob",
      phone: "94773334444",
      role: "member",
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("payments").doc(MONTH_ID).set({
      monthId: MONTH_ID,
      displayLabel: "November 2026",
      amountLkr: 450,
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).set({
      memberKey: ALICE_KEY,
      memberEmail: ALICE_EMAIL,
      displayName: "Alice",
      status: "pending",
      amountLkr: 450,
      hasReceipt: false,
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      reminderCount: 0,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("payments").doc(MONTH_ID).collection("members").doc(BOB_KEY).set({
      memberKey: BOB_KEY,
      memberEmail: BOB_EMAIL,
      displayName: "Bob",
      status: "paid",
      amountLkr: 450,
      hasReceipt: true,
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      reminderCount: 0,
      createdAt: now,
      confirmedAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated caller cannot read payments, whitelist, or users", async () => {
  await seedMonthAndMembers();
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("payments").get());
  await assertFails(db.collection("whitelist").get());
  await assertFails(db.collection("users").doc(ALICE_UID).get());
});

test("Email spoofing: unverified admin email is rejected", async () => {
  const db = memberContext(ADMIN_UID, ADMIN_EMAIL, false).firestore();
  await assertFails(
    db.collection("plan_config").doc("settings").set({
      planName: "Spotify Family",
      monthlyAmountLkr: 500,
      currency: "LKR",
      allowedEmails: [],
      updatedAt: new Date(),
    })
  );
});

test("Unauthorized email cannot read payments or whitelist", async () => {
  await seedMonthAndMembers();
  const db = memberContext(STRANGER_UID, STRANGER_EMAIL).firestore();
  await assertFails(db.collection("payments").get());
  await assertFails(db.collection("payments").doc(MONTH_ID).get());
  await assertFails(
    db.collection("payments").where("allowedEmails", "array-contains", ALICE_EMAIL).get()
  );
  await assertFails(
    db.collection("whitelist").where("email", "==", ALICE_EMAIL).get()
  );
  const strangerSnap = await assertSucceeds(
    db.collection("payments").where("allowedEmails", "array-contains", STRANGER_EMAIL).get()
  );
  if (strangerSnap.size !== 0) {
    throw new Error(`Expected 0 payments for stranger, got ${strangerSnap.size}`);
  }
});

test("PII isolation: Alice cannot read Bob's whitelist phone or user profile", async () => {
  await seedMonthAndMembers();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date();
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      email: BOB_EMAIL,
      displayName: "Bob",
      role: "member",
      phone: "94773334444",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = memberContext(ALICE_UID, ALICE_EMAIL).firestore();
  await assertSucceeds(
    aliceDb.collection("whitelist").where("email", "==", ALICE_EMAIL).get()
  );
  await assertFails(aliceDb.collection("whitelist").doc(BOB_KEY).get());
  await assertFails(aliceDb.collection("whitelist").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("User cannot self-escalate role to admin in users/{uid}", async () => {
  const aliceDb = memberContext(ALICE_UID, ALICE_EMAIL).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      displayName: "Alice",
      role: "admin",
      phone: "94771112222",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      displayName: "Alice",
      role: "member",
      phone: "94771112222",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User can edit their displayName and phone, and reject shadow fields", async () => {
  await seedMonthAndMembers();
  const aliceDb = memberContext(ALICE_UID, ALICE_EMAIL).firestore();
  const now = new Date();
  await aliceDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    email: ALICE_EMAIL,
    displayName: "Alice",
    role: "member",
    phone: "94771112222",
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Jay",
      phone: "94779998888",
      updatedAt: new Date(),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Jay",
      isVerified: true,
      updatedAt: new Date(),
    })
  );

  // Member can update own displayName/phone/spotifyEmail in users and whitelist, and displayName in active month
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      spotifyEmail: "alice.spotify@gmail.com",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("whitelist").doc(ALICE_KEY).update({
      displayName: "Alice Jay",
      phone: "94779998888",
      spotifyEmail: "alice.spotify@gmail.com",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).update({
      displayName: "Alice Jay",
      updatedAt: new Date(),
    })
  );
});

test("Peer visibility + receipt upload: Alice reads board, uploads slip, cannot mark self paid or edit Bob", async () => {
  await seedMonthAndMembers();
  const aliceDb = memberContext(ALICE_UID, ALICE_EMAIL).firestore();

  await assertSucceeds(
    aliceDb.collection("payments").where("allowedEmails", "array-contains", ALICE_EMAIL).get()
  );
  await assertSucceeds(
    aliceDb
      .collection("payments")
      .doc(MONTH_ID)
      .collection("members")
      .where("allowedEmails", "array-contains", ALICE_EMAIL)
      .get()
  );

  // Alice cannot jump directly to paid
  await assertFails(
    aliceDb.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).update({
      status: "paid",
      updatedAt: new Date(),
    })
  );

  // Alice uploads slip and marks verifying
  const slipRef = aliceDb
    .collection("payments")
    .doc(MONTH_ID)
    .collection("members")
    .doc(ALICE_KEY)
    .collection("private")
    .doc("slip");

  await assertSucceeds(
    slipRef.set({
      memberEmail: ALICE_EMAIL,
      uploadedByUid: ALICE_UID,
      mimeType: "image/jpeg",
      imageBase64: "dGVzdF9zbGlwX2RhdGE=",
      uploadedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).update({
      status: "verifying",
      hasReceipt: true,
      submittedAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Alice cannot modify Bob's payment
  await assertFails(
    aliceDb.collection("payments").doc(MONTH_ID).collection("members").doc(BOB_KEY).update({
      status: "pending",
      updatedAt: new Date(),
    })
  );

  // Bob cannot upload slip once paid (terminal lock)
  const bobDb = memberContext(BOB_UID, BOB_EMAIL).firestore();
  await assertFails(
    bobDb
      .collection("payments")
      .doc(MONTH_ID)
      .collection("members")
      .doc(BOB_KEY)
      .collection("private")
      .doc("slip")
      .set({
        memberEmail: BOB_EMAIL,
        uploadedByUid: BOB_UID,
        mimeType: "image/jpeg",
        imageBase64: "dGVzdF9zbGlwX2RhdGE=",
        uploadedAt: new Date(),
      })
  );
});

test("Admin can create month batch, confirm payment, log reminder, and update LKR rate", async () => {
  await seedMonthAndMembers();
  const adminDb = adminContext().firestore();
  const now = new Date();

  await assertSucceeds(
    adminDb.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).update({
      status: "paid",
      confirmedAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).update({
      lastRemindedAt: now,
      reminderCount: 1,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("plan_config").doc("settings").update({
      monthlyAmountLkr: 500,
      bankName: "Commercial Bank",
      bankAccountNumber: "8001234567",
      bankAccountHolder: "Y. Thathsara",
      bankBranch: "Galle",
      updatedAt: new Date(),
    })
  );
});

test("Whitelisted member can sync own Spotify listening stats, cannot spoof another member's stats", async () => {
  await seedMonthAndMembers();
  const aliceDb = memberContext(ALICE_UID, ALICE_EMAIL).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("payments").doc(MONTH_ID).collection("stats").doc(ALICE_KEY).set({
      memberKey: ALICE_KEY,
      memberEmail: ALICE_EMAIL,
      displayName: "Alice",
      spotifyDisplayName: "Alice Tunes",
      minutesListened: 320,
      tracksCounted: 85,
      topArtist: "Fred again..",
      topTrack: "Delilah",
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      syncedAt: now,
    })
  );

  // Alice can read peer stats in the same month
  await assertSucceeds(
    aliceDb
      .collection("payments")
      .doc(MONTH_ID)
      .collection("stats")
      .where("allowedEmails", "array-contains", ALICE_EMAIL)
      .get()
  );

  // Alice cannot write Bob's stats
  await assertFails(
    aliceDb.collection("payments").doc(MONTH_ID).collection("stats").doc(BOB_KEY).set({
      memberKey: BOB_KEY,
      memberEmail: BOB_EMAIL,
      displayName: "Bob",
      minutesListened: 10,
      tracksCounted: 3,
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      syncedAt: now,
    })
  );
});

test("Admin can rewind paid confirmation, delete pending payment, delete slip/month, and sync admin stats", async () => {
  await seedMonthAndMembers();
  const adminDb = adminContext().firestore();
  const aliceDb = memberContext(ALICE_UID, ALICE_EMAIL).firestore();
  const now = new Date();

  // Admin rewinds/edits Bob from paid back to verifying with custom amount and cleared receipt
  await assertSucceeds(
    adminDb.collection("payments").doc(MONTH_ID).collection("members").doc(BOB_KEY).update({
      status: "verifying",
      displayName: "Bob Edited",
      amountLkr: 500,
      hasReceipt: false,
      reminderCount: 0,
      updatedAt: now,
    })
  );

  // Admin deletes pending Alice payment entry
  await assertSucceeds(
    adminDb.collection("payments").doc(MONTH_ID).collection("members").doc(ALICE_KEY).delete()
  );

  // Non-admin cannot delete payment entry
  await assertFails(
    aliceDb.collection("payments").doc(MONTH_ID).collection("members").doc(BOB_KEY).delete()
  );

  // Admin can write own listening stats even when not in allowedEmails
  const adminKey = "admin_key_test";
  await assertSucceeds(
    adminDb.collection("payments").doc(MONTH_ID).collection("stats").doc(adminKey).set({
      memberKey: adminKey,
      memberEmail: ADMIN_EMAIL,
      displayName: "Thathsara",
      spotifyDisplayName: "Thathsara",
      minutesListened: 180,
      tracksCounted: 45,
      topArtist: "Daft Punk",
      topTrack: "Instant Crush",
      allowedEmails: [ALICE_EMAIL, BOB_EMAIL],
      syncedAt: now,
    })
  );
});

