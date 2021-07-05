package com.project.review.reviews.services

import com.project.review.models.Review
import com.project.review.settings.Marketplace

/**
 * defines the methods that the various marketplaces must implement to extract reviews
 */
interface ReviewListIterator {

    val marketplace: Marketplace
    var end: Boolean

    /**
     * gets the next review
     */
    fun next(): Review?

    /**
     * @return true if there are other reviews you can check out
     */
    fun hasNext(): Boolean

    /**
     * initializes the iterator
     */
    fun start()
}