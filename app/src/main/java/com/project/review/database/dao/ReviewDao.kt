package com.project.review.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.project.review.models.Review

@Dao
interface ReviewDao {

    @Insert
    fun insert(vararg reviews: Review)

    @Delete
    fun delete(review: Review)

    @Query("SELECT r.* FROM Review r WHERE r.product_code=:code")
    fun getReviews(code: String): LiveData<MutableList<Review>>

    @Query("SELECT r.* FROM Review r WHERE r.product_code=:code")
    fun suspendGetReviews(code: String): MutableList<Review>

    @Query("DELETE FROM Review WHERE product_code=:code")
    fun deleteByCode(code: String): Int

}