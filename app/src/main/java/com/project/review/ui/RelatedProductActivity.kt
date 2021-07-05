package com.project.review.ui

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.project.review.R
import com.project.review.databinding.HomeActivityRecyclerviewBinding
import com.project.review.models.RelatedProduct
import com.project.review.view_models.HomeViewModel
import com.project.review.settings.Settings
import com.project.review.ui.recyclerview_adapters.FilterWordAdapter
import com.project.review.ui.recyclerview_adapters.RelatedProductAdapter
import java.util.concurrent.locks.ReentrantLock

/**
 * Activity that shows the list of all the suggested products based on the previous researches
 *
 * @see com.project.review.ui.Home.setRelatedProducts
 */
class RelatedProductActivity : AppCompatActivity(), RelatedProductAdapter.Actions {
    private lateinit var binding: HomeActivityRecyclerviewBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.home_activity_recyclerview)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.setUpRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }
    }

    var code: String = ""

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(Settings.CODE, code)
        })
        finish()
    }



    private lateinit var recyclerViewAdapter: RelatedProductAdapter

    private fun setUpRecyclerView() {
        val recyclerView = binding.itemsFullRecyclerview
        recyclerViewAdapter = RelatedProductAdapter(this)

        val manager = GridLayoutManager(this, 2)

        recyclerView.layoutManager = manager

        recyclerView.adapter = recyclerViewAdapter

        viewModel.allRelatedProducts.observe(this, {
            recyclerViewAdapter.submitList(it)
        })

    }


    override fun onRelatedProductClick(product: RelatedProduct) {
        this.code = product.code
        this.onBackPressed()
    }

}