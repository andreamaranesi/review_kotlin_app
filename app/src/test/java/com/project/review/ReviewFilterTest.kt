package com.project.review

import com.project.review.models.Review
import com.project.review.settings.Marketplace
import com.project.review.settings.Tools
import org.junit.Test
import org.junit.Assert.*

class ReviewFilterTest {

    private val reviews = mutableListOf<Review>()


    @Test
    fun testFilters() {
        val firstReview = Review("0", "Titolo 1", "articolo stupendo", "", 4.0F, Marketplace.AMAZON)
        val secondReview = Review("1", "Titolo 2", "veramente deluso", "", 1.0F, Marketplace.AMAZON)
        val thirdReview = Review("2", "Titolo 3", "eccezionale", "", 5.0F, Marketplace.AMAZON)

        reviews.apply {
            add(firstReview)
            add(secondReview)
            add(thirdReview)
        }

        val listOfWords = listOf<String>("ARTICOLO")
        var tempMutableList = reviews.sortedByDescending { value -> value.stars }

        val searchText = "stupendo"

        if (tempMutableList.isNotEmpty() && searchText.isNotEmpty())
            tempMutableList = tempMutableList.filter {
                Tools.highlightWordsOnReview(mutableListOf(searchText), it)
                it.body.toLowerCase().contains(
                    searchText.toLowerCase(
                    )
                )
            }

        if (tempMutableList.isNotEmpty() && listOfWords.isNotEmpty()) {
            tempMutableList = tempMutableList.filter {
                Tools.highlightWordsOnReview(listOfWords, it)
                var result = false
                for (word in listOfWords) {
                    result = it.body.toUpperCase().contains(word)
                    if (!result)
                        break
                }
                result
            }
        }

        assertEquals(tempMutableList[0], firstReview)


    }


}