package com.project.review.ui


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.color.MaterialColors
import com.project.review.R
import com.project.review.databinding.ReviewDialogBinding
import com.project.review.dialogs.ConfirmDialog
import com.project.review.view_models.ReviewDialogViewModel
import com.project.review.models.Review
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.locks.ReentrantLock

/**
 * Shows the selected review and allows you to share it
 * It also allows you to save or remove the review from favorites
 *
 * @see SavedReviewActivity.reviewOnClick
 * @see Results.reviewOnClick
 */
class ReviewDialogActivity : AppCompatActivity(), ConfirmDialog.Actions {

    private lateinit var binding: ReviewDialogBinding
    private lateinit var review: Review
    private val viewModel: ReviewDialogViewModel by viewModels()
    private var requestMade = false
    private var productCode = ""
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.review_dialog)

        val reviewEncoded: String = intent.extras?.getString(Settings.REVIEW)!!
        productCode = intent.extras?.getString(Settings.CODE)!!
        isSearching = intent.extras?.getBoolean(Settings.IS_SEARCHING)!!


        review = Json.decodeFromString(reviewEncoded)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }


        binding.collapseActionView.title = review.title
        binding.body = review.body
        binding.reviewStars.rating = review.stars

        if (!isSearching) {
            binding.wishlistButton.visibility = View.GONE
            binding.cancelWishlistButton.visibility = View.VISIBLE
        }


        this.setActions()
        this.listener()
        Tools.setWishlistButton(binding.root.rootView, review, binding.wishlistButton)

    }

    private fun toastMaker(txt: String) {
        Toast.makeText(this, txt, Toast.LENGTH_SHORT).show()
    }

    /**
     * observe if the image associated with the current product has been saved or not
     */
    private fun listener() {
        viewModel._resultUri.observe(this, {

            if (it != null && requestMade) {
                this.callShareAction(it)
            } else if (it == null && requestMade) {
                toastMaker(getString(R.string.error_message))
            }


            requestMade = false
        })
    }

    /**
     * allows user to share the review using an implied intent
     */
    private fun callShareAction(uri: Uri?) {

        val share = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, review.initBody)
            putExtra(
                Intent.EXTRA_TITLE,
                getString(R.string.review_from) + " " + review.marketplace + " (" + review.stars + "/5.0)"
            )
            if (uri != null) {
                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )
            }
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }, getString(R.string.review))
        startActivity(share)
    }

    private val storeLock = ReentrantLock(true)

    private fun setActions() {
        binding.cancelWishlistButton.setOnClickListener {
            Tools.showConfirmDialog(binding.root, supportFragmentManager)
        }
        binding.shareButton.setOnClickListener {
            this.shareReview()
        }
        binding.wishlistButton.setOnClickListener {
            review.stored = !review.stored
            this.saveReview()
        }
    }

    private fun saveReview() {
        viewModel.storeReview(storeLock, review, review.stored, productCode)
        Tools.setWishlistButton(binding.root.rootView, review, binding.wishlistButton)
    }

    /**
     * method that initiates the review sharing process
     * starts the download of the image possibly associated with the product
     *
     * @see ReviewDialogViewModel.saveImage
     */
    private fun shareReview() {

        if (Settings.networkAvailable.value!!) {
            if (!requestMade) {
                if (review.imageUrl.isNotEmpty()) {
                    requestMade = true
                    viewModel.saveImage(review.imageUrl)
                    toastMaker(getString(R.string.wait))
                } else
                    this.callShareAction(null)
            }
        } else {
            this.callShareAction(null)
        }
    }

    /**
     * returns the value of "stored" property of the Review
     */
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(Settings.SAVED, review.stored)
        })

        finish()
    }

    /**
     * method that is called when a user wants to remove a saved review from the database
     *
     * can ONLY be activated when the class is called by SavedReviewActivity
     *
     * @see SavedReviewActivity.reviewOnClick
     */
    override fun onPositiveClick() {
        review.stored = false
        this.saveReview()
        this.onBackPressed()
    }

    override fun onNegativeClick() {
        // DO NOTHING
    }
}