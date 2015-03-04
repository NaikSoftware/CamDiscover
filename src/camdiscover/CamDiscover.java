/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package camdiscover;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 *
 * @author Naik
 */
public class CamDiscover extends Application {
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/camdiscover/main.fxml"));
        Scene scene = new Scene(loader.load());
        MainController controller = loader.getController();
        controller.setMainClass(this);
        
        primaryStage.setTitle("CamDiscover");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
