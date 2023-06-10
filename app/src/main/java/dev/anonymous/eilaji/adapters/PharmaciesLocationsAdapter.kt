package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemPharmacyLocationBinding
import dev.anonymous.eilaji.models.PharmacyModel

class PharmaciesLocationsAdapter(private var listPharmacies: ArrayList<PharmacyModel>) :
    RecyclerView.Adapter<PharmaciesLocationsAdapter.PharmaciesLocationsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmaciesLocationsViewHolder {
        val binding =
            ItemPharmacyLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PharmaciesLocationsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PharmaciesLocationsViewHolder, position: Int) {
        val listModels = listPharmacies[position]
        holder.bind(listModels)
    }

    override fun getItemCount(): Int {
        return listPharmacies.size
    }

    class PharmaciesLocationsViewHolder(private var binding: ItemPharmacyLocationBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(model: PharmacyModel?) {
            binding.apply {
                ivPharmacyLocation.setImageResource(model!!.image)
                tvPharmacyNameLocation.text = model.name
                tvPharmacyDistanceLocation.text = "30k.m"
            }
        }
    }
}