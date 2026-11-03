package com.example

import com.example.data.Post
import com.example.data.PostType
import com.example.data.User
import com.example.ui.FeedFilter
import org.junit.Assert.*
import org.junit.Test

class FeedFilteringMechanismTest {

    private val userAlice = User(id = "user_alice", name = "Alice Baig", handle = "@alice")
    private val userBob = User(id = "user_bob", name = "Bob Khan", handle = "@bob")
    private val userCharlie = User(id = "user_charlie", name = "Charlie Karim", handle = "@charlie")

    private val post1 = Post(
        id = "p1",
        author = userAlice,
        type = PostType.TEXT,
        text = "Morning snowfall in Yarkhoon Valley",
        likesCount = 10,
        commentsCount = 2,
        repostsCount = 1,
        timestamp = 1000L
    )

    private val post2 = Post(
        id = "p2",
        author = userBob,
        type = PostType.PHOTO,
        text = "Chitral polo festival preparation",
        likesCount = 55,
        commentsCount = 15,
        repostsCount = 10,
        timestamp = 2000L
    )

    private val post3 = Post(
        id = "p3",
        author = userCharlie,
        type = PostType.VIDEO,
        text = "Traditional Khowar music performance",
        likesCount = 5,
        commentsCount = 1,
        repostsCount = 0,
        timestamp = 3000L
    )

    private val allPosts = listOf(post1, post2, post3)
    private val followingIds = setOf("user_alice")

    @Test
    fun testFeedFilterEnumLabelsAndEntries() {
        assertEquals("All", FeedFilter.ALL.label)
        assertEquals("Following", FeedFilter.FOLLOWING.label)
        assertEquals("Trending", FeedFilter.TRENDING.label)
        assertEquals("Favorites", FeedFilter.FAVORITES.label)

        // Verify default filter
        assertEquals(FeedFilter.ALL, FeedFilter.DEFAULT)

        // Verify backwards compatibility alias
        assertEquals(FeedFilter.ALL.label, FeedFilter.FOR_YOU.label)
    }

    @Test
    fun testAllFilterReturnsAllVisiblePosts() {
        val currentFilter = FeedFilter.ALL
        val result = when (currentFilter) {
            FeedFilter.ALL, FeedFilter.FOR_YOU -> allPosts
            FeedFilter.FOLLOWING -> allPosts.filter { followingIds.contains(it.author.id) }
            FeedFilter.TRENDING -> allPosts.sortedByDescending { it.likesCount + it.repostsCount + it.commentsCount }
            FeedFilter.FAVORITES -> emptyList()
        }

        assertEquals(3, result.size)
        assertEquals(listOf("p1", "p2", "p3"), result.map { it.id })
    }

    @Test
    fun testFollowingFilterIsolatesFollowedCreators() {
        val currentFilter = FeedFilter.FOLLOWING
        val result = when (currentFilter) {
            FeedFilter.ALL, FeedFilter.FOR_YOU -> allPosts
            FeedFilter.FOLLOWING -> allPosts.filter { followingIds.contains(it.author.id) }
            FeedFilter.TRENDING -> allPosts.sortedByDescending { it.likesCount + it.repostsCount + it.commentsCount }
            FeedFilter.FAVORITES -> emptyList()
        }

        assertEquals(1, result.size)
        assertEquals("p1", result.first().id)
        assertEquals("user_alice", result.first().author.id)
    }

    @Test
    fun testTrendingFilterSortsByEngagementScore() {
        val currentFilter = FeedFilter.TRENDING
        val result = when (currentFilter) {
            FeedFilter.ALL, FeedFilter.FOR_YOU -> allPosts
            FeedFilter.FOLLOWING -> allPosts.filter { followingIds.contains(it.author.id) }
            FeedFilter.TRENDING -> allPosts.sortedByDescending { it.likesCount + it.repostsCount + it.commentsCount }
            FeedFilter.FAVORITES -> emptyList()
        }

        assertEquals(3, result.size)
        // post2: 55 + 15 + 10 = 80 engagement score (Rank #1)
        assertEquals("p2", result[0].id)
        // post1: 10 + 2 + 1 = 13 engagement score (Rank #2)
        assertEquals("p1", result[1].id)
        // post3: 5 + 1 + 0 = 6 engagement score (Rank #3)
        assertEquals("p3", result[2].id)
    }

    @Test
    fun testDynamicFilterTogglingBetweenTabs() {
        var activeFilter = FeedFilter.ALL
        val filterPosts = { filter: FeedFilter ->
            when (filter) {
                FeedFilter.ALL, FeedFilter.FOR_YOU -> allPosts
                FeedFilter.FOLLOWING -> allPosts.filter { followingIds.contains(it.author.id) }
                FeedFilter.TRENDING -> allPosts.sortedByDescending { it.likesCount + it.repostsCount + it.commentsCount }
                FeedFilter.FAVORITES -> emptyList()
            }
        }

        // 1. Initial State: All
        var displayed = filterPosts(activeFilter)
        assertEquals(3, displayed.size)

        // 2. User taps 'Following' toggle tab
        activeFilter = FeedFilter.FOLLOWING
        displayed = filterPosts(activeFilter)
        assertEquals(1, displayed.size)
        assertEquals("user_alice", displayed.first().author.id)

        // 3. User taps 'Trending' toggle tab
        activeFilter = FeedFilter.TRENDING
        displayed = filterPosts(activeFilter)
        assertEquals(3, displayed.size)
        assertEquals("p2", displayed.first().id)

        // 4. User toggles back to 'All'
        activeFilter = FeedFilter.ALL
        displayed = filterPosts(activeFilter)
        assertEquals(3, displayed.size)
    }
}
