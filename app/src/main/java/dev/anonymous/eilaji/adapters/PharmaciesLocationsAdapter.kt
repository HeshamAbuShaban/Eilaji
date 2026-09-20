package dev.anonymous.eilaji.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import dev.anonymous.eilaji.R
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemPharmacyLocationBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.utils.GeneralUtils

class PharmaciesLocationsAdapter(
    private var listPharmacies: ArrayList<Pharmacy>,
    private val navigateToChat: (model: Pharmacy) -> Unit
) :
    RecyclerView.Adapter<PharmaciesLocationsAdapter.PharmaciesLocationsViewHolder>() {

    fun updateList(newList: List<Pharmacy>) {
        listPharmacies = ArrayList(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PharmaciesLocationsViewHolder {
        val binding =
            ItemPharmacyLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PharmaciesLocationsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PharmaciesLocationsViewHolder, position: Int) {
        val listModels = listPharmacies[position]
        holder.bind(listModels, navigateToChat)
    }

    override fun getItemCount(): Int {
        return listPharmacies.size
    }

    class PharmaciesLocationsViewHolder(private var binding: ItemPharmacyLocationBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        @SuppressLint("SetTextI18n")
        fun bind(model: Pharmacy, navigateToChat: (model: Pharmacy) -> Unit) {
            binding.apply {
                if (model.pharmacy_image_url.isBlank()) ivPharmacyLocation.setImageResource(R.drawable.temp_ads_image)
                else try { GeneralUtils.getInstance().loadImage(model.pharmacy_image_url).into(ivPharmacyLocation) } catch (_: Exception) {}

                tvPharmacyNameLocation.text = model.pharmacy_name
                tvPharmacyAddress.text = model.address
                tvPharmacyRating.text = if (model.totalRatings > 0) String.format("%.1f (%d)", model.ratingAvg, model.totalRatings) else "New"
                tvPharmacyDistanceLocation.text = model.distanceKm?.let { String.format("%.1f km", it) } ?: ""
                tvPharmacyOpenBadge.text = root.context.getString(if (model.isOpen) R.string.open_now else R.string.closed_now)
                tvPharmacyOpenBadge.setTextColor(
                    androidx.core.content.ContextCompat.getColor(root.context, if (model.isOpen) R.color.primary_color else R.color.gray_dark)
                )

                buPharmacyChatLocation.setOnClickListener { navigateToChat(model) }
                try {
                    buPharmacyCallLocation.setOnClickListener {
                        val ctx = it.context
                        if (model.phone.isBlank()) return@setOnClickListener
                        try {
                            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${model.phone}")))
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }
    }
}