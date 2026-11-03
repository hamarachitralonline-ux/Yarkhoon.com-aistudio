package com.example.data

/**
 * Preview and initial seed data for YARKHOON.COM social platform.
 * Clearly separated for development and previewing social media UX.
 */
object SampleData {

    val sampleCurrentUser = User(
        id = "user_host",
        name = "Hamid Shah",
        handle = "@hamid_yarkhoon",
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
        coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1000",
        bio = "Explorer, software creator & storyteller from Yarkhoon Valley. Celebrating high mountain communities and digital connectivity.",
        location = "Yarkhoon & Chitral",
        isVerified = true,
        followersCount = 1420,
        followingCount = 312,
        postsCount = 48
    )

    val users = listOf(
        sampleCurrentUser,
        User(
            id = "u1",
            name = "Khowar Tech Network",
            handle = "@khowartech",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
            coverUrl = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=1000",
            bio = "Empowering digital creators, software developers, and remote freelancers across Chitral and Gilgit-Baltistan.",
            location = "Chitral Town",
            isVerified = true,
            followersCount = 5820,
            followingCount = 140,
            postsCount = 120
        ),
        User(
            id = "u2",
            name = "Shandur Alpine Riders",
            handle = "@shandur_riders",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
            coverUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000",
            bio = "Freestyle mountain polo, equestrian breeding, and high-altitude polo updates.",
            location = "Shandur Pass (3,700m)",
            isVerified = true,
            followersCount = 12400,
            followingCount = 85,
            postsCount = 210
        ),
        User(
            id = "u3",
            name = "Yarkhoon Artisan Collective",
            handle = "@yarkhoon_crafts",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
            coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=1000",
            bio = "Pure woolen Chitrali Patti, handmade shawls, and embroidered caps crafted in Upper Chitral.",
            location = "Booni & Mastuj",
            isVerified = true,
            isBusinessAccount = true,
            businessCategory = "Crafts & Textiles",
            followersCount = 3400,
            followingCount = 210,
            postsCount = 94
        ),
        User(
            id = "u4",
            name = "Pamir Mountain Treks",
            handle = "@pamir_expeditions",
            avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300",
            coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000",
            bio = "Certified mountain guides, eco-tours from Mastuj to Broghil and Karambar Lake.",
            location = "Broghil & Yarkhoon",
            isVerified = true,
            isBusinessAccount = true,
            businessCategory = "Tourism & Hospitality",
            followersCount = 7890,
            followingCount = 410,
            postsCount = 180
        )
    )

    val stories = listOf(
        Story(
            id = "s1",
            author = users[1],
            mediaUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600",
            caption = "Early morning view of snowy Tirich Mir peak (7,708m)",
            isViewed = false
        ),
        Story(
            id = "s2",
            author = users[2],
            mediaUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600",
            caption = "Training match for the upcoming mountain polo championship!",
            isViewed = false
        ),
        Story(
            id = "s3",
            author = users[3],
            mediaUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600",
            caption = "Pure sheep wool dyed with walnut bark. Handcrafted in Booni.",
            isViewed = true
        ),
        Story(
            id = "s4",
            author = users[4],
            mediaUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600",
            caption = "Crossing the crystal streams of Yarkhoon river on horseback.",
            isViewed = false
        )
    )

