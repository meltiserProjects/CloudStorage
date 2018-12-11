package ru.grigorev.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.grigorev.common.ConnectionSingleton;
import ru.grigorev.common.Info;
import ru.grigorev.common.message.AuthMessage;
import ru.grigorev.common.message.Message;
import ru.grigorev.common.message.MessageType;
import ru.grigorev.common.utils.FileHandler;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class MainController implements Initializable {
    public ListView<String> serverListView;
    public ListView<String> clientListView;
    public Label mainLabel;

    private AuthController authController;
    private Scene authScene;
    private Stage primaryStage;
    private ObservableList<String> clientList = FXCollections.observableList(new ArrayList<>());
    private ObservableList<String> serverList = FXCollections.observableList(new ArrayList<>());
    private FileHandler fileHandler;
    private String currentClientDir;

    private BlockingQueue<Runnable> taskQueue;
    private ExecutorService singleThreadExecutor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        taskQueue = new LinkedBlockingQueue<>();
        singleThreadExecutor = new ThreadPoolExecutor(1, 1, 0L,
                TimeUnit.MILLISECONDS, taskQueue);
        fileHandler = new FileHandler();
        fileHandler.checkFolderExisting(Info.CLIENT_FOLDER_NAME);
        currentClientDir = Info.CLIENT_FOLDER_NAME;
        GUIhelper.initDragAndDropClientListView(clientListView, clientList);
        GUIhelper.initClientContextMenu(clientListView);
        GUIhelper.initServerContextMenu(serverListView);
        GUIhelper.initListViewIcons(clientListView);
        GUIhelper.initListViewIcons(serverListView);
        GUIhelper.runWatchServiceThread();
        refreshAll();
    }

    public void initClientMainLoop() {
        Thread thread = new Thread(() -> {
            System.out.println("Initing main loop");
            try {
                while (true) {
                    Object received = ConnectionSingleton.getInstance().receiveMessage();
                    if (received instanceof AuthMessage) {
                        AuthMessage authMessage = (AuthMessage) received;
                        if (authMessage.getType().equals(MessageType.SIGN_OUT_RESPONSE)) {
                            System.out.println("Breaking main loop");
                            break;
                        }
                        if (authMessage.getType().equals(MessageType.DISCONNECTING)) {
                            System.out.println("Closing connection...");
                            Platform.exit();
                            ConnectionSingleton.getInstance().close();
                            break;
                        }
                    }
                    if (received instanceof Message) {
                        Message message = (Message) received;
                        if (message.getType().equals(MessageType.ABOUT_FILE)) {
                            String context = String.format("Size: %s\nLast modified: %s",
                                    GUIhelper.getFormattedSize(message.getFileSize()),
                                    GUIhelper.getFormattedLastModified(message.getLastModified()));
                            Platform.runLater(() ->
                                    GUIhelper.showAlert(context, message.getFileName(), "About",
                                            Alert.AlertType.INFORMATION));
                        }
                        if (message.getType().equals(MessageType.FILE)) {
                            System.out.println(MessageType.FILE);

                            Files.write(Paths.get(currentClientDir + message.getFileName()),
                                    message.getByteArr(), StandardOpenOption.CREATE);
                            refreshClientsFilesList();
                            Platform.runLater(() -> mainLabel.setText(message.getFileName() + " downloaded!"));
                        }
                        if (message.getType().equals(MessageType.REFRESH_RESPONSE)) {
                            System.out.println(MessageType.REFRESH_RESPONSE);

                            refreshServerFilesList(message);
                        }
                        if (message.getType().equals(MessageType.FILE_PART)) {
                            Path path = Paths.get(currentClientDir + message.getFileName());
                            if (!fileHandler.isWriting()) fileHandler.startWriting(path);
                            fileHandler.continueWriting(message);
                            if (message.getCurrentPart() == message.getPartsCount())
                                Platform.runLater(() -> mainLabel.setText(message.getFileName() + " downloaded!"));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
        clientListView.setItems(FXCollections.observableArrayList());
        refreshClientsFilesList();
    }

    public void signOut() {
        ConnectionSingleton.getInstance().sendAuthMessage(new AuthMessage(MessageType.SIGN_OUT_REQUEST));
        primaryStage.close();
        authController.setAuthorized(false);
        authController.getPrimaryStage().setScene(authController.getAuthScene());
        authController.initAuthLoop();
        authController.getPrimaryStage().show();
    }

    private void sendServerRefreshFileListMessage() {
        ConnectionSingleton.getInstance().sendMessage(new Message(MessageType.REFRESH_REQUEST));
    }

    private void refreshServerFilesList(Message message) {
        Platform.runLater(() -> {
            serverList.clear();
            serverList.addAll(message.getListFileNames());
            serverListView.setItems(serverList);
            if (serverListView.getItems().isEmpty())
                serverListView.setPlaceholder(new Label("The storage is empty!"));
        });
    }

    public void refreshClientsFilesList() {
        Platform.runLater(() -> {
            try {
                clientListView.getItems().clear();
                if (!isClientRootFolder()) {
                    clientListView.getItems().add("..");
                }
                Files.list(Paths.get(currentClientDir))
                        .filter((f) -> Files.isDirectory(f))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> clientListView.getItems().add(o));
                Files.list(Paths.get(currentClientDir))
                        .filter((f) -> !Files.isDirectory(f))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> clientListView.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (clientListView.getItems().isEmpty())
                clientListView.setPlaceholder(new Label("Folder is empty!"));
        });
    }

    public void refreshAll() {
        refreshClientsFilesList();
        sendServerRefreshFileListMessage();
    }

    public void downloadFile(ActionEvent actionEvent) {
        String selectedItem = getSelectedAndClearSelection(serverListView);
        if (selectedItem == null) return;
        if (!isFileExisting(selectedItem, clientListView)) {
            ConnectionSingleton.getInstance().sendMessage(new Message(MessageType.FILE_REQUEST, selectedItem));
            Platform.runLater(() -> mainLabel.setText("Downloading " + selectedItem + "..."));
        }
    }

    private String getSelectedAndClearSelection(ListView<String> listView) {
        String selectedItem = listView.getSelectionModel().getSelectedItem();
        listView.getSelectionModel().clearSelection();
        return selectedItem;
    }

    public void sendFile(ActionEvent actionEvent) throws InterruptedException {
        String selectedItem = getSelectedAndClearSelection(clientListView);
        if (selectedItem == null) {
            return;
        }
        Path file = Paths.get(currentClientDir + selectedItem);
        System.out.println(file);
        if (Files.isDirectory(file)) return; //can't send directories now
        if (!isFileExisting(file.getFileName().toString(), serverListView)) {
            mainLabel.setText(mainLabel.getText() + "; " + selectedItem + " is next");
            taskQueue.put(() -> {
                try {
                    Platform.runLater(() -> mainLabel.setText("Sending " + selectedItem + "..."));
                    fileHandler.sendFile(ConnectionSingleton.getInstance(), file);
                    sendServerRefreshFileListMessage();
                    Platform.runLater(() -> mainLabel.setText(selectedItem + " uploaded!"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            singleThreadExecutor.submit(taskQueue.poll());
        }
    }

    public boolean isFileExisting(String fileName, ListView<String> listView) {
        if (listView.getItems().contains(fileName)) {
            Platform.runLater(() -> GUIhelper.showAlert(
                    "File is already exists!", null, "Warning!", Alert.AlertType.WARNING));
            return true;
        } else
            return false;
    }

    public void openFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        List<File> list = fileChooser.showOpenMultipleDialog(primaryStage);
        if (list != null) {
            for (File file : list) {
                try {
                    Files.copy(file.toPath(),
                            Paths.get(currentClientDir + file.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            refreshClientsFilesList();
        }
    }

    public void deleteFileClient(ActionEvent actionEvent) {
        String selectedItem = getSelectedAndClearSelection(clientListView);
        if (selectedItem == null) return;
        try {
            Files.delete(Paths.get(currentClientDir + selectedItem + "\\"));
            System.out.println("deleting " + currentClientDir + selectedItem + "\\");
        } catch (DirectoryNotEmptyException e) {
            GUIhelper.showAlert("The folder is not empty!", "Cannot delete folder", "Warning!",
                    Alert.AlertType.WARNING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshClientsFilesList();
    }

    public void handleClientMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String selected = clientListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if (clientListView.getSelectionModel().getSelectedItem().equals("..")) {
                String dirWithoutSlash = currentClientDir.substring(0, currentClientDir.length() - 1);
                //one level up directory
                currentClientDir = dirWithoutSlash.substring(0, dirWithoutSlash.lastIndexOf('\\') + 1);
                mainLabel.setText(currentClientDir);
                refreshClientsFilesList();
                return;
            }

            Path path = Paths.get(currentClientDir + "\\" + selected);
            if (Files.isDirectory(path)) {
                currentClientDir = path.toString() + "\\";
                mainLabel.setText(currentClientDir);
                refreshClientsFilesList();
            } else {
                try {
                    Desktop.getDesktop().open(path.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isClientRootFolder() throws IOException {
        return Files.isSameFile(Paths.get(Info.CLIENT_FOLDER_NAME), Paths.get(currentClientDir));
    }

    public void exit() {
        singleThreadExecutor.shutdown();
        ConnectionSingleton.getInstance().sendAuthMessage(new AuthMessage(MessageType.DISCONNECTING));
    }

    public void openLink(ActionEvent actionEvent) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/meltiser/GBCloud"));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void deleteFileServer(ActionEvent actionEvent) {
        String selectedItem = getSelectedAndClearSelection(serverListView);
        if (selectedItem == null) return;
        ConnectionSingleton.getInstance().sendMessage(new Message(MessageType.DELETE_FILE, selectedItem));
        sendServerRefreshFileListMessage();
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    public void setAuthScene(Scene authScene) {
        this.authScene = authScene;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public String getCurrentClientDir() {
        return currentClientDir;
    }
}
