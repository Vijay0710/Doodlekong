package com.plcoding.doodlekong.di

import android.content.Context
import com.google.gson.Gson
import com.plcoding.doodlekong.data.remote.api.SetUpApi
import com.plcoding.doodlekong.repository.DefaultSetUpRepository
import com.plcoding.doodlekong.repository.SetUpRepository
import com.plcoding.doodlekong.utils.Constants
import com.plcoding.doodlekong.utils.Constants.HTTP_BASE_URL
import com.plcoding.doodlekong.utils.Constants.HTTP_BASE_URL_LOCALHOST
import com.plcoding.doodlekong.utils.Constants.USER_LOCALHOST
import com.plcoding.doodlekong.utils.DispatcherProvider
import com.plcoding.doodlekong.utils.clientId
import com.plcoding.doodlekong.utils.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(clientId: String): OkHttpClient {
        return OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.newBuilder()
                    .addQueryParameter("client_id", clientId)
                    .build()

                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()

                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    @Singleton
    @Provides
    fun providesClientId(@ApplicationContext context: Context): String = runBlocking {
        context.dataStore.clientId()
    }

    @Singleton
    @Provides
    fun providesGsonInstance(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun providesApplicationContext(@ApplicationContext context: Context) = context

    @Singleton
    @Provides
    fun providesDispatcherProvider(): DispatcherProvider {
        return object : DispatcherProvider {
            override val main: CoroutineDispatcher
                get() = Dispatchers.Main
            override val io: CoroutineDispatcher
                get() = Dispatchers.IO
            override val default: CoroutineDispatcher
                get() = Dispatchers.Default
        }
    }

    @Singleton
    @Provides
    fun provideSetUpRepository(
        setUpApi: SetUpApi,
        @ApplicationContext context: Context
    ): SetUpRepository = DefaultSetUpRepository(setUpApi, context)

    @Singleton
    @Provides
    fun providesSetUpApi(okHttpClient: OkHttpClient): SetUpApi {
        return Retrofit.Builder()
            .baseUrl(if (USER_LOCALHOST) HTTP_BASE_URL_LOCALHOST else HTTP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetUpApi::class.java)
    }
}