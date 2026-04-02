package com.example.englishpractice.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.englishpractice.data.local.dao.ActivityDao
import com.example.englishpractice.data.local.dao.AttemptDao
import com.example.englishpractice.data.local.dao.MistakeDao
import com.example.englishpractice.data.local.dao.ReviewItemDao
import com.example.englishpractice.data.local.entity.ActivityEntity
import com.example.englishpractice.data.local.entity.AttemptEntity
import com.example.englishpractice.data.local.entity.MistakeEntity
import com.example.englishpractice.data.local.entity.ReviewItemEntity
import com.example.englishpractice.data.local.entity.UnitEntity

@Database(
    entities = [
        UnitEntity::class,
        ActivityEntity::class,
        AttemptEntity::class,
        MistakeEntity::class,
        ReviewItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun attemptDao(): AttemptDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun reviewItemDao(): ReviewItemDao
}
