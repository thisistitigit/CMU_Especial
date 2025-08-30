package com.example.reviewapp.utils

import android.util.Log

const val APP_TAG = "ReviewAppDebug"

inline fun logD(msg: String) = Log.d(APP_TAG, msg)
inline fun logI(msg: String) = Log.i(APP_TAG, msg)
inline fun logW(msg: String, t: Throwable? = null) = Log.w(APP_TAG, msg, t)
inline fun logE(msg: String, t: Throwable? = null) = Log.e(APP_TAG, msg, t)
