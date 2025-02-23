package com.plcoding.doodlekong.utils

sealed class Resource<T>(val data: T? = null, val message: String? = null) {

    data class Success<T>(val success: T? = null) : Resource<T>(success)

    data class Error<T>(val errorMessage: String? = null, val error: T? = null) :
        Resource<T>(error, errorMessage)
}