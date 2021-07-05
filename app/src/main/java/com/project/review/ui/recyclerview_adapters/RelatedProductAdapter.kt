package com.project.review.ui.recyclerview_adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.review.R
import com.project.review.databinding.ProductLabelItemBinding
import com.project.review.databinding.RelatedProductItemBinding
import com.project.review.models.Product
import com.project.review.models.RelatedProduct

/**
 * RecyclerView adapter showing related products
 *
 * @see com.project.review.ui.Home.setRelatedProducts
 * @see com.project.review.ui.RelatedProductActivity.setUpRecyclerView
 */
class RelatedProductAdapter(
    val actions: Actions,
) :
    ListAdapter<Any, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                if (oldItem !is RelatedProduct)
                    oldItem as Product

                if (newItem !is RelatedProduct)
                    newItem as Product

                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return this.areItemsTheSame(oldItem, newItem)
            }
        }
    }

    enum class ViewType {
        HEADER, RELATED_PRODUCT
    }

    interface Actions {
        fun onRelatedProductClick(product: RelatedProduct)
    }

    class ShowProduct(view: View) : RecyclerView.ViewHolder(view) {
        val binding = RelatedProductItemBinding.bind(view)
    }

    class ShowProductLabel(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ProductLabelItemBinding.bind(view)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return if (viewType == ViewType.RELATED_PRODUCT.ordinal) {
            val view: View =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.related_product_item, parent, false)
            ShowProduct(view)
        } else {
            val view: View =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.product_label_item, parent, false)
            ShowProductLabel(view)
        }
    }


    override fun getItemViewType(position: Int): Int {
        if (getItem(position) is Product)
            return ViewType.HEADER.ordinal

        return ViewType.RELATED_PRODUCT.ordinal
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {

        if (holder.itemViewType == ViewType.RELATED_PRODUCT.ordinal) {

            val view = holder as ShowProduct
            val binding = view.binding
            val product =
                getItem(position) as RelatedProduct
            binding.productTitle.text = product.name
            binding.reviewStars.rating = product.feedback
            this.setOnClick(binding, product)
            binding.imageUrl = product.imageUrl

        } else {
            val view = holder as ShowProductLabel
            val product = getItem(position) as Product
            view.binding.productTitle.text =
                holder.itemView.context.getString(R.string.from) + " " + product.name
        }


    }

    /**
     * click action on the suggested product
     */
    private fun setOnClick(binding: RelatedProductItemBinding, relatedProduct: RelatedProduct) {
        binding.productLayout.setOnClickListener {
            this.actions.onRelatedProductClick(relatedProduct)
        }
    }


}



