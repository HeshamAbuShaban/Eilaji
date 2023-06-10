package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemCategoriesPharmaceuticalsBinding
import dev.anonymous.eilaji.models.CategoriesPharmaceuticalModel

class CategoriesPharmaceuticalsAdapter(private var listAds: ArrayList<CategoriesPharmaceuticalModel>) :
    RecyclerView.Adapter<CategoriesPharmaceuticalsAdapter.CategoriesPharmaceuticalsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesPharmaceuticalsViewHolder {
        val binding =
            ItemCategoriesPharmaceuticalsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return CategoriesPharmaceuticalsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriesPharmaceuticalsViewHolder, position: Int) {
        val listModels = listAds[position]
        holder.bind(listModels)
    }

    override fun getItemCount(): Int {
        return listAds.size
    }

    class CategoriesPharmaceuticalsViewHolder(private var binding: ItemCategoriesPharmaceuticalsBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun bind(model: CategoriesPharmaceuticalModel?) {
            binding.apply {
                ivCategoriesPharmaceuticals.setImageResource(model!!.image)
                tvCategoriesPharmaceuticals.text = model.text
            }
        }
    }
}