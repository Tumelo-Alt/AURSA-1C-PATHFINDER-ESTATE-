/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.injection

import android.content.Context
import androidx.room.Room
import com.rtbishop.look4sat.data.LocalDataSource
import com.rtbishop.look4sat.data.RemoteDataSource
import com.rtbishop.look4sat.data.DataRepository
import com.rtbishop.look4sat.framework.remote.SatelliteApi
import com.rtbishop.look4sat.domain.SatelliteRepo
import com.rtbishop.look4sat.domain.DataReporter
import com.rtbishop.look4sat.domain.predict.Predictor
import com.rtbishop.look4sat.framework.remote.RemoteSource
import com.rtbishop.look4sat.framework.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideLocalDataSource(@ApplicationContext context: Context): LocalDataSource {
        val database = Room.databaseBuilder(context, SatelliteDb::class.java, "SatelliteDb")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        return LocalSource(database.entriesDao(), database.sourcesDao(), database.transmittersDao())
    }

    @Provides
    @Singleton
    fun provideRemoteDataSource(): RemoteDataSource {
        val satelliteApi = Retrofit.Builder().baseUrl("https://localhost")
            .addConverterFactory(MoshiConverterFactory.create()).build()
            .create(SatelliteApi::class.java)
        return RemoteSource(satelliteApi)
    }

    @Provides
    @Singleton
    fun provideSatelliteRepo(
        localSource: LocalDataSource,
        remoteSource: RemoteDataSource,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): SatelliteRepo {
        return DataRepository(localSource, remoteSource, dispatcher)
    }

    @Provides
    @Singleton
    fun providePredictor(@DefaultDispatcher dispatcher: CoroutineDispatcher): Predictor {
        return Predictor(dispatcher)
    }

    @Provides
    @Singleton
    fun provideDataReporter(@IoDispatcher dispatcher: CoroutineDispatcher): DataReporter {
        return DataReporter(dispatcher)
    }
}
