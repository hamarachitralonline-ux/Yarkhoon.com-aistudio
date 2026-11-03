package com.example.data

/**
 * Role-Based Access Control (RBAC) levels for the Yarkhoon Admin Control Panel.
 */
enum class AdminRole(val roleKey: String, val displayName: String, val badgeColorHex: Long) {
    SUPER_ADMIN("super_admin", "Super Admin", 0xFFD97706),
    ADMIN("admin", "Administrator", 0xFF1877F2),
    MODERATOR("moderator", "Moderator", 0xFF059669),
    NONE("none", "Ordinary User", 0xFF65676B);

    val canAccessAdminPanel: Boolean
        get() = this != NONE

    val canManageAdmins: Boolean
        get() = this == SUPER_ADMIN

    val canManageVerification: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    val canManageFeaturedAndPromoted: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    val canManageVillages: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    val canBroadcastNotifications: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    val canManageUsers: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    val canModerateContent: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN || this == MODERATOR

    companion object {
        fun fromKey(key: String?): AdminRole {
            val normalized = key?.trim()?.lowercase()
                ?.replace(" ", "_")
                ?.replace("-", "_")
            return when (normalized) {
                "super_admin", "superadmin", "owner" -> SUPER_ADMIN
                "admin", "administrator" -> ADMIN
                "moderator", "mod" -> MODERATOR
                else -> NONE
            }
        }

        fun fromString(key: String?): AdminRole = fromKey(key)
    }
}

/**
 * The 15 required navigation sections of the Yarkhoon Admin Control Panel.
 */
enum class AdminControlPanelSection(
    val id: String,
    val title: String,
    val subtitle: String
) {
    DASHBOARD("dashboard", "1. Dashboard", "Real-time database & platform overview"),
    USER_MANAGEMENT("user_management", "2. User Management", "Search, inspect, suspend, or activate accounts"),
    POST_MANAGEMENT("post_management", "3. Post Management", "Approve, reject, or review community posts"),
    VIRAL_POSTS("viral_posts", "4. Viral Posts", "High-engagement posts, pinning & trending labels"),
    FEATURED_POSTS("featured_posts", "5. Featured Posts", "Feature posts, schedule & set expiration dates"),
    BLUE_TICK_VERIFICATION("blue_tick", "6. Blue Tick Verification", "Approve, reject, or revoke account verification"),
    PROMOTED_POSTS("promoted_posts", "7. Promoted Posts", "Manage promoted feed placements & date ranges"),
    TRENDING_TOPICS("trending_topics", "8. Trending Topics", "Trending discussions, local news & village tags"),
    VILLAGE_MANAGEMENT("village_management", "9. Village Management", "Add, edit, or manage Yarkhoon & Chitral villages"),
    REPORTS_AND_COMPLAINTS("reports", "10. Reports & Complaints", "Investigate reported posts, comments & accounts"),
    NOTIFICATIONS("notifications", "11. Notifications", "Publish breaking news & official announcements"),
    ADMIN_MANAGEMENT("admin_management", "12. Admin Management", "Super Admin role assignment & permissions"),
    ACTIVITY_LOGS("activity_logs", "13. Activity Logs", "Complete chronological administrative audit trail"),
    SETTINGS("settings", "14. Settings", "Security policies, session status & configuration"),
    LOGOUT("logout", "15. Logout", "Securely terminate admin session and sign out");

    val label: String
        get() = title
}

/**
 * Real-time database statistics displayed on the Admin Dashboard.
 */
data class AdminDashboardStats(
    val totalRegisteredUsers: Int = 0,
    val activeUsers: Int = 0,
    val suspendedUsers: Int = 0,
    val totalPosts: Int = 0,
    val pendingPosts: Int = 0,
    val approvedPosts: Int = 0,
    val rejectedPosts: Int = 0,
    val totalVerifiedAccounts: Int = 0,
    val totalFeaturedPosts: Int = 0,
    val totalPromotedPosts: Int = 0,
    val trendingPostsCount: Int = 0,
    val pendingVerificationRequests: Int = 0,
    val reportedContentCount: Int = 0,
    val totalVillagesCount: Int = 0,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

/**
 * Represents an administrator or moderator account record in `adminRoles/{uid}`.
 */
data class AdminAccountRecord(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: AdminRole = AdminRole.MODERATOR,
    val enabled: Boolean = true,
    val isActive: Boolean = enabled,
    val permissions: List<String> = emptyList(),
    val grantedBy: String = "",
    val grantedAt: Long = System.currentTimeMillis(),
    val userId: String = uid
)

/**
 * Blue Tick Verification Request submitted by a user or created for admin review.
 */
enum class VerificationRequestStatus(val displayName: String) {
    PENDING("Pending Review"),
    APPROVED("Approved (Blue Tick Active)"),
    REJECTED("Rejected"),
    REVOKED("Verification Revoked")
}

data class VerificationRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val userHandle: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val category: String = "Community Figure",
    val supportingInfo: String = "",
    val documentReference: String = "",
    val status: VerificationRequestStatus = VerificationRequestStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long = requestedAt,
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null
)

/**
 * Village & District directory entry managed in Village Management.
 */
data class YarkhoonVillage(
    val id: String,
    val name: String,
    val normalizedName: String = name.trim().lowercase(),
    val district: String = "Upper Chitral",
    val tehsil: String = "Mastuj",
    val subValley: String = "Yarkhoon Valley",
    val description: String = "",
    val notableLandmarks: String = "",
    val populationEstimate: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = ""
)

/**
 * Official Announcement & Push/In-App Broadcast created by Administrators.
 */
data class OfficialAnnouncement(
    val id: String,
    val title: String,
    val body: String,
    val category: String = "Official Yarkhoon Announcement",
    val priority: String = "HIGH",
    val targetVillageOrLocation: String = "All Yarkhoon & Chitral",
    val targetLocation: String = targetVillageOrLocation,
    val sentByUid: String = "",
    val sentByEmail: String = "",
    val authorUid: String = sentByUid,
    val authorName: String = "Yarkhoon Admin",
    val sentAt: Long = System.currentTimeMillis(),
    val createdAt: Long = sentAt
)

/**
 * Curated or genuine Trending Topic / Discussion managed in Trending Topics section.
 */
data class CuratedTrendingTopic(
    val id: String,
    val tag: String = "",
    val headline: String = "",
    val category: String = "Local News",
    val villageName: String = "",
    val postCount: Int = 0,
    val postsCount: Int = postCount,
    val engagementScore: Int = 0,
    val isPinned: Boolean = false,
    val isManuallyFeatured: Boolean = isPinned,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = updatedAt
)
