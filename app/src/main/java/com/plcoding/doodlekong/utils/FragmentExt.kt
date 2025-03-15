package com.plcoding.doodlekong.utils

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar


fun Fragment.snackBar(text: String) {
    Snackbar.make(
        requireView(),
        text,
        Snackbar.LENGTH_LONG
    ).show()
}



fun Fragment.snackBar(@StringRes resId: Int) {
    Snackbar.make(
        requireView(),
        resId,
        Snackbar.LENGTH_LONG
    ).show()
}