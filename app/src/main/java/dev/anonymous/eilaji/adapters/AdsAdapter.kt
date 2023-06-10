package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemAdsBinding
import dev.anonymous.eilaji.models.AdModel

class AdsAdapter(private var listAds: ArrayList<AdModel>) :
    RecyclerView.Adapter<AdsAdapter.AdsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdsViewHolder {
        val binding =
            ItemAdsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdsViewHolder, position: Int) {
        val listModels = listAds[position]
        holder.bind(listModels)
    }

    override fun getItemCount(): Int {
        return listAds.size
    }

    class AdsViewHolder(private var binding: ItemAdsBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(model: AdModel?) {
            binding.apply {
                ivAds.setImageResource(model!!.image)
                tvAds.text = model.text
            }
        }
    }
}