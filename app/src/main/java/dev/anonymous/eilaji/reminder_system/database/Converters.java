package dev.anonymous.eilaji.reminder_system.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromList(List<String> v) {
        return v == null ? "[]" : gson.toJson(v);
    }

    @TypeConverter
    public static List<String> toList(String v) {
        if (v == null || v.isEmpty() || v.equals("[]")) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> l = gson.fromJson(v, t);
            return l == null ? new ArrayList<>() : l;
        } catch (Exception e) { return new ArrayList<>(); }
    }
}
