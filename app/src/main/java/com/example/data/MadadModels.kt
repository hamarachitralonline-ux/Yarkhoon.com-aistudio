package com.example.data

/**
 * Yarkhoon Madad Center Data Models
 * Supports:
 * 1. Job Support (Seekers & Vacancies)
 * 2. Blood Donation Network (Donors & Urgent Requests)
 * 3. Community Help & Donations (Medical, Education, Relief)
 * 4. Legal Guidance & Emergency Help
 */

enum class MadadCategory(val title: String, val subtitle: String, val iconName: String) {
    JOBS(
        title = "Job Support",
        subtitle = "Find jobs, hire local talent, and share employment opportunities",
        iconName = "Work"
    ),
    BLOOD_DONATION(
        title = "Blood Donors",
        subtitle = "Voluntary blood donation network and urgent hospital requests",
        iconName = "Bloodtype"
    ),
    COMMUNITY_AID(
        title = "Community Aid",
        subtitle = "Verified assistance for patients, students, and low-income families",
        iconName = "VolunteerActivism"
    ),
    LEGAL_EMERGENCY(
        title = "Legal & Emergency",
        subtitle = "Police, court procedures, documentation, and 24/7 emergency contacts",
        iconName = "Gavel"
    )
}

// ---------------- 1. Job Support Models ----------------

data class JobSeekerProfile(
    val id: String, // Matches User UID
    val userId: String,
    val name: String,
    val handle: String,
    val avatarUrl: String = "",
    val qualifications: String,
    val skills: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val preferredRoles: List<String> = emptyList(),
    val preferredCities: List<String> = emptyList(),
    val summary: String = "",
    val contactPreference: String = "in_app_chat", // "in_app_chat", "email", "phone"
    val phoneNumber: String? = null,
    val isPhonePublic: Boolean = false,
    val isActivelySeeking: Boolean = true,
    val cvFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class JobVacancy(
    val id: String,
    val employerId: String,
    val employerName: String,
    val employerHandle: String = "@employer",
    val employerAvatarUrl: String = "",
    val title: String,
    val companyName: String,
    val city: String,
    val location: String = "Chitral",
    val jobType: String = "Full-time", // "Full-time", "Part-time", "Remote", "Contract", "Internship"
    val salaryRange: String? = null,
    val requiredSkills: List<String> = emptyList(),
    val description: String,
    val applicationInstructions: String,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val status: String = "active", // "active", "closed"
    val createdAt: Long = System.currentTimeMillis()
)

// ---------------- 2. Blood Donation Models ----------------

data class BloodDonor(
    val id: String, // Matches User UID
    val userId: String,
    val name: String,
    val handle: String = "@donor",
    val avatarUrl: String = "",
    val bloodGroup: String, // "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    val city: String,
    val area: String = "",
    val isAvailable: Boolean = true,
    val lastDonationDate: String? = null,
    val contactPreference: String = "in_app_chat", // "in_app_chat", "phone_verified"
    val phoneNumber: String? = null,
    val isPhonePublic: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis()
)

data class BloodRequest(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val requesterHandle: String = "@requester",
    val requesterAvatarUrl: String = "",
    val patientName: String,
    val bloodGroup: String,
    val hospitalName: String,
    val city: String,
    val requiredUnits: Int = 1,
    val urgency: String = "URGENT", // "CRITICAL_URGENT", "URGENT", "NORMAL"
    val contactPhone: String,
    val notes: String = "",
    val status: String = "active", // "active", "fulfilled", "cancelled", "expired"
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (48 * 3600 * 1000L) // Default 48 hours
)

// ---------------- 3. Community Help & Donations Models ----------------

data class CommunityAidRequest(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val requesterHandle: String = "@member",
    val requesterAvatarUrl: String = "",
    val beneficiaryName: String,
    val category: String, // "Medical Treatment", "Education / Fees", "Emergency Relief", "Food & Ration", "Winter Clothing", "Disability Support", "Other"
    val title: String,
    val description: String,
    val city: String,
    val location: String = "Chitral",
    val assistanceType: String = "Financial", // "Financial", "Medical Supplies", "Education Material", "Ration / Groceries", "Services / Volunteer", "Other"
    val targetAmount: Double? = null,
    val status: String = "active", // "pending_review", "approved", "active", "fulfilled", "rejected", "closed"
    val verificationStatus: String = "pending_review", // "pending_review", "verified_by_moderator", "unverified_community_lead", "rejected"
    val verifiedBy: String? = null,
    val verificationNotes: String? = null,
    val contactMethod: String = "in_app_message", // "in_app_message", "moderator_mediated"
    val supportOffersCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ---------------- 4. Legal Guidance & Emergency Models ----------------

data class LegalGuidanceTopic(
    val id: String,
    val title: String,
    val category: String, // "Police Procedures", "Courts & Bail", "Identity & NADRA", "Land & Revenue", "Legal Aid"
    val summary: String,
    val fullGuide: String,
    val importantCaution: String,
    val relevantAuthorities: List<String> = emptyList()
)

data class EmergencyContact(
    val name: String,
    val serviceType: String,
    val dialNumber: String,
    val location: String,
    val description: String,
    val is24Hours: Boolean = true
)

data class LegalInquiry(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val category: String,
    val subject: String,
    val description: String,
    val isAnonymous: Boolean = false,
    val status: String = "open", // "open", "answered", "closed"
    val responsesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
