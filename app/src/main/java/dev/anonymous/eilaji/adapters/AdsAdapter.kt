package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemAdsBinding
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.utils.GeneralUtils

class AdsAdapter(private var listAds: ArrayList<Ad>) :
    RecyclerView.Adapter<AdsAdapter.AdsViewHolder>() {
    fun setListAds(listAds: ArrayList<Ad>) {
        this.listAds = listAds
        notifyItemRangeInserted(0, listAds.size)
    }

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
        fun bind(model: Ad) {
            binding.apply {
                GeneralUtils.getInstance().loadImage(model.imageUrl).into(ivAds)
                tvAds.text = model.title
            }
        }
    }
}