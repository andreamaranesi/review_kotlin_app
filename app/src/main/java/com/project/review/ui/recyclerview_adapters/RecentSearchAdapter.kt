package com.project.review.ui.recyclerview_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.review.R
import com.project.review.databinding.RecentResearchItemBinding
import com.project.review.models.Product

/**
 * RecyclerView adapter showing recent searches
 *
 * @see com.project.review.ui.Home.setRecentResearches
 * @see com.project.review.ui.RecentResearchActivity.setUpRecyclerView
 */
class RecentSearchAdapter(
    val actions: Actions,
    var actionMode: Boolean = false,
) :
    ListAdapter<Product, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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
        fun onRecentResearchClick(
            product: Product,
            value: Boolean = false,
        )

        fun onLongRecentResearchClick(product: Product)

    }

    class ShowProduct(view: View) : RecyclerView.ViewHolder(view) {
        val binding=RecentResearchItemBinding.bind(view)
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
        this.setIsSelected(binding, product)
        this.setOnClick(binding, product)
        binding.imageUrl = product.imageUrl

    }

    /**
     * checks if the product has been selected
     */
    private fun setIsSelected(binding: RecentResearchItemBinding, product: Product) {
        binding.checkbox.setOnCheckedChangeListener(null)
        if (actionMode) {
            binding.selected.visibility = View.VISIBLE
            binding.checkbox.isChecked = product.isSelected

            val checkboxCallback = CompoundButton.OnCheckedChangeListener { p0, value ->
                actions.onRecentResearchClick(product, value = value)
            }

            binding.checkbox.setOnCheckedChangeListener(checkboxCallback)

        } else {
            binding.selected.visibility = View.GONE
        }

    }


    private fun setOnClick(binding: RecentResearchItemBinding, product: Product) {
        binding.productLayout.setOnClickListener {
            if (actionMode)
                binding.checkbox.isChecked = !product.isSelected
            else
                actions.onRecentResearchClick(product)
        }

        binding.productLayout.setOnLongClickListener {
            this.actions.onLongRecentResearchClick(product)
            true
        }
    }


}



