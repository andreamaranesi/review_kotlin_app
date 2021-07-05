package com.project.review.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.project.review.database.GeneralDatabase
import com.project.review.database.dao.*
import com.project.review.models.*
import com.project.review.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.Lock

/**
 * contains various methods for communicating directly with the local database
 */
open class DatabaseRepository(application: Context) {

    private val database: GeneralDatabase = GeneralDatabase.getInstance(application)
    private val productDao: ProductDao = database.productDao()
    private val reviewDao: ReviewDao = database.reviewDao()
    private val relatedProductDao: RelatedProductDao = database.relatedProductDao()

    fun getSavedReviews(code: String, storage: MutableList<Review>) {
        storage.clear()
        val list = reviewDao.suspendGetReviews(code)
        storage.addAll(list)
    }


    private fun insertReview(review: Review, code: String) {
        this.reviewDao.insert(
            review.copy().apply {
                productCode = code
                body = initBody
            })
    }

    fun removeReview(review: Review) {
        this.reviewDao.delete(review)
    }


    fun deleteResearch(product: Product) {
        this.productDao.delete(product)
        this.relatedProductDao.deleteByCode(product.code)
        this.reviewDao.deleteByCode(product.code)
    }

    suspend fun deleteReview(review: Review, code: String) {
        this.storeReview(null, review, false, code)
    }


    /**
     * inserts the product and all related products found in the db
     */
    fun insertProduct(product: Product?) {
        if (product != null) {
            this.productDao.insert(product)
            for (relatedProduct in product.relatedProducts) {
                this.relatedProductDao.insert(relatedProduct.apply { parentCode = product.code })
            }
        }
    }

    suspend fun storeReview(lock: Lock?, review: Review, newValue: Boolean, code: String) {
        withContext(Dispatchers.IO) {
            lock?.lock()
            if (newValue)
                insertReview(review, code)
            else
                removeReview(review)
            lock?.unlock()
        }
    }

    fun getRelatedProducts(): LiveData<MutableList<RelatedProduct>> {
        return relatedProductDao.getProducts()
    }

    fun getLimitedRelatedProducts(): LiveData<MutableList<RelatedProduct>> {
        return relatedProductDao.getProducts(Settings.minListResults)
    }

    /**
     * obtained a list of suggested products from the database, this method allows you to return
    a list of objects where, at the beginning of each sequence of RelatedProduct objects with the same productCode,
    places the Product object from which they descend
     */
    suspend fun getRelatedProductsList(list: MutableList<RelatedProduct>): MutableList<Any> {
        val newGroup: MutableMap<String, MutableList<RelatedProduct>> =
            list.groupByTo(mutableMapOf()) { it.parentCode!! }

        val mutableList = mutableListOf<Any>()

        val iterator = newGroup.iterator()
        while (iterator.hasNext()) {
            val group = iterator.next()
            val product: Product = this.getProductByCode(group.key)
            mutableList.add(product)
            for (item in group.value)
                mutableList.add(item)
        }

        return mutableList
    }

    suspend fun getProductByCode(code: String): Product {
        return withContext(Dispatchers.IO) {
            productDao.getProductByCode(code)
        }
    }


    fun getProductWithReviews(minListResults: Int?): LiveData<MutableList<Product>> {
        if (minListResults != null)
            return productDao.getProductWithReviews(minListResults)
        return productDao.getProductWithReviews()
    }

    fun getResearches(minListResults: Int?): LiveData<MutableList<Product>> {
        if (minListResults != null)
            return productDao.getResearches(minListResults)
        return productDao.getResearches()
    }

    fun getReviews(productCode: String): LiveData<MutableList<Review>> {
        return reviewDao.getReviews(productCode)
    }

    fun suspendGetRelatedProducts(limit: Int): MutableList<RelatedProduct> {
        return relatedProductDao.suspendGetRelatedProducts(limit)
    }

    fun suspendGetScheduledProducts(): MutableList<RelatedProduct> {
        return relatedProductDao.suspendGetScheduledProducts()
    }

    fun insertRelatedProduct(product: RelatedProduct) {
        relatedProductDao.insert(product)
    }

}