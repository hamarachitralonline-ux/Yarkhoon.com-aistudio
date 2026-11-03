const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-yarkhoon-storage-test";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

before(async () => {
  const rules = fs.readFileSync("./storage.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    storage: {
      rules,
      host: "127.0.0.1",
      port: 9199,
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
    await testEnv.clearStorage();
  }
});

test("Unauthenticated user cannot read or write any storage paths", async () => {
  const unauthStorage = testEnv.unauthenticatedContext().storage();
  const publicRef = unauthStorage.ref(`users/${ALICE_UID}/public/avatar.jpg`);
  const protectedRef = unauthStorage.ref(`users/${ALICE_UID}/protected/post1.jpg`);
  const legacyRef = unauthStorage.ref(`users/${ALICE_UID}/posts/legacy1.jpg`);

  await assertFails(publicRef.getDownloadURL());
  await assertFails(protectedRef.getDownloadURL());
  await assertFails(legacyRef.getDownloadURL());
});

test("Protected media path (/protected/): Owner has direct access, Non-owner is denied", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID).storage();
  const bobStorage = testEnv.authenticatedContext(BOB_UID).storage();

  const aliceProtectedRef = aliceStorage.ref(`users/${ALICE_UID}/protected/post1.jpg`);
  const bobAccessToAliceRef = bobStorage.ref(`users/${ALICE_UID}/protected/post1.jpg`);

  // Alice uploads a dummy file to her protected path
  const dummyBuffer = Buffer.from("image content");
  await assertSucceeds(aliceProtectedRef.put(dummyBuffer, { contentType: "image/jpeg" }));

  // Alice (owner) can read her own protected file
  await assertSucceeds(aliceProtectedRef.getDownloadURL());

  // Bob (third party / non-owner) CANNOT read Alice's protected file directly via Storage SDK
  await assertFails(bobAccessToAliceRef.getDownloadURL());

  // Bob CANNOT write or delete Alice's protected file
  await assertFails(bobAccessToAliceRef.put(dummyBuffer, { contentType: "image/jpeg" }));
  await assertFails(bobAccessToAliceRef.delete());
});

test("Legacy media paths (/posts, /stories, /reels): Owner has access, Third-party is denied direct access", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID).storage();
  const bobStorage = testEnv.authenticatedContext(BOB_UID).storage();
  const dummyBuffer = Buffer.from("legacy image content");

  // Test legacy 'posts'
  const aliceLegacyPostRef = aliceStorage.ref(`users/${ALICE_UID}/posts/photo_legacy.jpg`);
  const bobAccessLegacyPostRef = bobStorage.ref(`users/${ALICE_UID}/posts/photo_legacy.jpg`);

  await assertSucceeds(aliceLegacyPostRef.put(dummyBuffer, { contentType: "image/jpeg" }));
  await assertSucceeds(aliceLegacyPostRef.getDownloadURL());
  // CRITICAL: Bob cannot directly read Alice's legacy post
  await assertFails(bobAccessLegacyPostRef.getDownloadURL());
  await assertFails(bobAccessLegacyPostRef.put(dummyBuffer, { contentType: "image/jpeg" }));

  // Test legacy 'stories'
  const aliceLegacyStoryRef = aliceStorage.ref(`users/${ALICE_UID}/stories/story_legacy.jpg`);
  const bobAccessLegacyStoryRef = bobStorage.ref(`users/${ALICE_UID}/stories/story_legacy.jpg`);

  await assertSucceeds(aliceLegacyStoryRef.put(dummyBuffer, { contentType: "image/jpeg" }));
  await assertSucceeds(aliceLegacyStoryRef.getDownloadURL());
  // CRITICAL: Bob cannot directly read Alice's legacy story
  await assertFails(bobAccessLegacyStoryRef.getDownloadURL());

  // Test legacy 'reels'
  const aliceLegacyReelRef = aliceStorage.ref(`users/${ALICE_UID}/reels/reel_legacy.mp4`);
  const bobAccessLegacyReelRef = bobStorage.ref(`users/${ALICE_UID}/reels/reel_legacy.mp4`);

  await assertSucceeds(aliceLegacyReelRef.put(dummyBuffer, { contentType: "video/mp4" }));
  await assertSucceeds(aliceLegacyReelRef.getDownloadURL());
  // CRITICAL: Bob cannot directly read Alice's legacy reel
  await assertFails(bobAccessLegacyReelRef.getDownloadURL());

  // Test unlisted legacy folder (e.g. 'drafts' or 'other')
  const aliceOtherRef = aliceStorage.ref(`users/${ALICE_UID}/other/draft.jpg`);
  await assertFails(aliceOtherRef.put(dummyBuffer, { contentType: "image/jpeg" }));
});

test("Public community media (/public/): Any signed-in user can read, only owner can write", async () => {
  const aliceStorage = testEnv.authenticatedContext(ALICE_UID).storage();
  const bobStorage = testEnv.authenticatedContext(BOB_UID).storage();
  const dummyBuffer = Buffer.from("avatar content");

  const alicePublicAvatarRef = aliceStorage.ref(`users/${ALICE_UID}/public/avatar.jpg`);
  const bobAccessAvatarRef = bobStorage.ref(`users/${ALICE_UID}/public/avatar.jpg`);

  await assertSucceeds(alicePublicAvatarRef.put(dummyBuffer, { contentType: "image/jpeg" }));
  await assertSucceeds(alicePublicAvatarRef.getDownloadURL());

  // Bob can read Alice's public asset
  await assertSucceeds(bobAccessAvatarRef.getDownloadURL());

  // Bob cannot overwrite Alice's public asset
  await assertFails(bobAccessAvatarRef.put(dummyBuffer, { contentType: "image/jpeg" }));
});
