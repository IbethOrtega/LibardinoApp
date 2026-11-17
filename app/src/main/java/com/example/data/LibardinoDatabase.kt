package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GarmentDao {
    @Query("SELECT * FROM garments ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<GarmentEntity>>

    @Query("SELECT * FROM garments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GarmentEntity?

    @Query("SELECT * FROM garments WHERE LOWER(barcode) = LOWER(:barcode) LIMIT 1")
    suspend fun getByBarcode(barcode: String): GarmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(garment: GarmentEntity): Long

    @Update
    suspend fun update(garment: GarmentEntity)

    @Query("DELETE FROM garments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sale_orders ORDER BY createdAt DESC, id DESC")
    fun observeAllOrders(): Flow<List<SaleOrderWithItems>>

    @Transaction
    @Query("SELECT * FROM sale_orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItems(orderId: Long): SaleOrderWithItems?

    @Insert
    suspend fun insertOrder(order: SaleOrderEntity): Long

    @Update
    suspend fun updateOrder(order: SaleOrderEntity)

    @Insert
    suspend fun insertLineItems(items: List<SaleLineItemEntity>)

    @Query("DELETE FROM sale_line_items WHERE orderId = :orderId")
    suspend fun deleteLineItemsByOrderId(orderId: Long)
}

@Database(
    entities = [GarmentEntity::class, SaleOrderEntity::class, SaleLineItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LibardinoDatabase : RoomDatabase() {
    abstract fun garmentDao(): GarmentDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: LibardinoDatabase? = null

        fun getInstance(context: Context): LibardinoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LibardinoDatabase::class.java,
                    "libardino_store.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
