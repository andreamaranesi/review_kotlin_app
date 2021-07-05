package com.project.review.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.review.models.RelatedProduct


@Dao
interface RelatedProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg products: RelatedProduct)

    @Query("SELECT r.* FROM RelatedProduct r JOIN Product p ON p.code = r.parent_code ORDER BY p.date DESC")
    fun getProducts(): LiveData<MutableList<RelatedProduct>>

    @Query("SELECT r.* FROM RelatedProduct r JOIN Product p ON p.code = r.parent_code ORDER BY p.date DESC LIMIT :limit")
    fun getProducts(limit: Int): LiveData<MutableList<RelatedProduct>>

    @Query("SELECT r.* FROM RelatedProduct r JOIN Product p ON p.code = r.parent_code ORDER BY p.date DESC LIMIT :limit")
    fun suspendGetRelatedProducts(limit: Int): MutableList<RelatedProduct>

    @Query("SELECT * FROM RelatedProduct WHERE is_scheduled")
    fun suspendGetScheduledProducts(): MutableList<RelatedProduct>

    @Query("DELETE FROM RelatedProduct WHERE parent_code=:code")
    fun deleteByCode(code: String): Int

}