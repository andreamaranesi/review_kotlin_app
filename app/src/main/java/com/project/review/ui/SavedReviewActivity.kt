package com.project.review.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.review.R
import com.project.review.databinding.HomeActivityRecyclerviewBinding
import com.project.review.dialogs.ConfirmDialog
import com.project.review.dialogs.ProgressDialog
import com.project.review.models.Review
import com.project.review.view_models.HomeViewModel
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import com.project.review.ui.recyclerview_adapters.SavedReviewAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Activity that shows all the saved reviews associated with a specific product
 *
 * @see ProductSavedReviewActivity.onProductWithReviewsClick
 */
class SavedReviewActivity : AppCompatActivity(),
    SavedReviewAdapter.Actions, ConfirmDialog.Actions {
    private lateinit var binding: HomeActivityRecyclerviewBinding
    private lateinit var productCode: String
    private lateinit var productName: String

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var dialog: Dialog

    /**
     * if HomeViewModel is working on the db, it shows up ProgressDialog
     *
     * @see com.project.review.dialogs.ProgressDialog
     * @see dialog
     * @see HomeViewModel
     */
    private fun setOnProgressChange() {
        viewModel._progress.observe(this, {
            if (!it)
                dialog.show()
            else {
                dialog.dismiss()

            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.home_activity_recyclerview)

        dialog = ProgressDialog.progressDialog(this)
        productCode = intent.extras?.getString(Settings.CODE)!!
        productName = intent.extras?.getString(Settings.PRODUCT_NAME)!!

        setOnProgressChange()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.reviews) + " " + this.productName

        this.setUpRecyclerView()


    }

    private lateinit var recyclerViewAdapter: SavedReviewAdapter


    private fun setUpRecyclerView() {
        val recyclerView = binding.itemsFullRecyclerview
        val linearLinearManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLinearManager
        recyclerViewAdapter = SavedReviewAdapter(this)
        recyclerView.adapter = recyclerViewAdapter

        viewModel.getReviews(productCode).observe(this, {
            if (it.isEmpty())
                finish()

            recyclerViewAdapter.submitList(it)
        })

    }


    val listOfReviews = mutableSetOf<Review>()

    private var actionMode: ActionMode? = null

    /**
     * defines the ActionMode that appears when the user long-presses on a product
     */
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.delete_item_menu, menu)
            mode?.title = getString(R.string.delete_researches)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item!!.itemId) {
                R.id.delete -> {
                    Tools.showConfirmDialog(binding.root, supportFragmentManager)
                    false
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            recyclerViewAdapter.actionMode = false
            for (review in listOfReviews)
                review.isSelected = false

            recyclerViewAdapter.notifyDataSetChanged()
        }

    }

    /**
     * initializes the ActionMode
     */
    override fun reviewOnLongClick(review: Review) {
        if (actionMode != null) {
            return
        }
        listOfReviews.apply {
            clear()
            add(review.apply { isSelected = true })
        }
        actionMode = startSupportActionMode(actionModeCallback)
        recyclerViewAdapter.actionMode = true
        recyclerViewAdapter.notifyDataSetChanged()
    }

    /**
     * when the user clicks on a review, if the ActionMode is not active
       it calls ReviewDialogActivity
     *
     * if the ActionMode is active it puts the review on the list of reviews to be deleted from the database
     *
     * @see ReviewDialogActivity
     * @see listOfReviews
     * @see actionModeCallback
     */
    override fun reviewOnClick(review: Review, value: Boolean) {
        if (actionMode == null) {
            val intent = Intent(this, ReviewDialogActivity::class.java).apply {
                putExtra(Settings.REVIEW, Json.encodeToString(review))
                putExtra(Settings.CODE, productCode)
                putExtra(Settings.IS_SEARCHING, false)
            }
            startActivity(intent)
            return
        }

        review.isSelected = value

        if (value)
            listOfReviews.add(review)
        else
            listOfReviews.remove(review)

    }

    /**
     * if the user in the ConfirmDialog that appears confirms to continue, the reviews will be deleted
     */
    override fun onPositiveClick() {
        viewModel.deleteReviews(listOfReviews, productCode)
        actionMode?.finish()
    }

    override fun onNegativeClick() {

    }
}