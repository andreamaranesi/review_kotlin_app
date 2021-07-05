package com.project.review.repositories

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project.review.models.Product
import com.project.review.models.Review
import com.project.review.notifications.Channels
import com.project.review.reviews.marketplace.Amazon
import com.project.review.reviews.marketplace.GoodReads
import com.project.review.reviews.marketplace.MarketplaceObject
import com.project.review.reviews.services.ReviewListIterator
import com.project.review.settings.Marketplace
import com.project.review.settings.Settings
import com.project.review.ui.filters.FilterWord
import kotlinx.coroutines.*
import java.math.RoundingMode
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.KClass

/**
 * deals with extracting reviews from marketplaces
 *
 * also communicates with DatabaseRepository and NetworkRepository
 *
 * @see DatabaseRepository
 * @see NetworkRepository
 * @see com.project.review.view_models.ReviewViewModel
 */
class MarketplaceRepository(
    application: Application,
    private val viewModelScope: CoroutineScope,
    private val excludedMarketplaces: MutableSet<Marketplace>,
) : DatabaseRepository(application) {

    private val lock: Lock = ReentrantLock(true)
    private val savedReviews = mutableListOf<Review>()
    internal val currentReviews = mutableListOf<Review>()

    val context: Context = application.applicationContext


    private val reviewListener = MutableLiveData<MutableList<Review>>()
    val _reviewListener: LiveData<MutableList<Review>>
        get() = reviewListener


    /**
     * it is used to save the active search threads for each marketplace
     */
    private val jobs = HashMap<Marketplace, Job>()

    /**
     * it is used to save, for each new review, the threads that are working on the update
    of the list of recurring words
     */
    private val filterWords = ArrayList<Job>()

    /**
     * interrupts the current search by suspending the several CoroutineScopes
     */
    fun forceStop() {
        lock.lock()
        val jobIterator = jobs.iterator()
        while (jobIterator.hasNext())
            jobIterator.next().value.cancel()


        for (job in filterWords) {
            job.cancel()
        }

        jobs.clear()
        filterWords.clear()
        lock.unlock()

    }

    /**
     * contains the list of MarketplaceObject to be instantiated
     *
     * @see MarketplaceObject
     */
    private val listOfObjects: MutableMap<Marketplace, KClass<MarketplaceObject>> =
        mutableMapOf()

    /**
     * contains the list of Iterators per marketplace
     *
     * @see ReviewListIterator
     */
    val listOfIterators: MutableMap<Marketplace, ReviewListIterator?> =
        mutableMapOf()

    private var maxResults = 10

    private val listOfWords = mutableListOf<FilterWord>()

    private var beginning = 0

    private var gettingNewProduct: Boolean = false


    /**
     * each marketplace is associated with the corresponding child class of MarketPlaceObject
     *
     * @see MarketplaceObject
     */
    private val mapMarketplaceToObjects: HashMap<Marketplace, KClass<MarketplaceObject>> =
        hashMapOf(
            Marketplace.AMAZON to Amazon::class as KClass<MarketplaceObject>,
            Marketplace.GOODREADS to GoodReads::class as KClass<MarketplaceObject>
        )

    /**
     * each marketplace is associated with the corresponding implementation class of ReviewListIterator
     *
     * @see ReviewListIterator
     */
    private val mapMarketplaceToIterator: HashMap<Marketplace, ReviewListIterator?> =
        hashMapOf(
            Marketplace.AMAZON to null,
            Marketplace.GOODREADS to null
        )


    /**
     * updates the list of reviews found
     */
    private fun setReviews(value: MutableList<Review>?) = this.reviewListener.postValue(value)


    /**
     * in case user starts a new search, this method
    allows the app to reset the values of local variables
     *
     * @see com.project.review.view_models.ReviewViewModel.clear
     */
    fun clear() {
        Log.i("MarketplaceRepository", "NUOVA RICERCA")
        this.beginning = 0
        this.currentReviews.clear()
        this.listOfWords.clear()
        this.wordsCounter.clear()
        this.setNewPopularWord()
        this.reviewListener.value = mutableListOf()
        this.product.value = null
        this.excludedMarketplaces.clear()

    }

    /**
     * updates the list of MarketplaceObject classes to instantiate
     *
     * @see MarketplaceObject
     */
    private fun getObjects(
        isOnlyAmazon: Boolean = false,
        excludeAmazon: Boolean = false,
    ): Map<Marketplace, KClass<MarketplaceObject>> {
        if (excludeAmazon)
            return mapMarketplaceToObjects.filterKeys { it != Marketplace.AMAZON }
        if (isOnlyAmazon)
            return mapMarketplaceToObjects.filterKeys { it == Marketplace.AMAZON }

        return mapMarketplaceToObjects
    }

    /**
     * updates the list of reviews and, for each of these that is found,
    calls the checkPopularWords method which updates the list of recurring words
     *
     * @see checkPopularWords
     */
    fun putReview(int: Int, review: Review, coroutineScope: CoroutineScope) {

        review.imageUrl = this.getCurrentProduct()?.imageUrl ?: ""
        review.wasStored(this.savedReviews)
        lock.lock()
        if (coroutineScope.isActive) {
            currentReviews.add(review)
            this.setReviews(this.currentReviews)
            val job = viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    checkPopularWords(this, listOfWords, review)
                }
            }
            this.filterWords.add(job)
        }
        lock.unlock()
    }

    private var product = MutableLiveData<Product>()
    val currentProduct: LiveData<Product>
        get() = product


    /**
     * initiates a new search or allows you to resume a suspended one
     *
     *
     * @param typeOfAction se 0 => NUOVA RICERCA; se 1 => RIPRENDE RICERCA SOSPESA
     * @see com.project.review.view_models.ReviewViewModel.onStart
     */
    fun onStart(
        typeOfAction: Int,
        maxResults: Int,
        code: String = "",
        isSpecificCode: Boolean = false,
        isString: Boolean = false,
        isAsin: Boolean = false,
    ): Boolean {

        this.maxResults = maxResults

        if (typeOfAction == 0) {
            this.gettingNewProduct = true
            this.code = code
            this.isSpecificCode = isSpecificCode
            this.isString = isString
            this.isAsin = isAsin


            if (!this.isOnlyAmazon())
                this.getStoredReviews()

            this.listOfIterators.apply {
                clear()
                putAll(mapMarketplaceToIterator)
            }

            this.listOfObjects.apply {
                clear()
                putAll(getObjects(isOnlyAmazon = isOnlyAmazon()))
            }

            this.getReviews()

        } else if (typeOfAction == 1)
            this.updateReviews()


        return true
    }

    /**
     * instantiates the threads that, for each marketplace, first look for the presence of the product,
    then they activate CoroutineScope's iterate method to extrapolate the various reviews
     *
     * @see CoroutineScope.iterate
     */
    private fun initJob(marketplace: Marketplace, reference: KClass<MarketplaceObject>) {
        val context = this
        val job = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val OBJ =
                    reference.constructors.first().call(code, isSpecificCode, isAsin)
                val init = OBJ.init()
                if (init) {
                    if (isActive) {
                        val product = OBJ.readProductData()
                        if (isActive && product != null) {
                            setCurrentProduct(product, marketplace)
                            if (isOnlyAmazon() && marketplace == Marketplace.AMAZON) {
                                code = product.code
                                isSpecificCode = true
                                getStoredReviews()

                                listOfObjects.apply {
                                    clear()
                                    putAll(getObjects(excludeAmazon = true))
                                }
                                getReviews()
                            }
                        }
                    }

                    /* THE THREAD IS SUSPENDED UNTIL SAVED REVIEWS ASSOCIATED
                       WITH THE CURRENT PRODUCT ARE FOUND
                    */
                    waitDatabaseInit()

                    if (isActive) {
                        val iterator = OBJ.iterator()
                        listOfIterators[marketplace] = iterator
                        this.iterate(iterator, max = maxResults + beginning, context)

                    }
                }
                if (isActive) {
                    jobs.remove(marketplace)
                    stop()
                }
            }

        }

        jobs[marketplace] = job

    }


    private fun setCurrentProduct(product: Product, marketplace: Marketplace) {
        this.gettingNewProduct = false

        if (this.product.value == null) {
            this.insertProduct(product.apply {
                this.marketplace.add(marketplace)
            })
            this.product.postValue(product)
        } else {
            val toInsert = this.product.value?.apply {
                this.marketplace.add(marketplace)
                this.feedback =
                    ((this.feedback + product.feedback) / this.marketplace.size).toBigDecimal()
                        .setScale(2, RoundingMode.UP).toFloat()
                this.relatedProducts.addAll(product.relatedProducts)
            }

            this.insertProduct(toInsert)
            this.product.postValue(toInsert)

        }

        Channels.scheduleNotification(context)

    }

    /**
     * resumes the search for reviews
     *
     * @see CoroutineScope.iterate
     */
    private fun initUpdateJob(marketplace: Marketplace, iterator: ReviewListIterator) {
        val context = this
        val job = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                this.iterate(iterator, max = maxResults + beginning, context)
                if (isActive) {
                    jobs.remove(marketplace)
                    stop()
                }
            }
        }
        jobs[marketplace] = job
    }

    private fun stop() {
        if (jobs.isEmpty())
            this.setReviews(null)
    }


    private var code = ""
    private var isSpecificCode = false
    private var isString = false
    private var isAsin = false

    private fun isOnlyAmazon(): Boolean {
        return this.isString || this.isAsin
    }

    private var databaseInit: Deferred<Unit>? = null

    /**
     * it is used to block the threads that deal with the research of reviews
       until those saved in the database for the current product are retrieved
     *
     * @see onStart
     * @see initJob
     */
    private suspend fun waitDatabaseInit() = databaseInit?.await()


    /**
     * retrieves the reviews saved in the database given the product code
     */
    private fun getStoredReviews() {
        databaseInit?.cancel()

        databaseInit = viewModelScope.async {
            withContext(Dispatchers.IO) {
                getSavedReviews(code, savedReviews)
            }
        }
    }


    private fun getReviews() {
        val iterator = this.listOfObjects.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            this.initJob(item.key, item.value)
        }
    }

    private fun updateReviews() {
        this.beginning = this.currentReviews.size

        val iterator = listOfIterators.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.value != null && !excludedMarketplaces.contains(item.key)) {
                this.initUpdateJob(item.key, item.value!!)
            }
        }
    }

    private fun getCurrentProduct(): Product? = this.currentProduct.value

    private val wordsCounter = mutableMapOf<String, Int>()

    /**
     * manages the critical section of the checkPopularWords method
     *
     * @see checkPopularWords
     */
    private val wordsLock = ReentrantLock(true)

    private val popularWords = MutableLiveData<MutableList<FilterWord>>()

    val _popularWords: LiveData<MutableList<FilterWord>>
        get() = popularWords


    /**
     * updates the list of recurring words
     */
    private fun setNewPopularWord() {
        this.popularWords.postValue(mutableListOf<FilterWord>().apply { addAll(listOfWords) })
    }

    /**
     * updates the list of recurring words by extrapolating the terms from the body of the current review
     *
     * If the i-th has been selected by the user among the words to be updated, it remains in the
       original position, the first unselected word after that one will be replaced
       by the algorithm
     *
     *
     * is a critical section
     */
    private fun checkPopularWords(
        coroutineScope: CoroutineScope,
        list: MutableList<FilterWord>,
        review: Review,
    ) {
        wordsLock.lock()
        if (coroutineScope.isActive) {
            val body = review.body
            val words = this.getWords(body)
            for (word in words) {
                wordsCounter[word] = wordsCounter[word]?.plus(1) ?: 1
            }

            var orderedList =
                wordsCounter.toList().sortedByDescending { (key, value) -> value }.toMap()
            val wordsInList = list.map { it.name }
            //orderedList = orderedList.filter { !wordsInList.contains(it.key) }

            val iterator = orderedList.iterator()
            var c = 0
            val setOfName = mutableSetOf<String>()

            while (coroutineScope.isActive && c < Settings.wordResults && iterator.hasNext()) {
                var canContinue = true
                val name = iterator.next().key

                if (list.size > c) {
                    val element = list[c]
                    val sameName = name == element.name
                    if (sameName || element.checked)
                        canContinue = false
                    if (element.checked && !sameName)
                        setOfName.add(name)

                }


                if (canContinue) {
                    if (setOfName.isEmpty()) {
                        this.setPopularWordList(list, c++, FilterWord(name))
                    } else {
                        this.setPopularWordList(list, c++, FilterWord(setOfName.first()))
                        setOfName.remove(setOfName.first())
                    }
                } else
                    c++

            }

            this.setNewPopularWord()
        }
        wordsLock.unlock()
    }

    private fun setPopularWordList(
        list: MutableList<FilterWord>,
        index: Int,
        filterWord: FilterWord,
    ) {
        if (list.size > index) {
            list[index] = filterWord
        } else
            list.add(filterWord)
    }

    /**
     * extrapolates the words from the body of the review
     */
    private fun getWords(body: String): List<String> {
        val regex = """(\w{4,})""".toRegex()
        return regex.findAll(body).map { it.value.toUpperCase() }.toList()
    }

}

/**
 * extends the basic functions of CoroutineScope
 *
 * through the hasNext() and next() methods of ReviewListIterator it extrapolates the various reviews
   and updates the current list
 *
 * @see ReviewListIterator
 */
private fun CoroutineScope.iterate(
    iterator: ReviewListIterator,
    max: Int,
    repository: MarketplaceRepository,
) {
    while (isActive && repository.currentReviews.size < max && iterator.hasNext()) {
        val review: Review? = iterator.next()
        if (review != null) {
            repository.putReview(repository.currentReviews.size, review, this)
        }
    }
}