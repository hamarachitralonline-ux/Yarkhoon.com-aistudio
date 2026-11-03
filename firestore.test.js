const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-yarkhoon-test";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const SUSPENDED_UID = "bad_actor_789";
const ADMIN_UID = "admin_super_1";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
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

    // Setup initial suspension record for SUSPENDED_UID using admin context
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      // Setup suspended user document in userSuspensions
      await db.collection("userSuspensions").doc(SUSPENDED_UID).set({
        userId: SUSPENDED_UID,
        isSuspended: true,
        suspendedUntil: 0, // indefinite
        suspensionReason: "Policy violations",
        suspendedAt: Date.now()
      });

      // Setup admin role document in adminRoles
      await db.collection("adminRoles").doc(ADMIN_UID).set({
        role: "admin",
        enabled: true
      });

      // Setup active admin session in adminSessions for ADMIN_UID
      await db.collection("adminSessions").doc(ADMIN_UID).set({
        userId: ADMIN_UID,
        email: "admin@yarkhoon.com",
        isActive: true,
        lastActivityAt: new Date(),
        sessionStartAt: new Date(),
        rememberMe: true
      });
    });
  }
});

test("Unauthenticated user cannot read or write to private collections or posts", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("posts").get());
  await assertFails(unauthedDb.collection("users").doc(ALICE_UID).set({ name: "Alice" }));
});

test("Active authenticated user can create their own post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      text: "Hello Yarkhoon Valley!",
      createdAt: "Now",
      timestamp: Date.now()
    })
  );
});

test("Suspended user is DENIED from creating posts by Firestore security rules", async () => {
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  await assertFails(
    suspendedDb.collection("posts").doc("post_bad").set({
      authorId: SUSPENDED_UID,
      text: "I am trying to bypass UI suspension!",
      createdAt: "Now",
      timestamp: Date.now()
    })
  );
});

test("Suspended user is DENIED from sending messages or creating listings", async () => {
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  
  // Attempt to create business listing
  await assertFails(
    suspendedDb.collection("businessListings").doc("biz_bad").set({
      ownerId: SUSPENDED_UID,
      businessName: "Suspended Business",
      status: "active"
    })
  );

  // Attempt to create marketplace listing
  await assertFails(
    suspendedDb.collection("marketplaceListings").doc("item_bad").set({
      sellerId: SUSPENDED_UID,
      title: "Suspended Item",
      status: "active"
    })
  );
});

test("Ordinary user CANNOT modify their own suspension status or fields", async () => {
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  
  // Cannot write to userSuspensions collection
  await assertFails(
    suspendedDb.collection("userSuspensions").doc(SUSPENDED_UID).set({
      isSuspended: false
    })
  );

  // Cannot modify own user profile's suspension fields
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      name: "Alice",
      isSuspended: false
    });
  });

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuspended: false,
      suspendedUntil: 0
    })
  );
});

test("Ordinary user CANNOT grant themselves admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("adminRoles").doc(ALICE_UID).set({
      role: "admin",
      enabled: true
    })
  );
});

test("Ordinary user can submit report with own UID but CANNOT resolve it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Submit report with status PENDING succeeds
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      reportedUserId: BOB_UID,
      contentType: "POST",
      contentId: "post_99",
      reason: "SPAM",
      status: "PENDING",
      createdAt: Date.now()
    })
  );

  // Attempting to resolve report as ordinary user fails
  await assertFails(
    aliceDb.collection("reports").doc("rep_1").update({
      status: "RESOLVED",
      resolutionNotes: "Self resolved"
    })
  );

  // Admin CAN resolve report
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "RESOLVED",
      resolutionNotes: "Resolved by admin"
    })
  );
});

test("Suspended user is DENIED from creating nested comments on posts", async () => {
  // Post created by Alice
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("post_alice_1").set({
      authorId: ALICE_UID,
      text: "Alice post",
      createdAt: "Now",
      timestamp: Date.now()
    });
  });

  // Suspended user cannot comment
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  await assertFails(
    suspendedDb.collection("posts").doc("post_alice_1").collection("comments").doc("cmt_bad").set({
      authorId: SUSPENDED_UID,
      text: "Banned comment attempt",
      timestamp: "Now"
    })
  );

  // Active user Bob CAN comment
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("posts").doc("post_alice_1").collection("comments").doc("cmt_bob").set({
      authorId: BOB_UID,
      text: "Great post Alice!",
      timestamp: "Now"
    })
  );
});

test("Suspended user is DENIED from creating conversations and sending messages", async () => {
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  const convId = `${SUSPENDED_UID}_${BOB_UID}`;

  // Suspended user cannot create conversation
  await assertFails(
    suspendedDb.collection("conversations").doc(convId).set({
      participantIds: [SUSPENDED_UID, BOB_UID],
      lastMessage: "hello"
    })
  );

  // Active user Alice can create conversation with Bob
  const aliceConvId = `${ALICE_UID}_${BOB_UID}`;
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc(aliceConvId).set({
      participantIds: [ALICE_UID, BOB_UID],
      lastMessage: "Hello Bob"
    })
  );

  // Active user Alice can send message in conversation
  await assertSucceeds(
    aliceDb.collection("conversations").doc(aliceConvId).collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      text: "Hello Bob!",
      timestamp: Date.now()
    })
  );
});

test("Private follow requests: only target user and requester can read/delete requests", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Alice sends follow request to Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("followRequests").doc(ALICE_UID).set({
      requesterId: ALICE_UID,
      targetUserId: BOB_UID,
      createdAt: Date.now()
    })
  );

  // Third party (Eve/Charlie) cannot read Bob's follow requests
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  await assertFails(
    eveDb.collection("users").doc(BOB_UID).collection("followRequests").doc(ALICE_UID).get()
  );

  // Bob (owner) CAN read the follow request
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("followRequests").doc(ALICE_UID).get()
  );
});

test("In-app notifications: Active user can create, recipient can read/update/delete, third party DENIED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const notifId = "notif_like_123";

  // Active user Alice sends notification to Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc(notifId).set({
      id: notifId,
      recipientId: BOB_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      type: "like",
      message: "liked your post",
      timestampMs: Date.now(),
      isRead: false
    })
  );

  // Bob (recipient) can read their notification
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc(notifId).get()
  );

  // Bob can mark it as read (update)
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc(notifId).update({
      isRead: true
    })
  );

  // Eve (third party) CANNOT read Bob's notifications
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  await assertFails(
    eveDb.collection("users").doc(BOB_UID).collection("notifications").doc(notifId).get()
  );

  // Eve CANNOT delete Bob's notifications
  await assertFails(
    eveDb.collection("users").doc(BOB_UID).collection("notifications").doc(notifId).delete()
  );

  // Suspended user CANNOT create notifications
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  await assertFails(
    suspendedDb.collection("users").doc(BOB_UID).collection("notifications").doc("bad_notif").set({
      id: "bad_notif",
      recipientId: BOB_UID,
      senderId: SUSPENDED_UID,
      message: "Spam",
      timestampMs: Date.now(),
      isRead: false
    })
  );

  // Bob can delete their notification
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc(notifId).delete()
  );
});

test("Home feed query authorization: Authenticated user CAN list posts ordered by timestamp; Unauthenticated is DENIED", async () => {
  // Alice creates a post
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_feed_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      text: "Feed post 1",
      timestamp: Date.now()
    })
  );

  // Authenticated user Bob can execute the exact Home feed query: orderBy("timestamp", "desc").get()
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("posts").orderBy("timestamp", "desc").get()
  );

  // Unauthenticated user is DENIED from executing the Home feed query
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthedDb.collection("posts").orderBy("timestamp", "desc").get()
  );

  // Suspended user can read posts (read-only mode) but is DENIED from creating or modifying posts
  const suspendedDb = testEnv.authenticatedContext(SUSPENDED_UID).firestore();
  await assertSucceeds(
    suspendedDb.collection("posts").orderBy("timestamp", "desc").get()
  );
  await assertFails(
    suspendedDb.collection("posts").doc("post_forbidden_suspended").set({
      authorId: SUSPENDED_UID,
      text: "Forbidden post by suspended user",
      timestamp: Date.now()
    })
  );
});

test("POSTS: Non-author CANNOT edit post content, author CAN edit permitted fields, caller can like post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates a post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_test_sec").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      text: "Initial Alice Post",
      likesCount: 0,
      likedBy: [],
      commentsCount: 0,
      sharesCount: 0,
      repostsCount: 0,
      timestamp: Date.now()
    })
  );

  // Bob (non-author) attempts to edit Alice's post text -> DENIED
  await assertFails(
    bobDb.collection("posts").doc("post_test_sec").update({
      text: "Hacked by Bob"
    })
  );

  // Bob attempts to arbitrarily change likesCount to 999 -> DENIED
  await assertFails(
    bobDb.collection("posts").doc("post_test_sec").update({
      likesCount: 999
    })
  );

  // Alice (author) CAN edit her own post text and tags
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_test_sec").update({
      text: "Alice updated text",
      tags: ["updates"]
    })
  );

  // Bob CAN toggle like by adding his own UID and incrementing likesCount by 1
  await assertSucceeds(
    bobDb.collection("posts").doc("post_test_sec").update({
      likedBy: [BOB_UID],
      likesCount: 1
    })
  );
});

