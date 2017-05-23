package clients;

import fr.tunaki.stackoverflow.chat.ChatHost;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.Room;

import services.Runner;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by bhargav.h on 18-May-17.
 */
public class RunDetector {

    public static void main(String[] args) {

        StackExchangeClient client;

        Properties prop = new Properties();

        try{
            prop.load(new FileInputStream("./properties/login.properties"));
        }
        catch (IOException e){
            e.printStackTrace();
        }

        String email = prop.getProperty("email");
        String password = prop.getProperty("password");

        client = new StackExchangeClient(email, password);

        Room room = client.joinRoom(ChatHost.STACK_OVERFLOW ,111347);

        room.send("[ [Citation Detector](https://git.io/v9jKB) ] started");
        
        Runner runner = new Runner(room);
        runner.startDetector();

    }
}
