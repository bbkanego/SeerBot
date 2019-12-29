package com.seerlogics.chatbot.mutters;

import com.rabidgremlin.mutters.core.SlotMatcher;
import com.rabidgremlin.mutters.core.Tokenizer;
import com.rabidgremlin.mutters.core.ml.AbstractMachineLearningIntentMatcher;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.SortedMap;

/**
 * Created by bkane on 9/2/18.
 */
public class CustomOpenNLPIntentMatcher extends AbstractMachineLearningIntentMatcher {

    public static final Logger LOGGER = LoggerFactory.getLogger(CustomOpenNLPIntentMatcher.class);

    /**
     * The document categoriser for the intent matcher.
     */
    private DoccatModel model;

    /**
     * Default minimum match score.
     */
    private static final float MIN_MATCH_SCORE = 0.75f;

    /**
     * Constructor. Sets up the matcher to use the specified model (via a URL) and specifies the minimum and maybe match
     * score.
     *
     * @param categorizerModelBytes Byets of the document categorizer model file to load.
     * @param minMatchScore         The minimum match score for an intent match to be considered good.
     * @param maybeMatchScore       The maybe match score. Use -1 to disable maybe matching.
     * @param tokenizer             The tokenizer to use when tokenizing an utterance.
     * @param slotMatcher           The slot matcher to use to extract slots from the utterance.
     */
    CustomOpenNLPIntentMatcher(byte[] categorizerModelBytes, Tokenizer tokenizer, SlotMatcher slotMatcher,
                                      float minMatchScore, float maybeMatchScore) {
        super(tokenizer, slotMatcher, minMatchScore, maybeMatchScore);

        try {
            model = new DoccatModel(new ByteArrayInputStream(categorizerModelBytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load intent model", e);
        }
    }

    @Override
    protected SortedMap<Double, Set<String>> generateSortedScoreMap(String[] utteranceTokens) {
        DocumentCategorizerME intentCategorizer = new DocumentCategorizerME(model);
        double[] outcome = intentCategorizer.categorize(utteranceTokens);
        LOGGER.debug("action=" + intentCategorizer.getBestCategory(outcome));
        return intentCategorizer.sortedScoreMap(utteranceTokens);
    }
}
