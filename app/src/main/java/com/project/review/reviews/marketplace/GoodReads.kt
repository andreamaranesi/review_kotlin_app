package com.project.review.reviews.marketplace

import com.project.review.network.HtmlResponse
import com.project.review.models.Product
import com.project.review.models.Review
import com.project.review.repositories.NetworkRepository
import com.project.review.reviews.services.ReviewListIterator
import com.project.review.settings.Marketplace
import com.project.review.settings.Settings
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * defines how reviews are to be sourced from GoodReads
 *
 * @see ReviewListIterator
 */
class GoodReadsListIterator(
    var currentUrl: String,
) : ReviewListIterator {
    override val marketplace: Marketplace = Marketplace.AMAZON

    private val network = NetworkRepository()
    private val service = network.goodReadsRetrofit(this.currentUrl + "/")

    private lateinit var dom: Document
    private var currentIndex: Int = 0
    private var nextLink: String = ""
    private var maxIterationSize: Int = 0
    override var end: Boolean = false
    var counter: Int = 0
    var currentPage: Int = 1
    private var currentReviews: Elements? = null

    init {
        this.readNextLink()
    }

    private var tempEnd: Boolean = false

    override fun start() {
        this.currentIndex = 0
        this.currentReviews = this.dom.select("div[itemprop='reviews']")
        this.maxIterationSize = this.currentReviews?.size ?: 0
        val li = this.dom.select("a[class='next_page']")
        this.nextLink = ""
        if (li != null) {
            if (li.isEmpty()) {
                this.tempEnd = true
            }
        } else
            this.tempEnd = true
    }

    override fun hasNext(): Boolean {
        if (this.currentIndex < this.maxIterationSize) {
            return true

        } else
            if (!this.tempEnd) {
                this.readNextLink()
                return true
            }

        this.end = true
        return false
    }

    override fun next(): Review? {
        var review: Review? = null
        if (this.currentIndex < this.maxIterationSize) {
            this.counter++
            val elements = this.currentReviews?.get(this.currentIndex)
            val bodyElements =
                elements?.select("span[id^='freeText']")
            val body = bodyElements?.get(bodyElements.size - 1)?.text() ?: ""

            val reviewID: String =
                elements?.attr("id").toString().replace("review_", "")

            val stars =
                elements?.select("span[class*='staticStars']")

            if (stars != null && stars.isNotEmpty()) {

                val stars: Float = stars[0]
                    ?.select("span[class='staticStar p10']")?.size?.toFloat() ?: 0F

                val title =
                    "titolo.."
                val date: String =
                    elements.select("a[class^='reviewDate']")?.get(0)?.text() ?: ""

                review = Review(
                    id = reviewID, title = title, body = body, date = date, stars = stars,
                    marketplace = Marketplace.GOODREADS
                )
            }
        }
        this.currentIndex++
        return review
    }

    fun readNextLink() =
        this.apply {
            dom =
                network.getDom(service.nextLink(Settings.shortLang, this.currentPage++))
        }.start()

}

/**
 *
 * contains the methods of obtaining the product from GoodReads starting from an ISBN
 *
 * @see MarketplaceObject
 */

class GoodReads(code: String, isSpecificCode: Boolean, isAsin: Boolean) :
    MarketplaceObject(code, isSpecificCode, isAsin) {

    private val network = NetworkRepository()
    private val service = network.goodReadsRetrofit(endPoint())
    private var productUrl: String? = null


    companion object {

        internal fun endPoint(): String {
            return Settings.goodReadsEndPoint()
        }

    }

    override fun init(): Boolean = this.getProductCode()

    private var listReviews: GoodReadsListIterator? = null


    override fun iterator(): GoodReadsListIterator {
        if (listReviews == null)
            listReviews = GoodReadsListIterator(productUrl!!)
        return listReviews!!
    }

    override fun readProductData(): Product = this.getProduct()


    private fun getProductImage(doc: Document): String {
        with(doc) {
            val img = select("img[id='coverImage']")
            if (img.size >= 1) {
                return img[0]?.attr("src").toString()
            }
        }
        return ""
    }


    private fun getProductName(doc: Document): String {
        with(doc) {
            val title = select("h1[id='bookTitle']")
            if (title.size >= 1)
                return title[0]?.text().toString()
        }
        return ""
    }

    private fun getProductRating(doc: Document): Float {
        with(doc) {
            val finalRating =
                select("span[itemprop='ratingValue']")
            if (finalRating.size >= 1) {
                return finalRating[0]?.text()?.toFloat() ?: -1F
            }
            return -1F
        }
    }

    private fun getProductUrlResponse(): HtmlResponse =
        network.get(service.getSearchResult(code), true)


    private fun getProductCode(): Boolean {

        val response: HtmlResponse = this.getProductUrlResponse()
        this.productUrl = response.url

        with(Jsoup.parse(response.body)) {
            val elements = select("h1[id='bookTitle']")
            if (elements.isNotEmpty()) {
                return true
            }
        }

        return false
    }


    private fun getProduct(): Product {

        val url = this.productUrl
        val productDom = network.getDom(service.getProduct(url!!))
        //throw Exception(productDom.toString())
        val productImage = this.getProductImage(productDom)
        val productTitle: String = this.getProductName(productDom)
        //        _object?.setIndex("Recensioni $productTitle")
        val feedback: Float = this.getProductRating(productDom)
        return Product(
            name = productTitle,
            code = this.code,
            feedback = feedback,
            url = this.productUrl!!,
            imageUrl = productImage,
            date = System.currentTimeMillis()
        )
    }


}
