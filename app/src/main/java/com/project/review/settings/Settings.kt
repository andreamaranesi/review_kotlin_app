package com.project.review.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.util.*


enum class Marketplace {
    AMAZON, GOODREADS
}

/**
 * contains the app's default settings
 */
class Settings {


    companion object {

        fun getSharedPreferences(context: Context): SharedPreferences? {
            return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        }


        const val GITHUB_URL = "https://github.com"
        const val IMAGE_ALPHA_DURATION: Long = 250

        const val NOTIFICATION_ID = "notification_id"
        const val PRODUCT_NAME: String = "product_name"
        const val BARCODE_FORMAT: String = "format"
        const val CODE: String = "code"
        const val REVIEW: String = "review"
        const val IS_SEARCHING: String = "isSearching"
        const val ORDER_BY = "high_to_low"
        const val PREFERENCES = "preferences"
        const val SAVED = "saved"
        const val RESULT = "result"


        const val minListResults = 4
        lateinit var lang: String
        var shortLang: String = Locale.getDefault().language

        fun initLang(context: Context) {
            lang =
                context.resources.configuration.locales.get(0).toString().toUpperCase(Locale.ROOT)

            Log.i("Settings", "Found Language: $lang")
        }


        enum class AmazonUrl(val url: String) {
            IT("https://www.amazon.it"), EN_UK("https://www.amazon.co.uk"), EN_US("https://www.amazon.com"),
            DE("https://www.amazon.de")
        }

        enum class CamelCamel(val url: String) {
            IT("https://it.camelcamelcamel.com"),
            EN_UK("https://uk.camelcamelcamel.com"), EN_US("https://camelcamelcamel.com"),
            DE("https://de.camelcamelcamel.com")
        }

        fun amazonEndPoint(): String {
            return AmazonUrl.valueOf("IT").url
        }

        fun goodReadsEndPoint(): String {
            return "https://www.goodreads.com"
        }


        const val firefoxUserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:87.0) Gecko/20100101 Firefox/87.0"
        const val frequencyUpdating = 3
        const val minReviewResults = 3
        const val wordResults = 15
        const val initShimmerCount = 10
        const val afterShimmerCount = 3
        const val maxBodyLength = 200
        const val maxTitleLength = 10
        const val reviewResults = 150
        var networkAvailable = MutableLiveData(true)

        /**
         * checks if the device has an active connection
         * updates the LiveData networkAvailable from time to time
         *
         */
        fun isOnline(context: Context) {

            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.registerDefaultNetworkCallback(object : NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        networkAvailable.postValue(true)
                    }

                    override fun onLost(network: Network) {
                        networkAvailable.postValue(false)
                    }
                })

            } catch (e: Exception) {
                networkAvailable.value = false
            }
        }

        fun camelcamelEndPoint(): String {
            return CamelCamel.valueOf("IT").url
        }


    }
}