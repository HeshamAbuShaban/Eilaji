package dev.anonymous.eilaji.adapters.server

import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemSendToPharmacyBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.utils.GeneralUtils

class SendToPharmacyAdapter(
    private val listPharmacies: ArrayList<Pharmacy>,
    private val myLocation: Location,
    private val navigateToChat: (model: Pharmacy) -> Unit
) :
    RecyclerView.Adapter<SendToPharmacyAdapter.PharmaciesLocationsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PharmaciesLocationsViewHolder {
        val binding =
            ItemSendToPharmacyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PharmaciesLocationsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PharmaciesLocationsViewHolder, position: Int) {
        val listModels = listPharmacies[position]
        holder.bind(listModels, myLocation, navigateToChat)
    }

    override fun getItemCount(): Int {
        return listPharmacies.size
    }

    class PharmaciesLocationsViewHolder(private var binding: ItemSendToPharmacyBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun bind(model: Pharmacy, myLocation: Location, navigateToChat: (model: Pharmacy) -> Unit) {
            binding.apply {
                GeneralUtils.getInstance()
                    .loadImage(model.pharmacy_image_url)
                    .into(ivPharmacyLocation)

                tvPharmacyNameLocation.text = model.pharmacy_name

                val pharmacy = Location("pharmacy")
                pharmacy.latitude = model.lat
                pharmacy.longitude = model.lng

                tvPharmacyDistanceLocation.text = myLocation.distanceTo(pharmacy).toString()

                buSendToPharmacy.setOnClickListener {
                    navigateToChat(model)
                }
            }
        }
    }
}