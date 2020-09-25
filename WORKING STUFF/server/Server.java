import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.EOFException;
/**
 * A multithreaded chat room server. When a client connects the server requests
 * a screen name by sending the client the text "SUBMITNAME", and keeps
 * requesting a name until a unique one is received. After a client submits a
 * unique name, the server acknowledges with "NAMEACCEPTED". Then all messages
 * from that client will be broadcast to all other clients that have submitted a
 * unique screen name. The broadcast messages are prefixed with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g.,
 * better logging. Another is to accept a lot of fun commands, like Slack.
 */
public class Server {

    // All client names, so we can check for duplicates upon registration.
    private static Set<String> names = new HashSet<>();

    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();
	
	private static ServerSocket fileServer;
    private static Socket fileClient;
    private static int filePort;
    private static DataOutputStream fileOut;//writer to other client
    private static DataInputStream fileIn;//reader from other client


    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running...");
        var pool = Executors.newFixedThreadPool(2);
        try (var listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    /**
     * The client handler task.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;
        private boolean image;
        private boolean text;

        /**
         * Constructs a handler thread, squirreling away the socket. All the interesting
         * work is done in the run method. Remember the constructor is called from the
         * server's main method, so this has to be as short as possible.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a screen name until a
         * unique one has been submitted, then acknowledges the name and registers the
         * output stream for the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
				 filePort = 59002;
				//find a port for the fileserver
				while (fileServer == null) {
					try {
						fileServer = new ServerSocket(filePort);
					} catch (IOException ioe) {
						filePort++;
						fileServer = null;
					}
				}
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);
                image = false;
                text = false; 

                // Keep requesting a name until we get a unique one.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!name.isBlank() && !names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the socket's print writer
                // to the set of all writers so this client can receive broadcast messages.
                // But BEFORE THAT, let everyone else know that the new person has joined!
                out.println("NAMEACCEPTED " + name);
				out.println("FILEPORT " + filePort);

                //get the file client to accept
                fileClient = fileServer.accept();
                fileOut = new DataOutputStream(fileClient.getOutputStream());
                fileIn = new DataInputStream(fileClient.getInputStream());

                //create thread to detect and store incoming files
                new Thread(new fileReaderThread()).start();
				
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");
                }
                writers.add(out);

                // Accept messages from this client and broadcast them.
                while (true) {
                        String input = in.nextLine();
                        if (input.toLowerCase().startsWith("/quit")) {
                            return;
                        }
                       for (PrintWriter writer : writers) {
                             writer.println("MESSAGE " + name + ": " + input);
                        }
                }
            } catch (Exception e) {
                //System.out.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null && !name.equals("null")) {
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
	
	
	private static class fileReaderThread implements Runnable {
        //create new thread to recieve commands and file data

        public void run() {
            try {
                //get the original file command from the user
                String command = fileIn.readUTF();
                
                //get the file size
                int fileSize = Integer.parseInt(command.substring(command.lastIndexOf("-") + 1, command.length() - 1));
                int dashIndex = command.indexOf("-");
                String fileName = command.substring(dashIndex + 1, command.lastIndexOf("-"));

               
                //create new file object to store data

                //make the file object and add to list
                file tmp = new file(fileSize, fileName);
                //files.add(tmp);
                //fileModel.add(fileModel.getSize(), fileName);
                System.out.println(fileName);
                fileIn.read(tmp.getData());


                //get the next command
            } catch (FileNotFoundException fnfe) {
               //mainReference.error("Could not read file.");
            } catch (EOFException eofe) {
                //mainReference.error(eofe.getMessage());
            } catch (IOException ioe) {
            }
        }
    }
	
}