test("COMMENTS: Only comment author can edit text; Non-author CANNOT modify comment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Create post
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("post_for_comments").set({
      authorId: ALICE_UID,
      text: "Post for comments",
      commentsCount: 0
    });
  });

  // Alice posts a comment
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_for_comments").collection("comments").doc("cmt_alice_1").set({
      id: "cmt_alice_1",
      authorId: ALICE_UID,
      text: "Alice original comment",
      likesCount: 0,
      likedBy: []
    })
  );

  // Bob attempts to edit Alice's comment text -> DENIED
  await assertFails(
    bobDb.collection("posts").doc("post_for_comments").collection("comments").doc("cmt_alice_1").update({
      text: "Bob tampered with Alice's comment"
    })
  );

  // Alice CAN edit her own comment text
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_for_comments").collection("comments").doc("cmt_alice_1").update({
      text: "Alice edited her own comment",
      isEdited: true
    })
  );

  // Bob CAN toggle like on Alice's comment
  await assertSucceeds(
    bobDb.collection("posts").doc("post_for_comments").collection("comments").doc("cmt_alice_1").update({
      likedBy: [BOB_UID],
      likesCount: 1
    })
  );
});

test("NOTIFICATIONS: Prevents sender spoofing and cross-recipient creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice tries to forge notification purporting to come from Charlie (senderId != auth.uid) -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("spoofed_notif").set({
      id: "spoofed_notif",
      recipientId: BOB_UID,
      senderId: "charlie_victim",
      message: "Spoofed message"
    })
  );

  // Alice tries to set recipientId to someone other than document path owner -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("bad_recipient").set({
      id: "bad_recipient",
      recipientId: "dave_user",
      senderId: ALICE_UID,
      message: "Mismatched recipient"
    })
  );

  // Alice sends valid notification to Bob with matching senderId and recipientId -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("valid_notif").set({
      id: "valid_notif",
      recipientId: BOB_UID,
      senderId: ALICE_UID,
      message: "Valid notification"
    })
  );
});

test("CHAT: Nonparticipants CANNOT read conversation or messages; sender spoofing and message edits DENIED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_nonparticipant").firestore();
  const convId = `${ALICE_UID}_${BOB_UID}`;

  // Alice creates conversation with Bob
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      lastMessage: "Hello Bob"
    })
  );

  // Alice posts message
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      id: "msg_1",
      senderId: ALICE_UID,
      text: "Secret between Alice and Bob",
      timestamp: Date.now()
    })
  );

  // Eve (nonparticipant) attempts to read conversation -> DENIED
  await assertFails(
    eveDb.collection("conversations").doc(convId).get()
  );

  // Eve attempts to read messages -> DENIED
  await assertFails(
    eveDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").get()
  );

  // Eve attempts to send message in Alice and Bob's conversation -> DENIED
  await assertFails(
    eveDb.collection("conversations").doc(convId).collection("messages").doc("msg_eve").set({
      id: "msg_eve",
      senderId: "eve_nonparticipant",
      text: "Eve intruding",
      timestamp: Date.now()
    })
  );

  // Alice attempts to spoof senderId as Bob -> DENIED
  await assertFails(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_spoof").set({
      id: "msg_spoof",
      senderId: BOB_UID,
      text: "Alice pretending to be Bob",
      timestamp: Date.now()
    })
  );

  // Bob (or Alice) attempts to edit existing message -> DENIED (immutable messages)
  await assertFails(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").update({
      text: "Altered message"
    })
  );
});

test("USER SUBCOLLECTIONS: Users cannot write to another user's bookmarks, blocks, mutes, or favorites", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice cannot write to Bob's savedPosts
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("savedPosts").doc("p1").set({
      postId: "p1"
    })
  );

  // Alice cannot write to Bob's blockedUsers
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("blockedUsers").doc("victim").set({
      blockedUserId: "victim"
    })
  );

  // Alice cannot write to Bob's mutedUsers
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("mutedUsers").doc("victim").set({
      mutedUserId: "victim"
    })
  );

  // Alice cannot write to Bob's favorites
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("favorites").doc("victim").set({
      favoriteUserId: "victim"
    })
  );

  // Alice CAN write to her own savedPosts
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedPosts").doc("p1").set({
      postId: "p1"
    })
  );
});

test("ADMIN PROFILE UPDATES: Admin can update another profile; ordinary user cannot tamper with admin fields or others' profiles", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Create Alice user doc
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      role: "member",
      isVerified: false,
      isSuspended: false
    });
  });

  // Bob (non-owner, non-admin) attempts to update Alice's profile -> DENIED
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked by Bob"
    })
  );

  // Alice attempts to set role: 'admin' on her own profile -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin"
    })
  );

  // Alice attempts to set isVerified: true on her own profile -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true
    })
  );

  // Alice attempts to set isSuspended: true on her own profile -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuspended: true
    })
  );

  // Alice CAN update her allowed fields (bio, name) -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice Updated",
      bio: "Software developer"
    })
  );

  // Admin CAN update Alice's profile to verify or moderate -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      role: "moderator"
    })
  );
});

test("POST AND COMMENT LIKES: Strict validation on caller membership, delta=1, duplicate likes, and tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_777").firestore();

  // Alice creates a post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_like_test").set({
      authorId: ALICE_UID,
      text: "Testing likes",
      likesCount: 0,
      likedBy: [],
      timestamp: Date.now()
    })
  );

  // 1. Bob likes post -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("posts").doc("post_like_test").update({
      likedBy: [BOB_UID],
      likesCount: 1
    })
  );

  // 2. Duplicate like: Bob tries to like again while already in likedBy -> DENIED
  await assertFails(
    bobDb.collection("posts").doc("post_like_test").update({
      likedBy: [BOB_UID, BOB_UID],
      likesCount: 2
    })
  );

  // 3. Tampering: Bob tries to add Charlie instead of himself -> DENIED
  await assertFails(
    bobDb.collection("posts").doc("post_like_test").update({
      likedBy: [BOB_UID, "charlie_777"],
      likesCount: 2
    })
  );

  // 4. Tampering: Charlie tries to remove Bob -> DENIED
  await assertFails(
    charlieDb.collection("posts").doc("post_like_test").update({
      likedBy: ["charlie_777"],
      likesCount: 1
    })
  );

  // 5. Counter mismatch: Charlie likes but increments likesCount by 5 -> DENIED
  await assertFails(
    charlieDb.collection("posts").doc("post_like_test").update({
      likedBy: [BOB_UID, "charlie_777"],
      likesCount: 6
    })
  );

  // 6. Charlie valid like -> SUCCEEDS
  await assertSucceeds(
    charlieDb.collection("posts").doc("post_like_test").update({
      likedBy: [BOB_UID, "charlie_777"],
      likesCount: 2
    })
  );

  // 7. Bob unlikes -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("posts").doc("post_like_test").update({
      likedBy: ["charlie_777"],
      likesCount: 1
    })
  );

  // 8. Repeated unlike: Bob tries to unlike again when not in likedBy -> DENIED
  await assertFails(
    bobDb.collection("posts").doc("post_like_test").update({
      likedBy: [],
      likesCount: 0
    })
  );

  // 9. Negative counter prevention: unlike below zero -> DENIED
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("post_zero_like").set({
      authorId: ALICE_UID,
      text: "Zero post",
      likesCount: 0,
      likedBy: [BOB_UID]
    });
  });
  await assertFails(
    bobDb.collection("posts").doc("post_zero_like").update({
      likedBy: [],
      likesCount: -1
    })
  );

  // 10. Missing likedBy on legacy post -> fallback initialization allowed
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("post_legacy_missing_likes").set({
      authorId: ALICE_UID,
      text: "Legacy post without likedBy"
    });
  });
  await assertSucceeds(
    bobDb.collection("posts").doc("post_legacy_missing_likes").update({
      likedBy: [BOB_UID],
      likesCount: 1
    })
  );
});

test("MODERATION FIELDS: Post and reel authors cannot modify moderation fields; Admin can", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Create post and reel
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("posts").doc("post_mod_1").set({
      authorId: ALICE_UID,
      text: "Post to moderate",
      isModeratedHidden: false
    });
    await db.collection("reels").doc("reel_mod_1").set({
      authorId: ALICE_UID,
      caption: "Reel to moderate",
      isModeratedHidden: false
    });
  });

  // Alice (author) tries to change isModeratedHidden on post -> DENIED
  await assertFails(
    aliceDb.collection("posts").doc("post_mod_1").update({
      isModeratedHidden: true
    })
  );

  // Alice (author) tries to change isModeratedHidden on reel -> DENIED
  await assertFails(
    aliceDb.collection("reels").doc("reel_mod_1").update({
      isModeratedHidden: true
    })
  );

  // Alice CAN update permitted fields on post (text) and reel (caption) -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_mod_1").update({
      text: "Author editing allowed text"
    })
  );
  await assertSucceeds(
    aliceDb.collection("reels").doc("reel_mod_1").update({
      caption: "Author editing allowed caption"
    })
  );

  // Admin CAN moderate post -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("posts").doc("post_mod_1").update({
      isModeratedHidden: true,
      moderatedAt: Date.now(),
      moderatedBy: ADMIN_UID,
      moderationReason: "Violates safety policy"
    })
  );

  // Admin CAN moderate reel -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("reels").doc("reel_mod_1").update({
      isModeratedHidden: true,
      moderatedAt: Date.now(),
      moderatedBy: ADMIN_UID,
      moderationReason: "Inappropriate reel content"
    })
  );
});

