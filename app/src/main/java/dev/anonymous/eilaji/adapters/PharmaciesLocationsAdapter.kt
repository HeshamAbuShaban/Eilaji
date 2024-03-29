package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemPharmacyLocationBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.utils.GeneralUtils

class PharmaciesLocationsAdapter(
    private val listPharmacies: ArrayList<Pharmacy>,
    private val navigateToChat: (model: Pharmacy) -> Unit
) :
    RecyclerView.Adapter<PharmaciesLocationsAdapter.PharmaciesLocationsViewHolder>() {

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
        fun bind(model: Pharmacy, navigateToChat: (model: Pharmacy) -> Unit) {
            binding.apply {

                GeneralUtils.getInstance().loadImage(model.pharmacy_image_url).into(ivPharmacyLocation)

                tvPharmacyNameLocation.text = model.pharmacy_name
                tvPharmacyDistanceLocation.text = model.address

                buPharmacyChatLocation.setOnClickListener {
                    navigateToChat(model)
                }
            }
        }
    }
}