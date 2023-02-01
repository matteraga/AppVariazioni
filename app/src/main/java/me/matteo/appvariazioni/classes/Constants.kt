package me.matteo.appvariazioni.classes

import me.matteo.appvariazioni.R

object Key {
    //Preferences
    //Variation
    const val VAR_PREFERENCES = "var_preferences"
    const val TODAY_URI = "today_uri"
    const val TODAY_VAR = "today_var"
    const val TOMORROW_URI = "tomorrow_uri"
    const val TOMORROW_VAR = "tomorrow_var"
    const val LAST_CHECK = "last_check"

    const val UNHANDLED_TEXT = "unhandled_text"
    const val TODAY_CLASSROOM_VARIATION = "today_classroom_variation"
    const val TOMORROW_CLASSROOM_VARIATION = "tomorrow_classroom_variation"

    //Settings
    const val SETTINGS_PREFERENCES = "var_preferences"
    const val BACKGROUND_CHECK = "background_check"
    const val CLASSROOM_POS = "classroom_pos"
    const val CLASSROOM = "classroom"
}

object Color {
    //Colors
    //IDK how to use android resources ones
    const val RED = "#c62828"
    const val GREEN = "#2e7d32"
    const val BLUE = "#0d469e"
}

object Day {
    //Days
    const val TODAY = 0
    const val TOMORROW = 1
}

object Strings {
    //Strings
    const val VARIATIONS_LINK = "https://www.ispascalcomandini.it/variazioni-orario-istituto-tecnico-tecnologico/2017/09/15/"
}

object Type {
    //Types
    const val SECTION_NAME = 0
    const val NORMAL = 1
    const val SIMPLE_TEXT_BACKGROUND = 2
}

object Icon {
    // Subjects
    const val COMPUTER_SCIENCE = R.drawable.icon_computer_science
    const val ENGLISH = R.drawable.icon_english
    const val HISTORY = R.drawable.icon_history
    const val ITALIAN = R.drawable.icon_italian
    const val MATH = R.drawable.icon_math
    const val NETWORKS = R.drawable.icon_networks
    const val PE = R.drawable.icon_pe
    const val RELIGION = R.drawable.icon_religion
    const val TELECOMMUNICATIONS = R.drawable.icon_telecommunications
    const val TPSIT = R.drawable.icon_tpsit

    // Variation
    const val EXIT =  R.drawable.icon_early_exit
    const val ENTRY =  R.drawable.icon_late_entry
    const val SUBSTITUTE =  R.drawable.icon_substitution
}