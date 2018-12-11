package ru.grigorev.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import ru.grigorev.common.ConnectionSingleton;
import ru.grigorev.common.Info;
import ru.grigorev.common.message.Message;
import ru.grigorev.common.message.MessageType;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;

/**
 * @author Dmitriy Grigorev
 */
public class GUIhelper {
    private static AuthController authController;
    private static MainController mainController;

    public static void showAlert(String info, String header, String title, Alert.AlertType type) {
        Alert alert = new Alert(type, info);
        alert.setHeaderText(header);
        alert.setTitle(title);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("/icon.png"));

        alert.showAndWait();
    }

    public static void initClientContextMenu(ListView<String> clientListView) {
        System.out.println("Initing client context menu");
        clientListView.setCellFactory(TextFieldListCell.forListView());
        ContextMenu contextMenu = new ContextMenu();
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> {
            String selected = clientListView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.equals("..")) return;
            long size = 0;
            FileTime lastModified = null;
            try {
                Path gottenFile = Paths.get(mainController.getCurrentClientDir() + selected);
                size = Files.size(gottenFile);
                lastModified = Files.getLastModifiedTime(gottenFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String context = String.format("Size: %s\nLast modified: %s",
                    GUIhelper.getFormattedSize(size),
                    GUIhelper.getFormattedLastModified(lastModified.toMillis()));
            GUIhelper.showAlert(context, selected, "About", Alert.AlertType.INFORMATION);
        });
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(event -> {
            String selected = clientListView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.equals("..")) return;
            Path gottenFile = Paths.get(mainController.getCurrentClientDir() + selected);
            String renamed = showInputDialog(selected, "Renaming...", "Enter new file name: ");
            if (renamed.equals("") || renamed.equals(selected)) {
                return;
            } else {
                try {
                    Files.move(gottenFile, Paths.get(mainController.getCurrentClientDir() + renamed));
                    mainController.refreshClientsFilesList();
                } catch (IOException e) {
                    GUIhelper.showAlert("File is already exists!", null, "Warning!",
                            Alert.AlertType.WARNING);
                }
            }
        });
        MenuItem createFolder = new MenuItem("Create folder");
        createFolder.setOnAction(event -> {
            String folderName = showInputDialog("New folder",
                    "New folder",
                    "Enter folder name: ");
            if (folderName.equals("")) {
                return;
            } else {
                try {
                    Files.createDirectory(Paths.get(mainController.getCurrentClientDir() + folderName));
                    mainController.refreshClientsFilesList();
                } catch (IOException e) {
                    GUIhelper.showAlert("Folder is already exists!", null, "Warning!",
                            Alert.AlertType.WARNING);
                }
            }
        });
        contextMenu.getItems().addAll(aboutItem, renameItem, createFolder);
        clientListView.setContextMenu(contextMenu);
    }

    public static String showInputDialog(String promptText, String title, String contentText) {
        TextInputDialog dialog = new TextInputDialog(promptText);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(contentText);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("/icon.png"));

        Optional<String> result = dialog.showAndWait();
        return result.orElse("");
    }

    public static void initServerContextMenu(ListView<String> serverListView) {
        System.out.println("Initing server context menu");
        serverListView.setCellFactory(TextFieldListCell.forListView());
        ContextMenu contextMenu = new ContextMenu();
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> {
            String selected = serverListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ConnectionSingleton.getInstance().sendMessage(new Message(MessageType.ABOUT_FILE, selected));
        });
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(event -> {
            String selected = serverListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            String renamed = showInputDialog(selected, "Renaming...", "Enter new file name: ");
            if (renamed.equals("") || renamed.equals(selected) || mainController.isFileExisting(renamed, serverListView)) {
                return;
            }
            Message renameMessage = new Message(MessageType.FIlE_RENAME, selected);
            renameMessage.setRename(renamed);
            ConnectionSingleton.getInstance().sendMessage(renameMessage);
        });
        contextMenu.getItems().addAll(aboutItem, renameItem);
        serverListView.setContextMenu(contextMenu);
    }

    /**
     * Honestly, this solution copypasted from one of students... But it's really brilliant!
     */
    public static String getFormattedSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String getFormattedLastModified(long lastModified) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(lastModified);
    }

    public static void initDragAndDropClientListView(ListView<String> listView, ObservableList<String> list) {
        listView.setOnDragOver(event -> {
            if (event.getGestureSource() != listView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        listView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    try {
                        Files.copy(file.toPath(), Paths.get(mainController.getCurrentClientDir() + file.getName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                success = true;
                listView.setItems(list);
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    public static void runWatchServiceThread() {
        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                System.out.println("Initing WatchServiceThread");
                Path pathToLocalStorage = Paths.get(Info.CLIENT_FOLDER_NAME);
                WatchService watchService = null;
                try {
                    watchService = pathToLocalStorage.getFileSystem().newWatchService();
                    pathToLocalStorage.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (true) {
                    WatchKey key = null;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (WatchEvent event : key.pollEvents()) {
                        try {
                            mainController.refreshClientsFilesList();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                    key.reset();
                }
            }
        };
        Thread wsThread = new Thread(task);
        wsThread.setDaemon(true);
        wsThread.start();
    }

    public static void initListViewIcons(ListView<String> listView) {
        final Image FILE = new Image("/file.png");
        final Image FOLDER = new Image("/folder.png");

        Platform.runLater(() -> {
            listView.setCellFactory(param -> new ListCell<>() {
                ImageView imageView = new ImageView();

                @Override
                public void updateItem(String name, boolean empty) {
                    super.updateItem(name, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (Files.isDirectory(Paths.get(mainController.getCurrentClientDir() + name)))
                            imageView.setImage(FOLDER);
                        else
                            imageView.setImage(FILE);
                        imageView.fitHeightProperty().set(17.0);
                        imageView.fitWidthProperty().set(14.0);
                        setText(name);
                        setGraphic(imageView);
                        if (name.equals(".."))
                            setGraphic(null);
                    }
                }
            });
        });
    }

    public static void setAuthController(AuthController authController) {
        GUIhelper.authController = authController;
    }

    public static void setMainController(MainController mainController) {
        GUIhelper.mainController = mainController;
    }
}
