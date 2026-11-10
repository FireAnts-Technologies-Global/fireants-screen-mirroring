package com.hyper.castwave.ui.component.iptv.util

object CalendarUtil {

    const val TIME_ONE_SECOND = 1000L

    fun getDistanceTimeFromTo(from: Long, to: Long): Triple<Int, Int, Int> {
        val distance = to - from
        if (distance <= 0) return Triple(0, 0, 0)

        val totalSeconds = distance / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()

        return Triple(hours, minutes, seconds)
    }

}
