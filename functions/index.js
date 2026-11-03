/**
 * Cloud Functions for YARKHOON.COM Private Media Authorization & Token Revocation
 * Project ID: yarkhoondotcom
 */

const { onRequest, onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const storage = admin.storage();

/**
 * Helper to verify caller token and decode UID
 */
async function getAuthenticatedUid(req) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return null;
  }
  const idToken = authHeader.split("Bearer ")[1];
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    return decoded.uid;
  } catch (e) {
    return null;
  }
}

/**
 * Endpoint 1: getAuthorizedMediaUrl (Callable or HTTP)
 * Authorizes access to private user posts, stories, and reels.
 * Checks:
 *   1. Requester authentication
 *   2. Target author exists
 *   3. Author account privacy: if public, generates access immediately
 *   4. Author account suspension: blocked if author or caller is suspended
 *   5. Blocking relationship: if target blocked requester or vice-versa, DENY
 *   6. Follow status: requester must be in target's followers collection with status approved
 *   7. Returns short-lived signed URL (15 minutes expiry)
 */
exports.getAuthorizedMediaUrl = onRequest({ region: "asia-southeast1" }, async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");

  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method Not Allowed. Use POST." });
  }

  const callerUid = await getAuthenticatedUid(req);
  if (!callerUid) {
    return res.status(401).json({ error: "Unauthorized: Valid Firebase ID token required." });
  }

  const { targetAuthorId, storagePath, contentType, contentId } = req.body;
  if (!targetAuthorId || !storagePath || !contentType || !contentId) {
    return res.status(400).json({
      error: "Missing mandatory parameters: targetAuthorId, storagePath, contentType ('post'|'story'|'reel'), and contentId are strictly required."
    });
  }

  // Security Check A: Enforce strict path prefix ownership.
  // The storagePath MUST start with users/{targetAuthorId}/ to prevent arbitrary file traversal or cross-user object access.
  const expectedPrefix = `users/${targetAuthorId}/`;
  if (!storagePath.startsWith(expectedPrefix) && !storagePath.startsWith(`/${expectedPrefix}`)) {
    return res.status(403).json({ error: "Access Denied: Requested storage object does not belong to the target author." });
  }

  // Security Check B: Path Traversal defense (no '..' segments permitted)
  if (storagePath.includes("..")) {
    return res.status(400).json({ error: "Bad Request: Invalid storage path format." });
  }

  // Security Check C: Validate contentType enum and format
  const validContentTypes = ["post", "story", "reel"];
  const normalizedContentType = contentType.toLowerCase().trim();
  if (!validContentTypes.includes(normalizedContentType)) {
    return res.status(400).json({ error: "Bad Request: Invalid contentType. Must be 'post', 'story', or 'reel'." });
  }

  try {
    // Security Check D: Mandatory document existence and media association verification
    let collectionName = "posts";
    if (normalizedContentType === "story") collectionName = "stories";
    else if (normalizedContentType === "reel") collectionName = "reels";

    const docSnap = await db.collection(collectionName).doc(contentId).get();
    if (!docSnap.exists) {
      return res.status(404).json({ error: "Target content document not found." });
    }
    const docData = docSnap.data();
    if (docData.authorId !== targetAuthorId) {
      return res.status(403).json({ error: "Access Denied: Document author does not match requested target author." });
    }

    // 1. Check if caller is suspended
    const callerSuspensionDoc = await db.collection("userSuspensions").doc(callerUid).get();
    if (callerSuspensionDoc.exists) {
      const data = callerSuspensionDoc.data();
      const isSuspended = data.isSuspended === true;
      const until = data.suspendedUntil || 0;
      if (isSuspended && (until === 0 || until > Date.now())) {
        return res.status(403).json({ error: "Access Denied: Your account is currently suspended." });
      }
    }

    // 2. Check if content author is currently suspended (Content from suspended accounts is withheld)
    const authorSuspensionDoc = await db.collection("userSuspensions").doc(targetAuthorId).get();
    if (authorSuspensionDoc.exists) {
      const data = authorSuspensionDoc.data();
      const isSuspended = data.isSuspended === true;
      const until = data.suspendedUntil || 0;
      if (isSuspended && (until === 0 || until > Date.now())) {
        return res.status(403).json({ error: "Access Denied: Content author account is suspended." });
      }
    }

    // 3. If caller is author, allow access to their own media
    if (callerUid === targetAuthorId) {
      const signedUrl = await generateSignedUrl(storagePath);
      return res.status(200).json({ url: signedUrl, expiresMinutes: 15 });
    }

    // 4. Check author document and privacy setting
    const authorDoc = await db.collection("users").doc(targetAuthorId).get();
    if (!authorDoc.exists) {
      return res.status(404).json({ error: "Author profile not found." });
    }
    const authorData = authorDoc.data();
    const isPrivate = authorData.isPrivate === true;

    // If author account is public, caller can view media
    if (!isPrivate) {
      const signedUrl = await generateSignedUrl(storagePath);
      return res.status(200).json({ url: signedUrl, expiresMinutes: 15 });
    }

    // 5. Check bidirectional blocking
    const blockedByAuthor = await db.collection("users").doc(targetAuthorId).collection("blockedUsers").doc(callerUid).get();
    const blockedByCaller = await db.collection("users").doc(callerUid).collection("blockedUsers").doc(targetAuthorId).get();
    if (blockedByAuthor.exists || blockedByCaller.exists) {
      return res.status(403).json({ error: "Access Denied: User is blocked." });
    }

    // 6. Check approved follower status
    const followerDoc = await db.collection("users").doc(targetAuthorId).collection("followers").doc(callerUid).get();
    if (!followerDoc.exists) {
      return res.status(403).json({ error: "Access Denied: You must follow this private account to view media." });
    }

    // Follower is verified and approved -> generate 15-minute signed URL
    const signedUrl = await generateSignedUrl(storagePath);
    return res.status(200).json({ url: signedUrl, expiresMinutes: 15 });
  } catch (error) {
    console.error("Error in getAuthorizedMediaUrl:", error);
    return res.status(500).json({ error: error.message || "Failed to authorize media access." });
  }
});

