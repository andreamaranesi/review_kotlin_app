package com.project.review.models

import androidx.room.Ignore
import kotlinx.serialization.Serializable


/**
 * parent of Product and Review
 * is used to understand if the current object has been selected by the user
 */
@Serializable
open class ModelAdapter(
    @Ignore
    var isSelected: Boolean = false,
)