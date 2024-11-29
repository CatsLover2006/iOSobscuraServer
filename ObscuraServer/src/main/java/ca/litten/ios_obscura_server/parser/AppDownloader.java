package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.xml.sax.SAXParseException;

import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            String version = "";
            String minimumVersion = "1.2";
            String artwork = "";
            String developer = "";
            String buildVersion = "0.0";
            boolean usesMetaName = false;
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            boolean foundOther = false;
            String binaryName = "";
            String iconName = "";
            try {
                while (entry != null) {
                    if (entry.getName().toLowerCase().contains("/watch/")) {
                        entry = zipExtractor.getNextEntry();
                        continue;
                    }
                    if (entry.getName().toLowerCase().endsWith(".app/info.plist")) {
                        NSDictionary parsedData;
                        byte[] bytes = IOUtils.toByteArray(zipExtractor);
                        try {
                            parsedData = (NSDictionary) PropertyListParser.parse(bytes);
                        } catch (SAXParseException e) {
                            String decoded = new String(bytes, StandardCharsets.UTF_8);
                            if (decoded.startsWith("bplist"))
                                parsedData = (NSDictionary) BinaryPropertyListParser.parse(bytes);
                            else
                                parsedData = (NSDictionary) PropertyListParser.parse(decoded.substring(0, decoded.lastIndexOf(">") + 1).getBytes(StandardCharsets.UTF_8));
                        }
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
                                case "CFBundleShortVersionString": {
                                    String str = String.valueOf(parsedData.get("CFBundleShortVersionString"));
                                    if (str.equals("null")) break;
                                    version = str;
                                    break;
                                }
                                case "CFBundleVersion": {
                                    String str = String.valueOf(parsedData.get("CFBundleVersion"));
                                    if (str.equals("null")) break;
                                    buildVersion = str;
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
                                case "CFBundleIconFiles~ipad":
                                case "CFBundleIcons~ipad":
                                    if (parsedData.containsKey(key.split("~")[0])) break; // If not, overflow
                                case "CFBundleIconFiles":
                                case "CFBundleIcons": {
                                    if (!iconName.isEmpty()) break;
                                    try {
                                        NSArray icons = (NSArray) ((NSDictionary) ((NSDictionary) parsedData.get(key))
                                                .get("CFBundlePrimaryIcon")).get("CFBundleIconFiles");
                                        iconName = icons.objectAtIndex(0).toString();
                                    } catch (Throwable e) {
                                        try {
                                            NSArray icons = (NSArray) parsedData.get(key);
                                            iconName = icons.objectAtIndex(0).toString();
                                        } catch (Throwable ex) {
                                            // Do nothing
                                        }
                                    }
                                    if (iconName.isEmpty()) break;
                                    if (!iconName.endsWith(".png"))
                                        iconName += "@2x.png";
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
                    if (entry.getName().toLowerCase().endsWith("itunesmetadata.plist")) {
                        NSDictionary parsedData;
                        byte[] bytes = IOUtils.toByteArray(zipExtractor);
                        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                        try {
                            parsedData = (NSDictionary) PropertyListParser.parse(stream);
                        } catch (SAXParseException e) {
                            String decoded = new String(bytes, StandardCharsets.UTF_8);
                            if (decoded.startsWith("bplist"))
                                parsedData = (NSDictionary) BinaryPropertyListParser.parse(bytes);
                            else
                                parsedData = (NSDictionary) PropertyListParser.parse(decoded.substring(0, decoded.lastIndexOf(">") + 1).getBytes(StandardCharsets.UTF_8));
                        }
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
            } catch (EOFException e) {
                System.err.println("Unexpected end of ZIP file; continuing with parsing...");
            }
            if (version.isEmpty()) version = buildVersion;
            Binary binary = null;
            if (iconName.isEmpty()) iconName = "icon.png"; // Just in cased
            BufferedImage iconImage = null;
            iconName = iconName.toLowerCase();
            binaryName = binaryName.toLowerCase();
            try {
                while (entry != null) {
                    if (entry.getName().toLowerCase().contains("/watch/")) {
                        entry = zipExtractor.getNextEntry();
                        continue;
                    }
                    if (!binaryName.isEmpty() && entry.getName().toLowerCase().endsWith("/" + binaryName)) {
                        binary = Binary.parseBinary(zipExtractor);
                        if (iconImage != null || !artwork.isEmpty()) break;
                    }
                    if (artwork.isEmpty() && entry.getName().toLowerCase().endsWith("/" + iconName)) {
                        try {
                            iconImage = ImageIO.read(new ByteArrayInputStream(IOUtils.toByteArray(zipExtractor)));
                        } catch (IIOException e) {
                            System.err.println("Image error");
                        }
                        if (binary != null) break;
                    }
                    entry = zipExtractor.getNextEntry();
                }
            }  catch (EOFException e) {
                System.err.println("Unexpected end of ZIP file; continuing with parsing...");
            }
            if (binary == null || (artwork.isEmpty() && iconImage == null)) {
                connection.disconnect();
                connection = (HttpURLConnection) url.openConnection();
                zipExtractor = new ZipInputStream(connection.getInputStream());
                try {
                    entry = zipExtractor.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().toLowerCase().contains("/watch/")) {
                            entry = zipExtractor.getNextEntry();
                            continue;
                        }
                        if (!binaryName.isEmpty() && entry.getName().toLowerCase().endsWith("/" + binaryName)) {
                            binary = Binary.parseBinary(zipExtractor);
                            if (iconImage != null || !artwork.isEmpty()) break;
                        }
                        if (artwork.isEmpty() && entry.getName().toLowerCase().endsWith("/" + iconName)) {
                            try {
                                iconImage = ImageIO.read(new ByteArrayInputStream(IOUtils.toByteArray(zipExtractor)));
                            } catch (IIOException e) {
                                System.err.println("Image error");
                            }
                            if (binary != null) break;
                        }
                        entry = zipExtractor.getNextEntry();
                    }
                }  catch (EOFException e) {
                    System.err.println("Unexpected end of ZIP file; continuing with parsing...");
                }
            }
            if (artwork.isEmpty() && iconImage == null) {
                connection.disconnect();
                connection = (HttpURLConnection) url.openConnection();
                zipExtractor = new ZipInputStream(connection.getInputStream());
                entry = zipExtractor.getNextEntry();
                try {
                    while (entry != null) {
                        if (entry.getName().toLowerCase().endsWith("itunesartwork")) {
                            try {
                                iconImage = ImageIO.read(new ByteArrayInputStream(IOUtils.toByteArray(zipExtractor)));
                            } catch (IIOException e) {
                                System.err.println("Image error");
                            }
                            break;
                        }
                        entry = zipExtractor.getNextEntry();
                    }
                } catch (EOFException e) {
                    System.err.println("Unexpected end of ZIP file; continuing with parsing...");
                }
            }
            if (binary == null) {
                binary = Binary.fromJSON(new JSONObject());
            }
            if (artwork.isEmpty() && iconImage != null) {
                ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality((float) Math.min(0.1 + (0.918 * Math.exp(-0.0131 * iconImage.getWidth())), 1.0));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                jpgWriter.setOutput(new MemoryCacheImageOutputStream(out));
                BufferedImage image;
                if (iconImage.getTransparency() == Transparency.OPAQUE) {
                    image = iconImage;
                } else {
                    image = new BufferedImage(iconImage.getWidth(), iconImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = image.createGraphics();
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
                    g2d.drawImage(iconImage, 0, 0, null);
                    g2d.dispose();
                }
                jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
                jpgWriter.dispose();
                byte[] bytes = out.toByteArray();
                artwork = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
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
            app.addAppVersion(version, new App.VersionLink[]{new App.VersionLink(binary, url.toString(), buildVersion, size)}, minimumVersion);
        } catch (Throwable e) {
            System.err.println(e);
        }
    }
}
