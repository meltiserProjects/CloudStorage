package ru.grigorev.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ru.grigorev.common.ConnectionSingleton;
import ru.grigorev.common.message.AuthMessage;
import ru.grigorev.common.message.MessageType;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Authorization.fxml"));
        Parent root = loader.load();
        AuthController authController = loader.getController();
        Scene authScene = new Scene(root, 400, 200);
        authController.setAuthScene(authScene);
        authController.setPrimaryStage(primaryStage);
        primaryStage.setTitle("GBCloud");
        primaryStage.setScene(authScene);
        primaryStage.getIcons().add(new Image("/icon.png"));
        primaryStage.setOnCloseRequest((c) -> {
            ConnectionSingleton.getInstance().sendAuthMessage(new AuthMessage(MessageType.DISCONNECTING));
        });
        GUIhelper.setAuthController(authController);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
