package com.hitomatito.hardwire.data.chipset

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.hitomatito.hardwire.data.command.CommandParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ChipsetRepository(context: Context) {

    private val prefs = context.getSharedPreferences("chipset_cache", Context.MODE_PRIVATE)

    suspend fun resolve(codename: String): ChipsetInfo {
        val key = codename.trim().lowercase(Locale.US)
        if (key.isBlank()) return ChipsetInfo()

        val cached = getFromCache(key)
        if (cached != null) {
            Log.d("Hardwire", "[ChipsetRepo] Cache hit: $key -> ${cached.chipset}")
            return cached
        }

        val apiResult = fetchFromApi(key)
        if (apiResult != null) {
            saveToCache(key, apiResult)
            Log.d("Hardwire", "[ChipsetRepo] API hit: $key -> ${apiResult.brand} ${apiResult.chipset} gpu=${apiResult.gpu}")
            return apiResult
        }

        val fallback = getFromLocalDatabase(key)
        if (fallback != null) {
            Log.d("Hardwire", "[ChipsetRepo] Local fallback: $key -> ${fallback.brand} ${fallback.chipset}")
            return fallback
        }

        Log.d("Hardwire", "[ChipsetRepo] Not found: $key")
        return ChipsetInfo()
    }

    private fun getFromCache(key: String): ChipsetInfo? {
        val json = prefs.getString(key, null) ?: return null
        return try {
            val obj = JSONObject(json)
            ChipsetInfo(
                brand = obj.optString("brand", ""),
                chipset = obj.optString("chipset", ""),
                cpu = obj.optString("cpu", ""),
                gpu = obj.optString("gpu", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToCache(key: String, info: ChipsetInfo) {
        val obj = JSONObject().apply {
            put("brand", info.brand)
            put("chipset", info.chipset)
            put("cpu", info.cpu)
            put("gpu", info.gpu)
        }
        prefs.edit { putString(key, obj.toString()) }
    }

    private suspend fun fetchFromApi(codename: String): ChipsetInfo? {
        val endpoint = getApiEndpoint(codename) ?: return null
        val urlStr = "https://tolepcoy.pages.dev/$endpoint?code=$codename"

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"

                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@withContext null
                }

                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)
                if (json.has("error")) return@withContext null

                val chipset = json.optString("chipset", "")
                if (chipset.isBlank()) return@withContext null

                ChipsetInfo(
                    brand = json.optString("brand", ""),
                    chipset = chipset,
                    cpu = json.optString("cpu", ""),
                    gpu = json.optString("gpu", "")
                )
            } catch (e: Exception) {
                Log.e("Hardwire", "[ChipsetRepo] API error: ${e.message}")
                null
            }
        }
    }

    private fun getApiEndpoint(codename: String): String? {
        val lower = codename.lowercase(Locale.US)
        return when {
            lower.startsWith("mt") -> "tolepcoy_mediatek"
            lower.startsWith("msm") || lower.startsWith("sm") || lower.startsWith("sd") ||
            lower.startsWith("qcs") || lower.startsWith("qcm") -> "tolepcoy_qualcomm"
            lower.startsWith("exynos") || lower.startsWith("s5e") || lower.startsWith("s5p") -> "tolepcoy_samsung"
            else -> null
        }
    }

    private fun getFromLocalDatabase(codename: String): ChipsetInfo? {
        val commercialName = CommandParser.lookupSocCommercialName(codename) ?: return null
        val vendor = when {
            commercialName.startsWith("Snapdragon") -> "Qualcomm"
            commercialName.startsWith("Helio") || commercialName.startsWith("Dimensity") -> "MediaTek"
            commercialName.startsWith("Exynos") -> "Samsung"
            commercialName.startsWith("Kirin") -> "HiSilicon"
            commercialName.startsWith("Unisoc") -> "Unisoc"
            else -> ""
        }
        return ChipsetInfo(
            brand = vendor,
            chipset = commercialName,
            cpu = "",
            gpu = ""
        )
    }
}
