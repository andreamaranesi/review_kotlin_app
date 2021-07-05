package com.project.review.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.project.review.settings.Marketplace


@Entity(tableName = "Product")
data class Product(
    val name: String,
    @PrimaryKey var code: String,
    var feedback: Float,
    val url: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val marketplace: MutableList<Marketplace> = mutableListOf(),
    val date: Long,
) : ModelAdapter() {

    @Ignore
    var relatedProducts: MutableList<RelatedProduct> = mutableListOf()
}