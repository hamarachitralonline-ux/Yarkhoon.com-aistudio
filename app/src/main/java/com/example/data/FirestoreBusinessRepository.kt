package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreBusinessRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val BUSINESS_COLLECTION = "businessListings"
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    private fun parseBusinessDocument(doc: com.google.firebase.firestore.DocumentSnapshot): BusinessProfile {
        val data = doc.data ?: return BusinessProfile(
            id = doc.id,
            owner = User(id = "unknown", name = "Local Business", handle = "@business", avatarUrl = ""),
            businessName = "Business",
            category = BusinessCategory.OTHER,
            serviceHighlight = "",
            description = "",
            location = "Yarkhoon Valley",
            phone = ""
        )

        val ownerMap = data["owner"] as? Map<*, *>
        val ownerUser = if (ownerMap != null) {
            User(
                id = (data["ownerId"] as? String) ?: (ownerMap["id"] as? String) ?: doc.id,
                name = (ownerMap["name"] as? String) ?: "Local Business",
                handle = (ownerMap["handle"] as? String) ?: "@business",
                avatarUrl = (ownerMap["avatarUrl"] as? String) ?: "",
                isVerified = (ownerMap["isVerified"] as? Boolean) ?: false
            )
        } else {
            val ownerId = (data["ownerId"] as? String) ?: "unknown"
            User(
                id = ownerId,
                name = (data["ownerName"] as? String) ?: "Local Business",
                handle = "@business",
                avatarUrl = ""
            )
        }

        val categoryStr = (data["category"] as? String) ?: ""
        val category = try {
            BusinessCategory.valueOf(categoryStr)
        } catch (_: Exception) {
            BusinessCategory.values().find { it.displayName.equals(categoryStr, ignoreCase = true) }
                ?: BusinessCategory.OTHER
        }

        return BusinessProfile(
            id = doc.id,
            owner = ownerUser,
            businessName = (data["businessName"] as? String) ?: "Local Business",
            category = category,
            serviceHighlight = (data["serviceHighlight"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            location = (data["location"] as? String) ?: "Yarkhoon Valley",
            phone = (data["phone"] as? String) ?: "",
            whatsapp = (data["whatsapp"] as? String) ?: "",
            priceRange = (data["priceRange"] as? String) ?: "",
            rating = (data["rating"] as? Number)?.toFloat() ?: 5.0f,
            reviewsCount = (data["reviewsCount"] as? Number)?.toInt() ?: 1,
            isVerified = (data["isVerified"] as? Boolean) ?: false,
            status = (data["status"] as? String) ?: "active",
            portfolioImages = (data["portfolioImages"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    suspend fun getBusinessListings(): Result<List<BusinessProfile>> {
        return try {
            val snapshot = firestore.collection(BUSINESS_COLLECTION)
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val list = snapshot.documents.map { parseBusinessDocument(it) }
            Result.success(list)
        } catch (e: Exception) {
            // Fallback: try querying without order if index is building or composite constraint
            try {
                val fallbackSnapshot = firestore.collection(BUSINESS_COLLECTION)
                    .limit(50)
                    .get()
                    .await()
                val list = fallbackSnapshot.documents
                    .map { parseBusinessDocument(it) }
                    .filter { it.status == "active" }
                    .sortedByDescending { it.createdAt }
                Result.success(list)
            } catch (fallbackEx: Exception) {
                Result.failure(mapFirestoreException(e))
            }
        }
    }

    suspend fun createBusinessListing(business: BusinessProfile): Result<BusinessProfile> {
        val currentUid = currentAuthUid
        if (currentUid == null) {
            return Result.failure(IllegalStateException("User is not signed in. Please sign in to register a business."))
        }

        val businessId = if (business.id.isNotBlank() && business.id.startsWith("biz_")) {
            business.id
        } else {
            "biz_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        }

        val now = System.currentTimeMillis()

        return try {
            val data = hashMapOf<String, Any?>(
                "id" to businessId,
                "ownerId" to currentUid,
                "ownerName" to business.owner.name,
                "owner" to mapOf(
                    "id" to currentUid,
                    "name" to business.owner.name,
                    "handle" to business.owner.handle,
                    "avatarUrl" to business.owner.avatarUrl,
                    "isVerified" to business.owner.isVerified
                ),
                "businessName" to business.businessName.trim(),
                "category" to business.category.name,
                "categoryDisplayName" to business.category.displayName,
                "serviceHighlight" to business.serviceHighlight.trim(),
                "description" to business.description.trim(),
                "location" to business.location.trim().ifBlank { "Yarkhoon Valley" },
                "phone" to business.phone.trim(),
                "whatsapp" to business.whatsapp.trim().ifBlank { business.phone.trim() },
                "priceRange" to business.priceRange.trim(),
                "rating" to business.rating,
                "reviewsCount" to business.reviewsCount,
                "isVerified" to business.isVerified,
                "status" to "active",
                "portfolioImages" to business.portfolioImages,
                "createdAt" to now,
                "updatedAt" to now
            )

            firestore.collection(BUSINESS_COLLECTION)
                .document(businessId)
                .set(data, SetOptions.merge())
                .await()

            val created = business.copy(
                id = businessId,
                owner = business.owner.copy(id = currentUid),
                createdAt = now,
                updatedAt = now
            )
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun updateBusinessListing(business: BusinessProfile): Result<BusinessProfile> {
        val currentUid = currentAuthUid
        if (currentUid == null) {
            return Result.failure(IllegalStateException("User is not signed in. Please sign in to edit listings."))
        }
        if (business.owner.id != currentUid) {
            return Result.failure(SecurityException("Unauthorized: You can only edit your own business listing."))
        }

        val now = System.currentTimeMillis()

        return try {
            val updates = hashMapOf<String, Any?>(
                "businessName" to business.businessName.trim(),
                "category" to business.category.name,
                "categoryDisplayName" to business.category.displayName,
                "serviceHighlight" to business.serviceHighlight.trim(),
                "description" to business.description.trim(),
                "location" to business.location.trim().ifBlank { "Yarkhoon Valley" },
                "phone" to business.phone.trim(),
                "whatsapp" to business.whatsapp.trim(),
                "priceRange" to business.priceRange.trim(),
                "portfolioImages" to business.portfolioImages,
                "status" to business.status,
                "updatedAt" to now
            )

            firestore.collection(BUSINESS_COLLECTION)
                .document(business.id)
                .update(updates)
                .await()

            Result.success(business.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun deleteBusinessListing(listingId: String): Result<Unit> {
        val currentUid = currentAuthUid
        if (currentUid == null) {
            return Result.failure(IllegalStateException("User is not signed in. Please sign in to delete listings."))
        }

        return try {
            val docRef = firestore.collection(BUSINESS_COLLECTION).document(listingId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return Result.success(Unit)
            }
            val ownerId = doc.getString("ownerId") ?: ""
            if (ownerId != currentUid) {
                return Result.failure(SecurityException("Unauthorized: You can only delete your own business listing."))
            }

            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private fun mapFirestoreException(e: Exception): Exception {
        if (e is FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied: Access not granted by Firestore security rules."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Cloud Firestore service is temporarily unavailable."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Connection timed out contacting Firestore."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "User session expired. Please sign in again."
                else -> e.localizedMessage ?: "Firestore error: ${e.code}"
            }
            return Exception(message, e)
        }
        return Exception(e.localizedMessage ?: "An error occurred with Cloud Firestore.", e)
    }
}
