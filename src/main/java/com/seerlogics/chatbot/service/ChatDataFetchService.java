package com.seerlogics.chatbot.service;

import com.lingoace.common.NLPProcessingException;
import com.seerlogics.chatbot.noggin.ChatSession;
import com.rabidgremlin.mutters.core.session.Session;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.net.URI;
import java.util.List;

/**
 * Created by bkane on 5/27/18.
 */
@Service
public class ChatDataFetchService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private ChatSession chatSession;

    public String getNearbyEvents(String addressOrZip) {
        // http://localhost:8080/webflow/api/event/v1/nearbyevents?lat=35.0535496&lng=-80.82116959999999&radius=20000
        /*ObjectMapper mapper = new ObjectMapper();
        String jsonPayload = mapper.writeValueAsString(this.chatData);
        HttpEntity<String> headers = new HttpEntity<>(jsonPayload, createHeaders((String) cookieMap.get("JESESSIONID")));
        ResponseEntity<ChatData> responseMessage =
                restTemplate.exchange(new URI(chatUrl), HttpMethod.POST, headers, ChatData.class);

        ChatData response = responseMessage.getBody();*/
        VelocityContext context = new VelocityContext();
        context.put("jsonData", "Dummy Data");
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/nearByEvents.vm", "UTF-8", context, stringWriter);

        //return "test! -- " + chatSession.getAuthCode() + ", events = " + responseMessage.getBody().size();
        return stringWriter.toString().replaceAll("\\n", "");
    }

    public boolean performEventDelete(Session session, String eventId) {
        ChatSession chatSession = (ChatSession) session;
        String url = "http://localhost:8080/webflow/api/event/v1/" + eventId;
        ResponseEntity<Boolean> responseMessage = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", chatSession.getAuthCode());
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            responseMessage = restTemplate.exchange(new URI(url), HttpMethod.DELETE, entity, Boolean.class);
        } catch (Exception e) {
            throw new NLPProcessingException(e);
        }
        return responseMessage.getBody();
    }
}
