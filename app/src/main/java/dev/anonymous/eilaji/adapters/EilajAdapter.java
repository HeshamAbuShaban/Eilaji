package dev.anonymous.eilaji.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.anonymous.eilaji.R;
import dev.anonymous.eilaji.storage.database.tables.Eilaj;

public class EilajAdapter extends RecyclerView.Adapter<EilajViewHolder> {
    // im not sure of the list of the data that should be used
    private final List<Eilaj> eilajList;

    //  constructor
    public EilajAdapter(List<Eilaj> eilajList) {
        this.eilajList = eilajList;
    }

    @NonNull
    @Override
    public EilajViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Item isn't what we need >> now it is
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_eilaj_category, parent, false);
        return new EilajViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EilajViewHolder holder, int position) {
        /*final*/
        Eilaj eilaj = eilajList.get(position);
        holder.setData(eilaj);
    }

    @Override
    public int getItemCount() {
        return eilajList.size();
    }

}

class EilajViewHolder extends RecyclerView.ViewHolder {
    private TextView categoryNameTV;
    private ImageView categoryIV;

    public EilajViewHolder(@NonNull View itemView) {
        super(itemView);
        findViews();
    }

    private void findViews() {
        //initialize the views from xml
        categoryNameTV = itemView.findViewById(R.id.eilaj_categoryTV);
        categoryIV = itemView.findViewById(R.id.eilaj_categoryIV);
    }

    protected void setData(Eilaj data) {
        //should be able to associate the views with class data
        categoryIV.setImageResource(R.drawable.ic_launcher_foreground);
        categoryNameTV.setText(data.getEilajName());
    }
}
