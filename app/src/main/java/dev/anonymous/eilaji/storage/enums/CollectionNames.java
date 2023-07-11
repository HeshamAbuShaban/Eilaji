package dev.anonymous.eilaji.storage.enums;

public enum CollectionNames {
    Ad("Ads"),
    Category("Categories"),
    SubCategory("SubCategories"),
    Medicine("Medicines"),

    Favorite("Favorites"),
    Pharmacy("Pharmacies");

    public final String collection_name;

    CollectionNames(String collection_name) {
        this.collection_name = collection_name;
    }
}
