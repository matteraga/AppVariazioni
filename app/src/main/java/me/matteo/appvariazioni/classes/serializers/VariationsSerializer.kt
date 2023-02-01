package me.matteo.appvariazioni.classes.serializers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.matteo.appvariazioni.classes.variations.Variations
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AllVariations(
    val schoolClass: String = "",
    val lastCheckTime: Long = 0L,
    val variations: PersistentList<Variations> = persistentListOf()
)

class VariationsSerializer : Serializer<AllVariations> {
    override val defaultValue: AllVariations
        get() = AllVariations()

    override suspend fun readFrom(input: InputStream): AllVariations {
        return try {
            Json.decodeFromString(
                deserializer = AllVariations.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AllVariations, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = AllVariations.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}