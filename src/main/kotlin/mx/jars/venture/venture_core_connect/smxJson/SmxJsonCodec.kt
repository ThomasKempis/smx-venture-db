package mx.jars.venture.venture_core_connect.smxJson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SmxJsonCodec {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

    fun encode(value: Map<String, Any?>): String = gson.toJson(value)

    fun decodeMap(json: String): Map<String, Any?> = gson.fromJson(json, mapType)
}
