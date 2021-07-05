package com.project.review.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

data class HtmlResponse(val body: String, val url: String)

interface NetworkRequest {

    @GET
    fun getFile(@Url url: String): Call<ResponseBody>

}