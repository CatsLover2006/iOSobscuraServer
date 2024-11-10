package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AppDownloader {
    public static void downloadAndAddApp(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                System.err.println("Not found");
                return;
            }
            long size = connection.getContentLengthLong();
            String appName = "";
            String bundleID = "nil";
            String version = "0.0";
            String minimumVersion = "0.0";
            String artwork = null;
            String developer = null;
            boolean usesMetaName = false;
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            boolean foundOther = false;
            String binaryName = "";
            String iconName = "";
            while (entry != null) {
                if (entry.getName().toLowerCase().contains("/watch/")) {
                    entry = zipExtractor.getNextEntry();
                    continue;
                }
                if (entry.getName().endsWith(".app/Info.plist")) {
                    NSDictionary parsedData = (NSDictionary) PropertyListParser.parse(zipExtractor);
                    for (String key : parsedData.allKeys()) {
                        switch (key) {
                            case "CFBundleDisplayName": {
                                if (appName.isEmpty())
                                    appName = String.valueOf(parsedData.get("CFBundleDisplayName"));
                                break;
                            }
                            case "CFBundleIdentifier": {
                                String str = String.valueOf(parsedData.get("CFBundleIdentifier"));
                                if (str.equals("null")) break;
                                bundleID = str;
                                break;
                            }
                            case "CFBundleVersion": {
                                String str = String.valueOf(parsedData.get("CFBundleVersion"));
                                if (str.equals("null")) break;
                                version = str;
                                break;
                            }
                            case "MinimumOSVersion": {
                                String str = String.valueOf(parsedData.get("MinimumOSVersion"));
                                if (str.equals("null")) break;
                                minimumVersion = str;
                                break;
                            }
                            case "CFBundleExecutable": {
                                String str = String.valueOf(parsedData.get("CFBundleExecutable"));
                                if (str.equals("null")) break;
                                binaryName = str;
                                break;
                            }
                            case "CFBundleIconFiles": {
                                if (iconName.isEmpty()) break;
                                try {
                                    NSArray icons = (NSArray) parsedData.get("CFBundleIconFiles");
                                    iconName = icons.objectAtIndex(0).toString();
                                    if (!iconName.endsWith(".png"))
                                        iconName += "@2x.png";
                                } catch (Throwable e) {
                                    // Do nothing
                                }
                                break;
                            }
                            case "CFBundleIcons": {
                                if (iconName.isEmpty()) break;
                                try {
                                    NSArray icons = (NSArray) ((NSDictionary) ((NSDictionary) parsedData.get("CFBundleIcons"))
                                            .get("CFBundlePrimaryIcon")).get("CFBundleIconFiles");
                                    iconName = icons.objectAtIndex(0).toString();
                                    if (!iconName.endsWith(".png"))
                                        iconName += "@2x.png";
                                } catch (Throwable e) {
                                    // Do nothing
                                }
                                break;
                            }
                            case "CFBundleIconFile": {
                                String str = String.valueOf(parsedData.get("CFBundleIcon"));
                                if (str.equals("null")) break;
                                iconName = str;
                                break;
                            }
                        }
                    }
                    if (foundOther) {
                        break;
                    }
                    foundOther = true;
                }
                if (entry.getName().endsWith("iTunesMetadata.plist")) {
                    NSDictionary parsedData = (NSDictionary) PropertyListParser.parse(zipExtractor);
                    for (String key : parsedData.allKeys()) {
                        switch (key) {
                            case "softwareVersionBundleId": {
                                String str = String.valueOf(parsedData.get("softwareVersionBundleId"));
                                if (str.equals("null")) break;
                                bundleID = str;
                                break;
                            }
                            case "bundleShortVersionString": {
                                String str = String.valueOf(parsedData.get("bundleShortVersionString"));
                                if (str.equals("null")) break;
                                version = str;
                                break;
                            }
                            case "itemName": {
                                String str = String.valueOf(parsedData.get("itemName"));
                                if (str.equals("null")) break;
                                appName = str;
                                usesMetaName = true;
                                break;
                            }
                            case "softwareIcon57x57URL": {
                                String str = String.valueOf(parsedData.get("softwareIcon57x57URL"));
                                if (str.equals("null")) break;
                                artwork = str;
                                break;
                            }
                            case "artistName": {
                                String str = String.valueOf(parsedData.get("artistName"));
                                if (str.equals("null")) break;
                                developer = str;
                                break;
                            }
                        }
                    }
                    if (foundOther) {
                        break;
                    }
                    foundOther = true;
                }
                entry = zipExtractor.getNextEntry();
            }
            Binary binary = null;
            BufferedImage iconImage = null;
            if (!binaryName.isEmpty() || !iconName.isEmpty()) {
                while (entry != null) {
                    if (entry.getName().toLowerCase().contains("/watch/")) {
                        entry = zipExtractor.getNextEntry();
                        continue;
                    }
                    if (!binaryName.isEmpty() && entry.getName().endsWith("/" + binaryName)) {
                        binary = Binary.parseBinary(zipExtractor);
                        if (iconImage != null) break;
                    }
                    if (!iconName.isEmpty() && entry.getName().endsWith("/" + iconName)) {
                        iconImage = ImageIO.read(zipExtractor);
                        if (binary != null) break;
                    }
                    entry = zipExtractor.getNextEntry();
                }
                if (binary == null || iconImage == null) {
                    connection.disconnect();
                    connection = (HttpURLConnection) url.openConnection();
                    zipExtractor = new ZipInputStream(connection.getInputStream());
                    entry = zipExtractor.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().toLowerCase().contains("/watch/")) {
                            entry = zipExtractor.getNextEntry();
                            continue;
                        }
                        if (!binaryName.isEmpty() && entry.getName().endsWith("/" + binaryName)) {
                            binary = Binary.parseBinary(zipExtractor);
                            if (iconImage != null) break;
                        }
                        if (!iconName.isEmpty() && entry.getName().endsWith("/" + iconName)) {
                            iconImage = ImageIO.read(zipExtractor);
                            if (binary != null) break;
                        }
                        entry = zipExtractor.getNextEntry();
                    }
                }
            } else {
                binary = Binary.fromJSON(new JSONObject());
            }
            System.out.println(iconImage);
            if (artwork == null && iconImage != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(iconImage, "jpeg", out);
                byte[] bytes = out.toByteArray();
                artwork = "data:image/jpeg;base64," + Base64.getEncoder().encode(bytes);
            }
            System.out.println(artwork);
            App app = AppList.getAppByBundleID(bundleID);
            if (app == null) {
                app = new App(appName, bundleID);
                AppList.addApp(app);
            }
            if (usesMetaName) {
                app.updateName(appName);
            }
            app.updateArtwork(version, artwork);
            app.updateDeveloper(version, developer);
            app.addAppVersion(version, new App.VersionLink[]{new App.VersionLink(binary, url.toString(), size)}, minimumVersion);
        } catch (Throwable e) {
            System.err.println(e);
        }
    }
}
