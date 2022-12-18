package me.matteo.appvariazioni.classes.variations.timetable

data class TimetableHour(
    val hour: Int,
    val numberOfHours: Int,
    val subject: String,
    val subjectInt: Int,
    val professor: String,
    val classroom: String
)