package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val notificationRepository: FirestoreNotificationRepository = FirestoreNotificationRepository(firestore, auth)
) {
    companion object {
        const val USERS_COLLECTION = "users"
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    private fun parseUserDocument(doc: com.google.firebase.firestore.DocumentSnapshot): User {
        val data = doc.data ?: return User(id = doc.id, name = "Member", handle = "@${doc.id.take(8)}", avatarUrl = "")
        val rawCreatedAt = data["createdAt"]
        val parsedCreatedAt: Long = when (rawCreatedAt) {
            is Number -> rawCreatedAt.toLong()
            is com.google.firebase.Timestamp -> rawCreatedAt.toDate().time
            is String -> rawCreatedAt.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }

        return User(
            id = doc.id,
            name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: "Member",
            handle = (data["username"] as? String)
                ?: (data["handle"] as? String)
                ?: "@${doc.id.take(8)}",
            email = (data["email"] as? String).orEmpty(),
            phoneNumber = (data["phoneNumber"] as? String).orEmpty(),
            isBusinessAccount = (data["isBusinessAccount"] as? Boolean)
                ?: ((data["accountType"] as? String) == "business"),
            businessCategory = data["businessCategory"] as? String,
            jobTitle = (data["profession"] as? String)
                ?: (data["jobTitle"] as? String).orEmpty(),
            education = (data["education"] as? String).orEmpty(),
            location = (data["location"] as? String).orEmpty(),
            bio = (data["bio"] as? String).orEmpty(),
            interests = (data["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            avatarUrl = (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
            coverUrl = data["coverUrl"] as? String,
            isVerified = (data["isVerified"] as? Boolean) ?: false,
            followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
            followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
            postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
            isPrivate = (data["isPrivate"] as? Boolean) ?: false,
            createdAt = parsedCreatedAt
        )
    }

    suspend fun getUserProfile(uid: String): Result<User?> {
        if (uid.isBlank()) return Result.success(null)
        return try {
            val docRef = firestore.collection(USERS_COLLECTION).document(uid)
            val doc = docRef.get().await()
            val targetDoc = if (doc.exists()) {
                doc
            } else {
                // If not found by document ID, try finding user by username / handle in Firestore
                val cleanHandle = uid.removePrefix("@")
                val queryByHandle = try {
                    val qWithAt = firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("username", "@$cleanHandle")
                        .limit(1)
                        .get()
                        .await()
                    qWithAt.documents.firstOrNull() ?: firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("username", cleanHandle)
                        .limit(1)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                } catch (_: Exception) {
                    null
                }
                queryByHandle
            }

            if (targetDoc == null || !targetDoc.exists()) {
                // Check sample community creators fallback for dummy accounts not yet registered in Firestore
                val dummy = SampleData.users.find {
                    it.id == uid || it.handle.equals(uid, ignoreCase = true) || it.handle.removePrefix("@").equals(uid.removePrefix("@"), ignoreCase = true)
                }
                return Result.success(dummy)
            }

            val parsed = parseUserDocument(targetDoc)
            // Query real relationship subcollections for accurate followers/following counts
            val realFollowers = try {
                firestore.collection(USERS_COLLECTION).document(targetDoc.id).collection("followers").get().await().size()
            } catch (_: Exception) { 0 }
            val realFollowing = try {
                firestore.collection(USERS_COLLECTION).document(targetDoc.id).collection("following").get().await().size()
            } catch (_: Exception) { 0 }
            val realPosts = try {
                firestore.collection(FirestorePostRepository.POSTS_COLLECTION).whereEqualTo("authorId", targetDoc.id).get().await().size()
            } catch (_: Exception) { 0 }

            val enriched = parsed.copy(
                followersCount = if (realFollowers > 0) realFollowers else parsed.followersCount,
                followingCount = if (realFollowing > 0) realFollowing else parsed.followingCount,
                postsCount = if (realPosts > 0) realPosts else parsed.postsCount
            )
            Result.success(enriched)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> {
        val cleanEmail = FirestoreAdminRepository.resolveCanonicalEmail(email)
        if (cleanEmail.isBlank()) return Result.success(null)
        return try {
            val exactQuery = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", cleanEmail)
                .limit(5)
                .get()
                .await()

            val matchedDoc = exactQuery.documents.firstOrNull() ?: run {
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .limit(100)
                    .get()
                    .await()
                snapshot.documents.firstOrNull { doc ->
                    val docEmail = FirestoreAdminRepository.resolveCanonicalEmail(doc.getString("email"))
                    docEmail == cleanEmail
                }
            }

            if (matchedDoc != null && matchedDoc.exists()) {
                Result.success(parseUserDocument(matchedDoc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun searchRegisteredUsers(currentUserId: String, query: String = "", limit: Long = 40): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .limit(limit)
                .get()
                .await()

            val q = query.trim().lowercase()
            val users = snapshot.documents.mapNotNull { doc ->
                if (doc.id == currentUserId) return@mapNotNull null
                parseUserDocument(doc)
            }.filter { user ->
                if (q.isBlank()) true
                else user.name.lowercase().contains(q) ||
                     user.handle.lowercase().contains(q) ||
                     user.jobTitle.lowercase().contains(q) ||
                     user.location.lowercase().contains(q)
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION).document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFcmToken(uid: String): Result<String?> {
        return try {
            val doc = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val token = doc.getString("fcmToken")?.takeIf { it.isNotBlank() }
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun saveUserProfile(user: User): Result<Unit> {
        val authUid = currentAuthUid
        if (authUid == null) {
            return Result.failure(IllegalStateException("No authenticated Firebase user found. Please sign in."))
        }
        if (authUid != user.id) {
            return Result.failure(SecurityException("Unauthorized: Cannot write profile data for another user."))
        }

        return try {
            val userDocRef = firestore.collection(USERS_COLLECTION).document(user.id)
            val docSnapshot = userDocRef.get().await()

            if (!docSnapshot.exists()) {
                // Initial creation conforming strictly to security rules (zero counters, unverified, unsuspended)
                val initialData = hashMapOf<String, Any?>(
                    "uid" to user.id,
                    "name" to user.name,
                    "username" to user.handle,
                    "email" to user.email,
                    "phoneNumber" to user.phoneNumber,
                    "accountType" to if (user.isBusinessAccount) "business" else "personal",
                    "isBusinessAccount" to user.isBusinessAccount,
                    "businessCategory" to user.businessCategory,
                    "profession" to user.jobTitle,
                    "jobTitle" to user.jobTitle,
                    "education" to user.education,
                    "location" to user.location,
                    "bio" to user.bio,
                    "interests" to user.interests,
                    "avatarUrl" to user.avatarUrl,
                    "coverUrl" to user.coverUrl,
                    "isVerified" to false,
                    "isSuspended" to false,
                    "followersCount" to 0,
                    "followingCount" to 0,
                    "postsCount" to 0,
                    "isPrivate" to user.isPrivate,
                    "createdAt" to System.currentTimeMillis()
                )
                userDocRef.set(initialData).await()
            } else {
                // Update existing user profile using only the whitelisted profile fields
                val updates = hashMapOf<String, Any?>(
                    "name" to user.name,
                    "username" to user.handle,
                    "email" to user.email,
                    "phoneNumber" to user.phoneNumber,
                    "accountType" to if (user.isBusinessAccount) "business" else "personal",
                    "isBusinessAccount" to user.isBusinessAccount,
                    "businessCategory" to user.businessCategory,
                    "profession" to user.jobTitle,
                    "jobTitle" to user.jobTitle,
                    "education" to user.education,
                    "location" to user.location,
                    "bio" to user.bio,
                    "interests" to user.interests,
                    "avatarUrl" to user.avatarUrl,
                    "coverUrl" to user.coverUrl,
                    "isPrivate" to user.isPrivate,
                    "updatedAt" to System.currentTimeMillis()
                )
                userDocRef.update(updates).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun updateUserFields(uid: String, updates: Map<String, Any?>): Result<Unit> {
        val authUid = currentAuthUid
        if (authUid == null) {
            return Result.failure(IllegalStateException("No authenticated Firebase user found. Please sign in."))
        }
        if (authUid != uid) {
            return Result.failure(SecurityException("Unauthorized: Cannot update profile data for another user."))
        }

        return try {
            val cleanUpdates = updates.filterValues { it != null }
            if (cleanUpdates.isNotEmpty()) {
                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .update(cleanUpdates)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun toggleFollowUser(currentUserId: String, targetUserId: String, isFollowingNow: Boolean): Result<Unit> {
        if (currentUserId.isBlank() || targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("User IDs must not be blank."))
        }
        if (currentUserId == targetUserId) {
            return Result.failure(IllegalArgumentException("You cannot follow your own account."))
        }

        return try {
            val followingRef = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)

            val followersRef = firestore.collection(USERS_COLLECTION)
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)

            val currentUserRef = firestore.collection(USERS_COLLECTION).document(currentUserId)

            if (isFollowingNow) {
                // Follow: Create relationship documents in approved following/followers subcollections
                followingRef.set(mapOf("followedAt" to System.currentTimeMillis())).await()
                followersRef.set(mapOf("followedAt" to System.currentTimeMillis())).await()

                // Send in-app notification to target user
                try {
                    val currentDoc = currentUserRef.get().await()
                    val myName = currentDoc.getString("name") ?: "A member"
                    val myHandle = currentDoc.getString("username") ?: "@$currentUserId"
                    val myAvatar = currentDoc.getString("avatarUrl").orEmpty()
                    val sender = User(id = currentUserId, name = myName, handle = myHandle, avatarUrl = myAvatar)
                    notificationRepository.sendFollowNotification(targetUserId, sender)
                } catch (_: Exception) {}
            } else {
                // Unfollow: Remove relationship documents
                followingRef.delete().await()
                followersRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getFollowingUserIds(currentUserId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("following")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    // ---------------- Stage 12: Account Privacy & Follow Requests ----------------

    suspend fun updateAccountPrivacy(userId: String, isPrivate: Boolean): Result<Unit> {
        val authUid = currentAuthUid ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (authUid != userId) return Result.failure(SecurityException("Unauthorized"))
        return try {
            firestore.collection(USERS_COLLECTION).document(userId)
                .update("isPrivate", isPrivate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun sendFollowRequest(requester: User, targetUserId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val requestData = hashMapOf<String, Any?>(
                "id" to requester.id,
                "requesterId" to requester.id,
                "requesterName" to requester.name,
                "requesterHandle" to requester.handle,
                "requesterAvatarUrl" to requester.avatarUrl,
                "targetUserId" to targetUserId,
                "status" to "pending",
                "timestamp" to now
            )

            // Save in target user's followRequests subcollection
            firestore.collection(USERS_COLLECTION)
                .document(targetUserId)
                .collection("followRequests")
                .document(requester.id)
                .set(requestData)
                .await()

            // Save in requester's sentFollowRequests subcollection
            firestore.collection(USERS_COLLECTION)
                .document(requester.id)
                .collection("sentFollowRequests")
                .document(targetUserId)
                .set(mapOf("targetUserId" to targetUserId, "timestamp" to now))
                .await()

            // Create notification for target user
            try {
                notificationRepository.sendFollowRequestNotification(targetUserId, requester)
                FcmNotificationDispatcher.dispatchFriendRequestNotification(firestore, requester, targetUserId)
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun cancelFollowRequest(requesterId: String, targetUserId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(targetUserId)
                .collection("followRequests")
                .document(requesterId)
                .delete()
                .await()

            firestore.collection(USERS_COLLECTION)
                .document(requesterId)
                .collection("sentFollowRequests")
                .document(targetUserId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun approveFollowRequest(owner: User, requester: User): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val ownerId = owner.id
            val requesterId = requester.id

            // Remove follow request records
            firestore.collection(USERS_COLLECTION).document(ownerId)
                .collection("followRequests").document(requesterId).delete().await()

            firestore.collection(USERS_COLLECTION).document(requesterId)
                .collection("sentFollowRequests").document(ownerId).delete().await()

            // Establish reciprocal follower/following documents
            firestore.collection(USERS_COLLECTION).document(requesterId)
                .collection("following").document(ownerId).set(mapOf("followedAt" to now)).await()

            firestore.collection(USERS_COLLECTION).document(ownerId)
                .collection("followers").document(requesterId).set(mapOf("followedAt" to now)).await()

            // Update counters
            try {
                firestore.collection(USERS_COLLECTION).document(ownerId)
                    .update("followersCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
                firestore.collection(USERS_COLLECTION).document(requesterId)
                    .update("followingCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
            } catch (_: Exception) {}

            // Send approval notification
            try {
                notificationRepository.sendFollowApprovalNotification(requesterId, owner)
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun rejectFollowRequest(ownerId: String, requesterId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION).document(ownerId)
                .collection("followRequests").document(requesterId).delete().await()

            firestore.collection(USERS_COLLECTION).document(requesterId)
                .collection("sentFollowRequests").document(ownerId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getPendingFollowRequests(ownerId: String): Result<List<FollowRequest>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(ownerId)
                .collection("followRequests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val reqId = data["requesterId"] as? String ?: doc.id
                val reqName = data["requesterName"] as? String ?: "Member"
                val reqHandle = data["requesterHandle"] as? String ?: "@$reqId"
                val reqAvatar = (data["requesterAvatarUrl"] as? String).orEmpty()
                val status = (data["status"] as? String) ?: "pending"
                val time = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()

                FollowRequest(
                    id = doc.id,
                    requester = User(id = reqId, name = reqName, handle = reqHandle, avatarUrl = reqAvatar),
                    targetUserId = ownerId,
                    status = status,
                    timestamp = time
                )
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getSentFollowRequestUserIds(currentUserId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("sentFollowRequests")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    // ---------------- Stage 12: Blocking System ----------------

    suspend fun blockUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            // 1. Add to blocked users
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("blockedUsers")
                .document(targetUserId)
                .set(mapOf("blockedUserId" to targetUserId, "blockedAt" to now))
                .await()

            // 2. Break follow relationship in both directions
            // Current user unfollows target
            firestore.collection(USERS_COLLECTION).document(currentUserId)
                .collection("following").document(targetUserId).delete().await()
            firestore.collection(USERS_COLLECTION).document(targetUserId)
                .collection("followers").document(currentUserId).delete().await()

            // Target user unfollows current user
            firestore.collection(USERS_COLLECTION).document(targetUserId)
                .collection("following").document(currentUserId).delete().await()
            firestore.collection(USERS_COLLECTION).document(currentUserId)
                .collection("followers").document(targetUserId).delete().await()

            // Decrement counters safely
            try {
                firestore.collection(USERS_COLLECTION).document(currentUserId)
                    .update("followingCount", com.google.firebase.firestore.FieldValue.increment(-1)).await()
                firestore.collection(USERS_COLLECTION).document(targetUserId)
                    .update("followersCount", com.google.firebase.firestore.FieldValue.increment(-1)).await()
            } catch (_: Exception) {}

            // 3. Cancel any pending follow requests in both directions
            try {
                firestore.collection(USERS_COLLECTION).document(currentUserId)
                    .collection("followRequests").document(targetUserId).delete().await()
                firestore.collection(USERS_COLLECTION).document(targetUserId)
                    .collection("sentFollowRequests").document(currentUserId).delete().await()

                firestore.collection(USERS_COLLECTION).document(targetUserId)
                    .collection("followRequests").document(currentUserId).delete().await()
                firestore.collection(USERS_COLLECTION).document(currentUserId)
                    .collection("sentFollowRequests").document(targetUserId).delete().await()
            } catch (_: Exception) {}

            // 4. Remove from favorites if present
            try {
                firestore.collection(USERS_COLLECTION).document(currentUserId)
                    .collection("favorites").document(targetUserId).delete().await()
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun unblockUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("blockedUsers")
                .document(targetUserId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getBlockedUserIds(currentUserId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("blockedUsers")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getBlockedUsers(currentUserId: String): Result<List<User>> {
        return try {
            val ids = getBlockedUserIds(currentUserId).getOrDefault(emptySet())
            val users = ids.mapNotNull { id ->
                getUserProfile(id).getOrNull()
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    // ---------------- Stage 12: Muting System ----------------

    suspend fun muteUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("mutedUsers")
                .document(targetUserId)
                .set(mapOf("mutedUserId" to targetUserId, "mutedAt" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun unmuteUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("mutedUsers")
                .document(targetUserId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getMutedUserIds(currentUserId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("mutedUsers")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getMutedUsers(currentUserId: String): Result<List<User>> {
        return try {
            val ids = getMutedUserIds(currentUserId).getOrDefault(emptySet())
            val users = ids.mapNotNull { id ->
                getUserProfile(id).getOrNull()
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    // ---------------- Stage 12: Favorites System ----------------

    suspend fun addFavorite(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("favorites")
                .document(targetUserId)
                .set(mapOf("favoriteUserId" to targetUserId, "addedAt" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun removeFavorite(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("favorites")
                .document(targetUserId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getFavoriteUserIds(currentUserId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection("favorites")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getFavoriteUsers(currentUserId: String): Result<List<User>> {
        return try {
            val ids = getFavoriteUserIds(currentUserId).getOrDefault(emptySet())
            val users = ids.mapNotNull { id ->
                getUserProfile(id).getOrNull()
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private fun mapFirestoreException(e: Exception): Exception {
        if (e is FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied: Access to profile data was not granted by Firestore security rules."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Cloud Firestore service is temporarily unavailable. Please check your internet connection."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Connection timed out while contacting Cloud Firestore."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "User profile document not found."
                FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                    "Profile document already exists."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "User session expired or unauthenticated. Please sign in again."
                else -> e.localizedMessage ?: "A Cloud Firestore error occurred: ${e.code}"
            }
            return Exception(message, e)
        }
        return Exception(e.localizedMessage ?: "An error occurred while communicating with Cloud Firestore.", e)
    }
}
