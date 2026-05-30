package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SocialMediaRepository(private val dao: SocialMediaDao) {

    val allPosts: Flow<List<Post>> = dao.getAllPosts()
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val currentUser: Flow<User?> = dao.getCurrentUser()
    val allGroups: Flow<List<Group>> = dao.getAllGroups()
    val allMarketplaceItems: Flow<List<MarketplaceItem>> = dao.getAllMarketplaceItems()
    val allServiceListings: Flow<List<ServiceListing>> = dao.getAllServiceListings()

    fun getChatMessages(myId: String, theirId: String): Flow<List<ChatMessage>> =
        dao.getChatMessages(myId, theirId)

    suspend fun insertPost(post: Post) = dao.insertPost(post)
    suspend fun updatePost(post: Post) = dao.updatePost(post)
    suspend fun deletePost(postId: Int) = dao.deletePostById(postId)

    suspend fun updateUser(user: User) = dao.updateUser(user)
    suspend fun insertUsers(users: List<User>) = dao.insertUsers(users)

    suspend fun deleteUser(userId: String) = dao.deleteUserById(userId)

    suspend fun insertChatMessage(message: ChatMessage) = dao.insertChatMessage(message)

    suspend fun insertGroups(groups: List<Group>) = dao.insertGroups(groups)
    suspend fun updateGroup(group: Group) = dao.updateGroup(group)

    suspend fun insertMarketplaceItem(item: MarketplaceItem) = dao.insertMarketplaceItem(item)
    suspend fun updateMarketplaceItem(item: MarketplaceItem) = dao.updateMarketplaceItem(item)

    suspend fun insertServiceListing(listing: ServiceListing) = dao.insertServiceListing(listing)
    suspend fun deleteServiceListing(listingId: Int) = dao.deleteServiceListingById(listingId)

    // Check if empty and prepopulate with realistic social media data
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val allUsers = dao.getAllUsers().first()
        if (allUsers.isEmpty()) {
            val defaultCurrentUser = User(
                id = "currentUser",
                username = "",
                fullName = "",
                avatarUrl = "",
                coverUrl = "",
                bio = "",
                friendStatus = "NONE",
                isCurrentUser = true,
                isProfileCompleted = false,
                email = "",
                password = ""
            )

            val initialUsers = listOf(
                defaultCurrentUser,
                User(
                    id = "user_ali",
                    username = "alikhan99",
                    fullName = "Ali Khan",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=500&auto=format&fit=crop",
                    bio = "Tech enthusiast, Android developer, and hiker. Let's build community tech solutions!",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "ali@yarkhoon.com",
                    password = "password123"
                ),
                User(
                    id = "user_zara",
                    username = "zarashah",
                    fullName = "Zara Shah",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=500&auto=format&fit=crop",
                    bio = "Founder of Valley Threads. Empowering female weavers & sharing Chitrali walnut woodwork globally 🧵✨",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = false,
                    email = "zara@yarkhoon.com",
                    password = "password123"
                ),
                User(
                    id = "user_sher",
                    username = "sherfilm",
                    fullName = "Sher Jang",
                    avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=500&auto=format&fit=crop",
                    bio = "Filmmaker & mountaineer. Documenting untouched glaciers and peaks of High Yarkhoon Valley 🏔️🎥",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "sher@yarkhoon.com",
                    password = "password123"
                ),
                User(
                    id = "user_shazia",
                    username = "shazia_edu",
                    fullName = "Shazia Parveen",
                    avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=500&auto=format&fit=crop",
                    bio = "Teacher at Yarkhoon Public Academy. Passionate about literacy, poetry, and children's welfare.",
                    friendStatus = "NONE",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = false,
                    email = "shazia@yarkhoon.com",
                    password = "password123"
                )
            )
            dao.insertUsers(initialUsers)

            // Feed posts
            val initialPosts = listOf(
                Post(
                    authorId = "user_sher",
                    authorName = "Sher Jang",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    content = "Exploring the upper heights of Yarkhoon Valley near the Broghil Pass! Check out this short video update of our trek. The scenery is absolutely breathtaking. Hindukush holds so many hidden wonders!",
                    mediaType = "VIDEO",
                    mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                    likesCount = 34,
                    isLikedByMe = true,
                    commentsCount = 8
                ),
                Post(
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    content = "Proudly launching our premium handwoven winter woolen blankets (Shu) on the yarkhoon.com Marketplace! Check the Marketplace tab for more details. Supporting hand-weaving families directly in our valley.",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1580301762395-21ce84d00bc6?w=600&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                    likesCount = 28,
                    isLikedByMe = false,
                    commentsCount = 3
                ),
                Post(
                    authorId = "user_shazia",
                    authorName = "Shazia Parveen",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop",
                    content = "Today in the Yarkhoon Public Academy library, we received new textbooks donated by the online community! Thank you all who participated in our book drive. Education is the ultimate bridge to a brighter future. 📚🎓",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 14400000, // 4 hours ago
                    likesCount = 42,
                    isLikedByMe = false,
                    commentsCount = 12
                ),
                Post(
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    content = "Hello world! Welcome to yarkhoon.com - our customized social network. Built this platform so we can connect, post, share hiking groups, trade items locally, and build secure communication pathways for everyone in the valley and beyond! Let me know if you want to collaborate on open-source code.",
                    mediaType = "NONE",
                    mediaUrl = "",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    likesCount = 95,
                    isLikedByMe = true,
                    commentsCount = 24
                )
            )
            for (post in initialPosts) {
                dao.insertPost(post)
            }

            // Groups
            val initialGroups = listOf(
                Group(
                    name = "Hindukush & Yarkhoon Explorers",
                    description = "A group for mountain trekkers, hikers, climbers, and photographers to coordinate and share expeditions across Yarkhoon Valley.",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop",
                    category = "Active & Travel",
                    memberCount = 1240,
                    isJoined = true
                ),
                Group(
                    name = "Chitral Bazaar Online",
                    description = "Buy or sell handmade items, dried fruits, woolen garments (Shu, Patti), pakols, and local services in Chitral.",
                    coverUrl = "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?w=600&auto=format&fit=crop",
                    category = "Marketplace & Trade",
                    memberCount = 3850,
                    isJoined = false
                ),
                Group(
                    name = "Khowar Language & Culture Preservation",
                    description = "For discussion of local poetry, ancient folk songs, dances, historic sites, and preservation of Khowar language.",
                    coverUrl = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=600&auto=format&fit=crop",
                    category = "Art & Culture",
                    memberCount = 670,
                    isJoined = false
                ),
                Group(
                    name = "Hindukush Tech Community",
                    description = "Youth learning programming, web design, Android development, digital marketing, and remote services.",
                    coverUrl = "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?w=600&auto=format&fit=crop",
                    category = "Education & Tech",
                    memberCount = 420,
                    isJoined = false
                )
            )
            dao.insertGroups(initialGroups)

            // Chat initial messages
            val chatPartner = "user_ali"
            val initialMessages = listOf(
                ChatMessage(
                    senderId = chatPartner,
                    receiverId = "currentUser",
                    content = "Hey! This app looks fantastic. Is the private chat working smoothly?",
                    timestamp = System.currentTimeMillis() - 36000000 // 10 hours ago
                ),
                ChatMessage(
                    senderId = "currentUser",
                    receiverId = chatPartner,
                    content = "Yes, Ali! It connects directly to our local Room database. You can send messages anytime.",
                    timestamp = System.currentTimeMillis() - 32000000 // 9 hours ago
                ),
                ChatMessage(
                    senderId = chatPartner,
                    receiverId = "currentUser",
                    content = "That's awesome! Offline-first persistence makes it very reliable.",
                    timestamp = System.currentTimeMillis() - 28000000 // 8 hours ago
                )
            )
            for (msg in initialMessages) {
                dao.insertChatMessage(msg)
            }

            // Marketplace items
            val initialItems = listOf(
                MarketplaceItem(
                    title = "Premium Handwoven Chitrali Woolen Blazer (Shu)",
                    description = "Gorgeous traditional overcoat made of 100% pure mountain sheep wool, handwoven by master weavers in Yarkhoon Valley. Thick, super-warm, wind-resistant, and styled to last a lifetime.",
                    price = 85.00,
                    imageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400&auto=format&fit=crop",
                    category = "Apparel",
                    sellerId = "user_zara",
                    sellerName = "Zara Shah",
                    sellerContact = "zara@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Authentic Brown Chitrali Pakol Cap",
                    description = "Authentic round rolled woolen pakol cap. Crafted from fine local wool, incredibly soft, and perfect for cold mountain evenings.",
                    price = 15.00,
                    imageUrl = "https://images.unsplash.com/photo-1521369909029-2afed882baee?w=400&auto=format&fit=crop", // Cap / Hat vibe
                    category = "Apparel",
                    sellerId = "user_zara",
                    sellerName = "Zara Shah",
                    sellerContact = "+92-345-XXXXXXX",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Premium Handcarved Walnut Wood Bowl",
                    description = "Exquisitely finished decorative and functional serving bowl, carved out of single-piece mature walnut wood from lower Chitral orchards. Excellent natural grains with protective food-grade finish.",
                    price = 40.00,
                    imageUrl = "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400&auto=format&fit=crop", // Bowl / craft vibe
                    category = "Home Decor",
                    sellerId = "user_ali",
                    sellerName = "Ali Khan",
                    sellerContact = "ali@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Organic Sun-Dried Apricots (1kg)",
                    description = "Naturally sun-dried organic sweet apricots harvested from pure water orchards in Yarkhoon. Unsulfured, high fiber, completely sweet and natural mountain energy booster.",
                    price = 12.00,
                    imageUrl = "https://images.unsplash.com/photo-1596515109352-00df2180ec62?w=400&auto=format&fit=crop", // Dried fruits vibe
                    category = "Food & Edibles",
                    sellerId = "user_shazia",
                    sellerName = "Shazia Parveen",
                    sellerContact = "school-office@yarkhoon.com",
                    isSold = false
                )
            )
            for (item in initialItems) {
                dao.insertMarketplaceItem(item)
            }

            // Default Service Listings
            val initialServices = listOf(
                ServiceListing(
                    providerId = "user_ali",
                    providerName = "Ali Khan",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    serviceType = "Plumber",
                    description = "Professional plumbing and water pipe installation services. Over 8 years of experience working in Chitral and Yarkhoon Valley. Specialty in solar water heater mounting and repairs.",
                    phoneNumber = "+92-300-9876543",
                    imageUrl = "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                ServiceListing(
                    providerId = "user_sher",
                    providerName = "Sher Jang",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    serviceType = "Driver",
                    description = "Reliable private 4x4 Jeep driver for off-road mountain trips. Safe driving to Broghil Pass, Mastuj, Chitral town, and Gilgit. Native guide who knows wilderness routes & accommodations.",
                    phoneNumber = "+92-345-4433221",
                    imageUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 36000000
                ),
                ServiceListing(
                    providerId = "user_zara",
                    providerName = "Zara Shah",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    serviceType = "Guest House",
                    description = "Welcome to Valley Crest Guest House located in scenic upper Yarkhoon. Traditional wooden architecture, hot organic tea, fresh garden apricots, and panoramic Hindukush views. Wi-Fi available.",
                    phoneNumber = "+92-333-1122334",
                    imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 14400000
                ),
                ServiceListing(
                    providerId = "user_shazia",
                    providerName = "Shazia Parveen",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop",
                    serviceType = "Carpenter",
                    description = "Chitrali traditional walnut wood engraving, door, window design, and furniture crafting. Specialist in local ceiling geometric patterns (Khatamband). Beautiful artisan work.",
                    phoneNumber = "+92-321-5556677",
                    imageUrl = "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
            for (service in initialServices) {
                dao.insertServiceListing(service)
            }
        }
    }
}

