package dev.anonymous.eilaji.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemMedicineBinding
import dev.anonymous.eilaji.models.MedicineModel

class MedicinesAdapter(private var medicineModels: ArrayList<MedicineModel>) :
    RecyclerView.Adapter<MedicinesAdapter.MedicinesViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun setListMedicines(medicineModels: ArrayList<MedicineModel>) {
        this.medicineModels = medicineModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicinesViewHolder {
        val binding =
            ItemMedicineBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return MedicinesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicinesViewHolder, position: Int) {
        val listModels = medicineModels[position]
        holder.bind(listModels)
    }

    override fun getItemCount(): Int {
        return medicineModels.size
    }


    class MedicinesViewHolder(private var binding: ItemMedicineBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        fun bind(model: MedicineModel) {
            binding.apply {
                ivMedicine.setImageResource(model.image)
                tvMedicineName.text = model.name
                tvMedicineSalary.text = "${model.price}$"

                setUpFavoriteIcon(model)

                buAddMedicineToFavorite.setOnClickListener {
                    model.isFavorite = !model.isFavorite
                    setUpFavoriteIcon(model)
                }
            }
        }

        private fun setUpFavoriteIcon(model: MedicineModel) {
            if (model.isFavorite) {
                binding.buAddMedicineToFavorite.setImageResource(R.drawable.ic_favorite)
            } else {
                binding.buAddMedicineToFavorite.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }
}