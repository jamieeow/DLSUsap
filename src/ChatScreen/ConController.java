package ChatScreen;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ConController {


    @FXML Button btnConnect, btnExit;
    @FXML TextField txtfieldIPortNumber, txtfieldIPaddress, txtfieldName;


    public void initialize(){

    }

    public void handleSend() {
        new Client(txtfieldIPaddress.getText(), txtfieldIPortNumber.getText(),  txtfieldName.getText());
    }

}
