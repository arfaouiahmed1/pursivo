package io.github.arfaouiahmed1.pursivo.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StageEntity::class,
        ApplicationEntity::class,
        EventEntity::class,
        TaskEntity::class,
        ProfileEntity::class,
        DocumentEntity::class,
        ProviderConfigEntity::class,
        AiInsightEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PursivoDatabase : RoomDatabase() {
    abstract fun dao(): PursivoDao
}
