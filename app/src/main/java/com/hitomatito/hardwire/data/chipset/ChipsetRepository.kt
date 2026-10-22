package com.hitomatito.hardwire.data.chipset

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.hitomatito.hardwire.data.command.CommandParser
import org.json.JSONObject
import java.util.Locale

class ChipsetRepository(context: Context) {

    private val prefs = context.getSharedPreferences("chipset_cache", Context.MODE_PRIVATE)

    suspend fun resolve(codename: String): ChipsetInfo {
        val key = codename.trim().lowercase(Locale.US)
        if (key.isBlank()) return ChipsetInfo()

        val cached = getFromCache(key)
        if (cached != null) {
            Log.d("HardWire", "[ChipsetRepo] Cache hit: $key -> ${cached.chipset}")
            return cached
        }

        val local = getFromLocalDatabase(key)
        if (local != null) {
            saveToCache(key, local)
            Log.d("HardWire", "[ChipsetRepo] Local hit: $key -> ${local.brand} ${local.chipset}")
            return local
        }

        Log.d("HardWire", "[ChipsetRepo] Not found locally: $key")
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
