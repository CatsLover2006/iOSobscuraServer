package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
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
            String appName = "";
            String bundleID = "";
            String version = "0.0";
            String minimumVersion = "0.0";
            String artwork = null;
            String developer = null;
            boolean usesMetaName = false;
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            boolean foundOther = false;
            String binaryName = "";
            while (entry != null) {
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
            if (!binaryName.isEmpty()) {
                while (entry != null) {
                    if (entry.getName().endsWith("/" + binaryName)) {
                        binary = Binary.parseBinary(zipExtractor);
                        break;
                    }
                    entry = zipExtractor.getNextEntry();
                }
                if (binary == null) {
                    connection.disconnect();
                    connection = (HttpURLConnection) url.openConnection();
                    zipExtractor = new ZipInputStream(connection.getInputStream());
                    entry = zipExtractor.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().endsWith("/" + binaryName)) {
                            binary = Binary.parseBinary(zipExtractor);
                            break;
                        }
                        entry = zipExtractor.getNextEntry();
                    }
                }
            } else {
                binary = Binary.fromJSON(new JSONObject());
            }
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
            app.addAppVersion(version, new App.VersionLink[]{new App.VersionLink(binary, url.toString())}, minimumVersion);
        } catch (Throwable e) {
            System.err.println(e);
        }
    }
}
