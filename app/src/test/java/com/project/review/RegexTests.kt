package com.project.review

import com.project.review.reviews.marketplace.Amazon
import org.junit.Test

import org.junit.Assert.*

class RegexTests {

    private lateinit var reviewBody: String
    private val reviewStar: String = "5,0 recensioni"


    @Test
    fun testAmazonReviews() {
        val review = Amazon.filterStars(reviewStar)
        assertEquals(review, 5.0f, 0.001f)
    }


    private fun getWords(body: String): List<String> {
        val regex = """(\w{4,})""".toRegex()
        return regex.findAll(body).map { it.value.toUpperCase() }.toList()
    }


    @Test
    fun testWords() {
        reviewBody = "Descrizione di Prova. Questo articolo è davvero stupendo!"

        val words = this.getWords(reviewBody).map { it.toLowerCase() }

        assertEquals(words,
            listOf("descrizione", "prova", "questo", "articolo", "davvero", "stupendo"))


    }


}