package com.seerlogics.chatbot.controller;

import com.seerlogics.chatbot.view.IntentsModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bkane on 10/20/18.
 */
@RestController
@RequestMapping(value = "/api")
public class IntentsController {
    @Value("classpath:nlp/models/custom/EventGenieBotIntents.train")
    private Resource resource;

    @GetMapping("/intents")
    public ResponseEntity<IntentsModel> readIntents() throws Exception {
        IntentsModel intentsModel = new IntentsModel();
        intentsModel.setIntentsToUtterance(readIntentsLocal());
        return new ResponseEntity<>(intentsModel, HttpStatus.OK);
    }

    @PostMapping("/intents")
    public ResponseEntity<Boolean> saveIntents(@RequestBody IntentsModel intentsModel) throws Exception {
        saveIntentsLocal(intentsModel);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    private void saveIntentsLocal(IntentsModel intentsModel) throws Exception {
        String fileStr = Thread.currentThread().getContextClassLoader().getResource("nlp/models/custom/EventGenieBotIntents.train").getFile();
        File file = new File(fileStr);
        FileWriter fileWriter = new FileWriter(file);
        Map<String, List<String>> intentsToUtterance = intentsModel.getIntentsToUtterance();
        for (String key : intentsToUtterance.keySet()) {
            List<String> intents = intentsToUtterance.get(key);
            for (String intent : intents) {
                fileWriter.write(key + " " + intent.trim() + "\n");
            }
        }
        fileWriter.close();
    }

    private Map<String, List<String>> readIntentsLocal() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        Map<String, List<String>> intentsToUtterance = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String parts[] = line.split(" ", 2);
            if (!intentsToUtterance.containsKey(parts[0])) {
                List<String> utterances = new ArrayList<>();
                intentsToUtterance.put(parts[0].trim(), utterances);
                utterances.add(parts[1].trim() + "\n");
            } else {
                List<String> utterances = intentsToUtterance.get(parts[0]);
                utterances.add(parts[1].trim() + "\n");
            }
        }
        return intentsToUtterance;
    }
}
