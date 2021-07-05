package com.project.review.ui

import android.app.Activity
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
import com.project.review.models.Product
import com.project.review.view_models.HomeViewModel
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import com.project.review.ui.recyclerview_adapters.RecentSearchAdapter


/**
 * Activity showing the list of recent researches
 *
 * @see com.project.review.ui.Home.setRecentResearches
 */
class RecentResearchActivity : AppCompatActivity(), RecentSearchAdapter.Actions,
    ConfirmDialog.Actions {
    private lateinit var binding: HomeActivityRecyclerviewBinding
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

        dialog = ProgressDialog.progressDialog(this)

        setOnProgressChange()

        binding = DataBindingUtil.setContentView(this, R.layout.home_activity_recyclerview)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.setUpRecyclerView()

    }

    var code: String = ""


    private lateinit var recyclerViewAdapter: RecentSearchAdapter


    private fun setUpRecyclerView() {
        val recyclerView = binding.itemsFullRecyclerview
        val linearLinearManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLinearManager
        recyclerViewAdapter = RecentSearchAdapter(this)
        recyclerView.adapter = recyclerViewAdapter


        viewModel.getResearches().observe(this, {
            recyclerViewAdapter.submitList(it)
        })
    }

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

            for (product in listOfProducts)
                product.isSelected = false

            recyclerViewAdapter.notifyDataSetChanged()
        }

    }

    val listOfProducts = mutableSetOf<Product>()

    /**
     * when the user clicks on a search if the ActionMode is not active
       Product reviews are searched again
     *
     * if the ActionMode is active the element is put on the list of products to be deleted,
       along with all saved reviews and related products associated with it
     *
     * @see listOfProducts
     * @see actionModeCallback
     */
    override fun onRecentResearchClick(product: Product, value: Boolean) {
        if (actionMode == null) {
            setResult(Activity.RESULT_OK, Intent().apply { putExtra(Settings.CODE, product.code) })
            finish()
        }


        product.isSelected = value
        if (value)
            listOfProducts.add(product)
        else
            listOfProducts.remove(product)


    }


    /**
     * inizializza l'ActionMode
     */
    override fun onLongRecentResearchClick(product: Product) {
        if (actionMode != null) {
            return
        }
        listOfProducts.apply {
            clear()
            add(product.apply { isSelected = true })
        }
        actionMode = startSupportActionMode(actionModeCallback)
        recyclerViewAdapter.actionMode = true
        recyclerViewAdapter.notifyDataSetChanged()
    }


    /**
     * se l'utente nel ConfirmDialog che compare conferma di proseguire, vengono eliminati i prodotti
     */
    override fun onPositiveClick() {
        viewModel.deleteResearches(listOfProducts)
        actionMode?.finish()
    }

    override fun onNegativeClick() {
        //NON FA NULLA
    }
}