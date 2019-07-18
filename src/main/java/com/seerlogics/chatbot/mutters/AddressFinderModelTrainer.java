package com.seerlogics.chatbot.mutters;

import com.seerlogics.chatbot.util.Trainer;
import opennlp.tools.namefind.*;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.*;

import java.io.*;

/**
 * Created by bkane on 5/10/18.
 * http://nishutayaltech.blogspot.in/2015/07/writing-custom-namefinder-model-in.html
 * https://opennlp.apache.org/docs/1.5.3/manual/opennlp.html
 * ner: Named Entity Recognition
 */
public class AddressFinderModelTrainer {
    public static void main(String[] args) throws Exception {
        File test = new File("nlp/models/custom/addressTrainingData.train");
        AddressFinderModelTrainer addressFinderModelTrainer = new AddressFinderModelTrainer();
        addressFinderModelTrainer.trainAddressFinderModel();

        //AddressFinderModelTrainer locFinder = new AddressFinderModelTrainer();
        //locFinder.findLocation("173 Essex Drive");
    }

    public void findLocation(String paragraph) throws IOException {
        InputStream inputStreamNameFinder = getClass().getClassLoader().getResourceAsStream("nlp/models/custom/en-ner-address.bin");
        TokenNameFinderModel model = new TokenNameFinderModel(inputStreamNameFinder);

        NameFinderME locFinder = new NameFinderME(model);
        String[] tokens = tokenize(paragraph);

        Span nameSpans[] = locFinder.find(tokens);
        for (Span span : nameSpans) {
            System.out.println("Position - " + span.toString() + "    LocationName - " + tokens[span.getStart()]);
        }
    }

    public String[] tokenize(String sentence) throws IOException {
        InputStream inputStreamTokenizer = getClass().getClassLoader().getResourceAsStream("nlp/models/standard/en-token.bin");
        TokenizerModel tokenModel = new TokenizerModel(inputStreamTokenizer);

        TokenizerME tokenizer = new TokenizerME(tokenModel);
        return tokenizer.tokenize(sentence);
    }

    private void trainAddressFinderModel() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("nlp/models/custom/addressTrainingData.train");
        Trainer trainer = new Trainer();
        trainer.trainNameFinder("nlp/models/custom/addressTrainingData.train", "nlp/models/custom/en-ner-address.bin", "address");
        /*Charset charset = Charset.forName("UTF-8");
        MarkableFileInputStreamFactory markableFileInputStreamFactory =
                new MarkableFileInputStreamFactory(new File("nlp/models/custom/addressTrainingData.train"));
        ObjectStream<String> lineStream =
                new PlainTextByLineStream(markableFileInputStreamFactory, charset);
        ObjectStream<NameSample> addressStream = new NameSampleDataStream(lineStream);

        TokenNameFinderModel model;
        OutputStream modelOut = null;
        try {
            TrainingParameters trainingParameters = new TrainingParameters();
            trainingParameters.put(TrainingParameters.ITERATIONS_PARAM, "5");
            trainingParameters.put(TrainingParameters.CUTOFF_PARAM, "200");
            byte[] featureGeneratorBytes = null;
            Map<String, Object> resources = Collections.<String, Object>emptyMap();
            SequenceCodec<String> seqCodec = new BioCodec();
            TokenNameFinderFactory tokenNameFinderFactory = TokenNameFinderFactory.create(null, featureGeneratorBytes, resources, seqCodec);
            model = NameFinderME.train("en", "ner-address", addressStream, TrainingParameters.defaultParams(), tokenNameFinderFactory);
        } finally {
            addressStream.close();
        }

        try {
            modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }*/
    }
}
