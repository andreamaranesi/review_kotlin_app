package com.project.review.network.goodreads

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface GoodReadsNetworkRequest {


    @GET("/search")
    fun getSearchResult(@Query("query") code: String): Call<ResponseBody>

    @GET(".")
    fun nextLink(
        @Query("language_code") languageCode: String,
        @Query("page") page: Int,
    ): Call<ResponseBody>

    @GET
    fun getProduct(@Url url: String): Call<ResponseBody>

}