    val initialPosts = listOf(
        Post(
            id = "post_1",
            author = users[1],
            type = PostType.PHOTO,
            text = "Exciting milestone for our northern tech community: 40+ youth from Upper Chitral and Yarkhoon completed our remote software development workshop! The future of remote freelancing and tech work in the mountains is brighter than ever. #TechFromMountains #ChitralFreelancers #Yarkhoon",
            mediaUrls = listOf("https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=800"),
            tags = listOf("TechFromMountains", "ChitralFreelancers", "Yarkhoon"),
            location = "Chitral Town",
            likesCount = 248,
            commentsCount = 32,
            repostsCount = 18,
            sharesCount = 14,
            isLiked = false,
            isReposted = false,
            isSaved = false,
            comments = listOf(
                Comment("c1", users[0], "Brilliant work! Let's connect on establishing fiber hubs in Booni.", "1h ago", 12),
                Comment("c2", users[3], "Great initiative for our educated youngsters.", "45m ago", 6)
            ),
            createdAt = "2h ago"
        ),
        Post(
            id = "post_2",
            author = users[2],
            type = PostType.PHOTO,
            text = "Official Dates Announced: The annual Shandur Freestyle Polo Cup is set! Teams from Chitral and Gilgit will clash at 3,700m elevation. Pure mountain tradition without referees — the game of kings at the roof of the world! Who is traveling up this year? #ShandurPolo #GameOfKings #HinduKush",
            mediaUrls = listOf("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800"),
            tags = listOf("ShandurPolo", "GameOfKings", "HinduKush"),
            location = "Shandur Pass",
            likesCount = 512,
            commentsCount = 74,
            repostsCount = 89,
            sharesCount = 45,
            isLiked = true,
            isReposted = false,
            isSaved = true,
            comments = listOf(
                Comment("c3", users[4], "Our horses and expedition team are ready for the ride from Mastuj!", "3h ago", 19)
            ),
            createdAt = "4h ago"
        ),
        Post(
            id = "post_3",
            author = users[3],
            type = PostType.BUSINESS_PROMO,
            text = "New Autumn Collection from our women artisans in Yarkhoon & Booni: 100% natural pure sheep wool Chitrali Patti (Shu) coats and Pakol caps. Every purchase directly sustains 25 artisan households in Upper Chitral. DM us or check our Business Hub profile for sizes. #ChitraliShu #ArtisanCrafts #HandmadeWithLove",
            mediaUrls = listOf("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800"),
            tags = listOf("ChitraliShu", "ArtisanCrafts", "HandmadeWithLove"),
            location = "Booni Bazaar",
            likesCount = 186,
            commentsCount = 21,
            repostsCount = 15,
            sharesCount = 28,
            isLiked = false,
            isReposted = false,
            isSaved = false,
            comments = emptyList(),
            createdAt = "6h ago"
        ),
        Post(
            id = "post_4",
            author = users[0],
            type = PostType.TEXT,
            text = "Welcome to the new YARKHOON.COM social platform! Designed to bring our people together across valleys, diaspora, businesses, and creative communities. What features would you like to see next? Share your thoughts below!",
            tags = listOf("YarkhoonCom", "CommunityFirst", "Welcome"),
            location = "Yarkhoon Valley",
            likesCount = 340,
            commentsCount = 48,
            repostsCount = 42,
            sharesCount = 22,
            isLiked = true,
            isReposted = true,
            isSaved = false,
            comments = emptyList(),
            createdAt = "Yesterday"
        )
    )

