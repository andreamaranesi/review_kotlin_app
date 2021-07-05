package com.project.review.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.project.review.settings.Marketplace

@Entity(tableName = "RelatedProduct")
data class RelatedProduct(
    val name: String,
    @PrimaryKey var code: String,
    var feedback: Float,
    val url: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val marketplace: Marketplace,
) {

    /**
     * it is used to understand if the product has been notified to the user previously
     */
    @ColumnInfo(name = "is_scheduled")
    var scheduledForNotification: Boolean = false

    @ColumnInfo(name = "parent_code")
    var parentCode: String? = null

    @Ignore
    var parent: Product? = null

}

