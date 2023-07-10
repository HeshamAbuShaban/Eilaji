package dev.anonymous.eilaji.firebase.notification;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MyResponse {
    @SerializedName("multicast_id")
    @Expose
    private Long multicastId;

    @SerializedName("success")
    @Expose
    private Integer success;

    @SerializedName("failure")
    @Expose
    private Integer failure;

    @SerializedName("canonical_ids")
    @Expose
    private Integer canonicalIds;

    @SerializedName("results")
    @Expose
    private List<Result> results;

    public Long getMulticastId() {
        return multicastId;
    }

    public void setMulticastId(Long multicastId) {
        this.multicastId = multicastId;
    }

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public Integer getFailure() {
        return failure;
    }

    public void setFailure(Integer failure) {
        this.failure = failure;
    }

    public Integer getCanonicalIds() {
        return canonicalIds;
    }

    public void setCanonicalIds(Integer canonicalIds) {
        this.canonicalIds = canonicalIds;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}