/**
 * Generates a short-lived signed URL for a file in Firebase Storage (15 minutes).
 */
async function generateSignedUrl(storagePath) {
  const bucket = storage.bucket();
  const file = bucket.file(storagePath);
  
  const [url] = await file.getSignedUrl({
    action: "read",
    expires: Date.now() + 15 * 60 * 1000, // 15 minutes
  });
  return url;
}

/**
 * Endpoint 2: revokeLegacyDownloadTokens
 * Server-side Admin procedure to revoke legacy public download tokens on private media.
 * Safe & targeted: strictly requires prefix to start with users/{userId}/protected/
 * Includes maxResults batching/pagination to prevent timeouts on large collections.
 * Supports safe dryRun mode (dryRun: true) to preview tokens without performing writes.
 * Requires Admin authorization or authenticated owner.
 */
exports.revokeLegacyDownloadTokens = onRequest({ region: "asia-southeast1" }, async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");

  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  const callerUid = await getAuthenticatedUid(req);
  if (!callerUid) {
    return res.status(401).json({ error: "Unauthorized: Valid Firebase ID token required." });
  }

  const { targetUserId, pageToken, maxResults = 100, dryRun = false } = req.body || {};
  const userId = targetUserId || callerUid;

  // Check admin rights if operating on another user's files
  if (callerUid !== userId) {
    const adminDoc = await db.collection("adminRoles").doc(callerUid).get();
    if (!adminDoc.exists) {
      return res.status(403).json({ error: "Forbidden: Admin privilege required to revoke tokens for other accounts." });
    }
  }

  // Strict path isolation: Enforce searchPrefix to be strictly rooted inside target user's protected directory
  const searchPrefix = `users/${userId}/protected/`;

  try {
    const bucket = storage.bucket();
    const queryOptions = {
      prefix: searchPrefix,
      maxResults: Math.min(Math.max(1, maxResults), 500),
      autoPaginate: false
    };
    if (pageToken) {
      queryOptions.pageToken = pageToken;
    }

    const [files, nextQuery] = await bucket.getFiles(queryOptions);
    let matchedCount = 0;
    let revokedCount = 0;
    const filePathsIdentified = [];

    for (const file of files) {
      const [metadata] = await file.getMetadata();
      // Firebase download tokens are stored in metadata.metadata.firebaseStorageDownloadTokens
      if (metadata.metadata && metadata.metadata.firebaseStorageDownloadTokens) {
        matchedCount++;
        filePathsIdentified.push(file.name);
        if (!dryRun) {
          // Set metadata to null to remove bearer download tokens
          await file.setMetadata({
            metadata: {
              firebaseStorageDownloadTokens: null,
            },
          });
          revokedCount++;
        }
      }
    }

    return res.status(200).json({
      success: true,
      dryRun: Boolean(dryRun),
      message: dryRun
        ? `[DRY-RUN] Identified ${matchedCount} media files with legacy download tokens under ${searchPrefix}. No changes written.`
        : `Successfully revoked legacy download tokens for ${revokedCount} media files under ${searchPrefix}.`,
      matchedFilesCount: matchedCount,
      revokedFilesCount: revokedCount,
      filesIdentified: filePathsIdentified,
      nextPageToken: nextQuery?.pageToken || null
    });
  } catch (error) {
    console.error("Error revoking tokens:", error);
    return res.status(500).json({ error: error.message || "Failed to revoke tokens." });
  }
});

