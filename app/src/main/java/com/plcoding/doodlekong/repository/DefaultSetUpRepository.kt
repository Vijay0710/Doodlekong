package com.plcoding.doodlekong.repository

import android.content.Context
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.data.remote.api.SetUpApi
import com.plcoding.doodlekong.data.remote.ws.Room
import com.plcoding.doodlekong.utils.Resource
import com.plcoding.doodlekong.utils.isNetworkConnectionAvailable
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject

class DefaultSetUpRepository @Inject constructor(
    private val setUpApi: SetUpApi,
    private val context: Context
) : SetUpRepository {
    override suspend fun createRoom(room: Room): Resource<Unit> {
        if (!context.isNetworkConnectionAvailable()) {
            return Resource.Error(
                errorMessage = context.getString(R.string.error_internet_turned_off)
            )
        }

        val response = try {
            setUpApi.createRoom(room)
        } catch (e: HttpException) {
            return Resource.Error(context.getString(R.string.error_http))
        } catch (e: IOException) {
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        return if (response.isSuccessful && response.body()?.successful == true) {
            Resource.Success(Unit)
        } else if (response.body()?.successful == false) {
            Resource.Error(response.body()!!.message!!)
        } else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }

    override suspend fun getRooms(searchQuery: String): Resource<List<Room>> {
        if (!context.isNetworkConnectionAvailable()) {
            return Resource.Error(
                errorMessage = context.getString(R.string.error_internet_turned_off)
            )
        }

        val response = try {
            setUpApi.getRooms(searchQuery)
        } catch (e: HttpException) {
            return Resource.Error(context.getString(R.string.error_http))
        } catch (e: IOException) {
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        return if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        } else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }

    override suspend fun joinRoom(username: String, roomName: String): Resource<Unit> {
        if (!context.isNetworkConnectionAvailable()) {
            return Resource.Error(
                errorMessage = context.getString(R.string.error_internet_turned_off)
            )
        }

        val response = try {
            setUpApi.joinRoom(username, roomName)
        } catch (e: HttpException) {
            return Resource.Error(context.getString(R.string.error_http))
        } catch (e: IOException) {
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        return if (response.isSuccessful && response.body()?.successful == true) {
            Resource.Success(Unit)
        } else if (response.body()?.successful == false) {
            Resource.Error(response.body()!!.message!!)
        } else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }
}