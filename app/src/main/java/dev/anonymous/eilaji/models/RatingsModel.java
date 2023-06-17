package dev.anonymous.eilaji.models;

public class RatingsModel {
    private float priceRate;
    private float speedOfRecoveryRate;
    private float medicineEfficacyRate;
    private float tasteOfMedicineRate;

    public RatingsModel(float priceRate, float speedOfRecoveryRate, float medicineEfficacyRate, float tasteOfMedicineRate) {
        this.priceRate = priceRate;
        this.speedOfRecoveryRate = speedOfRecoveryRate;
        this.medicineEfficacyRate = medicineEfficacyRate;
        this.tasteOfMedicineRate = tasteOfMedicineRate;
    }

    public float getPriceRate() {
        return priceRate;
    }

    public void setPriceRate(float priceRate) {
        this.priceRate = priceRate;
    }

    public float getSpeedOfRecoveryRate() {
        return speedOfRecoveryRate;
    }

    public void setSpeedOfRecoveryRate(float speedOfRecoveryRate) {
        this.speedOfRecoveryRate = speedOfRecoveryRate;
    }

    public float getMedicineEfficacyRate() {
        return medicineEfficacyRate;
    }

    public void setMedicineEfficacyRate(float medicineEfficacyRate) {
        this.medicineEfficacyRate = medicineEfficacyRate;
    }

    public float getTasteOfMedicineRate() {
        return tasteOfMedicineRate;
    }

    public void setTasteOfMedicineRate(float tasteOfMedicineRate) {
        this.tasteOfMedicineRate = tasteOfMedicineRate;
    }
}