    val businessProfiles = listOf(
        BusinessProfile(
            id = "biz_1",
            owner = users[3],
            businessName = "Shu & Pakol Artisan Guild",
            category = BusinessCategory.CRAFTS,
            serviceHighlight = "Authentic handwoven Chitrali woolen garments & caps",
            description = "Specializing in traditional natural wool Shu, Pakol hats, and embroidery. Custom tailoring and worldwide courier delivery available.",
            location = "Booni Main Bazaar, Upper Chitral",
            phone = "+92 345 9876543",
            whatsapp = "+923459876543",
            rating = 4.9f,
            reviewsCount = 46,
            isVerified = true,
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600",
                "https://images.unsplash.com/photo-1576871337632-b9aef4c17ab9?w=600"
            )
        ),
        BusinessProfile(
            id = "biz_2",
            owner = users[4],
            businessName = "Pamir & Yarkhoon Treks & Expeditions",
            category = BusinessCategory.TOURISM_GUIDES,
            serviceHighlight = "Licensed High Altitude Guides & 4x4 Mountain Transport",
            description = "Complete logistics for Broghil Valley, Karambar Lake, and Tirich Mir basecamp treks. Reliable jeep hire and homestay arrangements.",
            location = "Mastuj & Yarkhoon Lasht",
            phone = "+92 344 1234567",
            whatsapp = "+923441234567",
            rating = 4.8f,
            reviewsCount = 62,
            isVerified = true,
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600",
                "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600"
            )
        ),
        BusinessProfile(
            id = "biz_3",
            owner = User(
                id = "u_electric",
                name = "Karim Solar & Electricals",
                handle = "@karim_electric",
                avatarUrl = "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=300",
                location = "Mastuj",
                isVerified = true
            ),
            businessName = "Karim Solar & Hydro Solutions",
            category = BusinessCategory.SOLAR_TECH,
            serviceHighlight = "Solar inverter setups, micro-hydro wiring & home electricals",
            description = "Over 12 years of hands-on experience installing dependable off-grid solar power systems and wiring mountain residences across Upper Chitral.",
            location = "Mastuj & Yarkhoon Corridor",
            phone = "+92 333 8765432",
            whatsapp = "+923338765432",
            rating = 5.0f,
            reviewsCount = 28,
            isVerified = true,
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1509391365360-2e959784a276?w=600"
            )
        ),
        BusinessProfile(
            id = "biz_4",
            owner = User(
                id = "u_tutor",
                name = "Prof. Zafar Iqbal",
                handle = "@zafar_maths",
                avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=300",
                location = "Chitral",
                isVerified = true
            ),
            businessName = "Apex Science & Mathematics Coaching",
            category = BusinessCategory.TEACHERS_TUTORS,
            serviceHighlight = "Online & in-person tutoring for FSc & Entry Tests",
            description = "Specialized instruction in Physics, Mathematics, and Computer Science for higher secondary students preparing for engineering and medical entrance tests.",
            location = "Shahi Bazaar, Chitral",
            phone = "+92 346 5551234",
            whatsapp = "+923465551234",
            rating = 4.9f,
            reviewsCount = 39,
            isVerified = true,
            portfolioImages = emptyList()
        ),
        BusinessProfile(
            id = "biz_5",
            owner = User(
                id = "u_carpenter",
                name = "Ustad Sher Wali",
                handle = "@sherwali_wood",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                location = "Yarkhoon",
                isVerified = true
            ),
            businessName = "Heritage Woodcraft & Carpentry",
            category = BusinessCategory.CARPENTERS,
            serviceHighlight = "Traditional Chitrali carved wooden pillars, doors & roofing",
            description = "Master carpenter preserving authentic walnut and deodar cedar woodwork for mountain homes, guest lodges, and traditional majlis halls.",
            location = "Booni & Yarkhoon Lasht",
            phone = "+92 342 9988776",
            whatsapp = "+923429988776",
            rating = 5.0f,
            reviewsCount = 21,
            isVerified = true,
            portfolioImages = listOf(
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=600"
            )
        ),
        BusinessProfile(
            id = "biz_6",
            owner = User(
                id = "u_mechanic",
                name = "Khan 4x4 Workshop",
                handle = "@khan_jeeprepair",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
                location = "Mastuj",
                isVerified = true
            ),
            businessName = "Mastuj 4x4 & Mountain Vehicle Mechanics",
            category = BusinessCategory.MECHANICS,
            serviceHighlight = "Suspension repairs, diesel engines & emergency trail recovery",
            description = "Comprehensive mechanic services specialized for rough terrain Land Cruisers, Toyota Hilux, and mountain jeeps operating between Booni, Mastuj, and Broghil.",
            location = "Mastuj Bridge Junction",
            phone = "+92 348 4433221",
            whatsapp = "+923484433221",
            rating = 4.8f,
            reviewsCount = 35,
            isVerified = true,
            portfolioImages = emptyList()
        )
    )

    val sampleMarketplaceItems = listOf(
        MarketplaceItem(
            id = "item_1",
            seller = users[0], // currentUser (allows test of Edit/Delete!)
            title = "Toyota Land Cruiser Prado TZ (Mountain Ready)",
            category = MarketplaceCategory.CARS,
            price = "PKR 4,850,000",
            description = "1998 model with 1KZ turbo diesel engine. Perfect 4x4 suspension for high mountain terrain, Shandur pass, and Yarkhoon trails. Recently serviced, fresh tires, registered with original documents.",
            phone = "+92 345 1122334",
            location = "Booni & Chitral Town",
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800",
                "https://images.unsplash.com/photo-1551830820-330a71b99659?w=800"
            ),
            createdAt = "2 hours ago"
        ),
        MarketplaceItem(
            id = "item_2",
            seller = users[1],
            title = "Honda CG 125 Self-Start (2022 Model)",
            category = MarketplaceCategory.MOTORBIKES,
            price = "PKR 185,000",
            description = "First owner, low mileage (8,500 km). Mint condition with genuine parts. Ideal for commute between Booni, Mastuj, and Chitral. All taxes paid.",
            phone = "+92 344 7766554",
            location = "Mastuj, Upper Chitral",
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800"
            ),
            createdAt = "Yesterday"
        ),
        MarketplaceItem(
            id = "item_3",
            seller = users[2],
            title = "iPhone 13 Pro 256GB Sierra Blue (PTA Approved)",
            category = MarketplaceCategory.MOBILE_PHONES,
            price = "PKR 195,000",
            description = "Original box and cable included. 88% battery health. No scratches, glass protector and Spigen case included from day one. Factory unlocked.",
            phone = "+92 333 4455667",
            location = "Chitral Town",
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800"
            ),
            createdAt = "3 hours ago"
        ),
        MarketplaceItem(
            id = "item_4",
            seller = users[3],
            title = "Organic Yarkhoon Valley Red Apples (Fresh Crate)",
            category = MarketplaceCategory.FRUITS,
            price = "PKR 2,400 / 10kg Crate",
            description = "Freshly picked pesticide-free mountain apples from high-altitude orchards in Upper Yarkhoon. Super crisp, aromatic, and sweet. Bulk delivery available to Booni and Chitral.",
            phone = "+92 345 9876543",
            location = "Yarkhoon Lasht",
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=800"
            ),
            createdAt = "Just now"
        ),
        MarketplaceItem(
            id = "item_5",
            seller = users[4],
            title = "High-Altitude Organic Potatoes (100kg Bag)",
            category = MarketplaceCategory.VEGETABLES,
            price = "PKR 6,500 / Bag",
            description = "Famous high-grade seed and cooking potatoes grown in glacial waters of Upper Chitral. Long shelf life, ideal for winter storage. Special discount on multi-bag orders.",
            phone = "+92 344 1234567",
            location = "Broghil & Yarkhoon Corridor",
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=800"
            ),
            createdAt = "1 day ago"
        ),
        MarketplaceItem(
            id = "item_6",
            seller = users[0], // currentUser listing
            title = "Pure Mountain Honey & Walnut Kernel Mix",
            category = MarketplaceCategory.AGRICULTURE,
            price = "PKR 3,200 / kg",
            description = "100% natural raw wild flora honey harvested from local beehives, bundled with freshly shelled paper-skin Chitrali walnuts. Free home delivery in Booni.",
            phone = "+92 345 1122334",
            location = "Booni, Upper Chitral",
            imageUrls = listOf(
                "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=800"
            ),
            createdAt = "Just now"
        )
    )

    val notifications = listOf(
        NotificationItem(
            id = "n1",
            sender = users[1],
            type = "like",
            message = "liked your post about the YARKHOON.COM launch.",
            timestamp = "10m ago",
            isRead = false
        ),
        NotificationItem(
            id = "n2",
            sender = users[2],
            type = "comment",
            message = "commented: 'Our horses and expedition team are ready!'",
            timestamp = "1h ago",
            isRead = false
        ),
        NotificationItem(
            id = "n3",
            sender = users[3],
            type = "follow",
            message = "started following you.",
            timestamp = "3h ago",
            isRead = true
        ),
        NotificationItem(
            id = "n4",
            sender = users[4],
            type = "repost",
            message = "reposted your update to their followers.",
            timestamp = "1d ago",
            isRead = true
        )
    )

    val conversations = listOf(
        Conversation(
            id = "conv_1",
            recipient = users[1],
            lastMessage = "Let's coordinate the software workshop schedule for next week.",
            timestamp = "15m ago",
            unreadCount = 2
        ),
        Conversation(
            id = "conv_2",
            recipient = users[3],
            lastMessage = "Your custom woolen jacket order has been dispatched from Booni.",
            timestamp = "2h ago",
            unreadCount = 0
        ),
        Conversation(
            id = "conv_3",
            recipient = users[2],
            lastMessage = "Are you attending the Shandur opening match?",
            timestamp = "Yesterday",
            unreadCount = 0
        )
    )

    val trendingHashtags = listOf(
        "#Yarkhoon" to "3.4K posts",
        "#ShandurPolo" to "8.2K posts",
        "#TirichMir" to "2.1K posts",
        "#ChitralFreelancers" to "1.5K posts",
        "#MountainArtisans" to "980 posts",
        "#HinduKushAutumn" to "1.8K posts"
    )

    val sampleReels = listOf(
        Reel(
            id = "reel_1",
            author = users[2],
            videoMediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailMediaUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=900",
            caption = "Galloping across Shandur Pass at 3,700m! The raw adrenaline of traditional mountain polo. #ShandurPolo #Reels #Chitral",
            audioTrackTitle = "Khowar Daf & Surna - Shandur Anthem",
            likesCount = 1420,
            commentsCount = 89,
            sharesCount = 210,
            isLiked = false
        ),
        Reel(
            id = "reel_2",
            author = users[3],
            videoMediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailMediaUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=900",
            caption = "The ancient craft of spinning sheep wool by hand in Yarkhoon Valley. Preserving 800 years of heritage. #ArtisanCrafts #ChitraliShu",
            audioTrackTitle = "Traditional Folk Sitar - Booni Melodies",
            likesCount = 890,
            commentsCount = 44,
            sharesCount = 135,
            isLiked = true
        ),
        Reel(
            id = "reel_3",
            author = users[4],
            videoMediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnailMediaUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900",
            caption = "Autumn golden hour reflected on crystal Yarkhoon waters. Peaceful moments in the heart of Hindu Kush. #NatureReels #PamirTrails",
            audioTrackTitle = "Mountain Wind Ambient - High Altitude Soundscape",
            likesCount = 2310,
            commentsCount = 162,
            sharesCount = 450,
            isLiked = false
        )
    )
}
