package com.seerlogics.chatbot.noggin;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by bkane on 5/17/18.
 */
public class SynonymHelper {
    private Map<String, List<String>> synonymMap = new HashMap<>();

    public SynonymHelper() {
        this.synonymMap.put("search", Arrays.asList("look","find","scan","search"));
        this.synonymMap.put("near", Arrays.asList("close","nearby","closeby","near"));
        this.synonymMap.put("hello", Arrays.asList("hello","hi","hiya","ciao", "howdy", "ola"));
        //this.synonymMap.put("log in", Arrays.asList("logging","signing", "login", "signin", "sign in"));
        // this replaces any 5 digit number entered by the user with 28277
        this.synonymMap.put("28277|regex", Arrays.asList("^\\d{5}(?:[-\\s]\\d{4})?$"));
    }

    public String findSynonym(String input) {
        Set<String> allKeys = synonymMap.keySet();
        boolean foundSynonym = false;
        for (String key : allKeys) {
            List<String> synonyms = synonymMap.get(key);
            if (!key.contains("regex")) {
                for (String synonym : synonyms) {
                    if (input.toUpperCase().contains(synonym.toUpperCase())) {
                        foundSynonym = true;
                        break;
                    }
                }
            } else {
                for (String regex : synonyms) {
                    if (input.matches(regex)) {
                        foundSynonym = true;
                        key = StringUtils.split(key, "|")[0];
                        break;
                    }
                }
            }
            if (foundSynonym) {
                return key;
            }
        }
        // no synonym found
        return input;
    }

    public String[] replaceSynonyms(String[] inputArry) {
        String[] returnArray = new String[inputArry.length];
        for (int i = 0; i < inputArry.length; i++) {
            String s = inputArry[i];
            returnArray[i] = findSynonym(s);
        }
        return returnArray;
    }
}
