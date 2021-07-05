package com.project.review.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.project.review.models.Product

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg product: Product)

    @Delete
    fun delete(vararg products: Product)

    @Query("SELECT * FROM Product ORDER BY date DESC")
    fun getResearches(): LiveData<MutableList<Product>>

    @Query("SELECT * FROM Product ORDER BY date DESC LIMIT :limit")
    fun getResearches(limit: Int): LiveData<MutableList<Product>>

    @Query("SELECT p.* FROM Product p WHERE (SELECT COUNT(*) FROM Review r WHERE r.product_code=p.code) ORDER BY date DESC")
    fun getProductWithReviews(): LiveData<MutableList<Product>>

    @Query("SELECT p.* FROM Product p WHERE (SELECT COUNT(*) FROM Review r WHERE r.product_code=p.code) ORDER BY date DESC LIMIT :limit")
    fun getProductWithReviews(limit: Int): LiveData<MutableList<Product>>

    @Query("SELECT * FROM Product")
    fun getAll(): MutableList<Product>

    @Query("SELECT * FROM Product WHERE code=:code")
    fun getProductByCode(code: String): Product

}