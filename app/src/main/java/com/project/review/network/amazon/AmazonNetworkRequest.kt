package com.project.review.network.amazon

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface AmazonNetworkRequest : CamelCamel {

    @GET("/product-reviews/{asin}")
    fun getReviews(
        @Path("asin") asin: String,
        @Query("pageNumber") page: Int
    ): Call<ResponseBody>

    @GET("/dp/{code}")
    fun getProduct(@Path("code") code: String): Call<ResponseBody>

    @GET("/s")
    fun getSearchResult(@Query("k") code: String): Call<ResponseBody>

}