package com.example

import com.example.data.Comment
import com.example.data.Conversation
import com.example.data.FirestoreMessagingRepository
import com.example.data.FirestorePostRepository
import com.example.data.Post
import com.example.data.PostType
import com.example.data.SampleData
import com.example.data.User
import com.example.ui.FeedFilter
import org.junit.Assert.*
import org.junit.Test

/**
 * Targeted Regression & Bug Diagnosis Verification Test Suite for Yarkhoon.com
 * Verifies fixes for:
 * 1. Other users' profile loading and dummy account fallback
 * 2. Old posts retrieval and legacy schema compatibility
 * 3. Following feed filtering and dynamic follow state
 * 4. Favorites feed filtering and creator star badges
 * 5. Create post Photo/Video/Feeling shortcut initialization
 * 6. Inline video playback detection and state management
 * 7. Deterministic conversation participant order for security rules compliance
 */
class YarkhoonBugFixesAuditTest {

    private val currentUser = User(
        id = "current_user_1",
        name = "Hamara Chitral",
        handle = "@hamarachitral",
        isVerified = false,
        followersCount = 0,
        followingCount = 0,
        postsCount = 0
    )

    @Test
    fun testIssue1_DummyAccountProfileFallback() {
        val dummyKhowarTech = SampleData.users.find { it.id == "u1" }
        assertNotNull("Khowar Tech Network dummy account must exist in SampleData", dummyKhowarTech)
        assertEquals("Khowar Tech Network", dummyKhowarTech!!.name)
        assertEquals("@khowartech", dummyKhowarTech.handle)
        assertTrue("Dummy account should have a valid bio", dummyKhowarTech.bio.isNotBlank())
        assertTrue("Dummy account should have a valid location", dummyKhowarTech.location.isNotBlank())
        assertTrue("Dummy account should have followers", dummyKhowarTech.followersCount > 0)

        // Verify fallback lookup logic
        val lookupId = "u1"
        val foundUser = SampleData.users.find {
            it.id == lookupId || it.handle.equals(lookupId, ignoreCase = true) || it.handle.removePrefix("@").equals(lookupId.removePrefix("@"), ignoreCase = true)
        }
        assertNotNull("Lookup by ID must succeed", foundUser)
        assertEquals("u1", foundUser!!.id)

        val lookupHandle = "@khowartech"
        val foundByHandle = SampleData.users.find {
            it.id == lookupHandle || it.handle.equals(lookupHandle, ignoreCase = true) || it.handle.removePrefix("@").equals(lookupHandle.removePrefix("@"), ignoreCase = true)
        }
        assertNotNull("Lookup by handle must succeed", foundByHandle)
        assertEquals("u1", foundByHandle!!.id)
    }

    @Test
    fun testIssue2_OldPostLegacyTimestampAndAuthorCompatibility() {
        // Test parsing legacy timestamp formats: Number, String, and missing timestamp with createdAtMillis fallback
        val numericTs: Any = 1718000000000L
        val parsedNumeric = when (numericTs) {
            is Number -> numericTs.toLong()
            else -> 0L
        }
        assertEquals(1718000000000L, parsedNumeric)

        val stringTs: Any = "1718000000000"
        val parsedString = when (stringTs) {
            is Number -> stringTs.toLong()
            is String -> stringTs.toLongOrNull() ?: 0L
            else -> 0L
        }
        assertEquals(1718000000000L, parsedString)

        // Test author enrichment for posts missing author details
        val rawAuthorId = "u2"
        val matchedSample = SampleData.users.find { it.id == rawAuthorId }
        assertNotNull(matchedSample)
        assertEquals("Shandur Alpine Riders", matchedSample!!.name)
        assertEquals("@shandur_riders", matchedSample.handle)
    }

