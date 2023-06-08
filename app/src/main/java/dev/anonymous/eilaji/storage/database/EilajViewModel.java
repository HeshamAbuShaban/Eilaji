package dev.anonymous.eilaji.storage.database;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import dev.anonymous.eilaji.storage.database.tables.Category;
import dev.anonymous.eilaji.storage.database.tables.Eilaj;

public class EilajViewModel extends AndroidViewModel {
    private final Repository repository;

    public EilajViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
    }

    //*Eilaj*
    public void insertManyEilaj(Eilaj... eilaj) {
        repository.insertManyEilaj(eilaj);
    }

    public LiveData<List<Eilaj>> getAllEilaj() {
        return repository.getAllEilaj();
    }

    //*Category*
    public void insertManyCategory(Category... categories) {
        repository.insertManyCategory(categories);
    }

    public LiveData<List<Category>> getAllCategories() {
        return repository.getAllCategories();
    }

}
