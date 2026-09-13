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

class MedicinesAdapter(
    private var medicineModels: ArrayList<Medicine>,
    private val isGridLayout: Boolean = false,
    private val halfScreenWidth: Int = 0,
    private val onFavClick: ((Medicine) -> Unit)? = null,
    private val onItemClick: ((Medicine, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<MedicinesAdapter.MedicinesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicinesViewHolder {
        val binding = ItemMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicinesViewHolder(binding, onFavClick, onItemClick)
    }

    override fun onBindViewHolder(holder: MedicinesViewHolder, position: Int) {
        holder.bind(medicineModels[position], isGridLayout, halfScreenWidth, position)
    }

    fun updateList(newList: List<Medicine>) {
        medicineModels = ArrayList(newList)
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        try { recyclerView.scheduleLayoutAnimation() } catch (_: Exception) {}
    }

    companion object {
        fun pop(v: android.view.View) {
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

    class MedicinesViewHolder(private var binding: ItemMedicineBinding, private val onFavClick: ((Medicine) -> Unit)?, private val onItemClick: ((Medicine, android.view.View) -> Unit)?) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(model: Medicine, isGridLayout: Boolean, halfScreenWidth: Int, position: Int) {
            if (isGridLayout) {
                binding.root.layoutParams.width = halfScreenWidth
                if (position == 0 || position == 1) binding.root.setPadding(0, 60, 0, 0)
            }
            binding.apply {
                GeneralUtils.getInstance().loadImage(model.imageUrl).into(ivMedicine)
                tvMedicineName.text = model.title
                tvMedicineSalary.text = "${model.price} $"
                setUpFavoriteIcon(model)
                try { ivMedicine.transitionName = "medicine_image_${model.id}" } catch (_: Exception) {}
                root.setOnClickListener { onItemClick?.invoke(model, ivMedicine) }
                ivMedicine.setOnClickListener { onItemClick?.invoke(model, ivMedicine) }
                buAddToCart.setOnClickListener {
                    pop(it)
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
                    try {
                        val repo = FavoriteSyncRepository(ctx)
                        val isFav = repo.isFavoriteLocal(model.id, null)
                        if (isFav) {
                            val ent = dev.anonymous.eilaji.favorite_system.database.db.FavoriteDatabase.getDatabase(ctx).favoriteDao().findByMedicineId(model.id)
                            if (ent != null) repo.syncDelete(ent.id) else repo.syncDelete(model.id)
                        } else {
                            repo.syncCreateLocalFirst(model.id, null)
                        }
                    } catch (_: Exception) {}
                    pop(it)
                    onFavClick?.invoke(model)
                    model.isFavorite = !model.isFavorite
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
