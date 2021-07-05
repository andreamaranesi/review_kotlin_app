package com.project.review.view_models

import android.app.Application
import androidx.lifecycle.*
import com.project.review.repositories.DatabaseRepository
import com.project.review.models.Product
import com.project.review.models.RelatedProduct
import com.project.review.models.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * View Model linked with Home, ProductSavedReviewActivity, SavedReviewActivity, RelatedProductActivity,
 * RecentResearchActivity
 *
 * Allows you to communicate with the database to get the various entities
 *
 * @see com.project.review.ui.Home
 * @see com.project.review.ui.ProductSavedReviewActivity
 * @see com.project.review.ui.SavedReviewActivity
 * @see com.project.review.ui.RelatedProductActivity
 * @see com.project.review.ui.RecentResearchActivity
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseRepository = DatabaseRepository(application)

    private val progress = MutableLiveData<Boolean>()
    val _progress: LiveData<Boolean>
        get() = progress

    val allRelatedProducts = MediatorLiveData<MutableList<Any>>()
    val limitedRelatedProducts = MediatorLiveData<MutableList<Any>>()

    /**
     * the list obtained with databaseRepository.getRelatedProducts() is
       filtered by getRelatedProductsList before the LiveData is updated
     *
     * @see DatabaseRepository.getRelatedProducts
     * @see DatabaseRepository.getRelatedProductsList
     */
    private fun filterRelatedProducts() {
        allRelatedProducts.addSource(databaseRepository.getRelatedProducts()) {
            viewModelScope.launch {
                val list = databaseRepository.getRelatedProductsList(it)
                allRelatedProducts.value = list
            }
        }
        limitedRelatedProducts.addSource(databaseRepository.getLimitedRelatedProducts()) {
            viewModelScope.launch {
                val list = databaseRepository.getRelatedProductsList(it)
                limitedRelatedProducts.value = list
            }
        }
    }

    init {
        filterRelatedProducts()
    }

    /**
     * clears the searches, and with them, also the reviews associated with the product as well as the suggested products
     */
    fun deleteResearches(list: MutableSet<Product>) {
        progress.value = false

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (product in list)
                    databaseRepository.deleteResearch(product)
                progress.postValue(true)
            }
        }
    }

    /**
     * clear a list of reviews
     */
    fun deleteReviews(list: MutableSet<Review>, code: String) {
        progress.value = false

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (review in list)
                    databaseRepository.deleteReview(review, code)
                progress.postValue(true)
            }
        }
    }

    fun getProductWithReviews(minListResults: Int? = null): LiveData<MutableList<Product>> {
        return databaseRepository.getProductWithReviews(minListResults)
    }


    fun getResearches(minListResults: Int? = null): LiveData<MutableList<Product>> {
        return databaseRepository.getResearches(minListResults)
    }

    fun getReviews(productCode: String): LiveData<MutableList<Review>> {
        return databaseRepository.getReviews(productCode)
    }

}