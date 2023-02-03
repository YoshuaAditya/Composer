package com.example.composer.views

class DateTimeText {
    companion object {
        private const val SECOND_MILLIS = 1000L
        private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
        private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS = 24 * HOUR_MILLIS
        private const val WEEK_MILLIS = 7 * DAY_MILLIS
        private const val MONTH_MILLIS = 30 * DAY_MILLIS
        private const val YEAR_MILLIS = 12 * MONTH_MILLIS
        fun getTimeAgo(currentTime: Long): String {
            var time = currentTime
            if (time < 1000000000000L) {
                // if timestamp given in seconds, convert to millis
                time *= 1000
            }
            val now = System.currentTimeMillis()
            var diff = now - time
            return if (diff > 0) {
                if (diff < MINUTE_MILLIS) {
                    "just now"
                } else if (diff < HOUR_MILLIS) {
                    diff/= MINUTE_MILLIS
                    val minutes= if (diff==1L) "minute" else "minutes"
                    "$diff $minutes ago"
                } else if (diff < 24 * HOUR_MILLIS) {
                    diff/= HOUR_MILLIS
                    val hours= if (diff==1L) "hour" else "hours"
                    "$diff $hours ago"
                } else if (diff < 48 * HOUR_MILLIS) {
                    "yesterday"
                } else if (diff < 7 * DAY_MILLIS) {
                    diff/= DAY_MILLIS
                    "$diff days ago"
                } else if (diff < WEEK_MILLIS * 4) {
                    diff/= WEEK_MILLIS
                    val weeks= if (diff==1L) "week" else "weeks"
                    "$diff $weeks ago"
                } else if (diff < MONTH_MILLIS * 12) {
                    diff/= MONTH_MILLIS
                    val months= if (diff==1L) "month" else "months"
                    "$diff $months ago"
                } else {
                    diff/= YEAR_MILLIS
                    val years= if (diff==1L) "year" else "years"
                    "$diff $years ago"
                }
            } else {
                diff = time - now
                if (diff < MINUTE_MILLIS) {
                    "this minute"
                } else if (diff < HOUR_MILLIS) {
                    diff/= MINUTE_MILLIS
                    val minutes= if (diff==1L) "minute" else "minutes"
                    "$diff $minutes later"
                } else if (diff < 24 * HOUR_MILLIS) {
                    diff/= HOUR_MILLIS
                    val hours= if (diff==1L) "hour" else "hours"
                    "$diff $hours later"
                } else if (diff < 48 * HOUR_MILLIS) {
                    "tomorrow"
                } else if (diff < 7 * DAY_MILLIS) {
                    diff/= DAY_MILLIS
                    "$diff days later"
                } else if (diff < WEEK_MILLIS * 4) {
                    diff/= WEEK_MILLIS
                    val weeks= if (diff==1L) "week" else "weeks"
                    "$diff $weeks later"
                } else if (diff < MONTH_MILLIS * 12) {
                    diff/= MONTH_MILLIS
                    val months= if (diff==1L) "month" else "months"
                    "$diff $months later"
                } else {
                    diff/= YEAR_MILLIS
                    val years= if (diff==1L) "year" else "years"
                    "$diff $years later"
                }
            }
        }
    }
}