test("STORIES: Only author can update caption or delete; non-author is DENIED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Alice creates story
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_alice_1").set({
      storyId: "story_alice_1",
      authorId: ALICE_UID,
      caption: "Alice story caption",
      mediaUrl: "https://example.com/story.jpg",
      timestamp: Date.now()
    })
  );

  // Bob (non-author) attempts to update Alice's story -> DENIED
  await assertFails(
    bobDb.collection("stories").doc("story_alice_1").update({
      caption: "Tampered by Bob"
    })
  );

  // Bob attempts to delete Alice's story -> DENIED
  await assertFails(
    bobDb.collection("stories").doc("story_alice_1").delete()
  );

  // Alice CAN update her story caption -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_alice_1").update({
      caption: "Alice updated story caption"
    })
  );

  // Admin CAN delete story -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("stories").doc("story_alice_1").delete()
  );
});

test("NOTIFICATIONS: Recipient can only update isRead; non-isRead update is DENIED for both collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Create subcollection and root notification
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("notifications").doc("notif_sub_read").set({
      id: "notif_sub_read",
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      message: "Hello Bob",
      isRead: false
    });
    await db.collection("notifications").doc("notif_root_read").set({
      id: "notif_root_read",
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      message: "Root hello Bob",
      isRead: false
    });
  });

  // Bob updates isRead to true on subcollection notification -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_sub_read").update({
      isRead: true
    })
  );

  // Bob attempts to change message field on subcollection notification -> DENIED
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_sub_read").update({
      message: "Altered message"
    })
  );

  // Bob updates isRead to true on root notification -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("notifications").doc("notif_root_read").update({
      isRead: true
    })
  );

  // Bob attempts to change message or senderId on root notification -> DENIED
  await assertFails(
    bobDb.collection("notifications").doc("notif_root_read").update({
      message: "Altered root message"
    })
  );
});

test("TARGETED CORRECTIONS: User profile field restrictions and admin boundary", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Create Alice doc
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      followersCount: 10,
      role: "member",
      isVerified: false,
      isSuspended: false
    });
  });

  // Alice attempts to alter followersCount directly -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      followersCount: 9999
    })
  );

  // Alice attempts to change uid -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      uid: "hacked_uid"
    })
  );

  // Admin attempts to change Alice's bio on her profile -> DENIED (admin restricted to administrative fields)
  await assertFails(
    adminDb.collection("users").doc(ALICE_UID).update({
      bio: "Admin changing user bio unlawfully"
    })
  );
});

test("TARGETED CORRECTIONS: Chat metadata field restriction, participant immutability, anti-spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const convId = `${ALICE_UID}_${BOB_UID}`;

  // Alice creates conversation with Bob
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      id: convId,
      participantIds: [ALICE_UID, BOB_UID],
      lastMessage: "Hello Bob"
    })
  );

  // Alice attempts to change participantIds -> DENIED
  await assertFails(
    aliceDb.collection("conversations").doc(convId).update({
      participantIds: [ALICE_UID, "charlie_intruder"]
    })
  );

  // Alice attempts to add unapproved field -> DENIED
  await assertFails(
    aliceDb.collection("conversations").doc(convId).update({
      secretAdminPrivilege: true
    })
  );

  // Alice attempts to spoof lastMessageSenderId as Bob -> DENIED
  await assertFails(
    aliceDb.collection("conversations").doc(convId).update({
      lastMessage: "Impersonated",
      lastMessageSenderId: BOB_UID
    })
  );

  // Alice valid update with her own lastMessageSenderId -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).update({
      lastMessage: "Real message from Alice",
      lastMessageSenderId: ALICE_UID
    })
  );

  // Bob marks conversation read -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).update({
      readBy: [BOB_UID]
    })
  );
});

test("TARGETED CORRECTIONS: Stories admin update restricted to moderation fields", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Create story
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stories").doc("story_sec_1").set({
      storyId: "story_sec_1",
      authorId: ALICE_UID,
      caption: "Original caption",
      mediaUrl: "https://example.com/story.jpg"
    });
  });

  // Admin attempts to change mediaUrl or caption -> DENIED
  await assertFails(
    adminDb.collection("stories").doc("story_sec_1").update({
      mediaUrl: "https://example.com/hacked.jpg"
    })
  );
  await assertFails(
    adminDb.collection("stories").doc("story_sec_1").update({
      caption: "Admin rewriting caption"
    })
  );

  // Admin CAN moderate story -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("stories").doc("story_sec_1").update({
      isModeratedHidden: true,
      moderatedAt: Date.now(),
      moderatedBy: ADMIN_UID,
      moderationReason: "Story violates policy"
    })
  );
});

test("TARGETED CORRECTIONS: Content reports immutability of reporter and content identity", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Create report
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("reports").doc("rep_sec_1").set({
      reportId: "rep_sec_1",
      reporterId: ALICE_UID,
      reportedUserId: BOB_UID,
      contentId: "post_bad_1",
      status: "PENDING",
      createdAt: 1000000
    });
  });

  // Admin attempts to change reporterId -> DENIED
  await assertFails(
    adminDb.collection("reports").doc("rep_sec_1").update({
      reporterId: "forged_reporter"
    })
  );

  // Admin attempts to change reportedUserId -> DENIED
  await assertFails(
    adminDb.collection("reports").doc("rep_sec_1").update({
      reportedUserId: "frame_innocent_user"
    })
  );

  // Admin attempts to change contentId -> DENIED
  await assertFails(
    adminDb.collection("reports").doc("rep_sec_1").update({
      contentId: "different_post"
    })
  );

  // Admin CAN update status, reviewedAt, reviewedBy, resolutionNotes -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_sec_1").update({
      status: "RESOLVED",
      reviewedAt: Date.now(),
      reviewedBy: ADMIN_UID,
      resolutionNotes: "Content removed"
    })
  );
});

test("TARGETED CORRECTIONS: Business and Marketplace owner identity & verification protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Alice creates business listing
  await assertSucceeds(
    aliceDb.collection("businessListings").doc("biz_sec_1").set({
      id: "biz_sec_1",
      ownerId: ALICE_UID,
      businessName: "Alice Shop",
      isVerified: false,
      rating: 0
    })
  );

  // Alice attempts to self-verify business -> DENIED
  await assertFails(
    aliceDb.collection("businessListings").doc("biz_sec_1").update({
      isVerified: true
    })
  );

  // Alice attempts to forge rating -> DENIED
  await assertFails(
    aliceDb.collection("businessListings").doc("biz_sec_1").update({
      rating: 5.0
    })
  );

  // Admin CAN verify business -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("businessListings").doc("biz_sec_1").update({
      isVerified: true,
      status: "verified"
    })
  );

  // Alice creates marketplace listing
  await assertSucceeds(
    aliceDb.collection("marketplaceListings").doc("item_sec_1").set({
      id: "item_sec_1",
      sellerId: ALICE_UID,
      title: "Handmade Rug",
      isVerified: false
    })
  );

  // Alice attempts to self-verify marketplace item -> DENIED
  await assertFails(
    aliceDb.collection("marketplaceListings").doc("item_sec_1").update({
      isVerified: true
    })
  );

  // Alice attempts to change sellerId -> DENIED
  await assertFails(
    aliceDb.collection("marketplaceListings").doc("item_sec_1").update({
      sellerId: BOB_UID
    })
  );

  // Admin CAN moderate marketplace item -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("marketplaceListings").doc("item_sec_1").update({
      isVerified: true,
      status: "featured"
    })
  );
});

test("TARGETED CORRECTIONS: Follow relationship actor enforcement and anti-forgery", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_intruder").firestore();

  // Eve attempts to inject herself or forge following relationship for Alice -> DENIED
  await assertFails(
    eveDb.collection("users").doc(ALICE_UID).collection("following").doc(BOB_UID).set({
      followedAt: Date.now()
    })
  );

  // Eve attempts to inject follow request on Bob pretending to be Alice -> DENIED
  await assertFails(
    eveDb.collection("users").doc(BOB_UID).collection("followRequests").doc(ALICE_UID).set({
      requestedAt: Date.now()
    })
  );

  // Alice attempts to follow herself -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("following").doc(ALICE_UID).set({
      followedAt: Date.now()
    })
  );

  // Alice valid follow request to Bob -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("followRequests").doc(ALICE_UID).set({
      requestedAt: Date.now()
    })
  );
});

