package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemSubCategoryBinding
import dev.anonymous.eilaji.models.SubCategory
import dev.anonymous.eilaji.utils.GeneralUtils

class MedicinesSubCategoriesAdapter(private var listMedicinesSubCategoriesAdapter: ArrayList<SubCategory>) :
    RecyclerView.Adapter<MedicinesSubCategoriesAdapter.SubCategoriesViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubCategoriesViewHolder {
        val binding =
            ItemSubCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return SubCategoriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubCategoriesViewHolder, position: Int) {
        val listModels = listMedicinesSubCategoriesAdapter[position]
        holder.bind(listModels) {

        }
    }

    override fun getItemCount(): Int {
        return listMedicinesSubCategoriesAdapter.size
    }

    class SubCategoriesViewHolder(
        private var binding: ItemSubCategoryBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            model: SubCategory,
            onSelected: () -> Unit
        ) {
            binding.apply {
                GeneralUtils.getInstance().loadImage(model.imageUrl).into(ivSubCategories)
                tvSubCategories.text = model.title

                parentView.setOnClickListener { onSelected() }
            }
        }
    }
}