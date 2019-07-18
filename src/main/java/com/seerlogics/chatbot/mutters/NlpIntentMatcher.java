
package com.seerlogics.chatbot.mutters;

public class NlpIntentMatcher {

    private String intentModel;
    private String maybeMatchScore;
    private String minMatchScore;

    public String getIntentModel() {
        return intentModel;
    }

    public void setIntentModel(String intentModel) {
        this.intentModel = intentModel;
    }

    public String getMaybeMatchScore() {
        return maybeMatchScore;
    }

    public void setMaybeMatchScore(String maybeMatchScore) {
        this.maybeMatchScore = maybeMatchScore;
    }

    public String getMinMatchScore() {
        return minMatchScore;
    }

    public void setMinMatchScore(String minMatchScore) {
        this.minMatchScore = minMatchScore;
    }

}