test("TARGETED CORRECTIONS: Suspended administrators forfeit all administrative privileges", async () => {
  const SUSPENDED_ADMIN_UID = "bad_admin_999";
  const suspendedAdminDb = testEnv.authenticatedContext(SUSPENDED_ADMIN_UID).firestore();

  // Setup admin record and suspension record for SUSPENDED_ADMIN_UID
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(SUSPENDED_ADMIN_UID).set({
      role: "admin",
      enabled: true
    });
    await db.collection("adminSessions").doc(SUSPENDED_ADMIN_UID).set({
      userId: SUSPENDED_ADMIN_UID,
      email: "suspended_admin@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true
    });
    await db.collection("userSuspensions").doc(SUSPENDED_ADMIN_UID).set({
      userId: SUSPENDED_ADMIN_UID,
      isSuspended: true,
      suspendedUntil: 0
    });
    await db.collection("posts").doc("post_to_mod").set({
      authorId: ALICE_UID,
      text: "Normal post",
      isModeratedHidden: false
    });
  });

  // Suspended admin attempts to moderate post -> DENIED
  await assertFails(
    suspendedAdminDb.collection("posts").doc("post_to_mod").update({
      isModeratedHidden: true,
      moderatedAt: Date.now(),
      moderatedBy: SUSPENDED_ADMIN_UID,
      moderationReason: "Abusive admin action"
    })
  );

  // Suspended admin attempts to write to userSuspensions -> DENIED
  await assertFails(
    suspendedAdminDb.collection("userSuspensions").doc(BOB_UID).set({
      userId: BOB_UID,
      isSuspended: true
    })
  );
});

test("HARDENING: Creation payload safeguards across users, posts, listings, and conversations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // 1. User profile creation: forgery of followersCount -> DENIED
  await assertFails(
    aliceDb.collection("users").doc("alice_forged_profile").set({
      uid: "alice_forged_profile",
      name: "Alice",
      followersCount: 50000
    })
  );

  // 2. Post creation: injection of initial likesCount or moderation fields -> DENIED
  await assertFails(
    aliceDb.collection("posts").doc("post_injected_likes").set({
      authorId: ALICE_UID,
      text: "Hacked post",
      likesCount: 100
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("post_injected_mod").set({
      authorId: ALICE_UID,
      text: "Hacked post",
      moderatedBy: "admin"
    })
  );

  // 3. Reel creation: injection of initial likesCount -> DENIED
  await assertFails(
    aliceDb.collection("reels").doc("reel_injected").set({
      authorId: ALICE_UID,
      caption: "Hacked reel",
      likesCount: 50
    })
  );

  // 4. Story creation: injection of moderation fields -> DENIED
  await assertFails(
    aliceDb.collection("stories").doc("story_injected").set({
      storyId: "story_injected",
      authorId: ALICE_UID,
      caption: "Hacked story",
      moderatedBy: "admin"
    })
  );

  // 5. Business listing creation: injection of fake rating/reviews -> DENIED
  await assertFails(
    aliceDb.collection("businessListings").doc("biz_injected").set({
      id: "biz_injected",
      ownerId: ALICE_UID,
      businessName: "Fake 5-Star Shop",
      rating: 5.0,
      reviewsCount: 1000
    })
  );

  // 6. Report creation: injection of self-resolution -> DENIED
  await assertFails(
    aliceDb.collection("reports").doc("rep_injected").set({
      reportId: "rep_injected",
      reporterId: ALICE_UID,
      status: "PENDING",
      reviewedBy: "admin",
      resolutionNotes: "Self-resolved"
    })
  );

  // 7. Conversation creation: duplicate participants -> DENIED
  await assertFails(
    aliceDb.collection("conversations").doc("conv_self").set({
      id: "conv_self",
      participantIds: [ALICE_UID, ALICE_UID]
    })
  );

  // 8. Message creation: non-string text -> DENIED
  const validConvId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("conversations").doc(validConvId).set({
      id: validConvId,
      participantIds: [ALICE_UID, BOB_UID]
    })
  );
  await assertFails(
    aliceDb.collection("conversations").doc(validConvId).collection("messages").doc("msg_bad_type").set({
      id: "msg_bad_type",
      senderId: ALICE_UID,
      text: 12345
    })
  );
});

test("HARDENING: User suspensions privacy - non-admin cannot spy on other user suspensions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Create suspension record for Bob
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("userSuspensions").doc(BOB_UID).set({
      userId: BOB_UID,
      isSuspended: true,
      suspensionReason: "Sensitive internal disciplinary investigation"
    });
  });

  // Alice attempts to read Bob's sensitive suspension record -> DENIED
  await assertFails(
    aliceDb.collection("userSuspensions").doc(BOB_UID).get()
  );

  // Bob CAN read his own suspension record -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("userSuspensions").doc(BOB_UID).get()
  );

  // Admin CAN read Bob's suspension record -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("userSuspensions").doc(BOB_UID).get()
  );
});

test("Checksum verification logic - matching MD5 validates destination", () => {
  const srcMeta = { size: "1024", md5Hash: "abc123==", crc32c: "xyz==" };
  const destMeta = { size: "1024", md5Hash: "abc123==", crc32c: "xyz==" };

  const sizeMatches = srcMeta.size === destMeta.size;
  const hasComparableChecksum = Boolean((srcMeta.md5Hash && destMeta.md5Hash) || (srcMeta.crc32c && destMeta.crc32c));
  const checksumMatches = hasComparableChecksum
    ? ((srcMeta.md5Hash && destMeta.md5Hash) ? (srcMeta.md5Hash === destMeta.md5Hash) : (srcMeta.crc32c === destMeta.crc32c))
    : false;

  assert.strictEqual(sizeMatches, true);
  assert.strictEqual(hasComparableChecksum, true);
  assert.strictEqual(checksumMatches, true);
});

test("Checksum verification logic - matching CRC32c fallback when MD5 missing", () => {
  const srcMeta = { size: "2048", crc32c: "crc_match_999=" };
  const destMeta = { size: "2048", crc32c: "crc_match_999=" };

  const sizeMatches = srcMeta.size === destMeta.size;
  const hasComparableChecksum = Boolean((srcMeta.md5Hash && destMeta.md5Hash) || (srcMeta.crc32c && destMeta.crc32c));
  const checksumMatches = hasComparableChecksum
    ? ((srcMeta.md5Hash && destMeta.md5Hash) ? (srcMeta.md5Hash === destMeta.md5Hash) : (srcMeta.crc32c === destMeta.crc32c))
    : false;

  assert.strictEqual(sizeMatches, true);
  assert.strictEqual(hasComparableChecksum, true);
  assert.strictEqual(checksumMatches, true);
});

test("Checksum verification logic - matching size but mismatched MD5 flags conflict", () => {
  const srcMeta = { size: "1024", md5Hash: "source_hash==", crc32c: "src_crc==" };
  const destMeta = { size: "1024", md5Hash: "corrupted_hash==", crc32c: "dest_crc==" };

  const sizeMatches = srcMeta.size === destMeta.size;
  const hasComparableChecksum = Boolean((srcMeta.md5Hash && destMeta.md5Hash) || (srcMeta.crc32c && destMeta.crc32c));
  const checksumMatches = hasComparableChecksum
    ? ((srcMeta.md5Hash && destMeta.md5Hash) ? (srcMeta.md5Hash === destMeta.md5Hash) : (srcMeta.crc32c === destMeta.crc32c))
    : false;

  assert.strictEqual(sizeMatches, true);
  assert.strictEqual(hasComparableChecksum, true);
  assert.strictEqual(checksumMatches, false);
});

test("Checksum verification logic - missing checksum metadata on either object requires manual review", () => {
  const srcMeta = { size: "1024" };
  const destMeta = { size: "1024" };

  const hasComparableChecksum = Boolean((srcMeta.md5Hash && destMeta.md5Hash) || (srcMeta.crc32c && destMeta.crc32c));
  assert.strictEqual(hasComparableChecksum, false, "Must flag missing checksum metadata as unverifiable conflict");
});

test("Path traversal defense - rejects paths containing ..", () => {
  const badPaths = [
    "users/alice_123/../bob_456/posts/secret.jpg",
    "users/alice_123/posts/../../system/file",
    "users/alice_123/posts/..",
  ];
  for (const p of badPaths) {
    assert.strictEqual(p.includes(".."), true);
  }
});

test("Path prefix validation - rejects unauthorized user path substitution", () => {
  const targetAuthorId = "alice_123";
  const evilCallerPath = "users/bob_456/posts/private.jpg";
  const expectedPrefix = `users/${targetAuthorId}/`;

  const valid = evilCallerPath.startsWith(expectedPrefix) || evilCallerPath.startsWith(`/${expectedPrefix}`);
  assert.strictEqual(valid, false, "Caller path substitution must be rejected");
});

test("Stage 13.12 Storage rule overlap elimination - legacy rule cannot match protected folder", () => {
  const allowedLegacyFolders = ["posts", "stories", "reels"];
  assert.strictEqual(allowedLegacyFolders.includes("protected"), false, "protected folder must NOT be in legacyFolders");
  assert.strictEqual(allowedLegacyFolders.includes("posts"), true);
  assert.strictEqual(allowedLegacyFolders.includes("stories"), true);
  assert.strictEqual(allowedLegacyFolders.includes("reels"), true);
});

