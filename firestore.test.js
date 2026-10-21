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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: can create own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      wins: 0,
      losses: 0,
      gamesPlayed: 0,
      trophies: 100,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create user profile for someone else", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "Bob",
      wins: 0,
      losses: 0,
      gamesPlayed: 0,
      trophies: 100,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("MatchRoom: host can create room and joiners can update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomData = {
    roomId: "room_123",
    hostId: ALICE_UID,
    hostName: "Alice",
    status: "waiting",
    playerCount: 2,
    playerUids: [ALICE_UID],
    playerNames: ["Alice"],
    playerColors: ["RED"],
    playerTokens: [0, 0, 0, 0],
    turnIndex: 0,
    turnUid: ALICE_UID,
    diceValue: 1,
    diceRolled: false,
    lastAction: "Room created",
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  await assertSucceeds(aliceDb.collection("match_rooms").doc("room_123").set(roomData));

  // Bob joins room
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_123").update({
      playerUids: [ALICE_UID, BOB_UID],
      playerNames: ["Alice", "Bob"],
      playerColors: ["RED", "BLUE"],
      playerTokens: [0, 0, 0, 0, 0, 0, 0, 0],
      updatedAt: new Date(),
    })
  );
});

test("ChatMessage: player can send in-game chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_123").collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      roomId: "room_123",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Good luck, have fun!",
      createdAt: new Date(),
    })
  );
});

test("Leaderboards: player can create and update own leaderboard entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("leaderboards").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      wins: 5,
      gamesPlayed: 10,
      trophies: 250,
      updatedAt: new Date(),
    })
  );

  // Bob can read leaderboard
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("leaderboards").doc(ALICE_UID).get());
});
