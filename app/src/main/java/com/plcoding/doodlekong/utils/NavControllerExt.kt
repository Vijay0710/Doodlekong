package com.plcoding.doodlekong.utils

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator


fun NavController.navigateSafely(
    @IdRes resId: Int,
    args: Bundle?,
    navOptions: NavOptions? = null,
    navExtras: Navigator.Extras? = null
) {
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)

    if(action != null && currentDestination?.id != action.destinationId) {
        navigate(
            resId,
            args,
            navOptions,
            navExtras
        )
    }
}