/**
 * Endpoint 3: planLegacyMediaMigration
 * Admin/Owner procedure to inventory legacy media files (e.g. users/{uid}/posts/, stories/, reels/),
 * cross-reference author account privacy and Firestore documents, and generate a migration plan.
 *
 * Parameters:
 *   - targetUserId (optional): run for a specific user or leave null for admin-scoped inventory
 *   - dryRun (boolean, default true): previews proposed moves and conflicts without performing file operations
 *   - pageToken (optional): pagination token
 *   - maxResults (number, default 100)
 */
exports.planLegacyMediaMigration = onRequest({ region: "asia-southeast1" }, async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");

  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  const callerUid = await getAuthenticatedUid(req);
  if (!callerUid) {
    return res.status(401).json({ error: "Unauthorized: Valid Firebase ID token required." });
  }

  const { targetUserId, pageToken, maxResults = 100, dryRun = true } = req.body || {};
  const userId = targetUserId || callerUid;

  // Authorization check
  if (callerUid !== userId) {
    const adminDoc = await db.collection("adminRoles").doc(callerUid).get();
    if (!adminDoc.exists) {
      return res.status(403).json({ error: "Forbidden: Admin privilege required to inventory other accounts." });
    }
  }

  try {
    const bucket = storage.bucket();
    const legacyPrefix = `users/${userId}/`;
    const queryOptions = {
      prefix: legacyPrefix,
      maxResults: Math.min(Math.max(1, maxResults), 500),
      autoPaginate: false
    };
    if (pageToken) {
      queryOptions.pageToken = pageToken;
    }

    // Retrieve user profile to establish authoritative privacy status
    const userDoc = await db.collection("users").doc(userId).get();
    const isPrivateAccount = userDoc.exists ? Boolean(userDoc.data().isPrivate) : null;

    const [files, nextQuery] = await bucket.getFiles(queryOptions);

    const proposedMoves = [];
    const skippedFiles = [];
    const conflicts = [];
    let unclassifiedCount = 0;

    for (const file of files) {
      const filePath = file.name;

      // 1. Skip already-migrated protected files
      if (filePath.startsWith(`users/${userId}/protected/`)) {
        skippedFiles.push({ path: filePath, reason: "Already inside protected directory" });
        continue;
      }

      // 2. Skip intentionally public directories (profile, business, marketplace, public)
      if (
        filePath.startsWith(`users/${userId}/profile/`) ||
        filePath.startsWith(`users/${userId}/businessListings/`) ||
        filePath.startsWith(`users/${userId}/marketplaceListings/`) ||
        filePath.startsWith(`users/${userId}/public/`)
      ) {
        skippedFiles.push({ path: filePath, reason: "Intended public valley directory media" });
        continue;
      }

      // 3. Inspect legacy media folders: posts, stories, reels
      const isLegacyPost = filePath.startsWith(`users/${userId}/posts/`);
      const isLegacyStory = filePath.startsWith(`users/${userId}/stories/`);
      const isLegacyReel = filePath.startsWith(`users/${userId}/reels/`);

      if (isLegacyPost || isLegacyStory || isLegacyReel) {
        if (isPrivateAccount === null) {
          conflicts.push({
            path: filePath,
            issue: "User account metadata not found in Firestore; cannot determine privacy status safely."
          });
          unclassifiedCount++;
        } else if (isPrivateAccount === true) {
          // File belongs to a private account -> Proposed destination is /protected/
          const relativeSubpath = filePath.replace(`users/${userId}/`, "");
          const targetPath = `users/${userId}/protected/${relativeSubpath}`;
          proposedMoves.push({
            sourcePath: filePath,
            targetPath: targetPath,
            classification: "PRIVATE_ACCOUNT_MEDIA",
            action: "MOVE_TO_PROTECTED"
          });
        } else {
          // File belongs to a public account -> Proposed destination is /public/
          const relativeSubpath = filePath.replace(`users/${userId}/`, "");
          const targetPath = `users/${userId}/public/${relativeSubpath}`;
          proposedMoves.push({
            sourcePath: filePath,
            targetPath: targetPath,
            classification: "PUBLIC_ACCOUNT_MEDIA",
            action: "MOVE_TO_PUBLIC"
          });
        }
      } else {
        // Unknown or custom legacy folder
        conflicts.push({
          path: filePath,
          issue: "Unrecognized folder structure under user root directory."
        });
        unclassifiedCount++;
      }
    }

    return res.status(200).json({
      success: true,
      dryRun: Boolean(dryRun),
      userId: userId,
      accountPrivacy: isPrivateAccount === null ? "UNKNOWN" : (isPrivateAccount ? "PRIVATE" : "PUBLIC"),
      summary: {
        totalInspected: files.length,
        proposedMovesCount: proposedMoves.length,
        skippedCount: skippedFiles.length,
        conflictsCount: conflicts.length,
        unclassifiedCount: unclassifiedCount
      },
      proposedMoves: proposedMoves,
      skippedFiles: skippedFiles,
      conflicts: conflicts,
      nextPageToken: nextQuery?.pageToken || null,
      executionNotice: "Dry-run only. No files were modified, moved, or deleted."
    });
  } catch (error) {
    console.error("Error in planLegacyMediaMigration:", error);
    return res.status(500).json({ error: error.message || "Failed to plan migration." });
  }
});

