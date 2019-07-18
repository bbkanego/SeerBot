package com.seerlogics.chatbot.mutters;

import java.util.ArrayList;
import java.util.List;

public class BotConfiguration {

    private List<GlobalIntent> globalIntents = new ArrayList<>();
    private String inkStory;
    private List<Intent> intents = new ArrayList<>();
    private NlpIntentMatcher nlpIntentMatcher;
    private String sentenceDetectModel;
    private List<SlotMatcherModel> slotMatcherModels = new ArrayList<>();
    private String tokenizerModel;

    public List<GlobalIntent> getGlobalIntents() {
        return globalIntents;
    }

    public void setGlobalIntents(List<GlobalIntent> globalIntents) {
        this.globalIntents = globalIntents;
    }

    public String getInkStory() {
        return inkStory;
    }

    public void setInkStory(String inkStory) {
        this.inkStory = inkStory;
    }

    public List<Intent> getIntents() {
        return intents;
    }

    public void setIntents(List<Intent> intents) {
        this.intents = intents;
    }

    public NlpIntentMatcher getNlpIntentMatcher() {
        return nlpIntentMatcher;
    }

    public void setNlpIntentMatcher(NlpIntentMatcher nlpIntentMatcher) {
        this.nlpIntentMatcher = nlpIntentMatcher;
    }

    public String getSentenceDetectModel() {
        return sentenceDetectModel;
    }

    public void setSentenceDetectModel(String sentenceDetectModel) {
        this.sentenceDetectModel = sentenceDetectModel;
    }

    public List<SlotMatcherModel> getSlotMatcherModels() {
        return slotMatcherModels;
    }

    public void setSlotMatcherModels(List<SlotMatcherModel> slotMatcherModels) {
        this.slotMatcherModels = slotMatcherModels;
    }

    public String getTokenizerModel() {
        return tokenizerModel;
    }

    public void setTokenizerModel(String tokenizerModel) {
        this.tokenizerModel = tokenizerModel;
    }
}
