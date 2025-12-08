package com.example.dermahealth.data

enum class RoutineType(val priority: Int, val displayName: String) {

    // HOURLY TYPES
    HOURLY(1, "Hourly"),
    HOURLY_SPECIFIC_TIME(2, "Hourly (Specific Time)"),

    // EVERY X HOURS
    EVERY_X_HOURS(3, "Every X Hours"),

    // FIXED TIME ONLY (new meaning)
    SPECIFIC_TIME_ONLY(4, "Specific Time Only"),

    // DAILY TYPES
    DAILY(5, "Daily"),

    // EVERY X DAYS
    EVERY_X_DAYS(6, "Every X Days"),

    // ONE-TIME DATE & TIME
    SPECIFIC_DATE(7, "Specific Date");
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
