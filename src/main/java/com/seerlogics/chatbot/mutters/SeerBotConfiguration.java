package com.seerlogics.chatbot.mutters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingoace.common.NLPProcessingException;
import com.rabidgremlin.mutters.core.IntentMatcher;
import com.rabidgremlin.mutters.opennlp.intent.OpenNLPTokenizer;
import com.rabidgremlin.mutters.opennlp.ner.OpenNLPSlotMatcher;
import com.seerlogics.chatbot.exception.ConversationException;
import com.seerlogics.commons.model.Bot;
import com.seerlogics.commons.model.LaunchInfo;
import com.seerlogics.commons.model.TrainedModel;
import com.seerlogics.commons.repository.BotRepository;
import com.seerlogics.commons.repository.IntentRepository;
import com.seerlogics.commons.repository.LaunchInfoRepository;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bkane on 5/10/18.
 */
public class SeerBotConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeerBotConfiguration.class);

    private IntentMatcher intentMatcher;

    private SentenceDetectorME sentenceDetectorME;

    private LaunchInfo launchInfo;

    private Bot targetBot;

    private List<GlobalIntent> globalIntents = new ArrayList<>();

    public LaunchInfo getLaunchInfo() {
        return this.launchInfo;
    }

    public Bot getTargetBot() {
        return targetBot;
    }

    public SeerBotConfiguration(String uniqueBotId,
                                IntentRepository intentRepository, LaunchInfoRepository launchInfoRepository,
                                BotRepository botRepository) {

        this.launchInfo = launchInfoRepository.findByUniqueBotId(uniqueBotId);
        if (this.launchInfo == null) {
            throw new ConversationException("Launch info not found", ConversationException.Type.LAUNCH_INFO_NOT_FOUND);
        }
        this.targetBot = botRepository.getOne(this.launchInfo.getTargetBotId());

        ObjectMapper mapper = new ObjectMapper();
        URL botConfigURL = Thread.currentThread().getContextClassLoader().getResource("bot/config/botConfig.json");
        BotConfiguration botJsonConfiguration = null;

        try {
            botJsonConfiguration = mapper.readValue(botConfigURL, BotConfiguration.class);
            LOGGER.debug("The configuration is = {} ", mapper.writeValueAsString(botJsonConfiguration));
        } catch (IOException e) {
            throw new ConversationException("Error reading bot configuration", e,
                    ConversationException.Type.ERROR_READING_BOT_CONFIG);
        }

        //Loading sentence detector model
        String sentModel = botJsonConfiguration.getSentenceDetectModel();
        if (sentModel != null) {
            try {
                // https://www.tutorialspoint.com/opennlp/opennlp_sentence_detection.htm
                URL enSentenceDetectModelUrl = Thread.currentThread().getContextClassLoader().
                        getResource(sentModel);
                if (enSentenceDetectModelUrl != null) {
                    SentenceModel sentenceDetectModel = new SentenceModel(enSentenceDetectModelUrl);
                    //Instantiating the SentenceDetectorME class
                    sentenceDetectorME = new SentenceDetectorME(sentenceDetectModel);
                } else {
                    throw new NLPProcessingException("'nlp/models/standard/en-sent.bin' not found!");
                }
            } catch (IOException e) {
                throw new NLPProcessingException(e);
            }
        }

        LOGGER.debug("\n*********Set up tokenizer\n");

        OpenNLPTokenizer openNLPTokenizer = null;
        String tokenizerModel = botJsonConfiguration.getTokenizerModel();
        if (tokenizerModel != null) {

            // model was built with OpenNLP whitespace tokenizer
            URL enTokenModelUrl = Thread.currentThread().getContextClassLoader().getResource(tokenizerModel);
            TokenizerModel model = null;
            try {
                if (enTokenModelUrl != null) {
                    model = new TokenizerModel(enTokenModelUrl);
                } else {
                    throw new NLPProcessingException("'" + tokenizerModel + "' not found!");
                }
            } catch (IOException e) {
                throw new NLPProcessingException(e);
            }
            Tokenizer tokenBasedTokenizer = new TokenizerME(model);
            openNLPTokenizer = new CustomOpenNLPTokenizer(tokenBasedTokenizer);
        } else {
            throw new NLPProcessingException("Config Error: No Tokenizer model defined");
        }

        LOGGER.debug("\n*********Set getSlotMatcherModels\n");

        // use OpenNLP NER for slot matching
        List<SlotMatcherModel> slotMatcherModels = botJsonConfiguration.getSlotMatcherModels();
        OpenNLPSlotMatcher slotMatcher = new OpenNLPSlotMatcher(openNLPTokenizer);
        for (SlotMatcherModel slotMatcherModel : slotMatcherModels) {
            slotMatcher.addSlotModel(slotMatcherModel.getType(), slotMatcherModel.getModel());
        }

        /**
         * create intent matcher
         * I have created the matcher with min score of 0.20f so that we can get some kind of match with intents when
         * the conversation is close to what we think it is.
         */
        TrainedModel trainedModel = this.launchInfo.getTrainedModel();
        CustomOpenNLPIntentMatcher matcher =
                new CustomOpenNLPIntentMatcher(trainedModel.getFile(), openNLPTokenizer,
                        slotMatcher, Float.parseFloat(botJsonConfiguration.getNlpIntentMatcher().getMinMatchScore()),
                        Float.parseFloat(botJsonConfiguration.getNlpIntentMatcher().getMaybeMatchScore()));

        LOGGER.debug("\n*********get customIntentUtterances\n");

        List<com.seerlogics.commons.model.Intent> customIntentUtterances =
                intentRepository.findIntentsByCodeTypeAndOwnerId(targetBot.getCategory().getCode(),
                        com.seerlogics.commons.model.Intent.INTENT_TYPE.CUSTOM.name(),
                        targetBot.getOwner().getId());
        for (com.seerlogics.commons.model.Intent customIntentUtterance : customIntentUtterances) {
            Intent currentIntent = new Intent(customIntentUtterance.getIntent(), customIntentUtterance);
            matcher.addIntent(currentIntent);
        }

        LOGGER.debug("\n*********Done******\n");

        this.intentMatcher = matcher;
    }

    public SentenceDetectorME getSentenceDetector() {
        return sentenceDetectorME;
    }

    public IntentMatcher getIntentMatcher() {
        return intentMatcher;
    }

    public List<GlobalIntent> getGlobalIntents() {
        return globalIntents;
    }
}
