package com.project.review.view_models

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.project.review.models.Review
import com.project.review.repositories.DatabaseRepository
import com.project.review.repositories.NetworkRepository
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock

/**
 * View Model by ReviewDialogActivity
 *
 * @see com.project.review.ui.ReviewDialogActivity
 */
class ReviewDialogViewModel(application: Application) : AndroidViewModel(application) {
    val context: Context = application

    private val databaseRepository = DatabaseRepository(application)
    private val networkRepository = NetworkRepository()

    private val resultUri = MutableLiveData<Uri?>()
    val _resultUri: LiveData<Uri?>
        get() = resultUri

    /**
     * allows to download the image associated with a product
     */
    fun saveImage(url: String) {
        viewModelScope.launch {
            val uri = networkRepository.saveImage(context, url)
            resultUri.value = uri
        }
    }

    /**
     * allows to save or delete a review from the database
     */
    fun storeReview(
        storeLock: ReentrantLock,
        review: Review,
        stored: Boolean,
        productCode: String,
    ) {
        viewModelScope.launch {
            databaseRepository.storeReview(storeLock, review, stored, productCode)
        }
    }


}