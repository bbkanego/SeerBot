package com.seerlogics.chatbot.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bkane on 6/3/18.
 */
public class ChatUtil {
    public static List<String> extractStringsContaining(String input, String containing) {
        List<String> targetStrings = new ArrayList<>();
        String[] tokens = StringUtils.split(input);
        for (String token : tokens) {
            if (token.contains(containing)) {
                targetStrings.add(token);
            }
        }
        return targetStrings;
    }
}