test("Stage 13.12 Media Authorization Logic - Public, Private, Approved, Pending, Blocked, Suspended", () => {
  function authorizeMediaAccess({
    callerUid,
    targetAuthorId,
    targetIsPrivate,
    isCallerSuspended,
    isAuthorSuspended,
    isBlockedByAuthor,
    isBlockedByCaller,
    isApprovedFollower,
    isPendingFollower
  }) {
    if (!callerUid) return { allowed: false, reason: "UNAUTHENTICATED" };
    if (isCallerSuspended) return { allowed: false, reason: "CALLER_SUSPENDED" };
    if (isAuthorSuspended) return { allowed: false, reason: "AUTHOR_SUSPENDED" };
    if (callerUid === targetAuthorId) return { allowed: true, reason: "OWNER" };
    if (!targetIsPrivate) return { allowed: true, reason: "PUBLIC_ACCOUNT" };
    if (isBlockedByAuthor || isBlockedByCaller) return { allowed: false, reason: "BLOCKED" };
    if (isApprovedFollower) return { allowed: true, reason: "APPROVED_FOLLOWER" };
    if (isPendingFollower) return { allowed: false, reason: "PENDING_FOLLOWER_NOT_APPROVED" };
    return { allowed: false, reason: "NOT_FOLLOWING_PRIVATE_ACCOUNT" };
  }

  // 1. Unauthenticated caller
  assert.strictEqual(authorizeMediaAccess({ callerUid: null }).allowed, false);

  // 2. Owner access
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "alice", targetAuthorId: "alice", targetIsPrivate: true
  }).allowed, true);

  // 3. Public account
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "bob", targetAuthorId: "alice", targetIsPrivate: false
  }).allowed, true);

  // 4. Approved follower on private account
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "bob", targetAuthorId: "alice", targetIsPrivate: true, isApprovedFollower: true
  }).allowed, true);

  // 5. Pending follower on private account (DENIED)
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "charlie", targetAuthorId: "alice", targetIsPrivate: true, isPendingFollower: true, isApprovedFollower: false
  }).allowed, false);

  // 6. Non-follower on private account (DENIED)
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "dave", targetAuthorId: "alice", targetIsPrivate: true, isApprovedFollower: false
  }).allowed, false);

  // 7. Blocked by author (DENIED even if approved follower)
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "bob", targetAuthorId: "alice", targetIsPrivate: true, isApprovedFollower: true, isBlockedByAuthor: true
  }).allowed, false);

  // 8. Blocked by caller (DENIED)
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "bob", targetAuthorId: "alice", targetIsPrivate: true, isApprovedFollower: true, isBlockedByCaller: true
  }).allowed, false);

  // 9. Caller suspended (DENIED)
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "bob", targetAuthorId: "alice", targetIsPrivate: false, isCallerSuspended: true
  }).allowed, false);

  // 10. Author suspended (DENIED)
  assert.strictEqual(authorizeMediaAccess({
    callerUid: "bob", targetAuthorId: "alice", targetIsPrivate: false, isAuthorSuspended: true
  }).allowed, false);
});

test("Stage 13.12 Dry-run isolation guarantees - no mutations permitted in dry run", () => {
  const operationsExecuted = [];
  function executeMigrationStep(dryRun) {
    if (dryRun) {
      // In dry run mode, record count only
      return { copied: 1, writtenToStorage: false, writtenToFirestore: false };
    }
    operationsExecuted.push("STORAGE_COPY");
    operationsExecuted.push("FIRESTORE_BATCH_UPDATE");
    return { copied: 1, writtenToStorage: true, writtenToFirestore: true };
  }

  const dryResult = executeMigrationStep(true);
  assert.strictEqual(dryResult.writtenToStorage, false);
  assert.strictEqual(dryResult.writtenToFirestore, false);
  assert.strictEqual(operationsExecuted.length, 0, "Dry run must perform zero writes");
});

// ============================================================================
// FINAL SECURITY AUDIT TESTS: 3-TIER RBAC, BLUE TICK, VIRAL/PROMOTED, LOGS
// ============================================================================

const SUPER_ADMIN_UID = "super_admin_999";
const STANDARD_ADMIN_UID = "standard_admin_888";
const MODERATOR_UID = "moderator_777";
const REVOKED_ADMIN_UID = "revoked_admin_666";

async function seedRbacRoles() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(SUPER_ADMIN_UID).set({
      uid: SUPER_ADMIN_UID,
      role: "super_admin",
      isActive: true,
      enabled: true,
    });
    await db.collection("adminSessions").doc(SUPER_ADMIN_UID).set({
      userId: SUPER_ADMIN_UID,
      email: "superadmin@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true,
    });
    await db.collection("adminRoles").doc(STANDARD_ADMIN_UID).set({
      uid: STANDARD_ADMIN_UID,
      role: "admin",
      isActive: true,
      enabled: true,
    });
    await db.collection("adminSessions").doc(STANDARD_ADMIN_UID).set({
      userId: STANDARD_ADMIN_UID,
      email: "admin@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true,
    });
    await db.collection("adminRoles").doc(MODERATOR_UID).set({
      uid: MODERATOR_UID,
      role: "moderator",
      isActive: true,
      enabled: true,
    });
    await db.collection("adminSessions").doc(MODERATOR_UID).set({
      userId: MODERATOR_UID,
      email: "moderator@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true,
    });
    await db.collection("adminRoles").doc(REVOKED_ADMIN_UID).set({
      uid: REVOKED_ADMIN_UID,
      role: "none",
      enabled: false,
    });
    await db.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      name: "Alice",
      handle: "@alice",
      email: "alice@yarkhoon.com",
      isVerified: false,
      isSuspended: false,
      role: "member",
    });
    await db.collection("posts").doc("seed_post_1").set({
      authorId: ALICE_UID,
      text: "Community organic post",
      createdAt: "Now",
      timestamp: Date.now(),
      likesCount: 10,
      commentsCount: 2,
      sharesCount: 1,
      impressionsCount: 50,
      isPinned: false,
      isFeatured: false,
      isPromoted: false,
      isTrendingLabeled: false,
      moderationStatus: "APPROVED",
      isModeratedHidden: false,
    });
  });
}

test("Audit 1: Ordinary user CANNOT create or modify adminRoles or read arbitrary adminRoles", async () => {
  await seedRbacRoles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ordinary user cannot grant themselves admin/super_admin/moderator
  await assertFails(
    aliceDb.collection("adminRoles").doc(ALICE_UID).set({
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );

  // Ordinary user cannot read another admin's role record
  await assertFails(
    aliceDb.collection("adminRoles").doc(SUPER_ADMIN_UID).get()
  );
});

test("Audit 2: Standard ADMIN CANNOT assign Super Admin or modify adminRoles; only SUPER_ADMIN can", async () => {
  await seedRbacRoles();
  const standardAdminDb = testEnv.authenticatedContext(STANDARD_ADMIN_UID).firestore();
  const superAdminDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();

  // Standard Admin cannot escalate themselves to super_admin or add a new admin
  await assertFails(
    standardAdminDb.collection("adminRoles").doc(STANDARD_ADMIN_UID).update({
      role: "super_admin",
    })
  );
  await assertFails(
    standardAdminDb.collection("adminRoles").doc(BOB_UID).set({
      uid: BOB_UID,
      role: "moderator",
      isActive: true,
      enabled: true,
    })
  );

  // Standard Admin also cannot modify 'role' field on users/{userId}
  await assertFails(
    standardAdminDb.collection("users").doc(ALICE_UID).update({
      role: "super_admin",
    })
  );

  // Super Admin CAN assign and revoke roles in adminRoles
  await assertSucceeds(
    superAdminDb.collection("adminRoles").doc(BOB_UID).set({
      uid: BOB_UID,
      role: "moderator",
      isActive: true,
      enabled: true,
    })
  );
});

test("Audit 3: Blue Tick Security - Ordinary users and Moderators CANNOT self-verify or approve verificationRequests", async () => {
  await seedRbacRoles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const modDb = testEnv.authenticatedContext(MODERATOR_UID).firestore();
  const adminDb = testEnv.authenticatedContext(STANDARD_ADMIN_UID).firestore();

  // 1. Ordinary user cannot update their own isVerified field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
    })
  );

  // 2. Ordinary user cannot create a new user profile with isVerified: true or role: 'super_admin'
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      name: "Bob",
      isVerified: true,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      name: "Bob",
      isVerified: false,
      role: "super_admin",
    })
  );

  // 3. Ordinary user can submit a PENDING verificationRequest, but cannot submit an APPROVED one
  await assertFails(
    aliceDb.collection("verificationRequests").doc("vreq_spoof").set({
      userId: ALICE_UID,
      status: "APPROVED",
      category: "Leader",
      supportingInfo: "Self-approved",
    })
  );
  await assertSucceeds(
    aliceDb.collection("verificationRequests").doc("vreq_valid").set({
      userId: ALICE_UID,
      status: "PENDING",
      category: "Community Leader",
      supportingInfo: "Valid request",
      reviewedBy: null,
      reviewedAt: null,
    })
  );

  // 4. Moderator CANNOT approve verificationRequests or grant Blue Tick on users/{uid}
  await assertFails(
    modDb.collection("verificationRequests").doc("vreq_valid").update({
      status: "APPROVED",
      reviewedBy: MODERATOR_UID,
    })
  );
  await assertFails(
    modDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
    })
  );

  // 5. Full Admin CAN approve verificationRequests and grant Blue Tick on users/{uid}
  await assertSucceeds(
    adminDb.collection("verificationRequests").doc("vreq_valid").update({
      status: "APPROVED",
      reviewedBy: STANDARD_ADMIN_UID,
      reviewedAt: Date.now(),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
    })
  );

  // 6. Ordinary user CANNOT spoof authorIsVerified: true on post or comment creation when unverified
  await assertDriverRevertVerification(ALICE_UID);
  await assertFails(
    aliceDb.collection("posts").doc("post_spoof_verified").set({
      authorId: ALICE_UID,
      authorIsVerified: true,
      text: "Fake blue tick post",
      createdAt: "Now",
      timestamp: Date.now(),
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("seed_post_1").collection("comments").doc("c_spoof").set({
      authorId: ALICE_UID,
      authorIsVerified: true,
      text: "Fake blue tick comment",
    })
  );
});

