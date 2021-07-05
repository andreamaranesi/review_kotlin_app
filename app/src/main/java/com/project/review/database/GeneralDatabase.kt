package com.project.review.database

import android.content.Context
import androidx.room.*
import androidx.room.Database
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.project.review.database.dao.*
import com.project.review.models.*
import com.project.review.settings.Marketplace
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converter {

    @TypeConverter
    fun fromMarketplace(marketplace: Marketplace): String {
        return marketplace.name
    }

    @TypeConverter
    fun toMarketplace(name: String): Marketplace {
        return Marketplace.valueOf(name)
    }

    @TypeConverter
    fun toMutableListOfMarketplace(string: String): MutableList<Marketplace> {
        return Json.decodeFromString(string)
    }

    @TypeConverter
    fun fromMutableListOfMarketplace(list: MutableList<Marketplace>): String {
        return Json.encodeToString(list)
    }

}

/**
 * instantiates the database
 */
@Database(
    entities = [
        Review::class, Product::class, RelatedProduct::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class GeneralDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun reviewDao(): ReviewDao
    abstract fun relatedProductDao(): RelatedProductDao

    companion object {
        @Volatile
        private var INSTANCE: GeneralDatabase? = null

        @Synchronized
        fun getInstance(context: Context): GeneralDatabase {
            if (this.INSTANCE != null)
                return INSTANCE!!

            return Room.databaseBuilder(
                context.applicationContext,
                GeneralDatabase::class.java, "review"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        TODO("Not yet implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("Not yet implemented")
    }

    override fun clearAllTables() {
        TODO("Not yet implemented")
    }


}