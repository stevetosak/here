package net.tosak.here.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.tosak.here.shared.storage.DmMessageDao
import net.tosak.here.shared.storage.HereDatabase
import net.tosak.here.shared.storage.PostDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HereDatabase =
        Room.databaseBuilder(context, HereDatabase::class.java, "here.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePostDao(db: HereDatabase): PostDao = db.postDao()

    @Provides
    fun provideDmMessageDao(db: HereDatabase): DmMessageDao = db.dmMessageDao()
}
