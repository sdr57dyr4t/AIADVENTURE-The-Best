package com.metalfish.aiadventure.di

import com.metalfish.aiadventure.domain.image.ImageEngine
import com.metalfish.aiadventure.domain.image.YandexArtImageEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleImages {

    @Binds
    @Singleton
    abstract fun bindImageEngine(impl: YandexArtImageEngine): ImageEngine
}
