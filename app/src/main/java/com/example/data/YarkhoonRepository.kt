package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class YarkhoonRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: YarkhoonApiService = Retrofit.Builder()
        .baseUrl("https://yarkhoon.com/api/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(YarkhoonApiService::class.java)

    suspend fun fetchArticles(): List<Article> = withContext(Dispatchers.IO) {
        try {
            api.getArticles()
        } catch (_: Exception) {
            getCuratedArticles()
        }
    }

    fun getCuratedArticles(): List<Article> = listOf(
        Article(
            id = "1",
            title = "Spring Blooms Across Upper Chitral & Yarkhoon",
            summary = "Fresh glacial streams and apricot blossoms bring new vitality to villages across the valley.",
            content = "The Yarkhoon valley stretches along the river basin northeast of Mastuj up to the Broghil pass. With early spring melt, fruit orchards are blossoming across Lasht, Gazin, and Brep. Local communities prepare for the farming season with customary communal cooperation.",
            category = "Environment",
            date = "September 2026",
            author = "Yarkhoon Editorial"
        ),
        Article(
            id = "2",
            title = "Broghil Yak Festival Celebrations Announced",
            summary = "Cultural games including Yak Polo, horse racing, and folk music scheduled for the autumn gathering.",
            content = "The scenic Broghil National Park at the northern edge of the Yarkhoon corridor will host the annual Yak Festival, highlighting traditional Wakhi hospitality and heritage sports that have been practiced for centuries.",
            category = "Culture",
            date = "September 2026",
            author = "Chitral Heritage Council"
        ),
        Article(
            id = "3",
            title = "Solar Micro-Grids Expanding in Yarkhoon Villages",
            summary = "Clean energy community projects illuminate remote settlements across the upper valley.",
            content = "Local village support organizations continue installing micro-hydro and off-grid solar units to provide reliable electricity to schools, health centers, and households throughout the upper terrain.",
            category = "Development",
            date = "September 2026",
            author = "Community Desk"
        )
    )

    fun getValleyPlaces(): List<ValleyPlace> = listOf(
        ValleyPlace(
            id = "mastuj",
            name = "Mastuj Fort & Junction",
            elevation = "2,359 m",
            description = "Historic fortress town connecting Booni, Laspur, Shandur, and the entrance to the Yarkhoon valley.",
            highlights = listOf("Historical Fort", "Shandur Pass Gateway", "Walnut Orchards")
        ),
        ValleyPlace(
            id = "gazin",
            name = "Gazin & Thalo Pass",
            elevation = "2,700 m",
            description = "Famous for turquoise streams, panoramic views of Thalo Zom peak, and terraced grain fields.",
            highlights = listOf("Hiking Routes", "Glacial Waters", "Alpine Meadows")
        ),
        ValleyPlace(
            id = "lasht",
            name = "Yarkhoon Lasht",
            elevation = "3,100 m",
            description = "The last major village center before the rugged wilderness of Broghil and the high Pamir trails.",
            highlights = listOf("Basecamp Station", "Wakhi Culture", "Starlit Night Skies")
        ),
        ValleyPlace(
            id = "broghil",
            name = "Broghil National Park",
            elevation = "3,800 m",
            description = "High altitude plateau with glacial lakes including Karambar Lake and borders near the Wakhan Corridor.",
            highlights = listOf("Karambar Lake", "Yak Herding", "Glacier Passes")
        )
    )

    fun getCulturalFeatures(): List<CulturalFeature> = listOf(
        CulturalFeature(
            id = "khowar_poetry",
            title = "Khowar Folk Music & Sitar",
            language = "Khowar (Chitrali)",
            description = "Soulful Chitrali sitar melodies accompanied by traditional poetry and the joyous Dani rhythm.",
            traditionType = "Music & Literature"
        ),
        CulturalFeature(
            id = "wakhi_heritage",
            title = "Wakhi Shepherding Traditions",
            language = "Wakhi",
            description = "High-pasture transhumance and pastoral life preserved across generations in upper Yarkhoon.",
            traditionType = "Living Heritage"
        ),
        CulturalFeature(
            id = "shupen",
            title = "Handwoven Woolen Patti (Shu)",
            language = "Chitrali Crafts",
            description = "Fine warm hand-loomed Chitrali wool used to craft the iconic Chitrali Pakol caps and warm winter robes.",
            traditionType = "Artisanal Crafts"
        )
    )
}
