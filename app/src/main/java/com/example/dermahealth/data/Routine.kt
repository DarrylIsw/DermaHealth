package com.example.dermahealth.data

enum class RoutineType(val priority: Int) {

    // HOURLY TYPES
    HOURLY(1),
    HOURLY_SPECIFIC_TIME(2),

    // EVERY X HOURS
    EVERY_X_HOURS(3),

    // FIXED TIME ONLY (new meaning)
    SPECIFIC_TIME_ONLY(4),

    // DAILY TYPES
    DAILY(5),

    // EVERY X DAYS
    EVERY_X_DAYS(6),

    // ONE-TIME DATE & TIME
    SPECIFIC_DATE(7);
}



/**
 * Universal routine model:
 * - Use `type` to determine which fields are relevant.
 * - Times are stored as ints for hour/minute. specificDate is millis since epoch.
 */
data class Routine(
    var id: Int,
    var title: String,
    var type: RoutineType = RoutineType.DAILY,

    // Common time fields (used for DAILY and EVERY_X_DAYS)
    var hour: Int? = null,
    var minute: Int? = null,

    // Interval schedules
    var intervalHours: Int? = null,  // EVERY_X_HOURS
    var intervalDays: Int? = null,   // EVERY_X_DAYS

    // Specific absolute time
    var specificDate: Long? = null,  // milliseconds

    var note: String? = null
)
