package com.example.brain.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.brain.model.BrainAuditLog
import com.example.brain.model.BrainUserSettings
import com.example.brain.model.MemoryItem
import com.example.brain.model.MemoryRelation

@Database(
    entities = [
        MemoryItem::class,
        MemoryRelation::class,
        BrainAuditLog::class,
        BrainUserSettings::class,
        com.example.brain.model.ContextNode::class,
        com.example.brain.model.ContextEdge::class,
        com.example.brain.model.SkillEntity::class,
        com.example.brain.model.SkillExecutionRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BrainDatabase : RoomDatabase() {

    abstract fun brainDao(): BrainDao

    companion object {
        @Volatile
        private var INSTANCE: BrainDatabase? = null

        fun getInstance(context: Context): BrainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrainDatabase::class.java,
                    "maya_brain.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