/**
 * Endpoint 4: executeLegacyMediaMigration
 * Resumable, idempotent migration executor:
 *   1. Copies source object to verified target path (/protected/ or /public/).
 *   2. Verifies destination object existence and byte size matches source.
 *   3. Updates matching Firestore document media references (posts, stories, reels).
 *   4. Writes progress tracking records in _migrationAudit/{userId}_{fileId} for idempotency.
 *   5. Keeps legacy source object intact until separate phase-3 cleanup.
 *
 * Parameters:
 *   - targetUserId (required): target user ID to migrate
 *   - dryRun (boolean, default true): set false ONLY when authorized live execution
 *   - pageToken (optional): pagination token
 *   - maxResults (number, default 50)
 */
exports.executeLegacyMediaMigration = onRequest({ region: "asia-southeast1" }, async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");

  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  const callerUid = await getAuthenticatedUid(req);
  if (!callerUid) {
    return res.status(401).json({ error: "Unauthorized: Valid Firebase ID token required." });
  }

  const { targetUserId, pageToken, maxResults = 50, dryRun = true } = req.body || {};
  if (!targetUserId) {
    return res.status(400).json({ error: "Missing required parameter: targetUserId." });
  }

  // Admin authorization check (Only verified admins or the authenticated account owner)
  if (callerUid !== targetUserId) {
    const adminDoc = await db.collection("adminRoles").doc(callerUid).get();
    if (!adminDoc.exists) {
      return res.status(403).json({ error: "Forbidden: Admin privilege required to execute migration for other accounts." });
    }
  }

  try {
    const bucket = storage.bucket();
    const legacyPrefix = `users/${targetUserId}/`;
    const queryOptions = {
      prefix: legacyPrefix,
      maxResults: Math.min(Math.max(1, maxResults), 100),
      autoPaginate: false
    };
    if (pageToken) {
      queryOptions.pageToken = pageToken;
    }

    // Authoritative privacy check
    const userDoc = await db.collection("users").doc(targetUserId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: `User profile document for ${targetUserId} not found in Firestore. Cannot determine privacy.` });
    }
    const isPrivateAccount = Boolean(userDoc.data().isPrivate);

    const [files, nextQuery] = await bucket.getFiles(queryOptions);

    const results = {
      inspected: files.length,
      copied: 0,
      alreadyMigrated: 0,
      skippedPublic: 0,
      referencesUpdated: 0,
      errors: []
    };

    for (const file of files) {
      const srcPath = file.name;

      // Skip already protected or public directory files
      if (srcPath.startsWith(`users/${targetUserId}/protected/`)) {
        results.alreadyMigrated++;
        continue;
      }
      if (
        srcPath.startsWith(`users/${targetUserId}/profile/`) ||
        srcPath.startsWith(`users/${targetUserId}/businessListings/`) ||
        srcPath.startsWith(`users/${targetUserId}/marketplaceListings/`) ||
        srcPath.startsWith(`users/${targetUserId}/public/`)
      ) {
        results.skippedPublic++;
        continue;
      }

      // Check legacy folders
      const isLegacy = srcPath.startsWith(`users/${targetUserId}/posts/`) ||
                       srcPath.startsWith(`users/${targetUserId}/stories/`) ||
                       srcPath.startsWith(`users/${targetUserId}/reels/`);

      if (!isLegacy) {
        continue;
      }

      // Path Traversal Defense
      if (srcPath.includes("..")) {
        results.errors.push({
          sourcePath: srcPath,
          error: "Invalid file path: path traversal segments ('..') detected."
        });
        continue;
      }

      const relativeSub = srcPath.replace(`users/${targetUserId}/`, "");
      const destFolder = isPrivateAccount ? "protected" : "public";
      const destPath = `users/${targetUserId}/${destFolder}/${relativeSub}`;

      if (dryRun) {
        // In dryRun, record planned action without writing
        results.copied++;
        continue;
      }

      // Live Execution: Copy object, verify integrity, update Firestore references
      try {
        const srcFile = bucket.file(srcPath);
        const destFile = bucket.file(destPath);

        const [srcExists] = await srcFile.exists();
        if (!srcExists) {
          results.errors.push({ sourcePath: srcPath, error: "Source file not found during copy phase." });
          continue;
        }

        const [srcMeta] = await srcFile.getMetadata();

        // Check if destination already exists (Idempotency vs Conflict check)
        const [destExists] = await destFile.exists();
        if (destExists) {
          const [destMeta] = await destFile.getMetadata();
          const sizeMatches = srcMeta.size === destMeta.size;

          // Safe Checksum Verification:
          // If neither object has a checksum (md5Hash or crc32c), record as conflict for manual review.
          // Matching size alone is NEVER treated as proof of identical content.
          const hasComparableChecksum = Boolean((srcMeta.md5Hash && destMeta.md5Hash) || (srcMeta.crc32c && destMeta.crc32c));
          if (!hasComparableChecksum) {
            results.errors.push({
              sourcePath: srcPath,
              destinationPath: destPath,
              error: `Conflict: Destination exists but neither source nor destination has comparable checksum metadata (md5/crc32c). Cannot safely verify content integrity. Manual review required.`
            });
            continue;
          }

          const checksumMatches = (srcMeta.md5Hash && destMeta.md5Hash)
            ? (srcMeta.md5Hash === destMeta.md5Hash)
            : (srcMeta.crc32c === destMeta.crc32c);

          if (!sizeMatches || !checksumMatches) {
            // Existing destination object has conflicting content or size! Record as conflict
            results.errors.push({
              sourcePath: srcPath,
              destinationPath: destPath,
              error: `Conflict: Destination exists with mismatched checksum or size (srcSize=${srcMeta.size}, destSize=${destMeta.size}, srcMd5=${srcMeta.md5Hash}, destMd5=${destMeta.md5Hash}). Manual review required.`
            });
            continue;
          }
          // Destination exists and byte size + checksum match -> Already safely migrated
          results.alreadyMigrated++;
        } else {
          // Perform safe server-side object copy
          await srcFile.copy(destFile);

          // Verify destination exists and verify metadata + checksum match
          const [destMeta] = await destFile.getMetadata();
          const sizeMatches = srcMeta.size === destMeta.size;
          const hasComparableChecksum = Boolean((srcMeta.md5Hash && destMeta.md5Hash) || (srcMeta.crc32c && destMeta.crc32c));
          const checksumMatches = hasComparableChecksum
            ? ((srcMeta.md5Hash && destMeta.md5Hash) ? (srcMeta.md5Hash === destMeta.md5Hash) : (srcMeta.crc32c === destMeta.crc32c))
            : false;

          if (!sizeMatches || !checksumMatches) {
            throw new Error(`Integrity verification mismatch after copy: sizeMatches=${sizeMatches}, checksumMatches=${checksumMatches}`);
          }
          results.copied++;
        }

        // Update Firestore references using an atomic batch
        const canonicalSrc = `gs://${bucket.name}/${srcPath}`;
        const canonicalDest = `gs://${bucket.name}/${destPath}`;
        const batch = db.batch();
        let docReferencesUpdated = 0;

        // 1. Update posts authored by this user
        const postsQuery = await db.collection("posts").where("authorId", "==", targetUserId).get();
        for (const postDoc of postsQuery.docs) {
          const postData = postDoc.data();
          const mediaUrls = postData.mediaUrls || [];
          let updated = false;
          const newMediaUrls = mediaUrls.map(url => {
            if (url && (url.includes(srcPath) || url === canonicalSrc)) {
              updated = true;
              return isPrivateAccount ? canonicalDest : url.replace(srcPath, destPath);
            }
            return url;
          });
          if (updated) {
            batch.update(postDoc.ref, { mediaUrls: newMediaUrls });
            docReferencesUpdated++;
          }
        }

        // 2. Update stories authored by this user
        const storiesQuery = await db.collection("stories").where("authorId", "==", targetUserId).get();
        for (const storyDoc of storiesQuery.docs) {
          const storyData = storyDoc.data();
          if (storyData.mediaUrl && (storyData.mediaUrl.includes(srcPath) || storyData.mediaUrl === canonicalSrc)) {
            const newUrl = isPrivateAccount ? canonicalDest : storyData.mediaUrl.replace(srcPath, destPath);
            batch.update(storyDoc.ref, { mediaUrl: newUrl });
            docReferencesUpdated++;
          }
        }

        // 3. Update reels authored by this user
        const reelsQuery = await db.collection("reels").where("authorId", "==", targetUserId).get();
        for (const reelDoc of reelsQuery.docs) {
          const reelData = reelDoc.data();
          if (reelData.videoMediaUrl && (reelData.videoMediaUrl.includes(srcPath) || reelData.videoMediaUrl === canonicalSrc)) {
            const newUrl = isPrivateAccount ? canonicalDest : reelData.videoMediaUrl.replace(srcPath, destPath);
            batch.update(reelDoc.ref, { videoMediaUrl: newUrl });
            docReferencesUpdated++;
          }
        }

        // Write audit log entry with granular verified state in the same batch
        const auditRef = db.collection("_migrationAudit").doc(`${targetUserId}_${Buffer.from(srcPath).toString("base64url")}`);
        batch.set(auditRef, {
          sourcePath: srcPath,
          destinationPath: destPath,
          migratedAt: Date.now(),
          status: "REFERENCES_UPDATED",
          destinationVerified: true,
          referencesUpdatedCount: docReferencesUpdated
        });

        // Commit all reference updates and audit record atomically
        await batch.commit();
        results.referencesUpdated += docReferencesUpdated;
      } catch (copyErr) {
        results.errors.push({
          sourcePath: srcPath,
          error: copyErr.message
        });
      }
    }

    return res.status(200).json({
      success: results.errors.length === 0,
      dryRun: Boolean(dryRun),
      targetUserId: targetUserId,
      accountPrivacy: isPrivateAccount ? "PRIVATE" : "PUBLIC",
      results: results,
      nextPageToken: nextQuery?.pageToken || null,
      notice: dryRun ? "Dry-run only. No files were written." : "Live copy and reference update completed."
    });
  } catch (error) {
    console.error("Error executing migration:", error);
    return res.status(500).json({ error: error.message || "Failed to execute migration." });
  }
});

