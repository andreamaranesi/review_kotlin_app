package com.project.review.ui.recyclerview_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.project.review.R
import com.project.review.databinding.RecentResearchItemBinding
import com.project.review.models.Product

/**
 * RecyclerView adapter showing products with saved reviews
 *
 * @see com.project.review.ui.Home.setProductWithSavedReviews
 * @see com.project.review.ui.ProductSavedReviewActivity.setUpRecyclerView
 */
class ProductSavedReviewAdapter(
    val actions: Actions
) : ListAdapter<Product, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem == newItem
            }
        }

    }

    interface Actions {
        fun onProductWithReviewsClick(product: Product)
    }

    class ShowProduct(view: View) : RecyclerView.ViewHolder(view) {
        val binding: RecentResearchItemBinding = RecentResearchItemBinding.bind(view)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recent_research_item, parent, false)
        return ShowProduct(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val view = holder as ShowProduct
        val binding = view.binding
        val product = getItem(position)
        binding.productTitle.text = product.name
        binding.reviewStars.rating = product.feedback
        this.setOnClick(binding, product)
        binding.imageUrl = product.imageUrl
    }


    private fun setOnClick(binding: RecentResearchItemBinding, product: Product) {
        binding.productLayout.setOnClickListener {
            this.actions.onProductWithReviewsClick(product)
        }
    }


}