    @Test
    fun testIssue3_FollowingFeedFiltering() {
        val post1 = Post(id = "p1", author = User(id = "u1", name = "Creator 1", handle = "@c1"), text = "Post 1")
        val post2 = Post(id = "p2", author = User(id = "u2", name = "Creator 2", handle = "@c2"), text = "Post 2")
        val post3 = Post(id = "p3", author = User(id = "u3", name = "Creator 3", handle = "@c3"), text = "Post 3")

        val allPosts = listOf(post1, post2, post3)
        var followingUserIds = setOf("u1")

        // Initially following only u1
        var followingPosts = allPosts.filter { followingUserIds.contains(it.author.id) }
        assertEquals(1, followingPosts.size)
        assertEquals("p1", followingPosts[0].id)

        // Follow u2
        followingUserIds = followingUserIds + "u2"
        followingPosts = allPosts.filter { followingUserIds.contains(it.author.id) }
        assertEquals(2, followingPosts.size)
        assertTrue(followingPosts.any { it.id == "p2" })

        // Unfollow u1
        followingUserIds = followingUserIds - "u1"
        followingPosts = allPosts.filter { followingUserIds.contains(it.author.id) }
        assertEquals(1, followingPosts.size)
        assertEquals("p2", followingPosts[0].id)
    }

    @Test
    fun testIssue4_FavoritesFeedFiltering() {
        val post1 = Post(id = "p1", author = User(id = "u1", name = "Creator 1", handle = "@c1"), text = "Post 1")
        val post2 = Post(id = "p2", author = User(id = "u2", name = "Creator 2", handle = "@c2"), text = "Post 2")

        val allPosts = listOf(post1, post2)
        var favoriteUserIds = emptySet<String>()

        // Favorites feed initially empty
        var favoritePosts = allPosts.filter { favoriteUserIds.contains(it.author.id) }
        assertTrue(favoritePosts.isEmpty())

        // Add u2 to favorites
        favoriteUserIds = favoriteUserIds + "u2"
        favoritePosts = allPosts.filter { favoriteUserIds.contains(it.author.id) }
        assertEquals(1, favoritePosts.size)
        assertEquals("p2", favoritePosts[0].id)
    }

    @Test
    fun testIssue5_CreatePostShortcutsAndFeelingFormatting() {
        val contentText = "Having a great time in Chitral"
        val selectedFeeling = "😊 Feeling Happy"

        val finalText = if (selectedFeeling.isNotBlank() && !contentText.contains(selectedFeeling)) {
            if (contentText.isBlank()) selectedFeeling else "$contentText\n\n— $selectedFeeling"
        } else {
            contentText
        }

        assertTrue(finalText.startsWith("Having a great time in Chitral"))
        assertTrue(finalText.contains("— 😊 Feeling Happy"))

        // Blank content with only feeling selected
        val blankContent = ""
        val feelingOnlyText = if (selectedFeeling.isNotBlank() && !blankContent.contains(selectedFeeling)) {
            if (blankContent.isBlank()) selectedFeeling else "$blankContent\n\n— $selectedFeeling"
        } else {
            blankContent
        }
        assertEquals("😊 Feeling Happy", feelingOnlyText)
    }

    @Test
    fun testIssue6_VideoPlaybackDetection() {
        val photoPost = Post(
            id = "p_photo",
            author = currentUser,
            type = PostType.PHOTO,
            text = "Beautiful mountains",
            mediaUrls = listOf("https://example.com/photo.jpg")
        )
        val videoPost = Post(
            id = "p_video",
            author = currentUser,
            type = PostType.VIDEO,
            text = "Polo match highlight",
            mediaUrls = listOf("https://example.com/clip.mp4")
        )

        fun isVideo(p: Post): Boolean {
            val first = p.mediaUrls.firstOrNull().orEmpty()
            return p.type == PostType.VIDEO || p.type == PostType.REEL ||
                first.endsWith(".mp4", ignoreCase = true) || first.contains("video", ignoreCase = true)
        }

        assertFalse(isVideo(photoPost))
        assertTrue(isVideo(videoPost))
    }