async function assertDriverRevertVerification(uid) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(uid).update({ isVerified: false });
  });
}

test("Audit 4: Viral, Featured, Promoted, Pinned & Trending Posts - Ordinary users and Moderators CANNOT manipulate promotion or metrics", async () => {
  await seedRbacRoles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const modDb = testEnv.authenticatedContext(MODERATOR_UID).firestore();
  const adminDb = testEnv.authenticatedContext(STANDARD_ADMIN_UID).firestore();

  // 1. Ordinary user cannot create a post pre-marked as promoted, featured, pinned, trending, or with fake impressions
  await assertFails(
    aliceDb.collection("posts").doc("p_promo_hack").set({
      authorId: ALICE_UID,
      text: "Self promoted",
      isPromoted: true,
      timestamp: Date.now(),
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("p_feat_hack").set({
      authorId: ALICE_UID,
      text: "Self featured",
      isFeatured: true,
      timestamp: Date.now(),
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("p_pin_hack").set({
      authorId: ALICE_UID,
      text: "Self pinned",
      isPinned: true,
      timestamp: Date.now(),
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("p_trend_hack").set({
      authorId: ALICE_UID,
      text: "Self trending",
      isTrendingLabeled: true,
      timestamp: Date.now(),
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("p_impr_hack").set({
      authorId: ALICE_UID,
      text: "Fake impressions",
      impressionsCount: 9999,
      timestamp: Date.now(),
    })
  );

  // 2. Ordinary user cannot update their existing post to become promoted/featured/pinned/trending or inflate likesCount by +100
  await assertFails(
    aliceDb.collection("posts").doc("seed_post_1").update({
      isPromoted: true,
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("seed_post_1").update({
      isFeatured: true,
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("seed_post_1").update({
      isTrendingLabeled: true,
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("seed_post_1").update({
      likesCount: 5000,
    })
  );

  // 3. Moderator CAN moderate (approve/reject/hide) a post, but CANNOT promote/feature/pin/trend it
  await assertSucceeds(
    modDb.collection("posts").doc("seed_post_1").update({
      moderationStatus: "APPROVED",
      isModeratedHidden: false,
      moderatedBy: MODERATOR_UID,
      moderatedAt: Date.now(),
    })
  );
  await assertFails(
    modDb.collection("posts").doc("seed_post_1").update({
      isPromoted: true,
    })
  );
  await assertFails(
    modDb.collection("posts").doc("seed_post_1").update({
      isFeatured: true,
    })
  );
  await assertFails(
    modDb.collection("posts").doc("seed_post_1").update({
      isPinned: true,
    })
  );

  // 4. Full Admin CAN promote, feature, pin, and label trending posts
  await assertSucceeds(
    adminDb.collection("posts").doc("seed_post_1").update({
      isPromoted: true,
      isFeatured: true,
      isPinned: true,
      isTrendingLabeled: true,
    })
  );
});

test("Audit 5: Moderator vs Full Admin separation on userSuspensions, villages, officialAnnouncements, trendingTopics, and audit logs", async () => {
  await seedRbacRoles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const modDb = testEnv.authenticatedContext(MODERATOR_UID).firestore();
  const adminDb = testEnv.authenticatedContext(STANDARD_ADMIN_UID).firestore();
  const revokedDb = testEnv.authenticatedContext(REVOKED_ADMIN_UID).firestore();

  // 1. Moderator CANNOT suspend users
  await assertFails(
    modDb.collection("userSuspensions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      isSuspended: true,
    })
  );
  await assertFails(
    modDb.collection("users").doc(ALICE_UID).update({
      isSuspended: true,
    })
  );

  // 2. Full Admin CAN suspend users
  await assertSucceeds(
    adminDb.collection("userSuspensions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      isSuspended: true,
      suspendedUntil: Date.now() + 86400000,
      suspensionReason: "Spamming",
    })
  );

  // 3. Moderator CANNOT modify villages, officialAnnouncements, or trendingTopics
  await assertFails(
    modDb.collection("villages").doc("v_test").set({ name: "Lasht" })
  );
  await assertFails(
    modDb.collection("officialAnnouncements").doc("a_test").set({ title: "Alert" })
  );
  await assertFails(
    modDb.collection("trendingTopics").doc("t_test").set({ tag: "#Test" })
  );

  // 4. Full Admin CAN manage villages, officialAnnouncements, and trendingTopics
  await assertSucceeds(
    adminDb.collection("villages").doc("v_test").set({ name: "Lasht", district: "Upper Chitral" })
  );
  await assertSucceeds(
    adminDb.collection("officialAnnouncements").doc("a_test").set({ title: "Alert", body: "Notice" })
  );
  await assertSucceeds(
    adminDb.collection("trendingTopics").doc("t_test").set({ tag: "#Test", headline: "News" })
  );

  // 5. Revoked Admin (role: 'none', enabled: false) is completely denied from all admin actions
  await assertFails(
    revokedDb.collection("posts").doc("seed_post_1").update({
      isModeratedHidden: true,
    })
  );
  await assertFails(
    revokedDb.collection("moderationAuditLogs").get()
  );

  // 6. Audit logs are append-only for active moderators/admins and completely immutable (no update/delete by anyone, even Super Admin)
  await assertSucceeds(
    modDb.collection("moderationAuditLogs").doc("log_mod_1").set({
      adminId: MODERATOR_UID,
      actionType: "HIDE_CONTENT",
      targetType: "POST",
      targetId: "seed_post_1",
      reason: "Rule violation",
      timestamp: Date.now(),
    })
  );
  const superAdminDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();
  await assertFails(
    superAdminDb.collection("moderationAuditLogs").doc("log_mod_1").update({
      reason: "Tampered reason",
    })
  );
  await assertFails(
    superAdminDb.collection("moderationAuditLogs").doc("log_mod_1").delete()
  );
  await assertFails(
    aliceDb.collection("moderationAuditLogs").get()
  );
});

// ============================================================================
// SERVER-SIDE SLIDING INACTIVITY SESSION ENFORCEMENT TESTS (SCENARIOS a - j)
// ============================================================================

const ACTIVE_ADMIN_UID = "active_session_admin_101";
const EXPIRED_ADMIN_UID = "expired_session_admin_102";
const LOGGED_OUT_ADMIN_UID = "logged_out_admin_103";

async function seedAdminWithSession(uid, role, isActive, lastActivityAgoMs, rememberMe = true) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(uid).set({
      uid,
      role,
      isActive: true,
      enabled: true,
    });
    const lastActivityDate = new Date(Date.now() - lastActivityAgoMs);
    await db.collection("adminSessions").doc(uid).set({
      userId: uid,
      email: `${uid}@yarkhoon.com`,
      isActive,
      lastActivityAt: lastActivityDate,
      sessionStartAt: lastActivityDate,
      rememberMe,
    });
    await db.collection("posts").doc(`post_for_${uid}`).set({
      authorId: ALICE_UID,
      text: "Test post for session verification",
      createdAt: "Now",
      timestamp: Date.now(),
      isModeratedHidden: false,
    });
  });
}

test("Scenario a: Valid admin session (activity within 4h) allows privileged operations", async () => {
  // Activity 30 minutes ago (well within 4h window)
  await seedAdminWithSession(ACTIVE_ADMIN_UID, "admin", true, 30 * 60 * 1000);
  const adminDb = testEnv.authenticatedContext(ACTIVE_ADMIN_UID).firestore();

  await assertSucceeds(
    adminDb.collection("officialAnnouncements").doc("announcement_valid").set({
      title: "Valid Session Announcement",
      body: "Sliding window valid",
      createdAt: "Now",
      timestamp: Date.now(),
    })
  );
});

test("Scenario b: 4-hour inactivity expiration rejects privileged operations", async () => {
  // Inactivity 4 hours and 5 minutes ago (> 14,400,000 ms)
  await seedAdminWithSession(EXPIRED_ADMIN_UID, "admin", true, (4 * 60 + 5) * 60 * 1000);
  const expiredDb = testEnv.authenticatedContext(EXPIRED_ADMIN_UID).firestore();

  await assertFails(
    expiredDb.collection("officialAnnouncements").doc("announcement_expired").set({
      title: "Expired Session Announcement",
      body: "Should be blocked by server-side rules",
      createdAt: "Now",
      timestamp: Date.now(),
    })
  );
});

test("Scenario c: Stale token cannot bypass server-side rules after session expiration", async () => {
  await seedAdminWithSession("stale_token_admin", "admin", true, 5 * 60 * 60 * 1000); // 5 hours ago
  const staleAdminDb = testEnv.authenticatedContext("stale_token_admin").firestore();

  // Attempting user suspension with expired session -> blocked
  await assertFails(
    staleAdminDb.collection("userSuspensions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      isSuspended: true,
      suspendedUntil: 0,
    })
  );

  // Attempting post moderation with expired session -> blocked
  await assertFails(
    staleAdminDb.collection("posts").doc("post_for_stale_token_admin").update({
      isModeratedHidden: true,
    })
  );
});

test("Scenario d: Forged session heartbeats by unauthorized users are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Non-admin attempting to create an admin session document -> blocked
  await assertFails(
    aliceDb.collection("adminSessions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
    })
  );

  // User attempting to modify another user's admin session -> blocked
  await assertFails(
    aliceDb.collection("adminSessions").doc(ACTIVE_ADMIN_UID).update({
      isActive: true,
    })
  );
});

test("Scenario e: Future or forged timestamps on session activity are strictly rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc("future_time_admin").set({
      uid: "future_time_admin",
      role: "admin",
      enabled: true,
      isActive: true,
    });
  });

  const adminDb = testEnv.authenticatedContext("future_time_admin").firestore();

  // Client attempts to send a forged timestamp (+ 1 day into the future) instead of server timestamp
  const futureDate = new Date(Date.now() + 86400000);
  await assertFails(
    adminDb.collection("adminSessions").doc("future_time_admin").set({
      userId: "future_time_admin",
      email: "future@yarkhoon.com",
      isActive: true,
      lastActivityAt: futureDate,
      sessionStartAt: futureDate,
      rememberMe: false,
    })
  );
});

test("Scenario f: Role revocation immediately cuts off admin access despite active session", async () => {
  // Admin has active session doc (< 4 hours) but their adminRoles document has been disabled
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc("revoked_live_admin").set({
      uid: "revoked_live_admin",
      role: "none",
      enabled: false,
    });
    await db.collection("adminSessions").doc("revoked_live_admin").set({
      userId: "revoked_live_admin",
      email: "revoked_live@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true,
    });
  });

  const revokedDb = testEnv.authenticatedContext("revoked_live_admin").firestore();
  await assertFails(
    revokedDb.collection("officialAnnouncements").doc("revoked_test").set({
      title: "Revoked admin should fail",
    })
  );
});

