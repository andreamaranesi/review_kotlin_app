package com.project.review.repositories

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.project.review.network.HtmlResponse
import com.project.review.network.NetworkRequest
import com.project.review.network.amazon.AmazonNetworkRequest
import com.project.review.network.goodreads.GoodReadsNetworkRequest
import com.project.review.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Retrofit
import java.io.*
import java.util.concurrent.TimeUnit

/**
 * contains the various methods for communicating with the interfaces of the network directory
 *
 * @see com.project.review.network
 */
open class NetworkRepository {

    /**
     * allows, given a stream of bytes, to save the file obtained in public or private paths
     *
     */
    private fun saveLocally(
        context: Context,
        stream: InputStream,
        ext: String,
        saveLocally: Boolean = false,
    ): Uri? {
        val file = File(
            if (!saveLocally)
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) else
                context.filesDir,
            "product_image.$ext"
        )

        var output: OutputStream? = null
        try {
            output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            output.flush()
        } catch (e: IOException) {
            return null
        } finally {
            try {
                if (output != null) {
                    output.close()
                } else
                    return null
            } catch (e: IOException) {
                return null
            }
        }
        return Uri.parse(file.absolutePath)
    }


    /**
     * it is used to save an image locally
     */
    suspend fun saveImage(
        context: Context,
        url: String,
        saveLocally: Boolean = false,
    ): Uri? {
        return withContext(Dispatchers.IO) {
            val service: NetworkRequest =
                networkRequestRetrofit(Settings.amazonEndPoint())
            var saveData: Uri? = null
            try {
                val response = service.getFile(url).execute()
                if (response.isSuccessful) {
                    saveData = saveLocally(
                        context,
                        response.body()?.byteStream()!!,
                        response.body()?.contentType()?.subtype()!!,
                        saveLocally
                    )
                }

            } catch (e: Exception) {
            }
            saveData
        }

    }

    /**
     * gets the dom of an html page
     */
    fun getDom(
        call: Call<ResponseBody>,
    ): org.jsoup.nodes.Document {
        val response = this.get(call, true)
        return Jsoup.parse(response.body)
    }

    /**
     * instantiates a retrofit service
     */
    private fun retrofit(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .followRedirects(true)
            .writeTimeout(150, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("User-Agent", Settings.firefoxUserAgent)
                        .build()
                )
            }


        return Retrofit.Builder()
            .client(client.build())
            .baseUrl(baseUrl)
            .build()

    }

    fun amazonRetrofit(baseUrl: String): AmazonNetworkRequest {
        return retrofit(baseUrl).create(AmazonNetworkRequest::class.java)
    }

    fun goodReadsRetrofit(baseUrl: String): GoodReadsNetworkRequest {
        return retrofit(baseUrl).create(GoodReadsNetworkRequest::class.java)
    }

    private fun networkRequestRetrofit(baseUrl: String): NetworkRequest {
        return retrofit(baseUrl).create(NetworkRequest::class.java)
    }


    /**
     * manages the result of requests to web servers
     *
     * tries to retry the call in case the device connection or servers is unstable
     */
    fun get(
        call: Call<ResponseBody>,
        retry: Boolean,
        timeout: Int = 1000,
    ): HtmlResponse {
        if (Settings.networkAvailable.value!!) {
            try {
                val response = call.clone().execute()
                if (response.isSuccessful) {
                    return HtmlResponse(
                        response.body()?.string() ?: "",
                        response.raw().request().url().toString()
                    )
                }
            } catch (e: Exception) {
                Log.e("INTERNET", "UNSTABLE CONNECTION DURING CALL OR UNREACHABLE SERVER")
            }
        }
        if (retry) {
            Thread.sleep(timeout.toLong())
            return this.get(call, retry, timeout)
        }
        return HtmlResponse(
            "",
            ""
        )
    }
}