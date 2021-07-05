package com.project.review.reviews.marketplace

import com.project.review.models.Product
import com.project.review.reviews.services.ReviewListIterator

/**
 * defines the methods that the various marketplace classes must implement in order to search for the product
 */
abstract class MarketplaceObject(
    var code: String,
    var isSpecificCode: Boolean,
    val isAsin: Boolean
) {

    /**
     * @return true if the product is found in the corresponding marketplace
     */
    abstract fun init(): Boolean

    /**
     * @return an instance of the iterator associated with the corresponding marketplace
     */
    abstract fun iterator(): ReviewListIterator

    /**
     * @return a Product instance of the corresponding marketplace
     * @see Product
     */
    abstract fun readProductData(): Product?

}