package me.matteo.appvariazioni.classes

import android.net.Uri

data class Response(
    val success: Boolean,
    val string: String,
    val bytes: ByteArray,
    val stringList: List<String>,
    val uri: Uri,
    val bool: Boolean,
) {
    constructor(success: Boolean) : this(
        success,
        "",
        byteArrayOf(),
        emptyList(),
        Uri.EMPTY,
        false
    )

    constructor(success: Boolean, string: String) : this(
        success,
        string,
        byteArrayOf(),
        emptyList(),
        Uri.EMPTY,
        false
    )

    constructor(success: Boolean, bytes: ByteArray) : this(
        success,
        "",
        bytes,
        emptyList(),
        Uri.EMPTY,
        false
    )

    constructor(success: Boolean, stringList: List<String>) : this(
        success,
        "",
        byteArrayOf(),
        stringList,
        Uri.EMPTY,
        false
    )

    constructor(success: Boolean, uri: Uri) : this(
        success,
        "",
        byteArrayOf(),
        emptyList(),
        uri,
        false
    )

    constructor(success: Boolean, string: String, bytes: ByteArray) : this(
        success,
        string,
        bytes,
        emptyList(),
        Uri.EMPTY,
        false
    )

    constructor(success: Boolean, bool: Boolean) : this(
        success,
        "",
        byteArrayOf(),
        emptyList(),
        Uri.EMPTY,
        bool
    )
}
