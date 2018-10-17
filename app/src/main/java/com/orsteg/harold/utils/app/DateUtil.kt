package com.orsteg.harold.utils.app

import java.util.*

/**
 * Created by goodhope on 10/17/18.
 */
class DateUtil {
    companion object {
        fun isSameDay(dayOne: Calendar, dayTwo: Calendar): Boolean {
            return dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR) && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR)
        }
        fun today(): Calendar {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            return today
        }
    }
}