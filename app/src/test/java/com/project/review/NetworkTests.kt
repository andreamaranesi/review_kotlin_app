package com.project.review

import com.project.review.reviews.marketplace.Amazon
import com.project.review.reviews.marketplace.GoodReads
import org.junit.Test

import org.junit.Assert.*

class NetworkTests {

    private val amazon = Amazon("B08L6ZFDCG", isSpecificCode = false, isAsin = true)
    private val goodReads = GoodReads("9780870230769", isSpecificCode = true, isAsin = false)

    @Test
    fun getAmazonProduct() {
        amazon.init()
        val product = amazon.readProductData()
        if (product != null)
            assertEquals(product.name, "Apple iPhone 11 (256GB) - viola")
    }


    @Test
    fun getGoodReadsProduct() {
        goodReads.init()
        val product = goodReads.readProductData()
        assertEquals(product.name, "The Symposium of Plato")
        assertEquals(product.url,
            "https://www.goodreads.com/book/show/13812018-the-symposium-of-plato")
    }


}