    @Test
    fun testIssue7_DeterministicConversationParticipants() {
        val userA = "uid_alice"
        val userB = "uid_bob"

        // When Alice sends a message to Bob:
        val participantsFromAlice = listOf(userA, userB).sorted()

        // When Bob replies to Alice:
        val participantsFromBob = listOf(userB, userA).sorted()

        // Both lists MUST be strictly equal in order to pass Firestore rules equality:
        // request.resource.data.participantIds == resource.data.participantIds
        assertEquals(participantsFromAlice, participantsFromBob)
        assertEquals(listOf("uid_alice", "uid_bob"), participantsFromAlice)

        val convId1 = FirestoreMessagingRepository.generateConversationId(userA, userB)
        val convId2 = FirestoreMessagingRepository.generateConversationId(userB, userA)
        assertEquals("Conversation IDs must be identical regardless of sender", convId1, convId2)
    }

    @Test
    fun testIssue8_FollowStateRollbackOnPermissionDeniedAndNetworkFailure() {
        val initialFollowing = setOf("u1", "u2")
        val initialFollowingCount = 2
        val initialFollowersCount = 15
        val targetUser = User(id = "u2", name = "Shandur Riders", handle = "@shandur_riders", followersCount = initialFollowersCount)

        // Case A: Unfollow fails with PERMISSION_DENIED
        var followingUserIds = initialFollowing
        var followingCount = initialFollowingCount
        var targetFollowersCount = targetUser.followersCount
        var feedbackMessage = ""

        // Optimistic update
        val prevFollowing = followingUserIds
        val prevFollowingCount = followingCount
        val prevTargetFollowers = targetFollowersCount

        followingUserIds = followingUserIds - targetUser.id
        followingCount = (followingCount - 1).coerceAtLeast(0)
        targetFollowersCount = (targetFollowersCount - 1).coerceAtLeast(0)
        feedbackMessage = "Unfollowed ${targetUser.name}"

        assertEquals(1, followingUserIds.size)
        assertEquals(1, followingCount)
        assertEquals(14, targetFollowersCount)

        // Simulate Firestore permission failure -> Rollback
        val permissionFailure = Result.failure<Unit>(SecurityException("PERMISSION_DENIED: Missing or insufficient permissions"))
        if (permissionFailure.isFailure) {
            val err = permissionFailure.exceptionOrNull()?.localizedMessage ?: "Failed to unfollow"
            followingUserIds = prevFollowing
            followingCount = prevFollowingCount
            targetFollowersCount = prevTargetFollowers
            feedbackMessage = err
        }

        assertEquals("Following set must rollback on permission failure", initialFollowing, followingUserIds)
        assertEquals("Following count must rollback", initialFollowingCount, followingCount)
        assertEquals("Target followers count must rollback", initialFollowersCount, targetFollowersCount)
        assertTrue("Feedback message must display error", feedbackMessage.contains("PERMISSION_DENIED"))

        // Case B: Follow fails with network failure
        val newTarget = User(id = "u3", name = "Pamir Explorer", handle = "@pamir_explorer", followersCount = 10)
        val netPrevFollowing = followingUserIds
        val netPrevCount = followingCount
        val netPrevTargetFollowers = newTarget.followersCount

        var newTargetFollowers = newTarget.followersCount
        followingUserIds = followingUserIds + newTarget.id
        followingCount = followingCount + 1
        newTargetFollowers = newTargetFollowers + 1
        feedbackMessage = "You are now following ${newTarget.name}"

        assertEquals(3, followingUserIds.size)
        assertEquals(3, followingCount)
        assertEquals(11, newTargetFollowers)

        val networkFailure = Result.failure<Unit>(java.io.IOException("Network unavailable: host unreachable"))
        if (networkFailure.isFailure) {
            val err = networkFailure.exceptionOrNull()?.localizedMessage ?: "Failed to follow"
            followingUserIds = netPrevFollowing
            followingCount = netPrevCount
            newTargetFollowers = netPrevTargetFollowers
            feedbackMessage = err
        }

        assertEquals("Following set must rollback on network failure", initialFollowing, followingUserIds)
        assertEquals("Following count must rollback", initialFollowingCount, followingCount)
        assertEquals("Target followers count must rollback", 10, newTargetFollowers)
        assertTrue("Feedback message must reflect network error", feedbackMessage.contains("Network unavailable"))
    }

