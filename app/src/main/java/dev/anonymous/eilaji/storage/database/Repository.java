package dev.anonymous.eilaji.storage.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import dev.anonymous.eilaji.storage.database.daos.CategoryDao;
import dev.anonymous.eilaji.storage.database.daos.EilajDao;
import dev.anonymous.eilaji.storage.database.tables.Category;
import dev.anonymous.eilaji.storage.database.tables.Eilaj;

public class Repository {
    private final EilajDao eilajDao;
    private final CategoryDao categoryDao;

    public Repository(Application application) {
        EilajiDatabase db = EilajiDatabase.getDatabase(application);
        eilajDao = db.eilajDao();
        categoryDao = db.categoryDao();
    }

    public void insertManyEilaj(Eilaj... eilaj) {
        EilajiDatabase.databaseWriteExecutor.execute(() -> eilajDao.insertAll(eilaj));
    }

    public LiveData<List<Eilaj>> getAllEilaj() {
        return eilajDao.getAll();
    }

    public void insertManyCategory(Category... categories) {
        EilajiDatabase.databaseWriteExecutor.execute(() -> categoryDao.insertAll(categories));
    }

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAll();
    }
}