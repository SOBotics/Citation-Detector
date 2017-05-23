package services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.MessagePostedEvent;
import fr.tunaki.stackoverflow.chat.event.UserMentionedEvent;
import utils.JsonUtils;

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


    private Instant previousRunTime ;
    private Room room;
    private ScheduledExecutorService executorService;

    public Runner(Room room){
        this.room = room;
        this.previousRunTime = Instant.now().minus(1, ChronoUnit.HOURS);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startDetector(){
        room.addEventListener(EventType.USER_MENTIONED,event->mention(room, event, false));
        room.addEventListener(EventType.MESSAGE_POSTED ,event-> newMessage(room, event, false));

        Runnable runner = () -> runEditBotOnce(room);
        executorService.scheduleAtFixedRate(runner, 0, 30, TimeUnit.SECONDS);
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

            JsonObject json =  JsonUtils.get(url,
                    "sort","creation",
                    "site","hinduism",
                    "pagesize","100",
                    "page","1",
                    "fromdate", Long.toString(previousRunTime.getEpochSecond()),
                    "order","desc",
                    "filter","!-*f(6skrN-SN",
                    "key",apiKey);


            if (json.has("items")){
                for (JsonElement element: json.get("items").getAsJsonArray()){
                    JsonObject object = element.getAsJsonObject();
                    if (object.get("body_markdown").getAsString().matches(".*(cite|add|provide|include|give).*(\\ssource).*")){
                        room.send(desc+" Answer with comment with the text 'cite sources': "+object.get("link").getAsString());
                    }
                }
            }

            previousRunTime = Instant.now();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
