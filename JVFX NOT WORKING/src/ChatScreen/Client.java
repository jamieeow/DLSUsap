package ChatScreen;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the following Chat Protocol. When the server sends
 * "SUBMITNAME" the client replies with the desired screen name. The server will
 * keep sending "SUBMITNAME" requests as long as the client submits screen names
 * that are already in use. When the server sends a line beginning with
 * "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */
public class Client {

    String serverAddress;
    int port;
    Scanner in;
    static PrintWriter out;
    String name;

    public Client(String serverAddress, int port, String name) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.name = name;
        try {
            Stage stage = Main.getPrimaryStage();
            Parent root = FXMLLoader.load(getClass().getResource("chatscreen.fxml"));
            stage.setScene(new Scene(root, 600, 600));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void run() throws IOException {
        try {
            var socket = new Socket(serverAddress,port);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(this.name); //show connect box
                }
                else if (line.startsWith("NAMEACCEPTED")) {
                    System.out.println("MAMA MO 3");
                }
                else if (line.startsWith("MESSAGE")) {
                    System.out.println("hello");//CSController.addMessage(line.substring(8) + "\n"); //append message to dialogue box
                }
            }
        }
        catch (Exception e){
            System.out.println("FULL");
        }
        finally {
            //frame.setVisible(false);
           // frame.dispose();
        }
    }

    
    /* This method is used for sending a normal Message
     * @param msg - The message which the user generates
     */
    public static void send(String msg) throws IOException {
        out.println(msg);
    }

    public static void createClient(String ip, String port, String name)  throws Exception
    {
        int portnum = Integer.parseInt(port);
        var client = new Client(ip, portnum, name);
        client.run();
    }
}
