package com.project.review.view_models

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.project.review.models.*
import com.project.review.repositories.MarketplaceRepository
import com.project.review.settings.Marketplace
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import com.project.review.ui.filters.FilterWord
import com.project.review.ui.filters.RecyclerViewFilters
import kotlinx.coroutines.*
import java.util.concurrent.locks.ReentrantLock

/**
 * Main View Model
 * It initializes a new search and filter the results
 *
 * @see com.project.review.MainActivity
 * @see com.project.review.settings.Tools.searchReviews
 * @see com.project.review.settings.Tools.findMoreReviews
 */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
    val excludedMarketplaces = mutableSetOf<Marketplace>()
    var currentReviews = mutableListOf<Review>()

    private var searchText: String? = null
    private var maxReview: Int = 5
    private var minReview: Int = 0

    private var orderBy: RecyclerViewFilters.OrderBy = RecyclerViewFilters.OrderBy.HIGH_TO_LOW
    private val lock = ReentrantLock(true)

    private val reviews: MutableList<Review>
        get() = this._reviewListener.value ?: mutableListOf()


    /**
     * definitely sets the items on the list, which can be reviews
    or reviews where to apply the shimmer effect
     */
    fun set(list: MutableList<Review>): MutableList<Review> {
        val counter = this.reviewPreloaderCount()
        repeat(counter) {
            list.add(Review.preloaderItem().apply {
                if (counter == Settings.initShimmerCount)
                    this.firstPreloader = true
            })
        }
        return list
    }


    /**
     * gets the number of preload reviews
     */
    private fun reviewPreloaderCount(): Int {
        return if (this.scrollMore(fromView = false))
            if (this.noProduct()) Settings.initShimmerCount else Settings.afterShimmerCount
        else
            0
    }


    private val marketplaceRepository =
        MarketplaceRepository(application, viewModelScope, excludedMarketplaces)


    private val isApplyingFilters = MutableLiveData<Boolean>()
    val _isApplyingFilters: LiveData<Boolean>
        get() = isApplyingFilters


    private val reviewListener =
        MediatorLiveData<MutableList<Review>>()
    val _reviewListener: LiveData<MutableList<Review>>
        get() = reviewListener

    private val filteredItemCounter = MutableLiveData<Int>()
    val _filteredItemCounter: LiveData<Int>
        get() = filteredItemCounter


    private var isUsedInService: Boolean = false

    /**
     * for each new review obtained from MarketplaceRepository filters the list
     * if the received object is null then it means that the search is
    temporarily or permanently suspended
     *
     * @see setEnd
     * @see MarketplaceRepository
     */
    private fun getReviews() {
        reviewListener.addSource(marketplaceRepository._reviewListener) {
            if (it != null)
                currentReviews = it
            else
                setEnd()

            viewModelScope.launch {
                reviewListener.value = filterReviews()
            }
        }
    }

    /**
     * gets the latest default value for sorting reviews
     *
     * @see com.project.review.ui.filters.RecyclerViewFilters.OrderBy
     */
    private fun getOrderByDefaultValue() {
        Settings.isOnline(context)

        val sharedPreferences = Settings.getSharedPreferences(context)

        if (sharedPreferences != null) {
            val orderByString =
                sharedPreferences.getString(
                    Settings.ORDER_BY,
                    RecyclerViewFilters.OrderBy.HIGH_TO_LOW.name
                )
            val orderBy = RecyclerViewFilters.OrderBy.valueOf(orderByString!!)
            this.orderBy = orderBy
        }
    }

    init {
        this.context = application.applicationContext
        Settings.initLang(this.context)
        getReviews()
        getOrderByDefaultValue()
    }


    /**
     * method to apply filters to the current list of reviews
     *
     * è una sezione critica
     */
    private fun applyFilters(): MutableList<Review> {
        if (currentReviews.isNotEmpty()) {
            val listOfWords = this.selectedWords.toList()
            var tempMutableList = listOf<Review>()
            lock.lock()

            // ORDERS THE REVIEW LIST

            when (this.orderBy) {
                RecyclerViewFilters.OrderBy.HIGH_TO_LOW -> {
                    tempMutableList =
                        currentReviews.sortedByDescending { value -> value.stars }

                }
                RecyclerViewFilters.OrderBy.LOW_TO_HIGH -> {
                    tempMutableList =
                        currentReviews.sortedBy { value -> value.stars }

                }
                RecyclerViewFilters.OrderBy.DATE -> {
                    tempMutableList =
                        currentReviews.sortedByDescending { value -> value.id }
                }
                RecyclerViewFilters.OrderBy.INV_DATE -> {
                    tempMutableList =
                        currentReviews.sortedBy { value -> value.id }
                }
            }


            // FILTERS THE LIST OF REVIEWS

            if (this.excludedMarketplaces.isNotEmpty() && tempMutableList.isNotEmpty()) {
                tempMutableList =
                    tempMutableList.filter { !excludedMarketplaces.contains(it.marketplace) }

            }
            if ((this.minReview > 0 || this.maxReview < 5) && tempMutableList.isNotEmpty())
                tempMutableList =
                    tempMutableList.filter { it.stars >= this.minReview && it.stars <= this.maxReview }


            for (it in tempMutableList)
                Tools.removeHighlight(it)



            if (this.searchText != null && this.searchText!!.isNotEmpty() && tempMutableList.isNotEmpty()) {
                tempMutableList = tempMutableList.filter {
                    // HIGHLIGHTS FOUND WORDS IN YELLOW
                    Tools.highlightWordsOnReview(mutableListOf(this.searchText!!),
                        it)

                    it.initBody.toLowerCase().contains(
                        searchText!!.toLowerCase(
                        )
                    )
                }
            }

            if (listOfWords.isNotEmpty() && tempMutableList.isNotEmpty()) {
                tempMutableList = tempMutableList.filter {
                    // HIGHLIGHTS FOUND WORDS IN YELLOW
                    Tools.highlightWordsOnReview(listOfWords, it)
                    var result = false
                    for (word in listOfWords) {
                        result = it.initBody.toUpperCase().contains(word)
                        if (!result)
                            break
                    }
                    result
                }
            }

            lock.unlock()
            return set(tempMutableList.toMutableList())

        } else
            return set(mutableListOf())
    }


    fun getOrderBy(): RecyclerViewFilters.OrderBy = this.orderBy

    fun getOrderByText(): String = RecyclerViewFilters.OrderBy.getName(this.getOrderBy(), context)

    private fun setIsApplyingFilters(value: Boolean) {
        this.isApplyingFilters.value = value
    }

    /**
     * forces the review list to be updated following a filter change
     */
    private fun triggerUpdate() {
        viewModelScope.launch {
            setIsApplyingFilters(true)
            reviewListener.value = filterReviews()
            setIsApplyingFilters(false)
            forceUpdate()
        }
    }

    fun setOrderBy(orderBy: String) {
        this.orderBy = RecyclerViewFilters.OrderBy.valueOf(orderBy)
        val sharedPreferences = Settings.getSharedPreferences(context)
        sharedPreferences?.edit()?.putString(Settings.ORDER_BY, orderBy)?.commit()
        this.triggerUpdate()
    }

    fun setFilterbyReviews(min: Int, max: Int) {
        this.minReview = min
        this.maxReview = max
        this.triggerUpdate()
    }


    fun setExcludedMarketplace() {
        this.triggerUpdate()
    }

    private var selectedWords = mutableListOf<String>()

    fun setListOfWords(
        selectedWords: MutableList<FilterWord>,
    ) {
        this.selectedWords = selectedWords.filter { it.checked }.map { it.name }.toMutableList()
        this.triggerUpdate()
    }

    fun setSearchText(searchText: String) {
        this.searchText = searchText
        this.triggerUpdate()
    }

    private val storeLock = ReentrantLock(true)

    fun storeReview(review: Review, newValue: Boolean) {
        viewModelScope.launch {
            marketplaceRepository.storeReview(storeLock,
                review,
                newValue,
                currentProduct.value!!.code)
        }
    }


    /**
     * tells if the ViewModel is currently engaged in a search operation
     */
    fun isBusy(): Boolean = isUsedInService


    /**
     * provides the current number of reviews found
     */
    fun currentFilteredReviews(): Int = reviews.size - this.reviewPreloaderCount()


    /**
     * when the search is temporarily suspended, this method allows you to resume it in case
    where too few hits were found
     *
     */
    private fun forceUpdate() {
        if (this.currentFilteredReviews() <= Settings.minReviewResults
            && this.scrollMore()
        ) {
            this.onStart(typeOfAction = 1, maxResults = maxResults)
        }
    }

    /**
     * the research has been suspended
     */
    private fun setEnd() {
        isUsedInService = false
        this.forceUpdate()
    }


    fun forceStop() = marketplaceRepository.forceStop()

    private var maxResults = 10

    override fun onCleared() {
        println("destroy")
        super.onCleared()

    }


    /**
     * called when starting a new search
     * the method allows to reset the values of local variables
     *
     * @see MarketplaceRepository.clear
     */
    private fun clear() {
        forceStop()
        this.minReview = 0
        this.maxReview = 5
        this.selectedWords.clear()
        this.currentReviews.clear()
        this.searchText = null
    }


    val currentProduct: LiveData<Product>
        get() = marketplaceRepository.currentProduct


    /**
     * initiates a new search or allows you to resume a suspended one
     *
     * @param typeOfAction se 0 => NEW RESEARCH; if 1 => RESUME SEARCH SUSPENDED
     * @see MarketplaceRepository.onStart
     */
    fun onStart(
        typeOfAction: Int,
        maxResults: Int,
        code: String = "",
        isSpecificCode: Boolean = false,
        isString: Boolean = false,
        isAsin: Boolean = false,
    ) {

        if (typeOfAction == 0) {
            this.clear()
            marketplaceRepository.clear()
        }

        this.isUsedInService = true


        val product = currentProduct.value
        if (product != null) {
            this.maxResults =
                maxResults + maxResults * (excludedMarketplaces.size / product.marketplace.size)
        } else
            this.maxResults = maxResults


        marketplaceRepository.onStart(typeOfAction,
            this.maxResults,
            code,
            isSpecificCode,
            isString,
            isAsin)


    }


    /**
     * determines if there are still reviews to be extrapolated
     *
     * @see MarketplaceRepository.listOfIterators
     */
    fun scrollMore(fromView: Boolean = true): Boolean {
        if (isBusy() && fromView)
            return false
        if (isBusy() && !fromView)
            return true

        val value = marketplaceRepository.listOfIterators
        val iterator = value.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.value != null) {
                if (!item.value!!.end && !excludedMarketplaces.contains(item.key))
                    return true
            }
        }


        return false
    }


    fun getCurrentProduct(): Product? = this.currentProduct.value

    val _popularWords = marketplaceRepository._popularWords


    fun getMinReview(): Float {
        var min = this.minReview.toFloat()
        if (min < 1)
            min = 1F
        if (min > 5)
            min = 5F
        return min
    }

    fun getMaxReview(): Float = this.maxReview.toFloat()


    /**
     * determines the absence of results
     */
    fun noResults(): Boolean {
        if (!this.isBusy() && this.isEmpty()) {
            return true
        }

        return false
    }


    private fun isEmpty(): Boolean = this.currentFilteredReviews() == 0

    /**
     * determines if the product has not yet been found
     */
    fun noProduct(): Boolean {
        if (this.currentProduct.value == null)
            return true
        return false
    }


    private suspend fun filterReviews(): MutableList<Review> {
        return withContext(Dispatchers.Default) {
            val list = applyFilters()
            setFilteredItemCounter(list)
            list
        }
    }

    /**
     * updates the LiveData that represents the number of reviews found
     */
    private fun setFilteredItemCounter(list: MutableList<Review>) =
        this.filteredItemCounter.postValue(list.size - this.reviewPreloaderCount())


}