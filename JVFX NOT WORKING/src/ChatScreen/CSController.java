package ChatScreen;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.io.IOException;

public class CSController {


    @FXML Button btnImg, btnSend, btnLogout;
    @FXML TextField txtfieldInput;
    @FXML Label lblName;
    @FXML static TextFlow textflow;

    public void initialize(){

    }

    public void handleSend()  throws IOException {
        String msg = txtfieldInput.getText();
        if (!txtfieldInput.getText().isEmpty()) {
            Client.send(msg);
            txtfieldInput.clear();
           }
    }
    public static void addMessage(String message){
        textflow.getChildren().add(new Text(message));
    }

}