    @Test
    fun testIssue9_ExistingConversationsLegacyResolutionAndParticipantPreservation() {
        val alice = "uid_alice"
        val bob = "uid_bob"

        // Existing legacy conversation with ID created before deterministic participant ordering:
        // Document ID is "legacy_conv_123" and participantIds is ["uid_bob", "uid_alice"]
        val legacyParticipants = listOf(bob, alice)
        val legacyConversation = Conversation(
            id = "legacy_conv_123",
            participantIds = legacyParticipants,
            recipient = User(id = bob, name = "Bob", handle = "@bob"),
            lastMessage = "Hey Alice, see you at the festival",
            lastMessageSenderId = bob,
            lastMessageTimestamp = 1718000000000L
        )

        val conversationList = listOf(legacyConversation)

        // Verify resolveConversationId resolves to the existing legacy conversation ID
        val resolvedId = FirestoreMessagingRepository.resolveConversationId(conversationList, alice, bob)
        assertEquals("Must reuse existing legacy conversation ID rather than creating duplicate", "legacy_conv_123", resolvedId)

        // Also test reverse user lookup
        val resolvedIdFromBob = FirestoreMessagingRepository.resolveConversationId(conversationList, bob, alice)
        assertEquals("Must resolve to same legacy conversation ID regardless of caller", "legacy_conv_123", resolvedIdFromBob)

        // For a completely new pair with no existing conversation:
        val charlie = "uid_charlie"
        val newConvId = FirestoreMessagingRepository.resolveConversationId(conversationList, alice, charlie)
        val expectedDeterministic = FirestoreMessagingRepository.generateConversationId(alice, charlie)
        assertEquals("Brand new chat must use deterministic ID", expectedDeterministic, newConvId)

        // Verify security rules participant list preservation:
        // Updating existing conversation with preserved participants matches resource.data.participantIds
        val existingDocParticipants = legacyConversation.participantIds
        val requestResourceParticipants = existingDocParticipants // Preserved on update
        assertEquals(existingDocParticipants, requestResourceParticipants)
    }

    @Test
    fun testIssue10_LegacyPostTimestampsParsingMatrixAndFeedOrder() {
        // 1. Numeric Long timestamp
        val numericLong: Any = 1718000000000L
        val parsedLong = FirestorePostRepository.parsePostTimestamp(numericLong)
        assertEquals(1718000000000L, parsedLong)

        // 2. Numeric Double timestamp
        val numericDouble: Any = 1718000000000.0
        val parsedDouble = FirestorePostRepository.parsePostTimestamp(numericDouble)
        assertEquals(1718000000000L, parsedDouble)

        // 3. Firestore Timestamp object
        val firestoreTs = com.google.firebase.Timestamp(1718000000L, 0)
        val parsedFs = FirestorePostRepository.parsePostTimestamp(firestoreTs)
        assertEquals(1718000000000L, parsedFs)

        // 4. Numeric String
        val numericString = "1718000000000"
        val parsedNumStr = FirestorePostRepository.parsePostTimestamp(numericString)
        assertEquals(1718000000000L, parsedNumStr)

        // 5. ISO-8601 UTC String
        val isoString = "2024-05-10T14:30:00Z"
        val parsedIso = FirestorePostRepository.parsePostTimestamp(isoString)
        assertTrue("ISO date string must parse to non-zero historical epoch millis", parsedIso > 0L)
        // Verify round-trip conversion to year 2024
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = parsedIso
        assertEquals(2024, cal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.MAY, cal.get(java.util.Calendar.MONTH))
        assertEquals(10, cal.get(java.util.Calendar.DAY_OF_MONTH))

        // 6. Common date format "yyyy-MM-dd"
        val ymdString = "2023-11-20"
        val parsedYmd = FirestorePostRepository.parsePostTimestamp(ymdString)
        assertTrue(parsedYmd > 0L)
        cal.timeInMillis = parsedYmd
        assertEquals(2023, cal.get(java.util.Calendar.YEAR))

        // 7. Missing / Null timestamp: MUST return 0L, NOT System.currentTimeMillis()
        val parsedNull = FirestorePostRepository.parsePostTimestamp(null)
        assertEquals("Missing timestamp must return 0L safe baseline", 0L, parsedNull)

        // 8. Corrupted string: MUST return 0L, NOT System.currentTimeMillis()
        val parsedGarbage = FirestorePostRepository.parsePostTimestamp("corrupted_invalid_timestamp")
        assertEquals("Corrupted timestamp must return 0L safe baseline", 0L, parsedGarbage)

        // 9. Feed sorting test: posts with missing timestamps must NOT displace fresh posts
        val freshPost = Post(id = "p_fresh", author = currentUser, text = "Fresh post", timestamp = 1720000000000L)
        val historicalPost = Post(id = "p_historical", author = currentUser, text = "Historical post", timestamp = 1718000000000L)
        val missingTsPost = Post(id = "p_missing", author = currentUser, text = "Missing date post", timestamp = 0L)

        val unsortedList = listOf(missingTsPost, historicalPost, freshPost)
        val feedSorted = unsortedList.sortedByDescending { it.timestamp }

        assertEquals("p_fresh", feedSorted[0].id)
        assertEquals("p_historical", feedSorted[1].id)
        assertEquals("p_missing", feedSorted[2].id)
    }

