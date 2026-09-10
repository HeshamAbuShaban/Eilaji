package dev.anonymous.eilaji.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class CartItem(
    val medicineId: String,
    val title: String,
    val price: Double,
    val imageUrl: String? = null,
    var quantity: Int,
    val pharmacyId: String? = null
)

class CartRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "cart_json"

    fun getAll(): MutableList<CartItem> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Exception) { mutableListOf() }
    }

    fun getCount(): Int = getAll().sumOf { it.quantity }

    fun getTotal(): Double = getAll().sumOf { it.price * it.quantity }

    fun addItem(item: CartItem) {
        val list = getAll()
        val existing = list.find { it.medicineId == item.medicineId }
        if (existing != null) existing.quantity += item.quantity else list.add(item)
        save(list)
    }

    fun updateQuantity(medicineId: String, qty: Int) {
        val list = getAll()
        list.find { it.medicineId == medicineId }?.let { it.quantity = qty.coerceAtLeast(1) }
        save(list)
    }

    fun remove(medicineId: String) {
        val list = getAll().filterNot { it.medicineId == medicineId }.toMutableList()
        save(list)
    }

    fun clear() { prefs.edit().remove(key).apply() }

    private fun save(list: List<CartItem>) {
        prefs.edit().putString(key, gson.toJson(list)).apply()
    }

    companion object {
        @Volatile private var INSTANCE: CartRepository? = null
        fun getInstance(context: Context): CartRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: CartRepository(context).also { INSTANCE = it }
        }
    }
}
