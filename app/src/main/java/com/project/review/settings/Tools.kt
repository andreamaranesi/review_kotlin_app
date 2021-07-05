package com.project.review.settings

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.project.review.R
import com.project.review.databinding.ReviewItemBinding
import com.project.review.dialogs.ConfirmDialog

import com.project.review.models.Review
import com.project.review.view_models.ReviewViewModel

/**
 * contains static methods useful to multiple app components
 */
class Tools {
    companion object {

        /**
         * defines how the product image should be downloaded and displayed
         */
        fun setImage(
            imageView: ImageView,
            image: String,
            view: Context,
        ) {

            Glide.with(view).load(image).listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean,
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean,
                ): Boolean {
                    ObjectAnimator.ofFloat(imageView, "alpha", 1F).apply {
                        duration = Settings.IMAGE_ALPHA_DURATION
                    }.start()

                    return false
                }

            }).into(imageView)
        }

        /**
         * initialize a new search
         *
         * @see ReviewViewModel.onStart
         */
        fun searchReviews(
            context: ReviewViewModel,
            code: String,
            isSpecificCode: Boolean,
            max: Int = Settings.reviewResults,
            type: Int = 0,
            isAsin: Boolean = false,
        ) {

            context.onStart(type, max, code, isSpecificCode, !isSpecificCode, isAsin)

        }

        /**
         * resumes a search
         *
         * @see ReviewViewModel.onStart
         */
        fun findMoreReviews(
            context: ReviewViewModel,
            max: Int = Settings.reviewResults,
            type: Int = 1,
        ) {
            context.onStart(type, max)
        }


        fun getScreenWidthPx(context: Context): Double {
            val displayMetrics = context.resources.displayMetrics
            return (displayMetrics.widthPixels).toDouble()
        }

        fun getScreenHeightPx(context: Context): Double {
            val displayMetrics = context.resources.displayMetrics
            return (displayMetrics.heightPixels).toDouble()
        }

        /**
         * creates the disappearing effect on the title and body of a review in the RecyclerView that displays it
         *
         * @see com.project.review.ui.SavedReviewActivity
         * @see com.project.review.ui.Results
         */
        fun fadingEdges(binding: ReviewItemBinding, review: Review) {
            binding.reviewTitle.maxLength(Settings.maxTitleLength)
            binding.reviewBody.maxLength(Settings.maxBodyLength)

            binding.reviewTitle.text = review.marketplace.name + " - " + review.title
            if (review.title.length > Settings.maxTitleLength) {
                binding.reviewTitleFade.setFadeEdges(false, false, false, true)
                binding.reviewTitleFade.setFadeSizes(0, 0, 0, 50)
            }
            var body = review.body
            if (body.length > Settings.maxBodyLength)
                body = body.substring(0, Settings.maxBodyLength - 3) + "..."

            review.oldBody = review.body

            binding.body = body
            binding.executePendingBindings()
        }

        /**
         * set the icon to appear when the user saves or deletes a review from the database
         */
        fun setWishlistButton(view: View, review: Review, wishlistButton: FloatingActionButton) {
            val backgroundColor = Color.WHITE
            val onSelectedSrc = R.drawable.ic_wishlist_64
            val src = R.drawable.ic_outline_wishlist_64

            if (review.stored) {
                wishlistButton.backgroundTintList = ColorStateList.valueOf(backgroundColor)
                wishlistButton.setImageResource(onSelectedSrc)
            } else {
                wishlistButton.backgroundTintList = ColorStateList.valueOf(backgroundColor)
                wishlistButton.setImageResource(src)
            }
        }

        private fun highlight(): Array<String> {
            return arrayOf("<span style=\"background-color:#f3f402; color: #000;\">", "</span>")
        }

        /**
         * highlights in the body of a review the words contained in the list provided
         */
        fun highlightWordsOnReview(
            listOfWords: List<String>,
            it: Review,
        ) {
            val highlight = highlight()

            for (word in listOfWords) {
                it.body =
                    it.body.replace(
                        """($word)""".toRegex(RegexOption.IGNORE_CASE),
                        highlight[0] + "$1" + highlight[1]
                    )
            }
        }


        /**
         * removes any highlights in the body of the review
         */
        fun removeHighlight(it: Review, returnNewText: Boolean = false): Any {
            val highlight = highlight()
            var copy = it.body

            for (text in highlight) {
                if (!returnNewText)
                    it.body =
                        it.body.replace(text, "")
                else
                    copy = copy.replace(text, "")
            }
            return if (!returnNewText) Unit
            else
                copy

        }

        fun showConfirmDialog(root: View, supportFragmentManager: FragmentManager) {
            ConfirmDialog(root.context.getString(R.string.dialog_confirm), root =
            root).show(supportFragmentManager, "")
        }

    }

}

/**
 * definisce la lunghezza massima, in caratteri, che deve avere la TextView corrispondente
 */
fun TextView.maxLength(max: Int) {
    this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(max))
}
