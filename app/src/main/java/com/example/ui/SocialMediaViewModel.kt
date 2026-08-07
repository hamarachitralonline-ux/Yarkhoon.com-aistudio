package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SocialMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SocialMediaRepository(database.socialMediaDao)

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firebase initialization skipped (google-services.json missing or offline): ${e.localizedMessage}")
            null
        }
    }

    private val firebaseFirestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firestore initialization skipped: ${e.localizedMessage}")
            null
        }
    }

    // Exposed Flows
    val currentUser: StateFlow<User?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPosts: StateFlow<List<Post>> = repository.allPosts
        .map { list ->
            list.sortedWith(
                compareByDescending<Post> { it.isViral }
                    .thenByDescending { it.timestamp }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<Group>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMarketplaceItems: StateFlow<List<MarketplaceItem>> = repository.allMarketplaceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServiceListings: StateFlow<List<ServiceListing>> = repository.allServiceListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat handling
    private val _activeChatUserId = MutableStateFlow<String?>(null)
    val activeChatUserId: StateFlow<String?> = _activeChatUserId.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = combine(_activeChatUserId, repository.currentUser) { activeId, currUser ->
        Pair(activeId, currUser)
    }.flatMapLatest { (userId, currUser) ->
        val myId = currUser?.id ?: "currentUser"
        if (userId != null) {
            repository.getChatMessages(myId, userId)
        } else {
            flowOf(emptyList())
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.prepopulateIfEmpty()
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Failed to prepopulate database: ${e.localizedMessage}", e)
            }
        }
    }

    // --- Actions ---

    fun setActiveChatUser(userId: String?) {
        _activeChatUserId.value = userId
    }

    fun onCreatePost(content: String, mediaType: String, mediaUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val authorName = user?.fullName ?: "Hamara Chitral"
            val authorAvatarUrl = user?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop"

            val isUserYarkhoon = user?.id == "user_yarkhoon" || user?.fullName == "Yarkhoon.com"

            val newPost = Post(
                authorId = user?.id ?: "currentUser",
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                content = content,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis(),
                likesCount = if (isUserYarkhoon) 2450 else 0,
                isLikedByMe = if (isUserYarkhoon) true else false,
                commentsCount = if (isUserYarkhoon) 115 else 0,
                isViral = isUserYarkhoon
            )
            repository.insertPost(newPost)

            // Firestore sync
            try {
                val postMap = hashMapOf(
                    "authorId" to newPost.authorId,
                    "authorName" to newPost.authorName,
                    "authorAvatarUrl" to newPost.authorAvatarUrl,
                    "content" to newPost.content,
                    "mediaType" to newPost.mediaType,
                    "mediaUrl" to newPost.mediaUrl,
                    "timestamp" to newPost.timestamp,
                    "likesCount" to newPost.likesCount,
                    "commentsCount" to newPost.commentsCount,
                    "isViral" to newPost.isViral
                )
                firebaseFirestore?.collection("posts")?.add(postMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore post creation error: ${e.localizedMessage}")
            }
        }
    }

    fun onToggleLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPost = post.copy(
                isLikedByMe = !post.isLikedByMe,
                likesCount = if (post.isLikedByMe) post.likesCount - 1 else post.likesCount + 1
            )
            repository.updatePost(updatedPost)
        }
    }

    fun onAddComment(post: Post, commentText: String) {
        if (commentText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPost = post.copy(
                commentsCount = post.commentsCount + 1
            )
            repository.updatePost(updatedPost)
        }
    }

    fun onSendFriendRequest(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            if (user != null) {
                val updatedUser = user.copy(friendStatus = "SENT")
                repository.updateUser(updatedUser)
            }
        }
    }

    fun onAcceptFriendRequest(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            if (user != null) {
                val updatedUser = user.copy(friendStatus = "FRIENDS")
                repository.updateUser(updatedUser)
            }
        }
    }

    fun onRemoveFriend(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            if (user != null) {
                val updatedUser = user.copy(friendStatus = "NONE")
                repository.updateUser(updatedUser)
            }
        }
    }

    fun onJoinGroup(group: Group) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedGroup = group.copy(
                isJoined = !group.isJoined,
                memberCount = if (group.isJoined) group.memberCount - 1 else group.memberCount + 1
            )
            repository.updateGroup(updatedGroup)
        }
    }

    fun onSendChatMessage(theirId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val newMessage = ChatMessage(
                senderId = user?.id ?: "currentUser",
                receiverId = theirId,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(newMessage)

            // Firestore sync for chat messages
            try {
                val messageMap = hashMapOf(
                    "senderId" to newMessage.senderId,
                    "receiverId" to newMessage.receiverId,
                    "content" to newMessage.content,
                    "timestamp" to newMessage.timestamp
                )
                firebaseFirestore?.collection("chat_messages")?.add(messageMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore chat message sync error: ${e.localizedMessage}")
            }
        }
    }

    fun onCreateMarketplaceItem(title: String, description: String, price: Double, category: String, imageUrl: String, sellerContact: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val newItem = MarketplaceItem(
                title = title,
                description = description,
                price = price,
                imageUrl = imageUrl,
                category = category,
                sellerId = user?.id ?: "currentUser",
                sellerName = user?.fullName ?: "Hamara Chitral",
                sellerContact = sellerContact.ifBlank { "+92-345-1234567" },
                isSold = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMarketplaceItem(newItem)
        }
    }

    fun onCreateServiceListing(serviceType: String, description: String, phoneNumber: String, imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val newService = ServiceListing(
                providerId = user?.id ?: "currentUser",
                providerName = user?.fullName ?: "Hamara Chitral User",
                providerAvatarUrl = user?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop",
                serviceType = serviceType,
                description = description,
                phoneNumber = phoneNumber.ifBlank { "+92-345-1234567" },
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )
            repository.insertServiceListing(newService)
        }
    }

    fun onDeleteServiceListing(listingId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteServiceListing(listingId)
        }
    }

    fun onToggleMarketplaceItemSold(item: MarketplaceItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(isSold = !item.isSold)
            repository.updateMarketplaceItem(updated)
        }
    }

    fun onUpdateProfile(fullName: String, bio: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            if (user != null) {
                val updated = user.copy(
                    fullName = fullName,
                    bio = bio
                )
                repository.updateUser(updated)

                try {
                    val userMap = hashMapOf(
                        "id" to updated.id,
                        "username" to updated.username,
                        "fullName" to updated.fullName,
                        "bio" to updated.bio,
                        "avatarUrl" to updated.avatarUrl,
                        "coverUrl" to updated.coverUrl,
                        "email" to updated.email
                    )
                    firebaseFirestore?.collection("users")?.document(updated.id)?.set(userMap)
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firestore update profile sync error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onCompleteRegistration(fullName: String, username: String, email: String, password: String, bio: String, avatarUrl: String, coverUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val cleanEmail = email.trim().lowercase()

            // Firebase Auth Integration: create user in Firebase Auth
            if (cleanEmail.contains("@") && password.length >= 6) {
                try {
                    firebaseAuth?.createUserWithEmailAndPassword(cleanEmail, password)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                android.util.Log.d("SocialMediaViewModel", "Firebase Auth registered successfully: $cleanEmail")
                            } else {
                                // If account exists in Firebase Auth, attempt sign-in
                                firebaseAuth?.signInWithEmailAndPassword(cleanEmail, password)
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firebase Auth registration exception: ${e.localizedMessage}")
                }
            }

            // Set all other users as not current
            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }

            // Create a persistent completed user with username as ID
            val cleanUsername = username.trim().lowercase()
            val completeUser = User(
                id = cleanUsername,
                username = cleanUsername,
                fullName = fullName.trim(),
                bio = bio.trim(),
                avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop" },
                coverUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop" },
                isCurrentUser = true,
                isProfileCompleted = true,
                email = cleanEmail,
                password = password
            )
            repository.insertUsers(listOf(completeUser))

            // Sync user profile to Firestore
            try {
                val userMap = hashMapOf(
                    "id" to completeUser.id,
                    "username" to completeUser.username,
                    "fullName" to completeUser.fullName,
                    "bio" to completeUser.bio,
                    "avatarUrl" to completeUser.avatarUrl,
                    "coverUrl" to completeUser.coverUrl,
                    "email" to completeUser.email
                )
                firebaseFirestore?.collection("users")?.document(completeUser.id)?.set(userMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore complete registration sync error: ${e.localizedMessage}")
            }

            // Delete the placeholder user "currentUser" if that was the one being set up
            if (user != null && user.id == "currentUser") {
                repository.deleteUser("currentUser")
            }
        }
    }

    fun onAdminLoginSuccess() {
        viewModelScope.launch(Dispatchers.IO) {
            // Firebase Auth: sync admin account with Firebase Auth
            try {
                firebaseAuth?.signInWithEmailAndPassword("ceo@yarkhoon.com", "chitrali@786")
                    ?.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            firebaseAuth?.createUserWithEmailAndPassword("ceo@yarkhoon.com", "chitrali@786")
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firebase Auth admin login exception: ${e.localizedMessage}")
            }

            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }

            // Ensure admin user exists and is current
            val adminUser = User(
                id = "admin",
                username = "ceo",
                fullName = "Valley Administrator",
                bio = "Valley Administrator",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop",
                isCurrentUser = true,
                isProfileCompleted = true,
                email = "ceo@yarkhoon.com",
                password = "chitrali@786",
                isVerified = true
            )
            repository.insertUsers(listOf(adminUser))
        }
    }

    fun onSignOut() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firebase Auth sign out exception: ${e.localizedMessage}")
            }

            val user = currentUser.value
            if (user != null) {
                val updated = user.copy(isCurrentUser = false)
                repository.updateUser(updated)
                _activeChatUserId.value = null
            }
        }
    }

    fun onSignIn(emailOrUsername: String, passwordText: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanInput = emailOrUsername.trim().lowercase()
            val cleanPass = passwordText.trim()

            // Firebase Auth Sign In Attempt (if email format provided)
            if (cleanInput.contains("@") && cleanPass.isNotEmpty()) {
                try {
                    firebaseAuth?.signInWithEmailAndPassword(cleanInput, cleanPass)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                android.util.Log.d("SocialMediaViewModel", "Firebase Auth sign-in successful for $cleanInput")
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firebase Auth sign-in exception: ${e.localizedMessage}")
                }
            }

            // Match against Room users database
            val all = repository.allUsers.first()
            val matchedUser = all.find { 
                (it.username.lowercase() == cleanInput || it.email.lowercase() == cleanInput) && 
                (it.password == cleanPass || (it.id == "admin" && (cleanPass == "chitrali@786" || cleanPass == "adminpassword123" || cleanPass == "admin123"))) && 
                it.isProfileCompleted 
            }

            if (matchedUser != null) {
                // Set all other users as not current
                for (u in all) {
                    if (u.isCurrentUser) {
                        repository.updateUser(u.copy(isCurrentUser = false))
                    }
                }
                // Sign in this matching user
                repository.updateUser(matchedUser.copy(isCurrentUser = true))
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } else if ((cleanInput == "ceo@yarkhoon.com" || cleanInput == "admin@yarkhoon.com" || cleanInput == "ceo" || cleanInput == "admin") && 
                       (cleanPass == "chitrali@786" || cleanPass == "adminpassword123" || cleanPass == "admin123")) {
                // Compatibility for admin credentials
                onAdminLoginSuccess()
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun onSignInUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }
            repository.updateUser(user.copy(isCurrentUser = true))
        }
    }

    fun initiateNewSignUp() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }
            val defaultCurrentUser = User(
                id = "currentUser",
                username = "",
                fullName = "",
                avatarUrl = "",
                coverUrl = "",
                bio = "",
                friendStatus = "NONE",
                isCurrentUser = true,
                isProfileCompleted = false
            )
            repository.insertUsers(listOf(defaultCurrentUser))
        }
    }

    fun onCancelSignUp() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser("currentUser")
        }
    }

    fun onResetProfile() {
        onSignOut()
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePost(postId)
        }
    }

    fun insertGroups(groups: List<Group>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGroups(groups)
        }
    }

    // --- Admin Dashboard Controller Methods ---

    fun onTogglePostViral(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allPosts.first()
            val post = all.find { it.id == postId }
            if (post != null) {
                repository.updatePost(post.copy(isViral = !post.isViral))
            }
        }
    }

    fun onUpdatePostContent(postId: Int, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allPosts.first()
            val post = all.find { it.id == postId }
            if (post != null) {
                repository.updatePost(post.copy(content = newContent))
            }
        }
    }

    fun onToggleUserVerified(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allUsers.first()
            val user = all.find { it.id == userId }
            if (user != null) {
                repository.updateUser(user.copy(isVerified = !user.isVerified))
            }
        }
    }

    fun onDeleteUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser(userId)
        }
    }

    fun onAdminCreatePost(content: String, mediaType: String, mediaUrl: String, asYarkhoon: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (asYarkhoon) {
                val newPost = Post(
                    authorId = "user_yarkhoon",
                    authorName = "Yarkhoon.com",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                    content = content,
                    mediaType = mediaType,
                    mediaUrl = mediaUrl,
                    timestamp = System.currentTimeMillis(),
                    likesCount = 3890,
                    isLikedByMe = true,
                    commentsCount = 472,
                    isViral = true // Goes viral reaching all users immediately
                )
                repository.insertPost(newPost)
            } else {
                onCreatePost(content, mediaType, mediaUrl)
            }
        }
    }
}
