import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.FileInputStream;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;


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
    int ip;
    Scanner in;
    PrintWriter out;
    InputStream dis;
    OutputStream dos;
	private static Socket fileClient;
    private static int filePort;
    private static DataOutputStream fileOut;//writer to other client
    private static DataInputStream fileIn;//reader from other client
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);
    JButton fileButton = new JButton("File Button");

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Return in the listener sends the textfield
     * contents to the server. Note however that the textfield is initially NOT
     * editable, and only becomes editable AFTER the client receives the
     * NAMEACCEPTED message from the server.
     */
    public Client() {
        textField.setEditable(false);
        messageArea.setEditable(false);
        fileButton.setVisible(false);
        JPanel send = new JPanel();
        send.add(textField);
        send.add(fileButton);
        frame.getContentPane().add(send, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.setTitle("DLSUsap");
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });

        fileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp"));
                chooser.addChoosableFileFilter(new FileNameExtensionFilter("*.txt", "txt"));
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File fileToSend = chooser.getSelectedFile();
                    if (fileToSend == null) {
                        return;
                    }
					else {
						//create a temporary thread get the file and send the file
						new Thread(new Runnable() {

							public void run() {
								try {


									byte[] data;

									long fileLength = fileToSend.length();
									if (fileLength < 0) {
										throw new IOException("Invalid file size");
									}

									//send information about file name and length of file
									String command = "\\file-" + getFileName(fileToSend.getAbsolutePath()) + "-" + fileLength + "\\";

									fileOut.writeUTF(command);

									DataInputStream fileReader = new DataInputStream(new FileInputStream(new File(fileToSend.getAbsolutePath())));//reads the file from users computer

									data = new byte[(int) (fileLength)];

									int read = fileReader.read(data);

									fileOut.write(data, 0, read);//send to other user
									

								} catch (FileNotFoundException fnfe) {
									//mainReference.error("Could not send file.");
								} catch (EOFException eofe) {
									//mainReference.error(eofe.getMessage());
								} catch (IOException ioe) {
								}
							}
						}).start();
					}
                }
            }
        });
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }
    private String getconnection() {
        return JOptionPane.showInputDialog(frame, "Choose a Host:", "Host name selection",
                JOptionPane.PLAIN_MESSAGE);
    }
    private String getip() {
        return JOptionPane.showInputDialog(frame, "Choose a IP:", "IP selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void run() throws IOException {
        try {
            serverAddress = getconnection();
            ip = Integer.parseInt(getip());
            var socket = new Socket(serverAddress, ip);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            dis = socket.getInputStream();
            dos = socket.getOutputStream();
            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("DlSUsap - " + line.substring(13));
                    textField.setEditable(true);
                    fileButton.setVisible(true);
                    frame.pack();
                } else if (line.startsWith("FILEPORT")) {
                    filePort = Integer.parseInt(line.substring(9));
					System.out.println(line.substring(9));
					//create the connection with the file server
					fileClient = new Socket(serverAddress, filePort);
					//get the file streams
					fileOut = new DataOutputStream(fileClient.getOutputStream());
					fileIn = new DataInputStream(fileClient.getInputStream());
					//create thread to detect and store incoming files
					new Thread(new fileReaderThread()).start();
					
                }else if (line.startsWith("MESSAGE")) {
                    messageArea.append(line.substring(8) + "\n");
                }
            }
        }
        catch (Exception e){
            System.out.println("FULL");
        }
        finally {
            frame.setVisible(false);
            frame.dispose();
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

	
    public String getFileName(String fileName) {
        int dirSplitIndex = fileName.lastIndexOf("/");
        if (dirSplitIndex == -1)//other slash for windows
        {
            dirSplitIndex = fileName.lastIndexOf("\\");
        }
        fileName = fileName.substring(dirSplitIndex + 1);

        return fileName;
    }

    public static void main(String[] args) throws Exception {
        var client = new Client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }


}
