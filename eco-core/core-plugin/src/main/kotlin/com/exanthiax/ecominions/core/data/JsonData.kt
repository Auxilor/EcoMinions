package com.exanthiax.ecominions.core.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader

/** Reads the bundled JSON game-data files shipped in plugin resources. */
object JsonData {

    fun read(path: String): JsonObject {
        val stream = JsonData::class.java.getResourceAsStream(path)
            ?: error("Required data resource not found: $path")
        return InputStreamReader(stream, Charsets.UTF_8).use {
            JsonParser.parseReader(it).asJsonObject
        }
    }
}
