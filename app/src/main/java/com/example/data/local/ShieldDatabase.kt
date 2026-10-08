package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ShieldDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.RewardEventEntity
import com.example.data.local.entity.RiskEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity

@Database(
    entities = [
        UserEntity::class,
        RewardConfigEntity::class,
        RewardEventEntity::class,
        CoinTransactionEntity::class,
        WithdrawalEntity::class,
        AuditLogEntity::class,
        RiskEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShieldDatabase : RoomDatabase() {
    abstract fun shieldDao(): ShieldDao

    companion object {
        @Volatile
        private var INSTANCE: ShieldDatabase? = null

        fun getInstance(context: Context): ShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShieldDatabase::class.java,
                    "project_shield.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
