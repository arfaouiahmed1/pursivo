package io.github.arfaouiahmed1.pursivo.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.arfaouiahmed1.pursivo.core.data.BackupService
import io.github.arfaouiahmed1.pursivo.core.data.GitHubUpdateService
import io.github.arfaouiahmed1.pursivo.core.data.UpdateService
import io.github.arfaouiahmed1.pursivo.core.data.ZipBackupService
import io.github.arfaouiahmed1.pursivo.core.data.local.PursivoDao
import io.github.arfaouiahmed1.pursivo.core.data.local.PursivoDatabase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PursivoDatabase =
        Room.databaseBuilder(context, PursivoDatabase::class.java, "pursivo.db").build()

    @Provides fun provideDao(database: PursivoDatabase): PursivoDao = database.dao()

    @Provides @Singleton
    fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {
    @Binds abstract fun bindBackup(implementation: ZipBackupService): BackupService
    @Binds abstract fun bindUpdater(implementation: GitHubUpdateService): UpdateService
}
