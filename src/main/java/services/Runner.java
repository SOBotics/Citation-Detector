package services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.sobotics.chatexchange.chat.Room;
import org.sobotics.chatexchange.chat.event.EventType;
import org.sobotics.chatexchange.chat.event.MessagePostedEvent;
import org.sobotics.chatexchange.chat.event.UserMentionedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JsonUtils;

import javax.json.Json;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bhargav.h on 18-May-17.
 */
public class Runner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    private Instant previousRunTime ;
    private Room room;
    private ScheduledExecutorService executorService;

    public Runner(Room room){
        this.room = room;
        this.previousRunTime = Instant.now().minus(1, ChronoUnit.DAYS);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startDetector(){
        room.addEventListener(EventType.USER_MENTIONED,event->mention(room, event, false));
        room.addEventListener(EventType.MESSAGE_POSTED ,event-> newMessage(room, event, false));

        Runnable runner = () -> runEditBotOnce(room);
        executorService.scheduleAtFixedRate(runner, 0, 5, TimeUnit.MINUTES);
    }

    private static void newMessage(Room room, MessagePostedEvent event, boolean b) {
        String message = event.getMessage().getPlainContent();
        int cp = Character.codePointAt(message, 0);
        if(message.trim().startsWith("@bots alive")){
            room.send("Still new to the room, don't scare me plizz");
        }
        else if (cp == 128642 || (cp>=128644 && cp<=128650)){
            room.send("\uD83D\uDE83");
        }
    }

    private void mention(Room room, UserMentionedEvent event, boolean b) {
        String message = event.getMessage().getPlainContent();
        if(message.toLowerCase().contains("help")){
            room.send("I'm a bot that tracks citation posts");
        }
        else if(message.toLowerCase().contains("alive")){
            room.send("Yep");
        }
        else if(message.matches(".*(Namaste|Namaskaram|Hi|Vanakkam|Hello).*")){
            room.send("नमस्कार:");
        }
    }

    public void restartMonitor(){
        endDetector();
        startDetector();
    }

    public void endDetector(){
        executorService.shutdown();
    }

    public void runEditBotOnce(Room room){
        try{
            String desc = "[ [Citation Detector](https://git.io/v9jKB) ]";
            String url = "http://api.stackexchange.com/2.2/comments";
            String apiKey = "kmtAuIIqwIrwkXm1*p3qqA((";

            int number = 1;
            JsonObject json;
            do {

                 json = JsonUtils.get(url,
                        "sort", "creation",
                        "site", "hinduism",
                        "pagesize", "100",
                        "page", "" + number,
                        "fromdate", Long.toString(previousRunTime.getEpochSecond()),
                        "order", "desc",
                        "filter", "!-*f(6skrN-SN",
                        "key", apiKey);


                if (json.has("items")) {
                    for (JsonElement element : json.get("items").getAsJsonArray()) {
                        JsonObject object = element.getAsJsonObject();
                        if (object.get("body_markdown").getAsString().matches(".*(cite|add|provide|include|give).*(\\ssource|reference).*")) {
                            room.send(desc + " Answer with comment with the text 'cite sources': " + object.get("link").getAsString());
                        }
                    }
                }
                JsonUtils.handleBackoff(LOGGER,json);
                number++;
                LOGGER.debug(json.toString());
            }
            while (json.get("has_more").getAsBoolean());

            previousRunTime = Instant.now();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