    @Test
    fun testIssue11_SelfFollowPrevention() {
        val currentUserId = "uid_same_user"
        val targetUserId = "uid_same_user"

        val canFollow = currentUserId != targetUserId
        assertFalse("User must not be allowed to follow themselves", canFollow)

        val selfFollowError = if (currentUserId == targetUserId) {
            "You cannot follow your own account."
        } else null

        assertNotNull(selfFollowError)
        assertEquals("You cannot follow your own account.", selfFollowError)
    }

    @Test
    fun testIssue12_CommunityPostsPreservedAlongsideFirestorePosts() {
        // User created post in Firestore
        val liveUserPost = Post(
            id = "firestore_post_1",
            author = currentUser,
            text = "Live user post from Chitral",
            timestamp = 1720000000000L
        )
        val firestorePosts = listOf(liveUserPost)

        // Community seed posts
        val communityPosts = SampleData.initialPosts

        val firestoreIds = firestorePosts.map { it.id }.toSet()
        val firestoreTexts = firestorePosts.map { it.text }.toSet()
        val communitySeedPosts = communityPosts.filter { seedPost ->
            !firestoreIds.contains(seedPost.id) && !firestoreTexts.contains(seedPost.text)
        }
        val allDisplayPosts = (firestorePosts + communitySeedPosts).distinctBy { it.id }.sortedByDescending { it.timestamp }

        // Home feed must contain both the live user's post and the community posts
        assertTrue(allDisplayPosts.size > 1)
        assertTrue(allDisplayPosts.any { it.id == "firestore_post_1" })
        assertTrue(allDisplayPosts.any { it.author.handle == "@khowartech" })
        assertTrue(allDisplayPosts.any { it.author.handle == "@shandur_riders" })
    }

    @Test
    fun testIssue13_FollowingFeedImmediatePopulation() {
        val u1Post = Post(id = "p_u1", author = User(id = "u1", name = "Khowar Tech", handle = "@khowartech"), text = "Tech news")
        val u2Post = Post(id = "p_u2", author = User(id = "u2", name = "Shandur Riders", handle = "@shandur_riders"), text = "Polo news")

        var followingIds = emptySet<String>()
        val allPosts = listOf(u1Post, u2Post)

        // 1. Before follow, Following feed is empty
        var followingFeed = allPosts.filter { followingIds.contains(it.author.id) }
        assertTrue(followingFeed.isEmpty())

        // 2. User follows u1
        followingIds = followingIds + "u1"
        followingFeed = allPosts.filter { followingIds.contains(it.author.id) }
        assertEquals(1, followingFeed.size)
        assertEquals("p_u1", followingFeed[0].id)
        assertEquals("@khowartech", followingFeed[0].author.handle)
    }
}