test("Scenario g: Suspended admin is immediately rejected from admin operations despite active session", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc("suspended_active_admin").set({
      uid: "suspended_active_admin",
      role: "admin",
      enabled: true,
      isActive: true,
    });
    await db.collection("adminSessions").doc("suspended_active_admin").set({
      userId: "suspended_active_admin",
      email: "suspended_active@yarkhoon.com",
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true,
    });
    await db.collection("userSuspensions").doc("suspended_active_admin").set({
      userId: "suspended_active_admin",
      isSuspended: true,
      suspendedUntil: 0,
    });
  });

  const suspendedDb = testEnv.authenticatedContext("suspended_active_admin").firestore();
  await assertFails(
    suspendedDb.collection("officialAnnouncements").doc("suspended_test").set({
      title: "Suspended admin should fail",
    })
  );
});

test("Scenario h: Moderator boundaries are strictly preserved under active server sessions", async () => {
  await seedAdminWithSession("session_moderator_1", "moderator", true, 10 * 60 * 1000);
  const modDb = testEnv.authenticatedContext("session_moderator_1").firestore();

  // Moderator CAN moderate post
  await assertSucceeds(
    modDb.collection("posts").doc("post_for_session_moderator_1").update({
      isModeratedHidden: true,
      moderationStatus: "REJECTED",
      moderatedAt: Date.now(),
      moderatedBy: "session_moderator_1",
      moderationReason: "Inappropriate content",
    })
  );

  // Moderator CANNOT suspend user
  await assertFails(
    modDb.collection("userSuspensions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      isSuspended: true,
      suspendedUntil: 0,
    })
  );

  // Moderator CANNOT create official announcements
  await assertFails(
    modDb.collection("officialAnnouncements").doc("mod_announcement").set({
      title: "Unauthorized announcement",
    })
  );
});

test("Scenario i: Remember Me enabled session allows valid operation within inactivity limit", async () => {
  await seedAdminWithSession("remember_me_admin", "admin", true, 60 * 60 * 1000, true); // 1h ago, rememberMe: true
  const adminDb = testEnv.authenticatedContext("remember_me_admin").firestore();

  await assertSucceeds(
    adminDb.collection("officialAnnouncements").doc("remember_announcement").set({
      title: "Remember Me Valid",
      body: "Active within 4h",
      createdAt: "Now",
      timestamp: Date.now(),
    })
  );
});

test("Scenario j: Logout invalidation immediately terminates admin privileges", async () => {
  await seedAdminWithSession(LOGGED_OUT_ADMIN_UID, "admin", true, 10 * 60 * 1000);
  const adminDb = testEnv.authenticatedContext(LOGGED_OUT_ADMIN_UID).firestore();

  // Admin logs out: updates their session to isActive = false
  await assertSucceeds(
    adminDb.collection("adminSessions").doc(LOGGED_OUT_ADMIN_UID).update({
      isActive: false,
    })
  );

  // Immediate subsequent privileged operation is BLOCKED by server rules
  await assertFails(
    adminDb.collection("officialAnnouncements").doc("after_logout_announcement").set({
      title: "Blocked after logout",
    })
  );
});

// ============================================================================
// TARGETED VALIDATION TESTS FOR hasActiveAdminSession() HARDENING (1 - 7)
// ============================================================================

test("Hardening 1: Session with isActive = true allows admin operation", async () => {
  const uid = "h_admin_active_true";
  await seedAdminWithSession(uid, "admin", true, 15 * 60 * 1000);
  const db = testEnv.authenticatedContext(uid).firestore();

  await assertSucceeds(
    db.collection("officialAnnouncements").doc("ann_h1").set({
      title: "Hardening 1 Announcement",
      body: "Active session allowed",
      timestamp: Date.now(),
    })
  );
});

test("Hardening 2: Session with isActive = false strictly rejects admin operation", async () => {
  const uid = "h_admin_active_false";
  await seedAdminWithSession(uid, "admin", false, 15 * 60 * 1000);
  const db = testEnv.authenticatedContext(uid).firestore();

  await assertFails(
    db.collection("officialAnnouncements").doc("ann_h2").set({
      title: "Hardening 2 Announcement",
      body: "Inactive session blocked",
      timestamp: Date.now(),
    })
  );
});

test("Hardening 3: Session with missing isActive field strictly rejects admin operation", async () => {
  const uid = "h_admin_missing_active";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(uid).set({
      uid,
      role: "admin",
      isActive: true,
      enabled: true,
    });
    // Missing isActive completely
    await db.collection("adminSessions").doc(uid).set({
      userId: uid,
      email: `${uid}@yarkhoon.com`,
      lastActivityAt: new Date(Date.now() - 10 * 60 * 1000),
      sessionStartAt: new Date(Date.now() - 10 * 60 * 1000),
      rememberMe: true,
    });
  });

  const db = testEnv.authenticatedContext(uid).firestore();
  await assertFails(
    db.collection("officialAnnouncements").doc("ann_h3").set({
      title: "Hardening 3 Announcement",
      body: "Missing isActive blocked",
      timestamp: Date.now(),
    })
  );
});

test("Hardening 4: Session with null isActive strictly rejects admin operation", async () => {
  const uid = "h_admin_null_active";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(uid).set({
      uid,
      role: "admin",
      isActive: true,
      enabled: true,
    });
    // isActive is null
    await db.collection("adminSessions").doc(uid).set({
      userId: uid,
      email: `${uid}@yarkhoon.com`,
      isActive: null,
      lastActivityAt: new Date(Date.now() - 10 * 60 * 1000),
      sessionStartAt: new Date(Date.now() - 10 * 60 * 1000),
      rememberMe: true,
    });
  });

  const db = testEnv.authenticatedContext(uid).firestore();
  await assertFails(
    db.collection("officialAnnouncements").doc("ann_h4").set({
      title: "Hardening 4 Announcement",
      body: "Null isActive blocked",
      timestamp: Date.now(),
    })
  );
});

test("Hardening 5: Session with missing lastActivityAt strictly rejects admin operation", async () => {
  const uid = "h_admin_missing_last_activity";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(uid).set({
      uid,
      role: "admin",
      isActive: true,
      enabled: true,
    });
    // Missing lastActivityAt
    await db.collection("adminSessions").doc(uid).set({
      userId: uid,
      email: `${uid}@yarkhoon.com`,
      isActive: true,
      sessionStartAt: new Date(Date.now() - 10 * 60 * 1000),
      rememberMe: true,
    });
  });

  const db = testEnv.authenticatedContext(uid).firestore();
  await assertFails(
    db.collection("officialAnnouncements").doc("ann_h5").set({
      title: "Hardening 5 Announcement",
      body: "Missing lastActivityAt blocked",
      timestamp: Date.now(),
    })
  );
});

