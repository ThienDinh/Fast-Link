/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fastlink;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIConnectionListener;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxSharedLink;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ThienDinh
 */
public class BoxAPI {

    private static final String CLIENT_ID = "lv95a75ottkn2iaqsmj52yfhs893r3ft";
    private static final String CLIENT_SECRET = "P8i0WEG4gBTpCGQu4Nttoqi3UVemveQP";
    // The API connection.
    private static BoxAPIConnection api;
    private static BoxAPI theBoxAPI;

    private BoxAPI() {
    }

    public static BoxAPI getInstance() {
        if (theBoxAPI == null) {
            theBoxAPI = new BoxAPI();
            // Add initialization stuffs.
        }
        return theBoxAPI;
    }

    private class Uploader implements Runnable {

        private final InputStream uploadingFile;
        private String urlOfuploadedFile;
        private String fileName;

        public Uploader(InputStream stream, String fileName) {
            uploadingFile = stream;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            BoxFolder rootFolder = BoxFolder.getRootFolder(api);
            BoxFile.Info uploadedFile = rootFolder.uploadFile(uploadingFile, fileName);
            BoxFile onBoxFile = uploadedFile.getResource();
            BoxSharedLink.Permissions permis = new BoxSharedLink.Permissions();
            permis.setCanPreview(true);
            permis.setCanDownload(true);
            BoxSharedLink link = onBoxFile.createSharedLink(BoxSharedLink.Access.DEFAULT, null, permis);
            urlOfuploadedFile = link.getURL();
            api.refresh();
            FastLink.linkTextField(this.urlOfuploadedFile);
        }
    }

    /**
     * Upload a file onto Box.com on a separate Thread.
     *
     * @param file2Upload a file.
     */
    public void uploadOntoBox(InputStream stream, String fileName) {
        // Using inner class Uploader.
        Runnable uploader = new Uploader(stream, fileName);
        Thread uploadingThread = new Thread(uploader);
        uploadingThread.run();
    }

    public void connectByAuthenCode(String authenticationCode) {
        api = new BoxAPIConnection("lv95a75ottkn2iaqsmj52yfhs893r3ft",
                "P8i0WEG4gBTpCGQu4Nttoqi3UVemveQP",
                authenticationCode);
        handleRefreshToken();
    }

    public void connectByAccessRefreshToken(String accessToken, String refreshToken) {
        api = new BoxAPIConnection("lv95a75ottkn2iaqsmj52yfhs893r3ft",
                "P8i0WEG4gBTpCGQu4Nttoqi3UVemveQP",
                accessToken, refreshToken);
        handleRefreshToken();
    }

    private void handleRefreshToken() {
        api.setAutoRefresh(false);
        if (api.needsRefresh()) {
            api.refresh();
        }
        api.addListener(new BoxAPIConnectionListener() {

            @Override
            public void onRefresh(BoxAPIConnection bapic) {
                System.out.println("===API got refreshed!===");
                System.out.println("Access token:" + api.getAccessToken() + ". Refresh token:" + api.getRefreshToken());
                FastLink.saveUserProfile(new File("tokens.log"));
                System.out.println("Access token:" + api.getAccessToken() + ". Refresh token:" + api.getRefreshToken());
                System.out.println("===API got refreshed===");
            }

            @Override
            public void onError(BoxAPIConnection bapic, BoxAPIException bapie) {
                System.out.println(bapie.getMessage());
            }
        });
    }

    public static String getLink() {
        String link = "";
        try {
            link = "https://app.box.com/api/oauth2/authorize?response_type=code"
                    + "&client_id=" + CLIENT_ID
                    + "&state=security_token" + URLEncoder.encode("%", "UTF-8") + CLIENT_SECRET;
        } catch (IOException ex) {
            Logger.getLogger(FastLink.class.getName()).log(Level.SEVERE, null, ex);
        }
        return link;
    }

    /**
     * Open the web browser to request user's authentication code.
     */
    public static void requestAuthenCode() {
        try {
            String link = "https://app.box.com/api/oauth2/authorize?response_type=code"
                    + "&client_id=" + CLIENT_ID
                    + "&state=security_token" + URLEncoder.encode("%", "UTF-8") + CLIENT_SECRET;
            Desktop.getDesktop().browse(new URI(link));
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(FastLink.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String toString() {
        return api.getAccessToken() + "\n" + api.getRefreshToken();
    }
}
