package dev.anonymous.eilaji.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemMedicineBinding
import dev.anonymous.eilaji.favorite_system.repository.FavoriteSyncRepository
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.utils.GeneralUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicinesAdapter(
    private var medicineModels: ArrayList<Medicine>,
    private val isGridLayout: Boolean = false,
    private val halfScreenWidth: Int = 0,
    private val onFavClick: ((Medicine) -> Unit)? = null,
    private val onItemClick: ((Medicine, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<MedicinesAdapter.MedicinesViewHolder>() {

    /** Favourite medicine ids for the current list, loaded once in the background. */
    private val favoriteMedicineIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicinesViewHolder {
        val binding = ItemMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicinesViewHolder(binding, ::isFavorite, onFavClick, onItemClick)
    }

    override fun onBindViewHolder(holder: MedicinesViewHolder, position: Int) {
        holder.bind(medicineModels[position], isGridLayout, halfScreenWidth, position)
    }

    fun updateList(newList: List<Medicine>) {
        medicineModels = ArrayList(newList)
        notifyDataSetChanged()
    }

    /**
     * Loads which of these medicines are favourites, off the main thread, once per list.
     * Reading Room inside onBindViewHolder used to block the frame.
     */
    fun loadFavorites(context: android.content.Context) {
        val ids = medicineModels.map { it.id }
        if (ids.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val repo = FavoriteSyncRepository(context.applicationContext)
            val favorites = ids.filter { repo.isFavoriteLocal(it, null) }.toSet()
            withContext(Dispatchers.Main) {
                favoriteMedicineIds.clear()
                favoriteMedicineIds.addAll(favorites)
                medicineModels.forEach { it.isFavorite = it.id in favoriteMedicineIds }
                notifyDataSetChanged()
            }
        }
    }

    fun isFavorite(medicineId: String): Boolean = medicineId in favoriteMedicineIds

    fun currentList(): List<Medicine> = medicineModels.toList()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        try { recyclerView.scheduleLayoutAnimation() } catch (_: Exception) {}
    }

    companion object {
        fun pop(v: android.view.View) {
            try { dev.anonymous.eilaji.utils.SpringFx.pop(v); return } catch (_: Exception) {}
            try {
                v.animate().scaleX(0.82f).scaleY(0.82f).setDuration(80)
                    .withEndAction {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                            .setInterpolator(android.view.animation.OvershootInterpolator(2.2f)).start()
                    }.start()
            } catch (_: Exception) {}
        }
    }

    override fun getItemCount(): Int = medicineModels.size

    class MedicinesViewHolder(
        private var binding: ItemMedicineBinding,
        private val isFavorite: (String) -> Boolean,
        private val onFavClick: ((Medicine) -> Unit)?,
        private val onItemClick: ((Medicine, android.view.View) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(model: Medicine, isGridLayout: Boolean, halfScreenWidth: Int, position: Int) {
            if (isGridLayout) {
                binding.root.layoutParams.width = halfScreenWidth
                if (position == 0 || position == 1) binding.root.setPadding(0, 60, 0, 0)
            }
            model.isFavorite = isFavorite(model.id)
            binding.apply {
                GeneralUtils.getInstance().loadImage(model.imageUrl).into(ivMedicine)
                tvMedicineName.text = model.title
                tvMedicineSalary.text = dev.anonymous.eilaji.utils.Money.format(model.price, binding.root.context)
                setUpFavoriteIcon(model)
                try { ivMedicine.transitionName = "medicine_image_${model.id}" } catch (_: Exception) {}
                root.setOnClickListener { onItemClick?.invoke(model, ivMedicine) }
                ivMedicine.setOnClickListener { onItemClick?.invoke(model, ivMedicine) }
                buAddToCart.setOnClickListener {
                    pop(it)
                    try { it.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM) } catch (_: Exception) {}
                    val ctx = it.context
                    try {
                        dev.anonymous.eilaji.data.repository.CartRepository.getInstance(ctx).addItem(
                            dev.anonymous.eilaji.data.repository.CartItem(model.id, model.title, model.price, model.imageUrl, 1)
                        )
                        com.google.android.material.snackbar.Snackbar.make(root, ctx.getString(R.string.added_to_cart), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
                buAddMedicineToFavorite.setOnClickListener {
                    val ctx = it.context
                    try { it.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM) } catch (_: Exception) {}
                    // Reads the favourite set loaded for this list, not the database, and not a
                    // recycled row's flag.
                    val wasFav = isFavorite(model.id)
                    try {
                        val repo = FavoriteSyncRepository(ctx)
                        if (wasFav) repo.syncDeleteByMedicine(model.id, null)
                        else repo.syncCreateLocalFirst(model.id, null)
                    } catch (_: Exception) {}
                    pop(it)
                    onFavClick?.invoke(model)
                    model.isFavorite = !wasFav
                    setUpFavoriteIcon(model)
                }
            }
        }
        private fun setUpFavoriteIcon(model: Medicine) {
            if (model.isFavorite) binding.buAddMedicineToFavorite.setImageResource(R.drawable.ic_favorite)
            else binding.buAddMedicineToFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }
}
