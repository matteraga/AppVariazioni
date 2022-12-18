package me.matteo.appvariazioni.classes.variations.hourly

data class HourlyVariation(
    val hour: Int,
    val schoolClass: String,
    val classroom: String,
    val absent: String,
    val substitute: String?,
    val substitute2: String?,
    val paid: Boolean?,
    val notes: String,
    val signature: String?,
    val color: String,
    val type: Int
)