package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreMadadRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val JOB_PROFILES_COLLECTION = "madadJobProfiles"
        const val JOB_VACANCIES_COLLECTION = "madadJobVacancies"
        const val BLOOD_DONORS_COLLECTION = "madadBloodDonors"
        const val BLOOD_REQUESTS_COLLECTION = "madadBloodRequests"
        const val AID_REQUESTS_COLLECTION = "madadAidRequests"
        const val LEGAL_INQUIRIES_COLLECTION = "madadLegalInquiries"
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    // ==========================================
    // 1. Job Support Operations
    // ==========================================

    suspend fun saveJobSeekerProfile(profile: JobSeekerProfile): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to create a job profile."))
        return try {
            val data = mapOf(
                "userId" to uid,
                "name" to profile.name,
                "handle" to profile.handle,
                "avatarUrl" to profile.avatarUrl,
                "qualifications" to profile.qualifications,
                "skills" to profile.skills,
                "experienceYears" to profile.experienceYears,
                "preferredRoles" to profile.preferredRoles,
                "preferredCities" to profile.preferredCities,
                "summary" to profile.summary,
                "contactPreference" to profile.contactPreference,
                "phoneNumber" to (profile.phoneNumber ?: ""),
                "isPhonePublic" to profile.isPhonePublic,
                "isActivelySeeking" to profile.isActivelySeeking,
                "cvFileName" to (profile.cvFileName ?: ""),
                "createdAt" to profile.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(JOB_PROFILES_COLLECTION)
                .document(uid)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobSeekerProfile(userId: String): Result<JobSeekerProfile?> {
        return try {
            val doc = firestore.collection(JOB_PROFILES_COLLECTION)
                .document(userId)
                .get()
                .await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                Result.success(parseJobSeekerProfile(doc))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobSeekerProfiles(city: String? = null, skillQuery: String? = null): Result<List<JobSeekerProfile>> {
        return try {
            var query: Query = firestore.collection(JOB_PROFILES_COLLECTION)
                .whereEqualTo("isActivelySeeking", true)

            if (!city.isNullOrBlank() && city != "All Cities") {
                query = query.whereArrayContains("preferredCities", city)
            }

            val snapshot = query.get().await()
            val list = snapshot.documents.mapNotNull { parseJobSeekerProfile(it) }

            val filtered = if (!skillQuery.isNullOrBlank()) {
                val q = skillQuery.lowercase().trim()
                list.filter { p ->
                    p.skills.any { it.lowercase().contains(q) } ||
                    p.preferredRoles.any { it.lowercase().contains(q) } ||
                    p.qualifications.lowercase().contains(q) ||
                    p.name.lowercase().contains(q)
                }
            } else {
                list
            }

            Result.success(filtered.sortedByDescending { it.updatedAt })
        } catch (e: Exception) {
            // Provide offline fallback list if Firestore is uninitialized or network down
            Result.success(getFallbackJobSeekers(city, skillQuery))
        }
    }

    suspend fun createJobVacancy(vacancy: JobVacancy): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to post a vacancy."))
        return try {
            val docRef = if (vacancy.id.isNotBlank()) {
                firestore.collection(JOB_VACANCIES_COLLECTION).document(vacancy.id)
            } else {
                firestore.collection(JOB_VACANCIES_COLLECTION).document()
            }
            val data = mapOf(
                "id" to docRef.id,
                "employerId" to uid,
                "employerName" to vacancy.employerName,
                "employerHandle" to vacancy.employerHandle,
                "employerAvatarUrl" to vacancy.employerAvatarUrl,
                "title" to vacancy.title,
                "companyName" to vacancy.companyName,
                "city" to vacancy.city,
                "location" to vacancy.location,
                "jobType" to vacancy.jobType,
                "salaryRange" to (vacancy.salaryRange ?: ""),
                "requiredSkills" to vacancy.requiredSkills,
                "description" to vacancy.description,
                "applicationInstructions" to vacancy.applicationInstructions,
                "contactEmail" to (vacancy.contactEmail ?: ""),
                "contactPhone" to (vacancy.contactPhone ?: ""),
                "status" to vacancy.status,
                "createdAt" to vacancy.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobVacancies(city: String? = null, query: String? = null): Result<List<JobVacancy>> {
        return try {
            val snapshot = firestore.collection(JOB_VACANCIES_COLLECTION)
                .whereEqualTo("status", "active")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { parseJobVacancy(it) }

            val filtered = list.filter { v ->
                val cityMatches = city.isNullOrBlank() || city == "All Cities" || v.city.equals(city, ignoreCase = true)
                val textMatches = query.isNullOrBlank() ||
                        v.title.contains(query, ignoreCase = true) ||
                        v.companyName.contains(query, ignoreCase = true) ||
                        v.requiredSkills.any { it.contains(query, ignoreCase = true) }
                cityMatches && textMatches
            }

            if (filtered.isEmpty()) {
                Result.success(getFallbackJobVacancies(city, query))
            } else {
                Result.success(filtered.sortedByDescending { it.createdAt })
            }
        } catch (e: Exception) {
            Result.success(getFallbackJobVacancies(city, query))
        }
    }

    suspend fun closeJobVacancy(vacancyId: String): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Authentication required."))
        return try {
            firestore.collection(JOB_VACANCIES_COLLECTION)
                .document(vacancyId)
                .update("status", "closed")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. Blood Donation Operations
    // ==========================================

    suspend fun registerOrUpdateBloodDonor(donor: BloodDonor): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to register as a donor."))
        return try {
            val data = mapOf(
                "userId" to uid,
                "name" to donor.name,
                "handle" to donor.handle,
                "avatarUrl" to donor.avatarUrl,
                "bloodGroup" to donor.bloodGroup,
                "city" to donor.city,
                "area" to donor.area,
                "isAvailable" to donor.isAvailable,
                "lastDonationDate" to (donor.lastDonationDate ?: ""),
                "contactPreference" to donor.contactPreference,
                "phoneNumber" to (donor.phoneNumber ?: ""),
                "isPhonePublic" to donor.isPhonePublic,
                "registeredAt" to donor.registeredAt
            )
            firestore.collection(BLOOD_DONORS_COLLECTION)
                .document(uid)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBloodDonorProfile(userId: String): Result<BloodDonor?> {
        return try {
            val doc = firestore.collection(BLOOD_DONORS_COLLECTION)
                .document(userId)
                .get()
                .await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                Result.success(parseBloodDonor(doc))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchBloodDonors(bloodGroup: String? = null, city: String? = null): Result<List<BloodDonor>> {
        return try {
            var q: Query = firestore.collection(BLOOD_DONORS_COLLECTION)
                .whereEqualTo("isAvailable", true)

            if (!bloodGroup.isNullOrBlank() && bloodGroup != "All Groups") {
                q = q.whereEqualTo("bloodGroup", bloodGroup)
            }
            if (!city.isNullOrBlank() && city != "All Cities") {
                q = q.whereEqualTo("city", city)
            }

            val snapshot = q.get().await()
            val list = snapshot.documents.mapNotNull { parseBloodDonor(it) }

            if (list.isEmpty()) {
                Result.success(getFallbackBloodDonors(bloodGroup, city))
            } else {
                Result.success(list.sortedByDescending { it.registeredAt })
            }
        } catch (e: Exception) {
            Result.success(getFallbackBloodDonors(bloodGroup, city))
        }
    }

    suspend fun createBloodRequest(request: BloodRequest): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to request blood."))
        return try {
            val docRef = if (request.id.isNotBlank()) {
                firestore.collection(BLOOD_REQUESTS_COLLECTION).document(request.id)
            } else {
                firestore.collection(BLOOD_REQUESTS_COLLECTION).document()
            }
            val data = mapOf(
                "id" to docRef.id,
                "requesterId" to uid,
                "requesterName" to request.requesterName,
                "requesterHandle" to request.requesterHandle,
                "requesterAvatarUrl" to request.requesterAvatarUrl,
                "patientName" to request.patientName,
                "bloodGroup" to request.bloodGroup,
                "hospitalName" to request.hospitalName,
                "city" to request.city,
                "requiredUnits" to request.requiredUnits,
                "urgency" to request.urgency,
                "contactPhone" to request.contactPhone,
                "notes" to request.notes,
                "status" to request.status,
                "createdAt" to request.createdAt,
                "expiresAt" to request.expiresAt
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveBloodRequests(city: String? = null, bloodGroup: String? = null): Result<List<BloodRequest>> {
        return try {
            val snapshot = firestore.collection(BLOOD_REQUESTS_COLLECTION)
                .whereEqualTo("status", "active")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { parseBloodRequest(it) }

            val filtered = list.filter { r ->
                val cityMatches = city.isNullOrBlank() || city == "All Cities" || r.city.equals(city, ignoreCase = true)
                val groupMatches = bloodGroup.isNullOrBlank() || bloodGroup == "All Groups" || r.bloodGroup.equals(bloodGroup, ignoreCase = true)
                cityMatches && groupMatches
            }

            if (filtered.isEmpty()) {
                Result.success(getFallbackBloodRequests(city, bloodGroup))
            } else {
                Result.success(filtered.sortedByDescending { it.createdAt })
            }
        } catch (e: Exception) {
            Result.success(getFallbackBloodRequests(city, bloodGroup))
        }
    }

    suspend fun markBloodRequestFulfilled(requestId: String): Result<Unit> {
        return try {
            firestore.collection(BLOOD_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", "fulfilled")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 3. Community Help & Donations Operations
    // ==========================================

    suspend fun createCommunityAidRequest(request: CommunityAidRequest): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to request community aid."))
        return try {
            val docRef = if (request.id.isNotBlank()) {
                firestore.collection(AID_REQUESTS_COLLECTION).document(request.id)
            } else {
                firestore.collection(AID_REQUESTS_COLLECTION).document()
            }
            val data = mapOf(
                "id" to docRef.id,
                "requesterId" to uid,
                "requesterName" to request.requesterName,
                "requesterHandle" to request.requesterHandle,
                "requesterAvatarUrl" to request.requesterAvatarUrl,
                "beneficiaryName" to request.beneficiaryName,
                "category" to request.category,
                "title" to request.title,
                "description" to request.description,
                "city" to request.city,
                "location" to request.location,
                "assistanceType" to request.assistanceType,
                "targetAmount" to (request.targetAmount ?: 0.0),
                "status" to request.status,
                "verificationStatus" to "pending_review", // Users cannot self-verify
                "verifiedBy" to "",
                "verificationNotes" to "",
                "contactMethod" to request.contactMethod,
                "supportOffersCount" to 0,
                "createdAt" to request.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityAidRequests(category: String? = null): Result<List<CommunityAidRequest>> {
        return try {
            val snapshot = firestore.collection(AID_REQUESTS_COLLECTION)
                .whereEqualTo("status", "active")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { parseAidRequest(it) }

            val filtered = if (!category.isNullOrBlank() && category != "All Categories") {
                list.filter { it.category.equals(category, ignoreCase = true) }
            } else {
                list
            }

            if (filtered.isEmpty()) {
                Result.success(getFallbackAidRequests(category))
            } else {
                Result.success(filtered.sortedByDescending { it.createdAt })
            }
        } catch (e: Exception) {
            Result.success(getFallbackAidRequests(category))
        }
    }

    suspend fun markCommunityAidFulfilled(requestId: String): Result<Unit> {
        return try {
            firestore.collection(AID_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", "fulfilled")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 4. Legal Guidance & Emergency Resources
    // ==========================================

    fun getPredefinedLegalTopics(): List<LegalGuidanceTopic> {
        return listOf(
            LegalGuidanceTopic(
                id = "legal_police_fir",
                title = "Police FIR & Station Protocols",
                category = "Police Procedures",
                summary = "How to register a First Information Report (FIR) and your constitutional rights when visiting a police station in Pakistan.",
                fullGuide = "1. Section 154 CrPC mandates that the police must record an FIR regarding any cognizable offense without delay.\n2. You are entitled to a free, signed copy of the FIR immediately upon registration.\n3. If an SHO refuses to register an FIR, you may submit a formal written complaint to the District Police Officer (DPO) or file a petition under Section 22-A / 22-B CrPC before the Justice of Peace (Sessions Judge).\n4. In the event of arrest, police must present the detained person before an Area Magistrate within 24 hours under Article 10 of the Constitution.",
                importantCaution = "Never sign a blank paper or unread statement in a police station. Always keep a diary or witness with you.",
                relevantAuthorities = listOf("DPO Office Lower Chitral", "DPO Office Upper Chitral", "Police Station Mastuj", "Police Station Chitral")
            ),
            LegalGuidanceTopic(
                id = "legal_bail_procedure",
                title = "Bail Procedures & Court Protocols",
                category = "Courts & Bail",
                summary = "Understanding pre-arrest (protective/anticipatory) bail and post-arrest bail procedures before Sessions and High Courts.",
                fullGuide = "1. Pre-Arrest Bail (Section 498 CrPC): Applied before arrest where genuine apprehension of arrest on mala fide or ulterior grounds exists.\n2. Post-Arrest Bail (Section 497 CrPC): Applied after arrest. In bailable offenses, bail is a matter of right. In non-bailable offenses, bail is granted where reasonable grounds for guilt do not exist or during statutory delay.\n3. Surety Bonds: Requires a solvent surety residing in the local district.",
                importantCaution = "Bail petition drafting must be conducted by a licensed advocate enrolled with the KP Bar Council.",
                relevantAuthorities = listOf("District & Sessions Court Chitral", "Peshawar High Court Circuit Bench")
            ),
            LegalGuidanceTopic(
                id = "legal_nadra_identity",
                title = "NADRA, CNIC, Form-B & Domicile",
                category = "Identity & NADRA",
                summary = "Requirements for obtaining or modifying Computerized National Identity Cards, Child Registration Certificates (Form-B), and District Domicile.",
                fullGuide = "1. First-time CNIC (Age 18): Requires original Form-B, parent's CNIC, and verification by an attester or biological relative.\n2. Form-B: Requires Union Council Birth Registration Certificate.\n3. Domicile Certificate: Applied at the Deputy Commissioner's Office (DC Office). Requires proof of land/residence, voter list copy, and CNIC copy.",
                importantCaution = "NADRA services are strictly state-regulated. Do not pay touts or unofficial agents.",
                relevantAuthorities = listOf("NADRA Mega Center Chitral", "DC Office Lower Chitral", "DC Office Upper Chitral (Booni)")
            ),
            LegalGuidanceTopic(
                id = "legal_land_revenue",
                title = "Land Revenue, Fard & Property Verification",
                category = "Land & Revenue",
                summary = "Procedures for obtaining Jamabandi (Fard), Mutation (Inteqal), and verifying land ownership records in Chitral.",
                fullGuide = "1. Fard-e-Malkiat: Obtained through the local Patwari or Service Delivery Center (SDC).\n2. Inteqal (Mutation): Must be recorded before the Tehsildar or Naib Tehsildar in the presence of revenue field staff.\n3. Common Lands & Shamilat: Verify title boundaries before entering purchase contracts.",
                importantCaution = "Ensure stamp paper purchase and registry execution are verified at the local Sub-Registrar office.",
                relevantAuthorities = listOf("Tehsil Office Chitral", "Tehsil Office Mastuj", "Sub-Registrar Office")
            ),
            LegalGuidanceTopic(
                id = "legal_aid_resources",
                title = "Free Legal Aid & Bar Councils",
                category = "Legal Aid",
                summary = "Directory of free legal assistance desks for vulnerable individuals, women, and low-income families.",
                fullGuide = "1. District Legal Aid Committee (DLAC): Provides state-funded free counsel to indigent accused persons in criminal cases.\n2. Khyber Pakhtunkhwa Bar Council Free Legal Aid Committee.\n3. District Bar Association Chitral: Advocates available for pro-bono consultation on humanitarian matters.",
                importantCaution = "Legal aid services provided through DLAC are completely free of government fees.",
                relevantAuthorities = listOf("District Bar Association Chitral", "KP Bar Council Legal Aid Desk")
            )
        )
    }

    fun getEmergencyContacts(): List<EmergencyContact> {
        return listOf(
            EmergencyContact("Rescue 1122 Chitral", "Disaster, Ambulance & Fire Rescue", "1122", "Chitral & Booni", "24/7 emergency medical, accident, flood, and fire rescue service.", true),
            EmergencyContact("Police Emergency Helpline", "Law & Order", "15", "All Pakistan", "Immediate emergency police response helpline.", true),
            EmergencyContact("DHQ Hospital Chitral", "Emergency Healthcare & Blood Bank", "0943-412353", "Chitral Town", "Main District Headquarters Hospital emergency trauma center.", true),
            EmergencyContact("Mastuj Tehsil Hospital", "Emergency Medical Care", "0943-481020", "Mastuj Sub-Division", "Sub-divisional emergency medical center for Yarkhoon and Mastuj.", true),
            EmergencyContact("Booni Tehsil Hospital", "Healthcare & Emergency", "0943-470123", "Upper Chitral", "Tehsil Headquarter Hospital Booni.", true),
            EmergencyContact("Edhi Emergency Helpline", "Ambulance & Welfare", "115", "Nationwide", "Free/low-cost nationwide emergency ambulance service.", true),
            EmergencyContact("Women Protection Helpline", "Human Rights & Protection", "1099", "Nationwide", "Ministry of Human Rights helpline for women, children, and domestic assistance.", true),
            EmergencyContact("KP Disaster Management (PDMA)", "Emergency Weather & Landslide Alert", "1700", "Khyber Pakhtunkhwa", "Provincial Disaster Management Authority helpline.", true)
        )
    }

    suspend fun createLegalInquiry(inquiry: LegalInquiry): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to submit an inquiry."))
        return try {
            val docRef = if (inquiry.id.isNotBlank()) {
                firestore.collection(LEGAL_INQUIRIES_COLLECTION).document(inquiry.id)
            } else {
                firestore.collection(LEGAL_INQUIRIES_COLLECTION).document()
            }
            val data = mapOf(
                "id" to docRef.id,
                "requesterId" to uid,
                "requesterName" to if (inquiry.isAnonymous) "Anonymous Member" else inquiry.requesterName,
                "category" to inquiry.category,
                "subject" to inquiry.subject,
                "description" to inquiry.description,
                "isAnonymous" to inquiry.isAnonymous,
                "status" to inquiry.status,
                "responsesCount" to 0,
                "createdAt" to inquiry.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLegalInquiries(): Result<List<LegalInquiry>> {
        return try {
            val snapshot = firestore.collection(LEGAL_INQUIRIES_COLLECTION)
                .whereEqualTo("status", "open")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { parseLegalInquiry(it) }
            Result.success(list.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    // ==========================================
    // Parser Helpers
    // ==========================================

    @Suppress("UNCHECKED_CAST")
    private fun parseJobSeekerProfile(doc: com.google.firebase.firestore.DocumentSnapshot): JobSeekerProfile? {
        val data = doc.data ?: return null
        return JobSeekerProfile(
            id = doc.id,
            userId = (data["userId"] as? String) ?: doc.id,
            name = (data["name"] as? String) ?: "Member",
            handle = (data["handle"] as? String) ?: "@member",
            avatarUrl = (data["avatarUrl"] as? String) ?: "",
            qualifications = (data["qualifications"] as? String) ?: "",
            skills = (data["skills"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            experienceYears = ((data["experienceYears"] as? Number)?.toInt()) ?: 0,
            preferredRoles = (data["preferredRoles"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            preferredCities = (data["preferredCities"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            summary = (data["summary"] as? String) ?: "",
            contactPreference = (data["contactPreference"] as? String) ?: "in_app_chat",
            phoneNumber = data["phoneNumber"] as? String,
            isPhonePublic = (data["isPhonePublic"] as? Boolean) ?: false,
            isActivelySeeking = (data["isActivelySeeking"] as? Boolean) ?: true,
            cvFileName = data["cvFileName"] as? String,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJobVacancy(doc: com.google.firebase.firestore.DocumentSnapshot): JobVacancy? {
        val data = doc.data ?: return null
        return JobVacancy(
            id = doc.id,
            employerId = (data["employerId"] as? String) ?: "",
            employerName = (data["employerName"] as? String) ?: "Employer",
            employerHandle = (data["employerHandle"] as? String) ?: "@employer",
            employerAvatarUrl = (data["employerAvatarUrl"] as? String) ?: "",
            title = (data["title"] as? String) ?: "",
            companyName = (data["companyName"] as? String) ?: "",
            city = (data["city"] as? String) ?: "Chitral",
            location = (data["location"] as? String) ?: "Chitral",
            jobType = (data["jobType"] as? String) ?: "Full-time",
            salaryRange = data["salaryRange"] as? String,
            requiredSkills = (data["requiredSkills"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            description = (data["description"] as? String) ?: "",
            applicationInstructions = (data["applicationInstructions"] as? String) ?: "",
            contactEmail = data["contactEmail"] as? String,
            contactPhone = data["contactPhone"] as? String,
            status = (data["status"] as? String) ?: "active",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun parseBloodDonor(doc: com.google.firebase.firestore.DocumentSnapshot): BloodDonor? {
        val data = doc.data ?: return null
        return BloodDonor(
            id = doc.id,
            userId = (data["userId"] as? String) ?: doc.id,
            name = (data["name"] as? String) ?: "Community Donor",
            handle = (data["handle"] as? String) ?: "@donor",
            avatarUrl = (data["avatarUrl"] as? String) ?: "",
            bloodGroup = (data["bloodGroup"] as? String) ?: "O+",
            city = (data["city"] as? String) ?: "Chitral",
            area = (data["area"] as? String) ?: "",
            isAvailable = (data["isAvailable"] as? Boolean) ?: true,
            lastDonationDate = data["lastDonationDate"] as? String,
            contactPreference = (data["contactPreference"] as? String) ?: "in_app_chat",
            phoneNumber = data["phoneNumber"] as? String,
            isPhonePublic = (data["isPhonePublic"] as? Boolean) ?: false,
            registeredAt = (data["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun parseBloodRequest(doc: com.google.firebase.firestore.DocumentSnapshot): BloodRequest? {
        val data = doc.data ?: return null
        return BloodRequest(
            id = doc.id,
            requesterId = (data["requesterId"] as? String) ?: "",
            requesterName = (data["requesterName"] as? String) ?: "Requester",
            requesterHandle = (data["requesterHandle"] as? String) ?: "@requester",
            requesterAvatarUrl = (data["requesterAvatarUrl"] as? String) ?: "",
            patientName = (data["patientName"] as? String) ?: "Patient",
            bloodGroup = (data["bloodGroup"] as? String) ?: "O+",
            hospitalName = (data["hospitalName"] as? String) ?: "DHQ Hospital",
            city = (data["city"] as? String) ?: "Chitral",
            requiredUnits = ((data["requiredUnits"] as? Number)?.toInt()) ?: 1,
            urgency = (data["urgency"] as? String) ?: "URGENT",
            contactPhone = (data["contactPhone"] as? String) ?: "",
            notes = (data["notes"] as? String) ?: "",
            status = (data["status"] as? String) ?: "active",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 48 * 3600 * 1000L)
        )
    }

    private fun parseAidRequest(doc: com.google.firebase.firestore.DocumentSnapshot): CommunityAidRequest? {
        val data = doc.data ?: return null
        return CommunityAidRequest(
            id = doc.id,
            requesterId = (data["requesterId"] as? String) ?: "",
            requesterName = (data["requesterName"] as? String) ?: "Member",
            requesterHandle = (data["requesterHandle"] as? String) ?: "@member",
            requesterAvatarUrl = (data["requesterAvatarUrl"] as? String) ?: "",
            beneficiaryName = (data["beneficiaryName"] as? String) ?: "Community Family",
            category = (data["category"] as? String) ?: "Medical Treatment",
            title = (data["title"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            city = (data["city"] as? String) ?: "Chitral",
            location = (data["location"] as? String) ?: "Yarkhoon Valley",
            assistanceType = (data["assistanceType"] as? String) ?: "Financial",
            targetAmount = (data["targetAmount"] as? Number)?.toDouble(),
            status = (data["status"] as? String) ?: "active",
            verificationStatus = (data["verificationStatus"] as? String) ?: "pending_review",
            verifiedBy = data["verifiedBy"] as? String,
            verificationNotes = data["verificationNotes"] as? String,
            contactMethod = (data["contactMethod"] as? String) ?: "in_app_message",
            supportOffersCount = ((data["supportOffersCount"] as? Number)?.toInt()) ?: 0,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun parseLegalInquiry(doc: com.google.firebase.firestore.DocumentSnapshot): LegalInquiry? {
        val data = doc.data ?: return null
        return LegalInquiry(
            id = doc.id,
            requesterId = (data["requesterId"] as? String) ?: "",
            requesterName = (data["requesterName"] as? String) ?: "Member",
            category = (data["category"] as? String) ?: "General Legal",
            subject = (data["subject"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            isAnonymous = (data["isAnonymous"] as? Boolean) ?: false,
            status = (data["status"] as? String) ?: "open",
            responsesCount = ((data["responsesCount"] as? Number)?.toInt()) ?: 0,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    // ==========================================
    // Seed/Fallback Community Data
    // ==========================================

    private fun getFallbackJobSeekers(city: String?, skill: String?): List<JobSeekerProfile> {
        val base = listOf(
            JobSeekerProfile(
                id = "sample_job_seeker_1",
                userId = "sample_user_chitrali_1",
                name = "Kareemullah Chitrali",
                handle = "@kareem_tech",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                qualifications = "BS Computer Science (University of Chitral)",
                skills = listOf("Android", "Kotlin", "Web Design", "Graphic Design"),
                experienceYears = 2,
                preferredRoles = listOf("Mobile App Developer", "IT Support Specialist", "Graphic Designer"),
                preferredCities = listOf("Chitral", "Islamabad", "Remote"),
                summary = "Passionate young developer from Yarkhoon seeking remote software development or on-site IT support roles.",
                contactPreference = "in_app_chat",
                isPhonePublic = false,
                isActivelySeeking = true,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            JobSeekerProfile(
                id = "sample_job_seeker_2",
                userId = "sample_user_chitrali_2",
                name = "Amina Bibi",
                handle = "@amina_edu",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
                qualifications = "M.Sc Mathematics & B.Ed",
                skills = listOf("Teaching", "Curriculum Planning", "Mathematics", "Science"),
                experienceYears = 4,
                preferredRoles = listOf("Senior School Teacher", "Lecturer", "Educational Coordinator"),
                preferredCities = listOf("Chitral", "Peshawar"),
                summary = "Experienced mathematics and science educator with 4 years experience in local community colleges.",
                contactPreference = "in_app_chat",
                isPhonePublic = false,
                isActivelySeeking = true,
                createdAt = System.currentTimeMillis() - 86400000L * 5
            )
        )
        return base.filter { p ->
            (city.isNullOrBlank() || city == "All Cities" || p.preferredCities.contains(city)) &&
            (skill.isNullOrBlank() || p.skills.any { it.contains(skill, ignoreCase = true) })
        }
    }

    private fun getFallbackJobVacancies(city: String?, query: String?): List<JobVacancy> {
        val base = listOf(
            JobVacancy(
                id = "sample_vac_1",
                employerId = "sample_emp_1",
                employerName = "Hindukush Community Foundation",
                employerHandle = "@hindukush_ngo",
                employerAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                title = "Field Monitoring Officer",
                companyName = "Hindukush Community Foundation",
                city = "Chitral",
                location = "Upper Chitral & Yarkhoon",
                jobType = "Full-time",
                salaryRange = "PKR 45,000 - 60,000 / month",
                requiredSkills = listOf("Community Outreach", "Report Writing", "Field Surveys", "Khowar & Urdu Fluency"),
                description = "Responsible for regular monitoring of educational development programs and water supply schemes across Yarkhoon Valley.",
                applicationInstructions = "Contact via in-app message or submit CV details through your Yarkhoon profile.",
                status = "active",
                createdAt = System.currentTimeMillis() - 86400000L
            ),
            JobVacancy(
                id = "sample_vac_2",
                employerId = "sample_emp_2",
                employerName = "Pamir Mountain Logistics",
                employerHandle = "@pamir_logistics",
                employerAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
                title = "Accounts & Inventory Associate",
                companyName = "Pamir Mountain Logistics",
                city = "Islamabad",
                location = "I-9 Industrial Area",
                jobType = "Full-time",
                salaryRange = "PKR 55,000 - 75,000 / month",
                requiredSkills = listOf("Bookkeeping", "Excel", "Inventory Management", "QuickBooks"),
                description = "Managing supply shipments and invoice reconciliation between Chitral transport hubs and Islamabad warehouse.",
                applicationInstructions = "Message via Yarkhoon chat with your educational summary.",
                status = "active",
                createdAt = System.currentTimeMillis() - 86400000L * 3
            )
        )
        return base.filter { v ->
            (city.isNullOrBlank() || city == "All Cities" || v.city.equals(city, ignoreCase = true)) &&
            (query.isNullOrBlank() || v.title.contains(query, ignoreCase = true) || v.companyName.contains(query, ignoreCase = true))
        }
    }

    private fun getFallbackBloodDonors(bloodGroup: String?, city: String?): List<BloodDonor> {
        val base = listOf(
            BloodDonor(
                id = "sample_donor_1",
                userId = "sample_donor_user_1",
                name = "Sultan Ahmed",
                handle = "@sultan_chitral",
                bloodGroup = "O+",
                city = "Chitral",
                area = "Atalique Bazaar / DHQ Vicinity",
                isAvailable = true,
                lastDonationDate = "4 months ago",
                contactPreference = "in_app_chat",
                registeredAt = System.currentTimeMillis() - 86400000L * 10
            ),
            BloodDonor(
                id = "sample_donor_2",
                userId = "sample_donor_user_2",
                name = "Zubair Shah",
                handle = "@zubair_booni",
                bloodGroup = "B+",
                city = "Booni",
                area = "Booni Main Town",
                isAvailable = true,
                lastDonationDate = "6 months ago",
                contactPreference = "in_app_chat",
                registeredAt = System.currentTimeMillis() - 86400000L * 14
            ),
            BloodDonor(
                id = "sample_donor_3",
                userId = "sample_donor_user_3",
                name = "Tariq Aziz",
                handle = "@tariq_mastuj",
                bloodGroup = "A+",
                city = "Mastuj",
                area = "Mastuj Tehsil",
                isAvailable = true,
                lastDonationDate = "3 months ago",
                contactPreference = "in_app_chat",
                registeredAt = System.currentTimeMillis() - 86400000L * 8
            ),
            BloodDonor(
                id = "sample_donor_4",
                userId = "sample_donor_user_4",
                name = "Naveed Ur Rehman",
                handle = "@naveed_isb",
                bloodGroup = "AB+",
                city = "Islamabad",
                area = "G-9 / PIMS Vicinity",
                isAvailable = true,
                lastDonationDate = "5 months ago",
                contactPreference = "in_app_chat",
                registeredAt = System.currentTimeMillis() - 86400000L * 20
            ),
            BloodDonor(
                id = "sample_donor_5",
                userId = "sample_donor_user_5",
                name = "Dr. Farooq Khan",
                handle = "@dr_farooq",
                bloodGroup = "O-",
                city = "Peshawar",
                area = "Hayatabad / HMC Vicinity",
                isAvailable = true,
                lastDonationDate = "7 months ago",
                contactPreference = "in_app_chat",
                registeredAt = System.currentTimeMillis() - 86400000L * 30
            )
        )
        return base.filter { d ->
            (bloodGroup.isNullOrBlank() || bloodGroup == "All Groups" || d.bloodGroup.equals(bloodGroup, ignoreCase = true)) &&
            (city.isNullOrBlank() || city == "All Cities" || d.city.equals(city, ignoreCase = true))
        }
    }

    private fun getFallbackBloodRequests(city: String?, bloodGroup: String?): List<BloodRequest> {
        val base = listOf(
            BloodRequest(
                id = "sample_blood_req_1",
                requesterId = "sample_req_1",
                requesterName = "Rashid Minhas",
                requesterHandle = "@rashid_m",
                patientName = "Elderly Patient (Dialysis Unit)",
                bloodGroup = "O+",
                hospitalName = "DHQ Hospital Chitral",
                city = "Chitral",
                requiredUnits = 2,
                urgency = "CRITICAL_URGENT",
                contactPhone = "Contact via in-app chat",
                notes = "Emergency surgery scheduled for tomorrow morning. Volunteers please reach out immediately.",
                status = "active",
                createdAt = System.currentTimeMillis() - 3600000L * 3,
                expiresAt = System.currentTimeMillis() + 3600000L * 45
            ),
            BloodRequest(
                id = "sample_blood_req_2",
                requesterId = "sample_req_2",
                requesterName = "Gulzar Ali",
                requesterHandle = "@gulzar_booni",
                patientName = "Post-operative patient",
                bloodGroup = "B+",
                hospitalName = "THQ Hospital Booni",
                city = "Booni",
                requiredUnits = 1,
                urgency = "URGENT",
                contactPhone = "Contact via in-app chat",
                notes = "Required for blood transfusion after orthopedic surgery.",
                status = "active",
                createdAt = System.currentTimeMillis() - 3600000L * 8,
                expiresAt = System.currentTimeMillis() + 3600000L * 40
            )
        )
        return base.filter { r ->
            (city.isNullOrBlank() || city == "All Cities" || r.city.equals(city, ignoreCase = true)) &&
            (bloodGroup.isNullOrBlank() || bloodGroup == "All Groups" || r.bloodGroup.equals(bloodGroup, ignoreCase = true))
        }
    }

    private fun getFallbackAidRequests(category: String?): List<CommunityAidRequest> {
        val base = listOf(
            CommunityAidRequest(
                id = "sample_aid_1",
                requesterId = "sample_aid_req_1",
                requesterName = "Mohammad Younus",
                requesterHandle = "@younus_yarkhoon",
                beneficiaryName = "Low-income family with specialized patient",
                category = "Medical Treatment",
                title = "Emergency Support for Cardiac Medication & Travel",
                description = "A resident of Upper Yarkhoon requiring travel assistance to Peshawar for urgent cardiac stent follow-up. Family has exhausted local resources.",
                city = "Chitral",
                location = "Upper Yarkhoon Valley",
                assistanceType = "Medical Supplies & Travel",
                targetAmount = 45000.0,
                status = "active",
                verificationStatus = "verified_by_moderator",
                verifiedBy = "Yarkhoon Welfare Committee",
                verificationNotes = "Prescription and travel referral verified by local community elder.",
                contactMethod = "in_app_message",
                supportOffersCount = 3,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            CommunityAidRequest(
                id = "sample_aid_2",
                requesterId = "sample_aid_req_2",
                requesterName = "Fatima Karim",
                requesterHandle = "@fatima_edu",
                beneficiaryName = "Meritorious orphan student",
                category = "Education / Fees",
                title = "University Semester Fee Support for Engineering Student",
                description = "Hardworking student from Mastuj who secured merit admission in UET Peshawar. Requires assistance for 2nd semester laboratory fee.",
                city = "Peshawar",
                location = "Mastuj / Peshawar",
                assistanceType = "Education Material & Fee",
                targetAmount = 38000.0,
                status = "active",
                verificationStatus = "verified_by_moderator",
                verifiedBy = "Chitral Students Welfare",
                verificationNotes = "Admission letter and fee challan verified.",
                contactMethod = "in_app_message",
                supportOffersCount = 2,
                createdAt = System.currentTimeMillis() - 86400000L * 4
            )
        )
        return if (!category.isNullOrBlank() && category != "All Categories") {
            base.filter { it.category.equals(category, ignoreCase = true) }
        } else {
            base
        }
    }
}
