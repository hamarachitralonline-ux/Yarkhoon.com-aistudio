package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Post::class,
        PostComment::class,
        CommentLike::class,
        CommentReport::class,
        Group::class,
        GroupMember::class,
        GroupJoinRequest::class,
        GroupInvite::class,
        GroupPost::class,
        GroupPostComment::class,
        GroupReport::class,
        ChatMessage::class,
        MarketplaceItem::class,
        ServiceListing::class,
        Story::class,
        FriendConnection::class,
        AppNotification::class,
        KhowarDatasetEntry::class,
        PostReaction::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val socialMediaDao: SocialMediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yarkhwoon_db_v6"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
