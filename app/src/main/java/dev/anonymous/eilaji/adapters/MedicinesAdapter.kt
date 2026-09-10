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
    private val onItemClick: ((Medicine) -> Unit)? = null
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

    override fun getItemCount(): Int = medicineModels.size

    class MedicinesViewHolder(private var binding: ItemMedicineBinding, private val onFavClick: ((Medicine) -> Unit)?, private val onItemClick: ((Medicine) -> Unit)?) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(model: Medicine, isGridLayout: Boolean, halfScreenWidth: Int, position: Int) {
            if (isGridLayout) {
                binding.root.layoutParams.width = halfScreenWidth
                if (position == 0 || position == 1) binding.root.setPadding(0, 60, 0, 0)
            }
            binding.apply {
                GeneralUtils.getInstance().loadImage(model.imageUrl).into(ivMedicine)
                tvMedicineName.text = model.title
                tvMedicineSalary.text = "${model.price}$"
                setUpFavoriteIcon(model)
                root.setOnClickListener { onItemClick?.invoke(model) }
                ivMedicine.setOnClickListener { onItemClick?.invoke(model) }
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