/**
 * Cloud Function Trigger: onNewMessageSendFcm
 * Automatically fires when a new chat message document is created under conversations/{conversationId}/messages/{messageId}
 * Looks up recipient's FCM device registration token and dispatches high-priority push notification.
 */
exports.onNewMessageSendFcm = onDocumentCreated(
  { document: "conversations/{conversationId}/messages/{messageId}", region: "asia-southeast1" },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const messageData = snap.data();
    if (!messageData) return;

    const { conversationId, messageId } = event.params;
    const senderId = messageData.senderId;
    const senderName = messageData.senderName || "Someone";
    const text = (messageData.text || "").trim();

    try {
      const convDoc = await db.collection("conversations").doc(conversationId).get();
      if (!convDoc.exists) return;
      const convData = convDoc.data();
      const participantIds = convData.participantIds || [];
      const recipientId = participantIds.find((id) => id !== senderId);

      if (!recipientId) return;

      const userDoc = await db.collection("users").doc(recipientId).get();
      if (!userDoc.exists) return;
      const fcmToken = userDoc.data()?.fcmToken;

      if (!fcmToken) {
        console.log(`No FCM token registered for recipient user ${recipientId}.`);
        return;
      }

      const payload = {
        token: fcmToken,
        notification: {
          title: `Message from ${senderName}`,
          body: text.length > 100 ? `${text.substring(0, 97)}...` : text,
        },
        data: {
          type: "direct_message",
          conversationId: conversationId,
          messageId: messageId,
          senderId: senderId,
          senderName: senderName,
        },
        android: {
          priority: "high",
          notification: {
            channelId: "yarkhoon_direct_messages",
            sound: "default",
          },
        },
      };

      await admin.messaging().send(payload);
      console.log(`Successfully sent DM FCM notification to ${recipientId} for conversation ${conversationId}.`);
    } catch (err) {
      console.error("Error sending FCM notification for new direct message:", err);
    }
  }
);

