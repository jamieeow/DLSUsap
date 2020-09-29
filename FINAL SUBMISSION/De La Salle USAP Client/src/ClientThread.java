import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;

public class ClientThread implements Runnable{
    
    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;
    MainForm main;
    StringTokenizer st;
    protected DecimalFormat df = new DecimalFormat("##,#00");
    
    public ClientThread(Socket socket, MainForm main){
        this.main = main;
        this.socket = socket;
        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[IOException]: "+ e.getMessage(), "Error", Color.RED.darker(), Color.RED.darker());
        }
    }


    @Override
    public void run() {
        try {
            while(!Thread.currentThread().isInterrupted()){
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                /** Get Message CMD **/
                String CMD = st.nextToken();
                switch(CMD){
                    case "CMD_MESSAGE":
                        String msg = "";
                        String frm = st.nextToken();
                        while(st.hasMoreTokens()){
                            msg = msg +" "+ st.nextToken();
                        }
                        main.appendMessage(msg, frm, Color.DARK_GRAY.darker(), Color.DARK_GRAY);
                        break;
                        
                    case "CMD_NO_PARTNER_MESSAGE":
                        JOptionPane.showMessageDialog(main, "No one else online", "Error" ,JOptionPane.ERROR_MESSAGE);
                        break;
                        
                    case "CMD_ONLINE":
                        Vector online = new Vector();
                        while(st.hasMoreTokens()){
                            String list = st.nextToken();
                            if(!list.equalsIgnoreCase(main.username)){
                                online.add(list);
                            }
                        }
                        main.appendOnlineList(online);
                        break;
                    
                        
                    //  Notify client there is a file he can accept or reject 
                    case "CMD_FILE_XD":  // Format:  CMD_FILE_XD [sender] [receiver] [filename]
                        String sender = st.nextToken();
                        String receiver = st.nextToken();
                        String fname = st.nextToken();
                        int confirm = JOptionPane.showConfirmDialog(main, "From: "+sender+"\nfilename: "+fname+"\nAccept file?");
                        if(confirm == 0){ // Accepts file 
                            /* Chooses where to save file   */
                            main.openFolder();
                            try {
                                dos = new DataOutputStream(socket.getOutputStream());
                                // Format:  CMD_SEND_FILE_ACCEPT [ToSender] [Message]
                                String format = "CMD_SEND_FILE_ACCEPT "+sender+" Accepted";
                                dos.writeUTF(format);
                                
                                /*  Creates filesharing socket that closes upon completion  */
                                Socket fSoc = new Socket(main.getMyHost(), main.getMyPort());
                                DataOutputStream fdos = new DataOutputStream(fSoc.getOutputStream());
                                fdos.writeUTF("CMD_SHARINGSOCKET "+ main.getMyUsername());
                                /*  Run Thread for this   */
                                new Thread(new ReceivingFileThread(fSoc, main)).start();
                            } catch (IOException e) {
                                System.out.println("[CMD_FILE_XD]: "+e.getMessage());
                            }
                        } else { // Client rejects sender request
                            try {
                                dos = new DataOutputStream(socket.getOutputStream());
                                // Format:  CMD_SEND_FILE_ERROR [ToSender] [Message]
                                String format = "CMD_SEND_FILE_ERROR "+sender+" User has declined your request!";
                                dos.writeUTF(format);
                            } catch (IOException e) {
                                System.out.println("[CMD_FILE_XD]: "+e.getMessage());
                            }
                        }                       
                        break;   
                        
                    case "CMD_SAVE_EXIT_LOGS":
                        main.openFolder();
                        String path = main.getMyDownloadFolder() + ".txt"; 
                        try (PrintWriter p = new PrintWriter(new FileOutputStream(path, true))) {
                        p.println(data.substring(19));
                        JOptionPane.showMessageDialog(main, "Logs Successfully Saved", "Success" ,JOptionPane.PLAIN_MESSAGE);
                    } catch (FileNotFoundException e1) { 
                    }
                        break;
                            
                        
                    default: 
                        main.appendMessage("[CMDException]: Order unknown "+ CMD, "CMDException", Color.RED.darker(), Color.RED.darker());
                    break;
                }
            }
        } catch(IOException e){
            main.appendMessage(" Connection lost!", "Error", Color.RED.darker(), Color.RED.darker());
        }
    }
}