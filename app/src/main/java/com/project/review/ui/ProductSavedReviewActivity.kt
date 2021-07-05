package com.project.review.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.review.R
import com.project.review.databinding.HomeActivityRecyclerviewBinding
import com.project.review.models.Product
import com.project.review.view_models.HomeViewModel
import com.project.review.settings.Settings
import com.project.review.ui.recyclerview_adapters.ProductSavedReviewAdapter


/**
 * Activity showing the list of products that contain saved reviews
 *
 * @see com.project.review.ui.Home.setProductWithSavedReviews
 */
class ProductSavedReviewActivity : AppCompatActivity(), ProductSavedReviewAdapter.Actions {
    private lateinit var binding: HomeActivityRecyclerviewBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.home_activity_recyclerview)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        this.setUpRecyclerView()

    }


    private lateinit var recyclerViewAdapter: ProductSavedReviewAdapter


    private fun setUpRecyclerView() {
        val recyclerView = binding.itemsFullRecyclerview
        val linearLinearManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLinearManager
        recyclerViewAdapter = ProductSavedReviewAdapter(this)
        recyclerView.adapter = recyclerViewAdapter

        viewModel.getProductWithReviews().observe(this, {
            if (it.isEmpty())
                finish()

            recyclerViewAdapter.submitList(it)
        })

    }

    override fun onProductWithReviewsClick(product: Product) {
       startActivity(Intent(this, SavedReviewActivity::class.java).apply {
            putExtra(Settings.CODE, product.code)
            putExtra(Settings.PRODUCT_NAME, product.name)
        })

    }
}