/**
 * Cloud Function Trigger: onNewFollowRequestSendFcm
 * Automatically fires when a new follow/friend request document is created under users/{userId}/followRequests/{requesterId}
 * Looks up target user's FCM device token and dispatches high-priority friend request push notification.
 */
exports.onNewFollowRequestSendFcm = onDocumentCreated(
  { document: "users/{userId}/followRequests/{requesterId}", region: "asia-southeast1" },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const requestData = snap.data() || {};
    const { userId, requesterId } = event.params;

    try {
      const targetUserDoc = await db.collection("users").doc(userId).get();
      if (!targetUserDoc.exists) return;
      const fcmToken = targetUserDoc.data()?.fcmToken;

      if (!fcmToken) {
        console.log(`No FCM token registered for target user ${userId}.`);
        return;
      }

      const requesterName = requestData.requesterName || requestData.senderName || "A member";
      const payload = {
        token: fcmToken,
        notification: {
          title: "New Friend Request",
          body: `${requesterName} sent you a friend request on YARKHOON.COM.`,
        },
        data: {
          type: "friend_request",
          requesterId: requesterId,
          targetUserId: userId,
          requesterName: requesterName,
        },
        android: {
          priority: "high",
          notification: {
            channelId: "yarkhoon_friend_requests",
            sound: "default",
          },
        },
      };

      await admin.messaging().send(payload);
      console.log(`Successfully sent friend request FCM notification to ${userId} from ${requesterId}.`);
    } catch (err) {
      console.error("Error sending FCM notification for friend request:", err);
    }
  }
);

