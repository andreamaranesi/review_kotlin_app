package com.project.review.ui.recyclerview_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cooltechworks.views.shimmer.ShimmerRecyclerView
import com.project.review.R
import com.project.review.databinding.ReviewItemBinding
import com.project.review.models.Review
import com.project.review.settings.Tools


/**
 * adapter of the RecyclerView showing the filtered list of reviews
 *
 * @see com.project.review.ui.Results.initRecyclerView
 */
class ReviewRecyclerViewAdapter(
    val actions: Actions,
) :
    ListAdapter<Review, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    enum class ViewType {
        PRELOADER, REVIEW
    }


    interface Actions {
        fun reviewOnClick(review: Review, position: Int)
        fun storeReview(review: Review, position: Int)
    }

    class ShowReview(view: View) : RecyclerView.ViewHolder(view) {
        val binding: ReviewItemBinding = ReviewItemBinding.bind(view)
    }

    class ShowPreloader(view: View) : RecyclerView.ViewHolder(view) {
        val shimmerRecyclerView: ShimmerRecyclerView = view.findViewById(R.id.shimmer_recycler_view)
    }


    override fun getItemViewType(position: Int): Int {
        if (getItem(position).preloader)
            return ViewType.PRELOADER.ordinal
        return ViewType.REVIEW.ordinal
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {

        return if (viewType == ViewType.PRELOADER.ordinal) {
            val view: View =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.review_item_preloader, parent, false)
            ShowPreloader(view)
        } else {
            val view: View =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.review_item, parent, false)
            ShowReview(view)
        }

    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val review: Review = getItem(position)

        if (holder.itemViewType == ViewType.PRELOADER.ordinal) {
            holder as ShowPreloader
            holder.shimmerRecyclerView.showShimmerAdapter()
        } else {
            holder as ShowReview
            val binding = holder.binding
            binding.reviewStars.rating = review.stars
            Tools.setWishlistButton(holder.itemView, review, binding.wishlistButton)

            binding.wishlistButton.setOnClickListener {
                actions.storeReview(review, position)
            }

            Tools.fadingEdges(binding, review)
            setOnClick(actions, binding, review, position)

        }

    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem == newItem && !oldItem.firstPreloader
            }

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem.oldBody == newItem.body
            }
        }


        fun setOnClick(
            actions: Actions,
            binding: ReviewItemBinding,
            review: Review,
            position: Int,
        ) {
            binding.reviewLayout.setOnClickListener {
                actions.reviewOnClick(review, position)
            }
        }

    }


}

