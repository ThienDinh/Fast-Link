/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fastlink;

import com.box.sdk.BoxAPIException;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.imageio.ImageIO;

/**
 * This class represent the main UI.
 *
 * @author ThienDinh
 */
public class FastLink extends Application {

    // The Singleton BoxAPI
    private static BoxAPI theBoxAPI;
    private static MediaPlayer player;
    private static Dimension position;

    /**
     * Display the URL of a file has just been uploaded.
     *
     * @param link a link to be put into the text field.
     */
    public static void linkTextField(String link) {
        Stage stage = new Stage();
        TextField linkField = new TextField(link);
        linkField.setAlignment(Pos.CENTER);
        linkField.setTooltip(new Tooltip("Hit Enter to quit!"));
        linkField.setOnAction(e -> {
            stage.close();
        });
        linkField.setFont(new Font(14));
        linkField.setPrefColumnCount(40);
        //linkField.setPadding(new Insets(5));
        Scene scene = new Scene(linkField, linkField.getPrefWidth(), 50);
        stage.setScene(scene);
        stage.initStyle(StageStyle.UTILITY);
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    private static void runMusic() {
        File song = new File("./lib/media/Fireflies.mp3");
        System.out.println(song.toURI().toString());
        Media music = new Media(song.toURI().toString());
        player = new MediaPlayer(music);
        player.setAutoPlay(true);
        player.cycleCountProperty().setValue(MediaPlayer.INDEFINITE);
    }

    /**
     * This is where program runs.
     *
     * @param primaryStage
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public void start(Stage primaryStage) throws FileNotFoundException, MalformedURLException, IOException, URISyntaxException {
        position = Toolkit.getDefaultToolkit().getScreenSize();
        // Run music.
        runMusic();
        // Modify api constructor to make it work in general.        
        // Display
        Label contentArea = new Label();
        InputStream icon = new FileInputStream(new File("./lib/media/fastlinkSymbol.png"));
        Image bgImage = new Image(icon);
        ImageView bgView = new ImageView(bgImage);
        //bgView.setOpacity(0.8);
        contentArea.setWrapText(true);

        bgView.setOnDragOver(e -> {
            Dragboard board = e.getDragboard();
            //If it is not called during the event delivery 
            //or if none of the passed transfer modes is supported by gesture source,
            //then the potential drop target is not considered to be an actual drop target.
            if (board.hasFiles() || board.hasImage() || board.hasString()) {
                // If board has files or image or string, 
                // passing transfer modes it is willing to accept.
                e.acceptTransferModes(TransferMode.COPY);
            }
            //System.out.println(e.getTransferMode());
            //System.out.println("Something is being dragged over me." + (counter++));
            e.consume();
        });

        bgView.setOnDragDropped(e -> {
            Dragboard board = e.getDragboard();
            System.out.println("File: " + board.hasFiles()
                    + ". Image: " + board.hasImage()
                    + ". String: " + board.getString());
            //Proccess available files.
            if (board.hasFiles()) {
                try {
                    List<File> filesOnBoard = board.getFiles();
                    File file = filesOnBoard.get(0);
                    FileInputStream input = new FileInputStream(file);
                    theBoxAPI.uploadOntoBox(input, file.getName());
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(FastLink.class.getName()).log(Level.SEVERE, null, ex);
                }
            } // Currently the program can't recognize image files.
            else if (board.hasImage()) {
                Image img = board.getImage();

                PixelReader pixelReader = img.getPixelReader();
                // Write snapshot to file system as a .png image
                LocalDateTime timeStamp = LocalDateTime.now();
                String fileName = "[Image]" + String.format("[%d%s%d][%d.%d.%d].png",
                        timeStamp.getYear(), timeStamp.getMonth(), timeStamp.getDayOfMonth(),
                        timeStamp.getHour(), timeStamp.getMinute(), timeStamp.getSecond());
                WritableImage writableImage = new WritableImage(
                        (int) img.getWidth(),
                        (int) img.getHeight());
                PixelWriter pixelWriter = writableImage.getPixelWriter();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (int readY = 0; readY < img.getHeight(); readY++) {
                    for (int readX = 0; readX < img.getWidth(); readX++) {
                        Color color = pixelReader.getColor(readX, readY);
                        pixelWriter.setColor(readX, readY, color);
                    }
                }
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null),
                            "png", output);
                    output.close();
                    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
                    theBoxAPI.uploadOntoBox(input, fileName);
                } catch (IOException ex) {
                    System.out.println("[Write file or upload file unsuccessfully!] " + ex.getMessage());
                }
            } // Process clipboard with text.
            else if (board.hasString()) {
                // Process available string
                String stringOnBoard = board.getString();
                LocalDateTime timeStamp = LocalDateTime.now();
                String fileName = "[Text]" + String.format("[%d%s%d][%d.%d.%d].txt",
                        timeStamp.getYear(), timeStamp.getMonth(), timeStamp.getDayOfMonth(),
                        timeStamp.getHour(), timeStamp.getMinute(), timeStamp.getSecond());
                byte[] output = stringOnBoard.getBytes();
                ByteArrayInputStream input = new ByteArrayInputStream(output);
                theBoxAPI.uploadOntoBox(input, fileName);
            }
            boolean transferSuccess = true;

            e.setDropCompleted(transferSuccess);
            // ConsumeDragDropped event
            e.consume();
        });

        // DragEnter and DragExited more suitable to show a visual effect
//        contentArea.setOnDragEntered(e -> {
//            Dragboard board = e.getDragboard();
//            if (board.hasFiles()) {
//                contentArea.setText("Drop to upload the file!");
//            } else if (board.hasImage()) {
//                contentArea.setText("Drop to upload the image!");
//            } else if (board.hasString()) {
//                contentArea.setText("Drop to upload the text!");
//            }
//            e.consume();
//        });
//
//        contentArea.setOnDragExited(e -> {
//            contentArea.setText("Drag something?");
//            e.consume();
//        });
        File tokenFile = new File("tokens.log");
        contentArea.alignmentProperty().setValue(Pos.CENTER);
        Group g = new Group();
        g.getChildren().add(bgView);
        Scene sceneDragDrop = new Scene(g, bgImage.getWidth(), bgImage.getHeight(), Color.TRANSPARENT);

        theBoxAPI = BoxAPI.getInstance();
        boolean showPrimaryStage = false;

        // This will be called first.
        if (!tokenFile.exists()) {
            Stage loginStage = new Stage();
            Button loginButton = new Button("Log in");
            TextField authCode = new TextField();
            authCode.setPrefColumnCount(50);
            authCode.setAlignment(Pos.CENTER);
            VBox box = new VBox();
            box.setAlignment(Pos.TOP_CENTER);
            //box.getChildren().addAll(authCode, loginButton);
            loginButton.setOnAction(e -> {
                String authenCode = authCode.getText();
                theBoxAPI.connectByAuthenCode(authenCode);
                saveUserProfile(tokenFile);
                primaryStage.show();
                loginStage.close();
            });
            box.getChildren().addAll(authCode, loginButton);
            box.paddingProperty().set(new Insets(20));
            Scene sceneCode = new Scene(box);
            loginStage.initStyle(StageStyle.UTILITY);
            loginStage.setScene(sceneCode);
            loginStage.setAlwaysOnTop(true);
            loginStage.setTitle("FastLink by tdinhcs@gmail.com");
            loginStage.show();
            BoxAPI.requestAuthenCode();
        } // 
        else {
            theBoxAPI = BoxAPI.getInstance();
            // Read
            loadUserProfile(tokenFile);
            showPrimaryStage = true;
            // Print out welcom User
            try {
                //BoxUser.Info userInfo = BoxUser.getCurrentUser(api).getInfo();
                //System.out.format("Welcome, %s <%s>!\n\n", userInfo.getName(), userInfo.getLogin());
            } catch (BoxAPIException ex) {
                System.out.println("Error occurs with Api token!");
            }
        }
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(sceneDragDrop);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setOpacity(1);
        primaryStage.setX(position.width - bgImage.getWidth() * 1.5);
        primaryStage.setY(position.height - bgImage.getHeight() * 1.5);
        if(showPrimaryStage) primaryStage.show();
    }

    /**
     * Load user profile and tokens.
     *
     * @param tokenFile a file to be containing tokens.
     */
    private void loadUserProfile(File tokenFile) {
        try {
            Scanner reader = new Scanner(tokenFile);
            String accessToken = reader.nextLine().trim();
            String refreshToken = reader.nextLine().trim();
            reader.close();
            theBoxAPI.connectByAccessRefreshToken(accessToken, refreshToken);

        } catch (FileNotFoundException ex) {
            System.out.println("Cannot read tokens.");
        }
    }

    /**
     * Save tokens to file.
     *
     * @param tokenFile a file to write tokens to.
     */
    public static void saveUserProfile(File tokenFile) {
        try {
            PrintWriter writer = new PrintWriter(tokenFile);
            writer.println(theBoxAPI);
            writer.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Cannot save tokens.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
