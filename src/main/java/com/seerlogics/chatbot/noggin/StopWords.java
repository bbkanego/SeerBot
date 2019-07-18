package com.seerlogics.chatbot.noggin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bkane on 4/17/18.
 * https://github.com/apache/opennlp-sandbox/blob/master/summarizer/src/main/java/opennlp/summarization/preprocess/StopWords.java
 */
public class StopWords {
    private List<String> stopWords = new ArrayList<>();
    private List<String> salutations = new ArrayList<>();
    private static StopWords instance;

    private StopWords() {
        stopWords.add("0");
        stopWords.add("1");
        stopWords.add("2");
        stopWords.add("3");
        stopWords.add("4");
        stopWords.add("5");
        stopWords.add("6");
        stopWords.add("7");
        stopWords.add("8");
        stopWords.add("9");

        stopWords.add("a");
        stopWords.add("by");
        stopWords.add("me");
        stopWords.add("about");
        stopWords.add("above");
        stopWords.add("after");
        stopWords.add("again");
        stopWords.add("against");
        stopWords.add("all");
        stopWords.add("am");
        stopWords.add("an");
        stopWords.add("and");
        stopWords.add("any");
        //stopWords.add("are");
        stopWords.add("aren't");
        stopWords.add("as");
        stopWords.add("at");
        stopWords.add("be");
        stopWords.add("because");
        stopWords.add("been");
        stopWords.add("before");
        stopWords.add("being");
        stopWords.add("below");
        stopWords.add("between");
        stopWords.add("both");
        stopWords.add("but");
        stopWords.add("by");
        stopWords.add("can't");
        stopWords.add("cannot");
        stopWords.add("could");
        stopWords.add("couldn't");
        stopWords.add("did");
        stopWords.add("didn't");
        stopWords.add("do");
        stopWords.add("does");
        stopWords.add("doesn't");
        stopWords.add("doing");
        stopWords.add("don't");
        stopWords.add("down");
        stopWords.add("during");
        stopWords.add("each");
        stopWords.add("few");
        stopWords.add("for");
        stopWords.add("from");
        stopWords.add("further");
        stopWords.add("had");
        stopWords.add("hadn't");
        stopWords.add("has");
        stopWords.add("hasn't");
        stopWords.add("have");
        stopWords.add("haven't");
        stopWords.add("having");
        stopWords.add("he");
        stopWords.add("he'd");
        stopWords.add("he'll");
        stopWords.add("he's");
        stopWords.add("her");
        stopWords.add("here");
        stopWords.add("here's");
        stopWords.add("hers");
        stopWords.add("herself");
        stopWords.add("him");
        stopWords.add("himself");
        stopWords.add("his");
        //stopWords.add("how");
        stopWords.add("how's");
        stopWords.add("i");
        stopWords.add("i'd");
        stopWords.add("i'll");
        stopWords.add("i'm");
        stopWords.add("i've");
        stopWords.add("if");
        //stopWords.add("in");
        stopWords.add("into");
        stopWords.add("is");
        stopWords.add("isn't");
        stopWords.add("it");
        stopWords.add("it's");
        stopWords.add("its");
        stopWords.add("itself");
        stopWords.add("let's");
        stopWords.add("me");
        stopWords.add("more");
        stopWords.add("most");
        stopWords.add("mustn't");
        stopWords.add("my");
        stopWords.add("myself");
        stopWords.add("no");
        stopWords.add("nor");
        stopWords.add("not");
        stopWords.add("of");
        stopWords.add("off");
        stopWords.add("on");
        stopWords.add("once");
        stopWords.add("only");
        stopWords.add("or");
        stopWords.add("other");
        stopWords.add("ought");
        stopWords.add("our");
        stopWords.add("ours ");
        stopWords.add(" ourselves");
        stopWords.add("out");
        stopWords.add("over");
        stopWords.add("own");
        stopWords.add("same");
        stopWords.add("shan't");
        stopWords.add("she");
        stopWords.add("she'd");
        stopWords.add("she'll");
        stopWords.add("she's");
        stopWords.add("should");
        stopWords.add("shouldn't");
        stopWords.add("so");
        stopWords.add("some");
        stopWords.add("say");
        stopWords.add("said");
        stopWords.add("such");
        stopWords.add("than");
        stopWords.add("that");
        stopWords.add("that's");
        stopWords.add("the");
        stopWords.add("their");
        stopWords.add("theirs");
        stopWords.add("them");
        stopWords.add("themselves");
        stopWords.add("then");
        stopWords.add("there");
        stopWords.add("there's");
        stopWords.add("these");
        stopWords.add("they");
        stopWords.add("they'd");
        stopWords.add("they'll");
        stopWords.add("they're");
        stopWords.add("they've");
        stopWords.add("this");
        stopWords.add("those");
        stopWords.add("through");
        stopWords.add("to");
        stopWords.add("too");
        stopWords.add("under");
        stopWords.add("until");
        stopWords.add("up");
        stopWords.add("very");
        stopWords.add("was");
        stopWords.add("wasn't");
        stopWords.add("we");
        stopWords.add("we'd");
        stopWords.add("we'll");
        stopWords.add("we're");
        stopWords.add("we've");
        stopWords.add("were");
        stopWords.add("weren't");
        stopWords.add("what");
        stopWords.add("what's");
        stopWords.add("when");
        stopWords.add("when's");
        stopWords.add("where");
        stopWords.add("where's");
        stopWords.add("which");
        stopWords.add("while");
        stopWords.add("who");
        stopWords.add("who's");
        stopWords.add("whom");
        stopWords.add("why");
        stopWords.add("why's");
        stopWords.add("with");
        stopWords.add("won't");
        stopWords.add("would");
        stopWords.add("wouldn't");
        //stopWords.add("you");
        stopWords.add("you'd");
        stopWords.add("you'll");
        stopWords.add("you're");
        stopWords.add("you've");
        stopWords.add("your");
        stopWords.add("yours");
        stopWords.add("yourself");
        stopWords.add("yourselves ");
        stopWords.add("please");
        stopWords.add("help");
        stopWords.add("can");
        //stopWords.add("you");
    }

    public boolean isStopWord(String s) {
        boolean ret = stopWords.contains(s.toLowerCase());
        if (s.length() == 1) ret = true;
        return ret;
    }

    public static StopWords getInstance() {
        if (instance == null) {
            instance = new StopWords();
        }
        return instance;
    }

    public String[] removeStopWords(String[] tokens) {
        List noStopWordsToken = new ArrayList<>();
        for (String token : tokens) {
            if (!isStopWord(token)) noStopWordsToken.add(token);
        }
        String[] resultingArray = new String[noStopWordsToken.size()];
        noStopWordsToken.toArray(resultingArray);
        return resultingArray;
    }
}
