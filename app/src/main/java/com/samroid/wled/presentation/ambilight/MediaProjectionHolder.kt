package com.samroid.wled.presentation.ambilight


import android.media.projection.MediaProjection

object MediaProjectionHolder {
    @Volatile
    var projection: MediaProjection? = null
}