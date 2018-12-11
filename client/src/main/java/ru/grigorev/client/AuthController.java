package ru.grigorev.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ru.grigorev.common.ConnectionSingleton;
import ru.grigorev.common.message.AuthMessage;
import ru.grigorev.common.message.MessageType;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Dmitriy Grigorev
 */
public class AuthController implements Initializable {
    public TextField login;
    public PasswordField password;
    public VBox authVBox;
    public Label label;
    private Stage primaryStage;
    private Scene authScene;
    private boolean isAuthorized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ConnectionSingleton.getInstance().init();
        initAuthLoop();
    }

    public void initAuthLoop() {
        System.out.println("initing AUTH loop");
        password.clear();
        Thread thread = new Thread(() -> {
            while (true) {
                Object received = ConnectionSingleton.getInstance().receiveMessage();
                if (received instanceof AuthMessage) {
                    AuthMessage authMessage = (AuthMessage) received;
                    if (authMessage.getType().equals(MessageType.AUTH_FAIL)) {
                        Platform.runLater(() -> {
                            label.setTextFill(Color.RED);
                            label.setText(authMessage.getMessage());
                        });
                    }
                    if (authMessage.getType().equals(MessageType.AUTH_OK)) {
                        Platform.runLater(() -> {
                            GUIhelper.showAlert(
                                    authMessage.getMessage(),
                                    "Welcome, " + login.getText() + "!",
                                    "Authorization success",
                                    Alert.AlertType.INFORMATION);
                            authVBox.getScene().getWindow().hide();
                            isAuthorized = true;
                            initMainWindow();
                        });
                        System.out.println("Breaking auth loop");
                        break;
                    }
                    if (authMessage.getType().equals(MessageType.DISCONNECTING)) {
                        System.out.println("Closing connection...");
                        Platform.exit();
                        ConnectionSingleton.getInstance().close();
                        break;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void signIn() {
        Platform.runLater(() -> {
            System.out.println(String.format("signing in - login: %s, Pass: %s", login.getText(), password.getText()));
            ConnectionSingleton.getInstance().sendAuthMessage(new AuthMessage(MessageType.SIGN_IN_REQUEST, login.getText(), password.getText()));
        });
    }

    public void signUp() {
        Platform.runLater(() -> {
            System.out.println(String.format("signing up - Login: %s, Pass: %s", login.getText(), password.getText()));
            ConnectionSingleton.getInstance().sendAuthMessage(new AuthMessage(MessageType.SIGN_UP_REQUEST, login.getText(), password.getText()));
        });
    }

    private void initMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI.fxml"));
            Parent root = loader.load();
            MainController mainController = loader.getController();
            mainController.setAuthController(this);
            mainController.setPrimaryStage(primaryStage);
            mainController.initClientMainLoop();
            Scene mainScene = new Scene(root, 700, 400);
            mainController.setAuthScene(mainScene);
            primaryStage.setTitle("GBCloud (" + login.getText() + ")");
            primaryStage.setScene(mainScene);
            primaryStage.getIcons().add(new Image("/icon.png"));
            primaryStage.setOnCloseRequest((c) -> {
                mainController.exit();
            });
            GUIhelper.setMainController(mainController);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public Scene getAuthScene() {
        return authScene;
    }

    public void setAuthScene(Scene authScene) {
        this.authScene = authScene;
    }
}
