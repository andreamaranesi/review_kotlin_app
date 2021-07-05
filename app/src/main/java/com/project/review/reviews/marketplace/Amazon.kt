package com.project.review.reviews.marketplace

import android.net.Uri
import android.util.Log
import com.project.review.models.Product
import com.project.review.models.RelatedProduct
import com.project.review.models.Review
import com.project.review.repositories.NetworkRepository
import com.project.review.reviews.services.ReviewListIterator
import com.project.review.settings.Marketplace
import com.project.review.settings.Settings
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * defines how reviews are to be sourced from Amazon
 *
 * @see ReviewListIterator
 */
class AmazonListIterator(
    var code: String,
) : ReviewListIterator {
    override val marketplace: Marketplace = Marketplace.AMAZON

    private lateinit var dom: Document

    private val network = NetworkRepository()
    private var service = network.amazonRetrofit(Amazon.endPoint())
    private var currentPage: Int = 1


    init {
        this.readNextLink()
    }

    private var currentIndex: Int = 0
    private var maxIterationSize: Int = 0
    override var end: Boolean = false
    private var counter: Int = 0
    private var currentReviews: Elements? = null
    private var tempEnd: Boolean = false

    override fun start() {
        this.currentIndex = 0
        this.currentReviews = this.dom.select("div[data-hook='review']")
        this.maxIterationSize = this.currentReviews?.size ?: 0
        val a = this.dom.select("li[class='a-last'] a")
        if (a != null) {
            if (a.isEmpty())
                this.tempEnd = true
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
            val body: String =
                elements?.select("span[data-hook='review-body']")?.get(0)?.select("span")
                    ?.get(0)?.text() ?: ""
            val reviewID: String =
                elements?.attr("id").toString()

            val _stars =
                elements?.select("i[data-hook='review-star-rating']")

            if (_stars?.size ?: 0 > 0) {
                val stars = Amazon.filterStars(
                    _stars?.select("span")
                        ?.get(0)?.text() ?: ""
                )
                val title: String =
                    elements?.select("a[data-hook='review-title']")?.get(0)?.select("span")
                        ?.get(0)?.text() ?: ""
                val date: String =
                    elements?.select("span[data-hook='review-date']")?.get(0)?.text() ?: ""

                review = Review(
                    id = reviewID, title = title, body = body, date = date, stars = stars,
                    marketplace = Marketplace.AMAZON
                )
            }
        }

        this.currentIndex++
        return review
    }

    private fun readNextLink() =
        this.apply { dom = network.getDom(service.getReviews(code, this.currentPage++)) }.start()

}

/**
 *
 * contains the methods to get the product from Amazon starting from an Asin code or not
 *
 * @see MarketplaceObject
 */
