package org.example;

// MainApp.java (Classe de démarrage JavaFX)
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger la mise en page définie dans le fichier FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MetricsView.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("HAI913I - Analyse Statique OO");
        primaryStage.setScene(new Scene(root, 1000, 750));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}