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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
});

test("Authenticated user: cannot read another user's task unless shared", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task_1").set({
      id: "alice_task_1",
      title: "Alice's Task",
      userId: ALICE_UID,
      category: "Work",
      priority: "HIGH",
      completed: false,
      sharedWithUids: [],
      sharedWithEmails: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("tasks").doc("alice_task_1").get());
});

test("Collaborator: can read task when in sharedWithUids", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("shared_task_1").set({
      id: "shared_task_1",
      title: "Shared Task",
      userId: ALICE_UID,
      category: "Project",
      priority: "MEDIUM",
      completed: false,
      sharedWithUids: [BOB_UID],
      sharedWithEmails: ["bob@example.com"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("tasks").doc("shared_task_1").get());
});

test("Authenticated user: can query their own tasks (matching filter)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task_2").set({
      id: "alice_task_2",
      title: "Alice's Task 2",
      userId: ALICE_UID,
      category: "Personal",
      priority: "LOW",
      completed: false,
      sharedWithUids: [],
      sharedWithEmails: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query without userId filter (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").get());
});

test("Owner: cannot be deleted by non-owner", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task_3").set({
      id: "alice_task_3",
      title: "Alice's Task 3",
      userId: ALICE_UID,
      category: "Health",
      priority: "URGENT",
      completed: false,
      sharedWithUids: [BOB_UID],
      sharedWithEmails: ["bob@example.com"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("tasks").doc("alice_task_3").delete());
});
