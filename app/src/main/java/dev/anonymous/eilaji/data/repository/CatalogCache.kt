package dev.anonymous.eilaji.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.network.MedicineDto

/**
 * Lightweight offline cache for catalog browsing: medicines + categories
 * are saved as JSON on every successful fetch and served when the
 * network is unavailable, so the app never goes blank on disconnect.
 */
object CatalogCache {
    private const val PREFS = "catalog_cache"
    private const val KEY_MEDS = "meds_home"
    private const val KEY_BEST = "meds_best"
    private const val KEY_CATS = "categories"
    private const val KEY_TS = "saved_at"
    private val gson = Gson()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveMedicines(context: Context, items: List<MedicineDto>) {
        try {
            prefs(context).edit()
                .putString(KEY_MEDS, gson.toJson(items))
                .putLong(KEY_TS, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {}
    }

    fun saveBestSellers(context: Context, items: List<MedicineDto>) {
        try { prefs(context).edit().putString(KEY_BEST, gson.toJson(items)).apply() } catch (_: Exception) {}
    }

    fun saveCategories(context: Context, items: List<CategoryDto>) {
        try { prefs(context).edit().putString(KEY_CATS, gson.toJson(items)).apply() } catch (_: Exception) {}
    }

    fun getMedicines(context: Context): List<MedicineDto> {
        return try {
            val json = prefs(context).getString(KEY_MEDS, null) ?: return emptyList()
            gson.fromJson(json, object : TypeToken<List<MedicineDto>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun getBestSellers(context: Context): List<MedicineDto> {
        return try {
            val json = prefs(context).getString(KEY_BEST, null) ?: return emptyList()
            gson.fromJson(json, object : TypeToken<List<MedicineDto>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun getCategories(context: Context): List<CategoryDto> {
        return try {
            val json = prefs(context).getString(KEY_CATS, null) ?: return emptyList()
            gson.fromJson(json, object : TypeToken<List<CategoryDto>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    fun hasCache(context: Context): Boolean {
        return try { prefs(context).contains(KEY_MEDS) } catch (_: Exception) { false }
    }
}
