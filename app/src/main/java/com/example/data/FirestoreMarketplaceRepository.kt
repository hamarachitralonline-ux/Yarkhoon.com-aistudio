package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreMarketplaceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val MARKETPLACE_COLLECTION = "marketplaceListings"
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    private fun parseMarketplaceDocument(doc: com.google.firebase.firestore.DocumentSnapshot): MarketplaceItem {
        val data = doc.data ?: return MarketplaceItem(
            id = doc.id,
            seller = User(id = "unknown", name = "Seller", handle = "@seller", avatarUrl = ""),
            title = "Item",
            category = MarketplaceCategory.OTHER,
            price = "PKR 0",
            description = "",
            phone = "",
            location = "Yarkhoon Valley"
        )

        val sellerMap = data["seller"] as? Map<*, *>
        val sellerUser = if (sellerMap != null) {
            User(
                id = (data["sellerId"] as? String) ?: (sellerMap["id"] as? String) ?: doc.id,
                name = (sellerMap["name"] as? String) ?: "Seller",
                handle = (sellerMap["handle"] as? String) ?: "@seller",
                avatarUrl = (sellerMap["avatarUrl"] as? String) ?: "",
                isVerified = (sellerMap["isVerified"] as? Boolean) ?: false
            )
        } else {
            val sellerId = (data["sellerId"] as? String) ?: "unknown"
            User(
                id = sellerId,
                name = (data["sellerName"] as? String) ?: "Seller",
                handle = "@seller",
                avatarUrl = ""
            )
        }

        val categoryStr = (data["category"] as? String) ?: ""
        val category = try {
            MarketplaceCategory.valueOf(categoryStr)
        } catch (_: Exception) {
            MarketplaceCategory.values().find { it.displayName.equals(categoryStr, ignoreCase = true) }
                ?: MarketplaceCategory.OTHER
        }

        return MarketplaceItem(
            id = doc.id,
            seller = sellerUser,
            title = (data["title"] as? String) ?: "Item",
            category = category,
            price = (data["price"] as? String) ?: "PKR 0",
            description = (data["description"] as? String) ?: "",
            phone = (data["phone"] as? String) ?: "",
            whatsapp = (data["whatsapp"] as? String) ?: "",
            location = (data["location"] as? String) ?: "Yarkhoon Valley",
            condition = (data["condition"] as? String) ?: "Good",
            status = (data["status"] as? String) ?: "active",
            imageUrls = (data["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            createdAt = (data["createdAtDisplay"] as? String) ?: "Recently",
            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    suspend fun getMarketplaceListings(): Result<List<MarketplaceItem>> {
        return try {
            val snapshot = firestore.collection(MARKETPLACE_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val list = snapshot.documents.map { parseMarketplaceDocument(it) }
            Result.success(list)
        } catch (e: Exception) {
            try {
                val fallbackSnapshot = firestore.collection(MARKETPLACE_COLLECTION)
                    .limit(50)
                    .get()
                    .await()
                val list = fallbackSnapshot.documents
                    .map { parseMarketplaceDocument(it) }
                    .sortedByDescending { it.timestamp }
                Result.success(list)
            } catch (fallbackEx: Exception) {
                Result.failure(mapFirestoreException(e))
            }
        }
    }

    suspend fun createMarketplaceListing(item: MarketplaceItem): Result<MarketplaceItem> {
        val currentUid = currentAuthUid
        if (currentUid == null) {
            return Result.failure(IllegalStateException("User is not signed in. Please sign in to list items for sale."))
        }

        val itemId = if (item.id.isNotBlank() && item.id.startsWith("item_")) {
            item.id
        } else {
            "item_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        }

        val now = System.currentTimeMillis()

        return try {
            val data = hashMapOf<String, Any?>(
                "id" to itemId,
                "sellerId" to currentUid,
                "sellerName" to item.seller.name,
                "seller" to mapOf(
                    "id" to currentUid,
                    "name" to item.seller.name,
                    "handle" to item.seller.handle,
                    "avatarUrl" to item.seller.avatarUrl,
                    "isVerified" to item.seller.isVerified
                ),
                "title" to item.title.trim(),
                "category" to item.category.name,
                "categoryDisplayName" to item.category.displayName,
                "price" to item.price.trim(),
                "description" to item.description.trim(),
                "phone" to item.phone.trim(),
                "whatsapp" to item.whatsapp.trim().ifBlank { item.phone.trim() },
                "location" to item.location.trim().ifBlank { "Yarkhoon Valley" },
                "condition" to item.condition.trim(),
                "status" to item.status.ifBlank { "active" },
                "imageUrls" to item.imageUrls,
                "createdAtDisplay" to "Just now",
                "timestamp" to now,
                "updatedAt" to now
            )

            firestore.collection(MARKETPLACE_COLLECTION)
                .document(itemId)
                .set(data, SetOptions.merge())
                .await()

            val created = item.copy(
                id = itemId,
                seller = item.seller.copy(id = currentUid),
                timestamp = now,
                updatedAt = now
            )
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun updateMarketplaceListing(item: MarketplaceItem): Result<MarketplaceItem> {
        val currentUid = currentAuthUid
        if (currentUid == null) {
            return Result.failure(IllegalStateException("User is not signed in. Please sign in to edit listings."))
        }
        if (item.seller.id != currentUid) {
            return Result.failure(SecurityException("Unauthorized: You can only edit your own marketplace listing."))
        }

        val now = System.currentTimeMillis()

        return try {
            val updates = hashMapOf<String, Any?>(
                "title" to item.title.trim(),
                "category" to item.category.name,
                "categoryDisplayName" to item.category.displayName,
                "price" to item.price.trim(),
                "description" to item.description.trim(),
                "phone" to item.phone.trim(),
                "whatsapp" to item.whatsapp.trim(),
                "location" to item.location.trim().ifBlank { "Yarkhoon Valley" },
                "condition" to item.condition.trim(),
                "status" to item.status,
                "imageUrls" to item.imageUrls,
                "updatedAt" to now
            )

            firestore.collection(MARKETPLACE_COLLECTION)
                .document(item.id)
                .update(updates)
                .await()

            Result.success(item.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun markListingSold(listingId: String, currentUserId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection(MARKETPLACE_COLLECTION).document(listingId)
            val doc = docRef.get().await()
            val sellerId = doc.getString("sellerId") ?: ""
            if (sellerId != currentUserId) {
                return Result.failure(SecurityException("Unauthorized: Only the seller can mark this item as sold."))
            }

            docRef.update(
                mapOf(
                    "status" to "sold",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun deleteMarketplaceListing(listingId: String): Result<Unit> {
        val currentUid = currentAuthUid
        if (currentUid == null) {
            return Result.failure(IllegalStateException("User is not signed in. Please sign in to delete listings."))
        }

        return try {
            val docRef = firestore.collection(MARKETPLACE_COLLECTION).document(listingId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return Result.success(Unit)
            }
            val sellerId = doc.getString("sellerId") ?: ""
            if (sellerId != currentUid) {
                return Result.failure(SecurityException("Unauthorized: You can only delete your own marketplace listing."))
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
