package com.seerlogics.chatbot.mutters;

import com.seerlogics.chatbot.noggin.StopWords;
import com.seerlogics.chatbot.noggin.SynonymHelper;
import com.rabidgremlin.mutters.opennlp.intent.OpenNLPTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bkane on 5/15/18.
 */
public class CustomOpenNLPTokenizer extends OpenNLPTokenizer {
    private SynonymHelper synonymHelper;
    public CustomOpenNLPTokenizer(Tokenizer tokenizer) {
        super(tokenizer);
        synonymHelper = new SynonymHelper();
    }

    @Override
    public String[] tokenize(String text) {
        String[] tokens = super.tokenize(text);
        StopWords stopWords = StopWords.getInstance();
        List<String> noStopWordsTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!stopWords.isStopWord(token)) noStopWordsTokens.add(token.toLowerCase());
        }
        tokens = new String[noStopWordsTokens.size()];
        noStopWordsTokens.toArray(tokens);
        tokens = synonymHelper.replaceSynonyms(tokens);
        return tokens;
    }
}