class Amazon(code: String, isSpecificCode: Boolean, isAsin: Boolean) :
    MarketplaceObject(code, isSpecificCode, isAsin) {

    private val network = NetworkRepository()
    private val service = network.amazonRetrofit(endPoint())
    private var specificCode: String? = null

    companion object {

        /**
         * ratings on Amazon are shown as "x, y {text}"
           the method is used to extrapolate a Float object from the received string
         */
        fun filterStars(string: String): Float {
            val expression = """(\s)(.)*"""
            return string.replace(expression.toRegex(), "")
                .replace(",", ".").toFloat()
        }

        fun endPoint(): String {
            return Settings.amazonEndPoint()
        }

    }

    override fun init(): Boolean {

        if (isAsin && !this.isSpecificCode)
            return true
        else {
            val finalCode = this.getProductCode() ?: return false
            this.code = finalCode


            if (this.isSpecificCode) {
                this.specificCode = this.code
                val dom = this.camelcamel()
                val button =
                    dom.select("a[class*='button expanded buy'], a[class='button buy']")
                if (button.isNotEmpty())
                    this.code = button[0].attr("x-camel-asin")
                else {
                    println("il codice a barre è => " + this.code)
                    return false
                }
            }
        }

        return true

    }

    private var listReviews: AmazonListIterator? = null


    override fun iterator(): AmazonListIterator {
        if (listReviews == null)
            listReviews = AmazonListIterator(code)
        return listReviews!!
    }

    override fun readProductData(): Product? = this.getProduct()


    /**
     * tries to find the img tag containing the data-a-dynamic-image attribute
     *
     * data-a-dynamic-image doesn't contain a simple url so, to get the right one,
       a regular expression is used
     */
    private fun getProductImage(doc: Document): String? {

        with(doc) {
            val selection =
                select("img")

            selection.forEach {
                if (
                    it.hasAttr("data-a-dynamic-image")
                ) {
                    val value = it.attr("data-a-dynamic-image").toString()
                    val regex = """(?=https\:\/\/)(?=([\w]*))(?=([\-])*)(?=([^"]*))""".toRegex()
                    val url = regex.find(value)?.groupValues
                    if (url?.size ?: 0 >= 3)
                        return regex.find(value)?.groupValues?.get(3)

                }
            }
            Log.e("AMAZON", "image not found: $doc")
            return null
        }
    }


    private fun getProductName(doc: Document): String {
        with(doc) {
            val name = select("span[id='productTitle']")
            if (name.isNotEmpty()) {
                return name[0].text()
            }
        }
        return ""
    }

    private fun getProductRating(doc: Document): Float {
        with(doc) {
            val finalRating =
                select("span[class^='reviewCountTextLinkedHistogram']")
            if (finalRating.size >= 1)
                return filterStars(finalRating[0].attr("title"))

            return -1F
        }
    }

    private fun getProductCode(): String? {
        if (isSpecificCode) {
            return this.code
        } else {
            with(network.getDom(service.getSearchResult(code))) {
                val elements = select("[data-component-type='s-search-result']")
                return if (elements.isNotEmpty()) {
                    elements[0].attr("data-asin").toString()
                } else
                    null
            }
        }

    }


    private fun getProduct(): Product? {

        val get = network.get(service.getProduct(code), true)
        val url = get.url
        if (url.contains("primevideo"))
            return null
        val productDom = Jsoup.parse(get.body)
        val productImage: String? = this.getProductImage(productDom)
        val productTitle: String = this.getProductName(productDom)
        val feedback: Float = this.getProductRating(productDom)

        val listOfRelatedProduct = mutableListOf<RelatedProduct>()

        this.getRelatedProducts(listOfRelatedProduct, productDom)

        return Product(
            name = productTitle,
            code = if (isSpecificCode) this.specificCode!! else this.ASINtoSpecific(),
            feedback = feedback,
            url = url,
            imageUrl = productImage,
            date = System.currentTimeMillis()
        ).apply { relatedProducts = listOfRelatedProduct }


    }

    /**
     * gets all related products from the product page
     */
    private fun getRelatedProducts(
        listOfRelatedProduct: MutableList<RelatedProduct>,
        dom: Document,
    ) {
        val products =
            dom.select("div[class*='a-carousel-container'] li[class='a-carousel-card']")
        for (product in products) {
            val links = product.select("a[class='a-link-normal']")
            if (links.size >= 2) {
                val url = endPoint() + links[0].attr("href")!!
                val title = links[1].select("div")[0].text()!!
                val code = Uri.parse(url).getQueryParameter("pd_rd_i")
                if (code != null) {
                    val marketplace = Marketplace.AMAZON

                    val feedbackResult = product.select("i[class*='a-icon'] span")

                    if (feedbackResult.isNotEmpty()) {
                        val feedback =
                            filterStars(feedbackResult[0].text()!!)
                        val imageUrl =
                            product.select("img[class*='a-dynamic-image']")[0].attr("src")!!

                        listOfRelatedProduct.add(
                            RelatedProduct(
                                title,
                                code,
                                feedback,
                                url,
                                imageUrl,
                                marketplace
                            )
                        )
                    }
                }
            }

        }
    }

    private fun camelcamel(): Document {
        val url = service.searchOnCamelCamel(Settings.camelcamelEndPoint() + "/search?sq=$code")
        return network.getDom(url)
    }


    private fun searchforCodes(elements: Elements) {
        for (element in elements) {
            val tds = element.select("td")
            if (tds.size >= 2) {
                val text = tds[0]?.text()?.toLowerCase()
                if (text?.contains("isbn") == true || text?.contains("upc") == true) {
                    this.specificCode = tds[1]?.text() ?: this.code
                    println("CODICE ISBN O UPC RICEVUTO => " + this.specificCode + " ; " + this.code)
                    break
                } else if (text?.contains("ean") == true) {
                    val ean = tds[1]?.text() ?: ""
                    //verifichiamo se l'ean è della lunghezza corretta
                    if (ean.length == 8 || ean.length == 13)
                        this.specificCode = ean
                }
            }
        }
    }


    private fun ASINtoSpecific(): String {
        val dom = this.camelcamel().select("table[class='product_fields']")
        if (dom.isNotEmpty()) {
            val obj = dom[0].select("tr")
            this.searchforCodes(obj)
        }
        return this.specificCode ?: this.code
    }


}
