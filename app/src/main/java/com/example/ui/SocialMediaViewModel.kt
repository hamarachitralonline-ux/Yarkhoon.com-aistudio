package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SocialMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SocialMediaRepository(database.socialMediaDao)

    // Exposed Flows
    val currentUser: StateFlow<User?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPosts: StateFlow<List<Post>> = repository.allPosts
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

            val newPost = Post(
                authorId = user?.id ?: "currentUser",
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                content = content,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
                isLikedByMe = false,
                commentsCount = 0
            )
            repository.insertPost(newPost)
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
            }
        }
    }

    fun onCompleteRegistration(fullName: String, username: String, email: String, password: String, bio: String, avatarUrl: String, coverUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
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
                email = email.trim().lowercase(),
                password = password
            )
            repository.insertUsers(listOf(completeUser))

            // Delete the placeholder user "currentUser" if that was the one being set up
            if (user != null && user.id == "currentUser") {
                repository.deleteUser("currentUser")
            }
        }
    }

    fun onSignOut() {
        viewModelScope.launch(Dispatchers.IO) {
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
            val all = repository.allUsers.first()
            val matchedUser = all.find { 
                (it.username.lowercase() == cleanInput || it.email.lowercase() == cleanInput) && 
                it.password == passwordText && 
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
}
