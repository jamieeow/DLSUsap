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
import java.io.BufferedInputStream;
import java.util.StringTokenizer;
import java.io.FileOutputStream;

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
	String username;
    private static PrintWriter out;
	private static Socket fileClient;
    private static int filePort;
    private static DataOutputStream fileOut;//writer to other client
    private static DataInputStream fileIn;//reader from other client
    static JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);
    JButton fileButton = new JButton("File Button");
	private static final int BUFFER_SIZE = 100;
    

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

									long fileLength = fileToSend.length();
									if (fileLength < 0) {
										throw new IOException("Invalid file size");
									}
									//send information about file name and length of file
									String command = username + "#\\file-" + getFileName(fileToSend.getAbsolutePath()) + "-" + fileLength + "\\";
									fileOut.writeUTF(command);
									
									/** Create an stream **/
									InputStream input = new FileInputStream(fileToSend);
									OutputStream output = fileClient.getOutputStream();
									// Read file
									BufferedInputStream bis = new BufferedInputStream(input);
									/** Creates a place to store file **/
									byte[] buffer = new byte[BUFFER_SIZE];
									int count, percent = 0;
									while((count = bis.read(buffer)) > 0){
										output.write(buffer, 0, count);
									}
									/* Update the AttachmentForm GUI*/
									System.out.println("File has been sent!");
									//JOptionPane.showMessageDialog(form, "File has been sent!", "Success", JOptionPane.INFORMATION_MESSAGE);
									/* Close the send file */
									output.flush();
									output.close();
									System.out.println("File has been sent!");									

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
            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
					username = getName();
                    out.println(username);
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
		protected StringTokenizer st;
        public void run() {
            try {
                //get the original file command from the user
                String data = fileIn.readUTF();
				st = new StringTokenizer(data);
				String filename = st.nextToken();
                int filesize = Integer.parseInt(st.nextToken());
                String consignee = st.nextToken(); // Get the Sender Username
				int result = JOptionPane.showConfirmDialog(frame,"Do yo want a file? from " + consignee, "Swing Tester",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(result == JOptionPane.YES_OPTION){
                    //main.setMyTitle("Loading File....");
                    System.out.println("Loading File....");
                    System.out.println("From: "+ consignee);
					JFileChooser chooser = new JFileChooser();
					//Insert File chooser
					if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
						File fileToSave = chooser.getSelectedFile();
						if (fileToSave == null) {
                        return;
						}
						else {
						String path =  fileToSave.getAbsolutePath() + filename;   
						/*  Creat Stream   */
						FileOutputStream fos = new FileOutputStream(path);
						InputStream input = fileClient.getInputStream();                                
						/*  Monitor Progress   */
						//ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(main, "Downloading file please wait...", input);
						/*  Buffer   */
						BufferedInputStream bis = new BufferedInputStream(input);
						/**  Create a temporary file **/
						byte[] buffer = new byte[BUFFER_SIZE];
						int count, percent = 0;
						while((count = bis.read(buffer)) != -1){
							//percent = percent + count;
							//int p = (percent / filesize);
							//main.setMyTitle("Downloading File  "+ p +"%");
							fos.write(buffer, 0, count);
						}
						fos.flush();
						fos.close();
						JOptionPane.showMessageDialog(null, "FIle downloaded \n'"+ path +"'");
						System.out.println("Saved in: "+ path);
						}
					}
				}else if (result == JOptionPane.NO_OPTION){
					out.println("File Transfer Failed");
				}else {
					out.println("File Transfer Failed");
				}
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
