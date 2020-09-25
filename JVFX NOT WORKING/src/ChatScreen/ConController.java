package ChatScreen;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class ConController {


    @FXML Button btnConnect, btnExit;
    @FXML TextField txtfieldIPortNumber, txtfieldIPaddress, txtfieldName;


    public void initialize(){

    }

    public void handleSend() throws Exception{
        Client.createClient(txtfieldIPaddress.getText(), txtfieldIPortNumber.getText(),  txtfieldName.getText());
    }
}
