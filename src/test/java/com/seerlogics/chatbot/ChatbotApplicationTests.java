package com.seerlogics.chatbot;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
		"seerchat.allowedOrigins=http://localhost:3004", "seerchat.botPort=8099", "seerchat.bottype=EVENT_BOT",
		"seerchat.botOwnerId=9988", "seerchat.trainedModelId=8727", "seerchat.botId=32423"
})
public class ChatbotApplicationTests {

	@Test
	public void contextLoads() {
	}

}
