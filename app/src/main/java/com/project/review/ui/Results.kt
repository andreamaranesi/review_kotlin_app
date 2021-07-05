package com.project.review.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cooltechworks.views.shimmer.ShimmerRecyclerView
import com.project.review.R
import com.project.review.models.Review
import com.project.review.view_models.ReviewViewModel
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import com.project.review.ui.recyclerview_adapters.ReviewRecyclerViewAdapter
import com.willy.ratingbar.RotationRatingBar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * displays the list of reviews during a search
 */
class Results : Fragment(), ReviewRecyclerViewAdapter.Actions {

    private val reviewModel: ReviewViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var productLayout: ConstraintLayout
    private lateinit var productTitle: TextView
    private lateinit var productReview: TextView
    private lateinit var productImage: ImageView
    private lateinit var recyclerViewAdapter: ReviewRecyclerViewAdapter
    private lateinit var productShimmer: ShimmerRecyclerView
    private lateinit var productStars: RotationRatingBar
    private lateinit var preloader: LinearLayout
    private lateinit var noResults: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.results, container, false)
    }


    /**
     * when the product is found, it cancels the shimmer effect, otherwise it sets it
     */
    private fun setProductVisibility(isVisible: Boolean) {

        productLayout.visibility = if (isVisible) View.VISIBLE else View.GONE
        productShimmer.visibility = if (!isVisible) View.VISIBLE else View.GONE

        if (!isVisible)
            productShimmer.showShimmerAdapter()
        else
            productShimmer.hideShimmerAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView(view)


        productShimmer = view.findViewById(R.id.product_shimmer)
        productShimmer.showShimmerAdapter()

        productStars = view.findViewById(R.id.review_stars)
        productLayout = view.findViewById(R.id.product_layout)
        productTitle = view.findViewById(R.id.product_title)
        productReview = view.findViewById(R.id.product_review)
        productImage = view.findViewById(R.id.product_image)
        preloader = view.findViewById(R.id.preloader)
        noResults = view.findViewById(R.id.no_results)

        // OBSERVE IF THE CURRENT PRODUCT IS FOUND
        reviewModel.currentProduct.observe(viewLifecycleOwner, {
            if (it != null) {
                this.setProductVisibility(true)
                Glide.with(this).load(it.imageUrl).into(productImage)
                productTitle.text = it.name
                if (it.feedback >= 1) {
                    productStars.rating = it.feedback
                    productReview.text = it.feedback.toString()
                } else {
                    productReview.text = ""
                    productStars.rating = 0F
                }

            } else
                this.setProductVisibility(false)


        })

    }

    private var hasToCheck: Boolean = true
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var actions: Actions

    /**
     * It is used to share the current LinearLayout with MainActivity
     * MainActivity, whenever the user filters reviews by content,
       can, in this way, brings the RecyclerView to the top of the list
     *
     * @see com.project.review.MainActivity
     */
    interface Actions {
        fun setLinearLayout(linearLayoutManager: LinearLayoutManager)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.actions = context as Actions
    }

    private fun initRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerViewAdapter = ReviewRecyclerViewAdapter(
            this
        )

        linearLayoutManager = LinearLayoutManager(context)
        this.actions.setLinearLayout(linearLayoutManager)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recyclerViewAdapter


        reviewModel._isApplyingFilters.observe(viewLifecycleOwner, {
            preloader.visibility = if (it) View.VISIBLE else View.GONE
        })

        reviewModel._reviewListener.observe(viewLifecycleOwner, {
            this.checkNoResults(it)
            hasToCheck = true
        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (hasToCheck)
                    triggerUpdate(linearLayoutManager)

            }
        })

    }

    /**
     * checks if no results have been found and updates the current list
     */
    private fun checkNoResults(list: MutableList<Review>) {
        if (reviewModel.noResults()) {
            noResults.visibility = View.VISIBLE
            if (reviewModel.noProduct()) {
                productLayout.visibility = View.GONE
                productShimmer.visibility = View.GONE
            } else
                productLayout.visibility = View.VISIBLE
        } else {
            noResults.visibility = View.GONE
        }

        recyclerViewAdapter.submitList(list)
    }


    /**
     * if ReviewViewModel is not busy, checks if user is almost at the bottom of the list
     * in the latter case, if there are other reviews to upload, ReviewViewModel will be notified
     *
     * @see com.project.review.settings.Tools.findMoreReviews
     * @see com.project.review.view_models.ReviewViewModel
     * @see com.project.review.view_models.ReviewViewModel.isBusy
     */
    private fun triggerUpdate(linearLayoutManager: LinearLayoutManager) {
        if (!reviewModel.isBusy()) {

            val firstVisible: Int = linearLayoutManager.findFirstVisibleItemPosition()
            val totalItem: Int = linearLayoutManager.itemCount
            if (firstVisible + Settings.initShimmerCount >= totalItem) {

                if (reviewModel.scrollMore()) {
                    Tools.findMoreReviews(reviewModel)
                    hasToCheck = false
                }
            }
        } else
            Log.d("Results", "viewModel busy")
    }


    /**
     * stores the reference to Review and its position in the list when
       the user presses on a review, activating the reviewOnClick method
     *
     * @see reviewOnClick
     * @see com.project.review.settings.Tools.findMoreReviews
     * @see com.project.review.view_models.ReviewViewModel
     */
    data class TempData(val review: Review, val position: Int)

    private var _tempData: TempData? = null

    /**
     *
     * navigates to ReviewDialogActivity and creates a TempData instance
     *
     * @see TempData
     * @see ReviewDialogActivity
     */
    override fun reviewOnClick(review: Review, position: Int) {
        val intent = Intent(context, ReviewDialogActivity::class.java).apply {
            putExtra(Settings.REVIEW, Json.encodeToString(review))
            putExtra(Settings.CODE, reviewModel.getCurrentProduct()?.code)
            putExtra(Settings.IS_SEARCHING, true)
        }


        _tempData = TempData(review, position)
        resultLauncher.launch(intent)


    }

    /**
     * when the user goes back from ReviewDialogActivity
       takes the TempData instance and notify the current list to update the item
     *
     * this mechanism has been implemented as the user can save or delete the review from favorites
       from within the called Activity. Consequently, when it comes back, it is good to notify the list
       since the two Reviews are different objects
     *
     * @see TempData
     * @see reviewOnClick
     * @see ReviewDialogActivity
     */
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && _tempData != null) {
                val reviewSaved = result.data?.extras?.getBoolean(Settings.SAVED, false)
                    ?: _tempData?.review?.stored
                /* IF THE USER HAS CHANGED THE stored PROPERTY IN ReviewDialogActivity,
                   HERE WE REFLECT THE CHANGE ON THE CURRENT REFERENCE OF Review */
                _tempData?.review?.stored = reviewSaved!!
                recyclerViewAdapter.notifyItemChanged(_tempData?.position!!)
            }
        }

    override fun storeReview(review: Review, position: Int) {
        review.stored = !review.stored
        reviewModel.storeReview(review, review.stored)
        recyclerViewAdapter.notifyItemChanged(position)
    }

}



