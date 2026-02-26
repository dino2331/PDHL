package com.pdrehab.handwritinglab.di

import com.pdrehab.handwritinglab.core.PdhlConstants
import com.pdrehab.handwritinglab.data.storage.AdminSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideAdminSession(): AdminSession =
        AdminSession(pin = PdhlConstants.ADMIN_PIN_DEFAULT)
}