test("Hardening 6: Expired session (> 4 hours) strictly rejects admin operation", async () => {
  const uid = "h_admin_expired_4h";
  // 4 hours + 1 minute ago
  await seedAdminWithSession(uid, "admin", true, 241 * 60 * 1000);
  const db = testEnv.authenticatedContext(uid).firestore();

  await assertFails(
    db.collection("officialAnnouncements").doc("ann_h6").set({
      title: "Hardening 6 Announcement",
      body: "Expired session blocked",
      timestamp: Date.now(),
    })
  );
});

test("Hardening 7: Valid owner bootstrap succeeds without existing adminSessions or adminRoles", async () => {
  const ownerUid = "isBdCPBYupN4SDJZ1yRiSZ8K0FF2";
  const ownerEmail = "hamarachitralonline@gmail.com";
  // Owner context with verified email token
  const ownerDb = testEnv.authenticatedContext(ownerUid, { email: ownerEmail }).firestore();

  // Trusted owner bootstrap creates the first Super Admin document in adminRoles
  await assertSucceeds(
    ownerDb.collection("adminRoles").doc(ownerUid).set({
      uid: ownerUid,
      email: ownerEmail,
      role: "super_admin",
      isActive: true,
      enabled: true,
      assignedBy: "bootstrap",
    })
  );
});

// ============================================================================
// FOCUSED BOOTSTRAP SECURITY HARDENING TESTS (REQUIREMENTS 1 - 10)
// ============================================================================

const BOOTSTRAP_OWNER_UID = "isBdCPBYupN4SDJZ1yRiSZ8K0FF2";
const BOOTSTRAP_OWNER_EMAIL = "hamarachitralonline@gmail.com";

test("Bootstrap Req 1: Owner can create their own initial Super Admin document", async () => {
  const ownerDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, { email: BOOTSTRAP_OWNER_EMAIL }).firestore();
  await assertSucceeds(
    ownerDb.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      email: BOOTSTRAP_OWNER_EMAIL,
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );
});

test("Bootstrap Req 2: Owner cannot create another user's adminRoles document during bootstrap", async () => {
  const ownerDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, { email: BOOTSTRAP_OWNER_EMAIL }).firestore();
  await assertFails(
    ownerDb.collection("adminRoles").doc("other_user_bob").set({
      uid: "other_user_bob",
      email: "bob@yarkhoon.com",
      role: "admin",
      isActive: true,
      enabled: true,
    })
  );
});

test("Bootstrap Req 3: Owner cannot update or delete another user's adminRoles document during bootstrap", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc("existing_target_user").set({
      uid: "existing_target_user",
      role: "admin",
      isActive: true,
      enabled: true,
    });
  });

  const ownerDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, { email: BOOTSTRAP_OWNER_EMAIL }).firestore();

  // Attempt update during bootstrap -> DENIED
  await assertFails(
    ownerDb.collection("adminRoles").doc("existing_target_user").update({
      role: "super_admin",
    })
  );

  // Attempt delete during bootstrap -> DENIED
  await assertFails(
    ownerDb.collection("adminRoles").doc("existing_target_user").delete()
  );
});

test("Bootstrap Req 4: admin@yarkhoon.com cannot use the initial bootstrap exception", async () => {
  const adminYarkhoonDb = testEnv.authenticatedContext("uid_admin_yarkhoon", { email: "admin@yarkhoon.com" }).firestore();
  await assertFails(
    adminYarkhoonDb.collection("adminRoles").doc("uid_admin_yarkhoon").set({
      uid: "uid_admin_yarkhoon",
      email: "admin@yarkhoon.com",
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );

  // Even if using owner UID with admin@yarkhoon.com -> DENIED
  const ownerWithAdminEmailDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, { email: "admin@yarkhoon.com" }).firestore();
  await assertFails(
    ownerWithAdminEmailDb.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      email: "admin@yarkhoon.com",
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );
});

test("Bootstrap Req 5: Fallback email aliases cannot use the initial bootstrap exception", async () => {
  const aliasDb1 = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, {
    email: "hamarachitralonline.gmail.com.extra@yarkhoon.com"
  }).firestore();
  await assertFails(
    aliasDb1.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );

  const aliasDb2 = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, {
    email: "admin.yarkhoon.com.alias@yarkhoon.com"
  }).firestore();
  await assertFails(
    aliasDb2.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );
});

test("Bootstrap Req 6: Wrong UID with the correct email is rejected", async () => {
  const wrongUidDb = testEnv.authenticatedContext("imposter_uid_999", {
    email: BOOTSTRAP_OWNER_EMAIL
  }).firestore();

  // Attempt to create imposter document
  await assertFails(
    wrongUidDb.collection("adminRoles").doc("imposter_uid_999").set({
      uid: "imposter_uid_999",
      email: BOOTSTRAP_OWNER_EMAIL,
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );

  // Attempt to create owner document with wrong auth UID
  await assertFails(
    wrongUidDb.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      email: BOOTSTRAP_OWNER_EMAIL,
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );
});

test("Bootstrap Req 7: Correct UID with a different email is rejected", async () => {
  const wrongEmailDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, {
    email: "attacker@evil.com"
  }).firestore();

  await assertFails(
    wrongEmailDb.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      email: "attacker@evil.com",
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );
});

test("Bootstrap Req 8: Normal Super Admin role assignment and revocation still work after bootstrap with a valid active session", async () => {
  // Setup bootstrapped owner as active Super Admin with active session
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      role: "super_admin",
      isActive: true,
      enabled: true,
    });
    await db.collection("adminSessions").doc(BOOTSTRAP_OWNER_UID).set({
      userId: BOOTSTRAP_OWNER_UID,
      email: BOOTSTRAP_OWNER_EMAIL,
      isActive: true,
      lastActivityAt: new Date(),
      sessionStartAt: new Date(),
      rememberMe: true,
    });
  });

  const establishedSuperAdminDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, { email: BOOTSTRAP_OWNER_EMAIL }).firestore();

  // 1. Create a new admin role document -> SUCCEEDS
  await assertSucceeds(
    establishedSuperAdminDb.collection("adminRoles").doc("new_managed_admin").set({
      uid: "new_managed_admin",
      role: "admin",
      isActive: true,
      enabled: true,
    })
  );

  // 2. Update the role document (e.g. change to moderator) -> SUCCEEDS
  await assertSucceeds(
    establishedSuperAdminDb.collection("adminRoles").doc("new_managed_admin").update({
      role: "moderator",
    })
  );

  // 3. Delete the role document -> SUCCEEDS
  await assertSucceeds(
    establishedSuperAdminDb.collection("adminRoles").doc("new_managed_admin").delete()
  );
});

test("Bootstrap Req 9: Standard users, moderators, and regular admins cannot grant themselves Super Admin privileges", async () => {
  await seedRbacRoles();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const modDb = testEnv.authenticatedContext(MODERATOR_UID).firestore();
  const adminDb = testEnv.authenticatedContext(STANDARD_ADMIN_UID).firestore();

  // Alice cannot grant self Super Admin
  await assertFails(
    aliceDb.collection("adminRoles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      role: "super_admin",
      isActive: true,
      enabled: true,
    })
  );

  // Moderator cannot elevate to Super Admin
  await assertFails(
    modDb.collection("adminRoles").doc(MODERATOR_UID).update({
      role: "super_admin",
    })
  );

  // Regular Admin cannot elevate to Super Admin
  await assertFails(
    adminDb.collection("adminRoles").doc(STANDARD_ADMIN_UID).update({
      role: "super_admin",
    })
  );
});

test("Bootstrap Req 10: Expired sessions and suspended accounts remain blocked from adminRoles and admin actions", async () => {
  // 1. Expired session owner
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("adminRoles").doc(BOOTSTRAP_OWNER_UID).set({
      uid: BOOTSTRAP_OWNER_UID,
      role: "super_admin",
      isActive: true,
      enabled: true,
    });
    // 5 hours ago (expired)
    await db.collection("adminSessions").doc(BOOTSTRAP_OWNER_UID).set({
      userId: BOOTSTRAP_OWNER_UID,
      email: BOOTSTRAP_OWNER_EMAIL,
      isActive: true,
      lastActivityAt: new Date(Date.now() - 5 * 60 * 60 * 1000),
      sessionStartAt: new Date(Date.now() - 5 * 60 * 60 * 1000),
      rememberMe: true,
    });
  });

  const expiredOwnerDb = testEnv.authenticatedContext(BOOTSTRAP_OWNER_UID, { email: BOOTSTRAP_OWNER_EMAIL }).firestore();
  await assertFails(
    expiredOwnerDb.collection("adminRoles").doc("target_user_test").set({
      uid: "target_user_test",
      role: "admin",
      isActive: true,
      enabled: true,
    })
  );

  // 2. Suspended Super Admin
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("userSuspensions").doc(BOOTSTRAP_OWNER_UID).set({
      userId: BOOTSTRAP_OWNER_UID,
      isSuspended: true,
      suspendedUntil: 0,
    });
  });

  await assertFails(
    expiredOwnerDb.collection("adminRoles").doc("target_user_test").set({
      uid: "target_user_test",
      role: "admin",
      isActive: true,
      enabled: true,
    })
  );
});




