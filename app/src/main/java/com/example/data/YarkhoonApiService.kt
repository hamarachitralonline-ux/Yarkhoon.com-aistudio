package com.example.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YarkhoonApiService {
    @GET("articles")
    suspend fun getArticles(@Query("category") category: String? = null): List<Article>

    @GET("articles/{id}")
    suspend fun getArticleDetail(@Path("id") id: String): Article
}
