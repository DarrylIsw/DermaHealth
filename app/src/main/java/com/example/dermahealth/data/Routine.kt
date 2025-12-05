package com.example.dermahealth.data

enum class RoutineType(val priority: Int) {
    HOURLY(1),
    EVERY_X_HOURS(2),
    DAILY(3),
    EVERY_X_DAYS(4),
    SPECIFIC_DATE(5)
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