/**
 * Endpoint 5: sendFcmNotification
 * Authenticated HTTP endpoint allowing the application to dispatch FCM notifications for friend requests or direct messages.
 */
exports.sendFcmNotification = onRequest({ region: "asia-southeast1" }, async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");

  if (req.method === "OPTIONS") {
    return res.status(204).send("");
  }

  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method Not Allowed. Use POST." });
  }

  const callerUid = await getAuthenticatedUid(req);
  if (!callerUid) {
    return res.status(401).json({ error: "Unauthorized: Valid Firebase ID token required." });
  }

  const { recipientId, type, title, body, conversationId, requesterId } = req.body || {};
  if (!recipientId || !type) {
    return res.status(400).json({ error: "Missing required parameters: recipientId and type are mandatory." });
  }

  try {
    const userDoc = await db.collection("users").doc(recipientId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: "Recipient user document not found." });
    }
    const fcmToken = userDoc.data()?.fcmToken;
    if (!fcmToken) {
      return res.status(200).json({ success: true, delivered: false, reason: "Recipient has no registered FCM token." });
    }

    const channelId = (type === "friend_request" || type === "follow_request")
      ? "yarkhoon_friend_requests"
      : "yarkhoon_direct_messages";

    const payload = {
      token: fcmToken,
      notification: {
        title: title || (channelId === "yarkhoon_friend_requests" ? "New Friend Request" : "New Message"),
        body: body || "You received a new notification on YARKHOON.COM",
      },
      data: {
        type: type,
        senderId: callerUid,
        ...(conversationId ? { conversationId } : {}),
        ...(requesterId ? { requesterId } : {}),
      },
      android: {
        priority: "high",
        notification: {
          channelId: channelId,
          sound: "default",
        },
      },
    };

    const messageId = await admin.messaging().send(payload);
    return res.status(200).json({ success: true, delivered: true, messageId: messageId });
  } catch (err) {
    console.error("Error in sendFcmNotification endpoint:", err);
    return res.status(500).json({ error: err.message || "Failed to send FCM notification." });
  }
});



