package com.project.review.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.project.review.settings.Marketplace
import com.project.review.settings.Tools
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "Review", primaryKeys = ["id", "marketplace"])
@Serializable
data class Review(
    val id: String, val title: String, var body: String, val date: String, val stars: Float,
    val marketplace: Marketplace, var imageUrl: String = "",
) : ModelAdapter() {

    /**
     * @see Tools.removeHighlight
     */
    @Ignore
    var initBody = Tools.removeHighlight(this, returnNewText = true) as String

    /**
     * stores the last value of body
     * is used to create the highlight effect in Results fragment
     *
     * @see com.project.review.ui.recyclerview_adapters.ReviewRecyclerViewAdapter.DIFF_CALLBACK
     * @see com.project.review.view_models.ReviewViewModel.applyFilters
     */
    @Ignore
    @Transient
    var oldBody: String = ""

    @ColumnInfo(name = "product_code")
    var productCode: String? = null

    /**
     * establishes whether the review is saved in the local database or not
     */
    @Ignore
    var stored: Boolean = false

    /**
     * determines whether the current review acts as a preloader
     */
    @Ignore
    @Transient
    var preloader: Boolean = false

    /**
     * determines whether the current review acts as an initial preloader
     */
    @Ignore
    @Transient
    var firstPreloader: Boolean = false

    /**
     * checks if the review is contained within a list
     */
    fun wasStored(list: MutableList<Review>) {
        if (list.isNotEmpty() && list.any { it.marketplace == this.marketplace && it.id == this.id })
            this.stored = true
    }

    companion object {
        fun preloaderItem(): Review {
            return Review("", "", "", "", 0F, Marketplace.AMAZON, "").apply {
                preloader = true
            }
        }
    }


}
