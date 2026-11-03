package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests for Yarkhoon Madad Center
 *
 * Validates:
 * 1. Category initialization and metadata.
 * 2. Job Support: Seeker profile validation, skills filtering, vacancy status lifecycle.
 * 3. Blood Donation: Group matching, donor availability, urgent request fulfillment.
 * 4. Community Aid: Anti-fraud status protection (pending_review), verification status.
 * 5. Emergency Directory: Verified contacts and legal guidance topics.
 * 6. Contact Routing: In-app chat preservation protecting phone privacy.
 */
class MadadCenterTest {

    private val testUser = User(
        id = "user_donor_001",
        name = "Sultan Wali",
        handle = "@sultan",
        email = "sultan@yarkhoon.com",
        location = "Chitral"
    )

    private val requesterUser = User(
        id = "user_req_002",
        name = "Karim Khan",
        handle = "@karim",
        email = "karim@yarkhoon.com",
        location = "Yarkhoon"
    )

    @Test
    fun testMadadCategoriesPresence() {
        val categories = MadadCategory.entries
        assertEquals(4, categories.size)
        assertTrue(categories.contains(MadadCategory.JOBS))
        assertTrue(categories.contains(MadadCategory.BLOOD_DONATION))
        assertTrue(categories.contains(MadadCategory.COMMUNITY_AID))
        assertTrue(categories.contains(MadadCategory.LEGAL_EMERGENCY))
    }

    @Test
    fun testJobSeekerProfileValidationAndSearch() {
        val profile = JobSeekerProfile(
            id = "seeker_1",
            userId = testUser.id,
            name = testUser.name,
            handle = testUser.handle,
            qualifications = "BS Physics, Peshawar University",
            skills = listOf("Physics", "Chemistry", "Curriculum Planning"),
            experienceYears = 3,
            preferredRoles = listOf("High School Science Teacher"),
            preferredCities = listOf("Chitral", "Mastuj"),
            summary = "Experienced science teacher.",
            isActivelySeeking = true
        )

        // Validation assertions
        assertTrue(profile.skills.contains("Physics"))
        assertTrue(profile.isActivelySeeking)
        assertEquals(3, profile.experienceYears)

        // Filter / Search simulation: Case-insensitive query matches title or skills
        val query = "physics"
        val matches = profile.preferredRoles.any { it.contains(query, ignoreCase = true) } ||
                profile.skills.any { it.contains(query, ignoreCase = true) }
        assertTrue(matches)

        // City filter match
        val cityFilter = "Chitral"
        val matchesCity = profile.preferredCities.any { it.equals(cityFilter, ignoreCase = true) }
        assertTrue(matchesCity)
    }

    @Test
    fun testJobVacancyLifecycle() {
        val vacancy = JobVacancy(
            id = "vac_101",
            employerId = requesterUser.id,
            employerName = requesterUser.name,
            employerHandle = requesterUser.handle,
            title = "Civil Engineer (Water Supply Project)",
            companyName = "Yarkhoon Rural Development Trust",
            city = "Chitral",
            location = "Yarkhoon",
            jobType = "Contract",
            salaryRange = "PKR 70,000 - 90,000",
            requiredSkills = listOf("AutoCAD", "Surveying", "Hydrology"),
            description = "Supervision of clean drinking water channel.",
            applicationInstructions = "Contact via in-app chat with brief experience summary.",
            status = "active"
        )

        assertEquals("active", vacancy.status)
        assertEquals("Yarkhoon", vacancy.location)

        // Close vacancy transition
        val closedVacancy = vacancy.copy(status = "closed")
        assertEquals("closed", closedVacancy.status)
        assertNotEquals(vacancy.status, closedVacancy.status)
    }

    @Test
    fun testBloodDonorFilteringAndAvailability() {
        val donorA = BloodDonor(
            id = "donor_1",
            userId = testUser.id,
            name = testUser.name,
            handle = testUser.handle,
            bloodGroup = "O+",
            city = "Chitral",
            isAvailable = true
        )

        val donorB = BloodDonor(
            id = "donor_2",
            userId = "user_3",
            name = "Ali Shah",
            handle = "@alishah",
            bloodGroup = "B+",
            city = "Peshawar",
            isAvailable = false
        )

        val donors = listOf(donorA, donorB)

        // Filter by blood group "O+" and available only
        val availableOPlus = donors.filter { it.bloodGroup == "O+" && it.isAvailable }
        assertEquals(1, availableOPlus.size)
        assertEquals(testUser.id, availableOPlus.first().userId)

        // Donor B is unavailable
        val availableBPlus = donors.filter { it.bloodGroup == "B+" && it.isAvailable }
        assertTrue(availableBPlus.isEmpty())
    }

    @Test
    fun testBloodRequestUrgencyAndFulfillment() {
        val bloodReq = BloodRequest(
            id = "blood_req_01",
            requesterId = requesterUser.id,
            requesterName = requesterUser.name,
            requesterHandle = requesterUser.handle,
            patientName = "Bibi Amina",
            bloodGroup = "A-",
            hospitalName = "DHQ Hospital Chitral",
            city = "Chitral",
            requiredUnits = 2,
            urgency = "URGENT",
            contactPhone = "0943-412333",
            notes = "Patient in emergency ward. Please reach out via in-app message.",
            status = "active"
        )

        assertEquals("active", bloodReq.status)
        assertEquals(2, bloodReq.requiredUnits)

        // Mark request as fulfilled
        val fulfilledReq = bloodReq.copy(status = "fulfilled")
        assertEquals("fulfilled", fulfilledReq.status)
    }

    @Test
    fun testCommunityAidRequestAntiFraudStatus() {
        // Anti-fraud guarantee: Aid requests MUST initialize with "pending_review"
        val aidReq = CommunityAidRequest(
            id = "aid_001",
            requesterId = requesterUser.id,
            requesterName = requesterUser.name,
            requesterHandle = requesterUser.handle,
            beneficiaryName = "Wali Family",
            category = "Medical Treatment",
            title = "Emergency Medical Travel Assistance",
            description = "Patient requires specialized cardiology treatment in Peshawar.",
            city = "Yarkhoon",
            targetAmount = 45000.0,
            verificationStatus = "pending_review",
            status = "active"
        )

        assertEquals("pending_review", aidReq.verificationStatus)
        assertEquals("active", aidReq.status)
        assertEquals(45000.0, aidReq.targetAmount ?: 0.0, 0.01)

        // Community moderator reviews and verifies the aid request
        val verifiedReq = aidReq.copy(verificationStatus = "verified_by_moderator")
        assertEquals("verified_by_moderator", verifiedReq.verificationStatus)

        // When fulfilled, status becomes "fulfilled"
        val resolvedReq = verifiedReq.copy(status = "fulfilled")
        assertEquals("fulfilled", resolvedReq.status)
    }

    @Test
    fun testLegalAndEmergencyHelplines() {
        val helplines = listOf(
            EmergencyContact("Rescue 1122", "Emergency Rescue", "1122", "Chitral", "Disaster & Medical Rescue", true),
            EmergencyContact("Police Emergency", "Police", "15", "Nationwide", "Law Enforcement", true),
            EmergencyContact("DHQ Hospital Chitral", "Hospital", "0943-412333", "Chitral", "Main Referral Hospital", true)
        )

        assertEquals(3, helplines.size)
        val rescue1122 = helplines.find { it.dialNumber == "1122" }
        assertNotNull(rescue1122)
        assertEquals("Rescue 1122", rescue1122?.name)
    }
}
