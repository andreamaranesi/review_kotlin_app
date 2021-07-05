package com.project.review.ui.recyclerview_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.review.R
import com.project.review.databinding.ReviewItemBinding
import com.project.review.models.Review
import com.project.review.settings.Tools

/**
 * RecyclerView adapter showing saved products associated with a specific product
 *
 * @see com.project.review.ui.SavedReviewActivity.setUpRecyclerView
 */
class SavedReviewAdapter(
    val actions: Actions,
    var actionMode: Boolean = false,
) :
    ListAdapter<Review, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem == newItem
            }
        }

    }

    interface Actions {
        fun reviewOnClick(review: Review, value: Boolean = false)
        fun reviewOnLongClick(review: Review)
    }

    class ShowReview(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ReviewItemBinding.bind(view)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.review_item, parent, false)
        return ShowReview(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        holder as ShowReview
        val review: Review = getItem(position)
        val binding = holder.binding
        binding.reviewStars.rating = review.stars
        binding.wishlistButton.visibility = View.GONE

        val params = binding.reviewConstraintLayout.layoutParams as ConstraintLayout.LayoutParams
        params.setMargins(0, 0, 0, 0) //substitute parameters for left, top, right, bottom
        binding.reviewConstraintLayout.layoutParams = params

        Tools.fadingEdges(binding, review)
        this.setIsSelected(binding, review)
        this.setOnClick(binding, review)
    }

    /**
     * checks if the review has been selected
     */
    private fun setIsSelected(binding: ReviewItemBinding, review: Review) {
        binding.checkbox.setOnCheckedChangeListener(null)
        if (actionMode) {
            binding.selected.visibility = View.VISIBLE
            binding.checkbox.isChecked = review.isSelected
            val checkboxCallback = CompoundButton.OnCheckedChangeListener { p0, value ->
                actions.reviewOnClick(review, value)
            }

            binding.checkbox.setOnCheckedChangeListener(checkboxCallback)

            binding.checkbox.setOnCheckedChangeListener(checkboxCallback)
        } else {
            binding.selected.visibility = View.GONE
        }

    }


    private fun setOnClick(
        binding: ReviewItemBinding,
        review: Review,
    ) {
        binding.reviewLayout.setOnClickListener {
            if (actionMode) {
                binding.checkbox.isChecked = !review.isSelected
            } else
                this.actions.reviewOnClick(review)
        }

        binding.reviewLayout.setOnLongClickListener {
            this.actions.reviewOnLongClick(review)
            true
        }
    }


}



