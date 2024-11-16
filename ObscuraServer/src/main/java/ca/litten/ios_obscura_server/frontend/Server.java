package ca.litten.ios_obscura_server.frontend;

import ca.litten.ios_obscura_server.Main;
import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.parser.CPUarch;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {
    private final HttpServer server;
    private static final HttpServerProvider provider = HttpServerProvider.provider();
    private static final Random rand = new Random();
    private static final byte[] searchIcon;
    private static final byte[] searchIcon7;
    private static final byte[] favicon;
    private static final byte[] mainicon;
    private static final byte[] icon32;
    private static final byte[] icon16;
    private static final byte[] iconMask7;
    private static long lastReload = 0;
    public static boolean allowReload = false;
    private static String serverName = "localhost";
    private static String donateURL = "";
    private static int port;
    
    static {
        try {
            File file = new File("searchIcon.jpg");
            FileInputStream search = new FileInputStream(file);
            searchIcon = new byte[Math.toIntExact(file.length())];
            search.read(searchIcon);
            search.close();
            file = new File("searchIcon7.jpg");
            FileInputStream search7 = new FileInputStream(file);
            searchIcon7 = new byte[Math.toIntExact(file.length())];
            search7.read(searchIcon7);
            search7.close();
            file = new File("iconMask7.svg");
            FileInputStream mask7 = new FileInputStream(file);
            iconMask7 = new byte[Math.toIntExact(file.length())];
            mask7.read(iconMask7);
            mask7.close();
            file = new File("favicon.ico");
            FileInputStream fav = new FileInputStream(file);
            favicon = new byte[Math.toIntExact(file.length())];
            fav.read(favicon);
            fav.close();
            file = new File("icon.png");
            FileInputStream icon = new FileInputStream(file);
            mainicon = new byte[Math.toIntExact(file.length())];
            icon.read(mainicon);
            icon.close();
            file = new File("icon16.png");
            FileInputStream icon16f = new FileInputStream(file);
            icon16 = new byte[Math.toIntExact(file.length())];
            icon16f.read(icon16);
            icon16f.close();
            file = new File("icon32.png");
            FileInputStream icon32f = new FileInputStream(file);
            icon32 = new byte[Math.toIntExact(file.length())];
            icon32f.read(icon32);
            icon32f.close();
            file = new File("config.json");
            FileReader reader = new FileReader(file);
            StringBuilder out = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while (reader.ready()) {
                read = reader.read(buf);
                for (int i = 0; i < read; i++)
                    out.append(buf[i]);
            }
            JSONObject object = new JSONObject(out.toString());
            serverName = object.getString("host");
            donateURL = object.getString("donate_url");
            port = object.getInt("port");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String iOS7mask = "-webkit-mask-image:url(\"/getIconMask7\");-webkit-mask-size:cover;mask-image:url(\"/getIconMask7\");mask-size:cover;";

    public Server() throws IOException {
        lastReload = System.currentTimeMillis();
        server = provider.createHttpServer(new InetSocketAddress(port), -1);
        server.createContext("/").setHandler(exchange -> {
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            if (!(exchange.getRequestURI().toString().equals("/") || exchange.getRequestURI().toString().isEmpty())) {
                byte[] bytes = ErrorPages.general404.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                return;
            }
            StringBuilder out = new StringBuilder();
            out.append(Templates.generateBasicHeader("iOS Obscura Locator"))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div><center><strong>iPhoneOS Obscura Locator Homepage</strong></center></div></div><div><div><form action=\"searchPost\"><input type\"text\" name=\"search\" value=\"\" style=\"-webkit-appearance:none;border-bottom:1px solid #999\" placeholder=\"Search\"><button style=\"float:right;background:none\" type=\"submit\"><img style=\"height:18px;border-radius:50%\" src=\"/searchIcon\"></button></form></div></div></fieldset><label>Some Apps</label><fieldset>");
            List<App> apps = AppList.listAppsThatSupportVersion(iOS_ver);
            App app;
            int random;
            int s = apps.size();
            for (int i = 0; i < Math.min(20, s); i++) {
                random = rand.nextInt(apps.size());
                app = apps.remove(random);
                out.append("<a style=\"height:77px\" href=\"getAppVersions/").append(app.getBundleID())
                        .append("\"><div><div style=\"height:77px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px\" src=\"getAppIcon/")
                    .append(app.getBundleID()).append("\"><center style=\"line-height:57px\">").append(cutStringTo(app.getName(), 15))
                        .append("</center></div></div></a>");
            }
            out.append("</fieldset><fieldset><a href=\"https://github.com/CatsLover2006/iOSobscuraServer\"><div><div>Check out the Github</div></div></a>");
            if (!donateURL.isEmpty())
                out.append("<a href=\"").append(donateURL).append("\"><div><div>Donate to this instance</div></div></a>");
            out.append("</fieldset></panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getCSS").setHandler(exchange -> {
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            boolean macOS_connection = userAgent.contains("Macintosh");
            StringBuilder out = new StringBuilder();
            String styleVariant = "3163da6b7950852a03d31ea77735f4e1d2ba6699";
            String radius = "border-radius:15.625%;";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                String ver = split2[split2.length - 1].replace("_", ".");
                if (App.isVersionLater("7.0", ver)) {
                    styleVariant = "c1ff8b8b33e0b3de6657c943de001d1aff84d634";
                    radius = iOS7mask;
                }
            }
            if (macOS_connection) {
                String[] split1 = userAgent.split("AppleWebKit");
                String[] split2 = split1[0].split("\\)")[0].split(" ");
                String ver = split2[split2.length - 1].replace("_", ".");
                if (App.isVersionLater("10.10", ver)) {
                    styleVariant = "c1ff8b8b33e0b3de6657c943de001d1aff84d634";
                    radius = iOS7mask;
                }
            }
            out.append("@import url(\"https://cydia.saurik.com/cytyle/style-")
                    .append(styleVariant).append(".css\");@import url(\"http://cydia.saurik.com/cytyle/style-")
                    .append(styleVariant).append(".css\");img{").append(radius).append("}");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            outgoingHeaders.set("Content-Type", "text/css; charset=utf-8");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getHeader").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            out.append("<!DOCTYPE html>\n<html><body><ol>");
            for (String key : incomingHeaders.keySet()) {
                out.append("<li>").append(key).append("<ol>");
                for (String val : incomingHeaders.get(key)) {
                    out.append("<li>").append(val).append("</li>");
                }
                out.append("</ol></li>");
            }
            out.append("</ol></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getAppIcon/").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            outgoingHeaders.set("Cache-Control", "max-age=1800,immutable");
            if (app == null || app.getArtworkURL().isEmpty()) {
                outgoingHeaders.set("Location", "/icon");
            } else if (app.getArtworkURL().startsWith("data")) {
                String[] relevantData = app.getArtworkURL().split(";");
                outgoingHeaders.set("Content-Type", relevantData[0].split(":")[1]);
                byte[] data = Base64.getDecoder().decode(relevantData[1].split(",")[1]);
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
                exchange.close();
                return;
            } else if (app.getArtworkURL().startsWith("http")) {
                outgoingHeaders.set("Location", app.getArtworkURL());
            }
            exchange.sendResponseHeaders(308, 0);
            exchange.close();
        });
        server.createContext("/getAppVersions/").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                byte[] bytes = ErrorPages.app404.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                return;
            }
            out.append(Templates.generateBasicHeader(app.getName()))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div style=\"height:57px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px\" src=\"/getAppIcon/")
                    .append(app.getBundleID()).append("\"><strong style=\"padding:.5em 0;line-height:57px\"><center>").append(cutStringTo(app.getName(), 20))
                    .append("</center></strong></div></div><div><div>").append(app.getDeveloper())
                    .append("</div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset><label>Versions</label><fieldset>");
            String[] versions = app.getSupportedAppVersions(iOS_ver);
            if (versions.length == 0) {
                out.append("<div><div>No Known Versions</div></div>");
            } else for (String version : versions) {
                out.append("<a href=\"/getAppVersionLinks/").append(app.getBundleID()).append("/").append(version)
                        .append("\"><div><div>").append(version).append("</div></div></a>");
            }
            out.append("</fieldset></panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/generateInstallManifest/").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                outgoingHeaders.set("Content-Type", "text/html");
                exchange.sendResponseHeaders(404, ErrorPages.app404.length());
                exchange.getResponseBody().write(ErrorPages.app404.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            outgoingHeaders.set("Content-Type", "text/xml");
            App.VersionLink[] versions = app.getLinksForVersion(splitURI[3]);
            NSDictionary root = new NSDictionary();
            NSDictionary item = new NSDictionary();
            NSDictionary[] asset = new NSDictionary[2];
            NSDictionary metadata = new NSDictionary();
            asset[0] = new NSDictionary();
            asset[0].put("kind", "software-package");
            asset[0].put("url", versions[Integer.parseInt(splitURI[4])].getUrl());
            asset[1] = new NSDictionary();
            asset[1].put("kind", "display-image");
            asset[1].put("needs-shine", false);
            asset[1].put("url", "https://" + serverName + "/getAppIcon/" + app.getBundleID());
            metadata.put("bundle-identifier", app.getBundleID());
            metadata.put("bundle-version", splitURI[3]);
            metadata.put("kind", "software");
            metadata.put("title", app.getName());
            item.put("assets", new NSArray(asset));
            item.put("metadata", metadata);
            root.put("items", new NSArray(new NSDictionary[] {item}));
            byte[] bytes = root.toXMLPropertyList().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getAppVersionLinks/").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                byte[] bytes = ErrorPages.app404.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                return;
            }
            out.append(Templates.generateBasicHeader(app.getName()))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div style=\"height:57px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px\" src=\"/getAppIcon/")
                    .append(app.getBundleID()).append("\"><strong style=\"padding:.5em 0;line-height:57px\"><center>").append(cutStringTo(app.getName(), 20))
                    .append("</center></strong></div></div><div><div>").append(app.getDeveloper())
                    .append("</div></div><div><div style=\"overflow:auto\">Version ").append(splitURI[3])
                    .append("<span style=\"float:right\">Requires iOS ").append(app.getCompatibleVersion(splitURI[3]))
                    .append("</span></div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset>");
            App.VersionLink[] versions = app.getLinksForVersion(splitURI[3]);
            for (int i = 0; i < versions.length; i++) {
                out.append("<label>#").append(i + 1).append(", ").append(versions[i].getUrl().split("//")[1].split("/")[0]);
                if (versions[i].getUrl().split("//")[1].split("/")[0].contains("archive.org"))
                    out.append(", ").append(versions[i].getUrl().split("//")[1].split("/")[2]);
                if (versions[i].getUrl().startsWith("https"))
                    out.append(", SSL");
                if (versions[i].getBinary() != null) {
                    HashMap<CPUarch, Boolean> supportMatrix = versions[i].getBinary().getEncryptionMatrix();
                    if (!supportMatrix.keySet().isEmpty()) {
                        out.append("<br>Supports: ");
                        for (CPUarch arch : supportMatrix.keySet()) {
                            out.append(arch.name());
                            if (supportMatrix.get(arch)) {
                                out.append(" (Encrypted)");
                            }
                            out.append(", ");
                        }
                        out.deleteCharAt(out.length() - 2);
                    } else {
                        out.append("<br>Mach-O Error");
                    }
                } else {
                    out.append("<br>Mach-O Error");
                }
                out.append("</label><fieldset><a href=\"").append(versions[i].getUrl())
                        .append("\"><div><div>Direct Download <small style=\"font-size:x-small\">").append(versions[i].getSize())
                        .append("</small></div></div></a>");
                if (iOS_connection || userAgent.contains("Macintosh"))
                    out.append("<a href=\"itms-services://?action=download-manifest&url=https://").append(serverName)
                            .append("/generateInstallManifest/").append(splitURI[2]).append("/").append(splitURI[3]).append("/").append(i)
                            .append("\"><div><div>iOS Direct Install <small style=\"font-size:x-small\">Requires AppSync</small></div></div></a>");
                if (iOS_connection) {
                    if ((App.isVersionLater("14.0", iOS_ver) && App.isVersionLater(iOS_ver, "16.6.1")) || (iOS_ver.startsWith("17.0") && iOS_ver.endsWith(".0")))
                        out.append("<a href=\"apple-magnifier://install?url=").append(versions[i].getUrl())
                                .append("\"><div><div>Install with TrollStore</div></div></a>");
                    if (App.isVersionLater("12.2", iOS_ver))
                        out.append("<a href=\"altstore://install?url=").append(versions[i].getUrl())
                                .append("\"><div><div>Install with AltStore Classic</div></div></a>");
                    if (App.isVersionLater("14.0", iOS_ver))
                        out.append("<a href=\"sidestore://install?url=").append(versions[i].getUrl())
                                .append("\"><div><div>Install with SideStore</div></div></a>");
                }
                out.append("</fieldset>");
            }
            out.append("</panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/sitemap").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            out.append("https://").append(serverName).append("/\n");
            outgoingHeaders.set("Content-Type", "text/plain");
            for (App app : AppList.searchApps("")) {
                out.append("https://").append(serverName).append("/getAppVersions/").append(app.getBundleID()).append("\n");
                for (String version : app.getSupportedAppVersions("99999999"))
                    out.append("https://").append(serverName).append("/getAppVersionLinks/").append(app.getBundleID())
                            .append("/").append(version).append("\n");
            }
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/searchPost").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("\\?");
            outgoingHeaders.set("Location", "/search/" + splitURI[1].substring(7));
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(308, 0);
            exchange.close();
        });
        server.createContext("/search").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            String query;
            try {
                query = splitURI[2];
            } catch (IndexOutOfBoundsException e) {
                query = "";
            }
            out.append(Templates.generateBasicHeader("Search: " + query))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div><center><strong>Search iPhoneOS Obscura</strong></center></div></div>")
                    .append("<div><div><form action=\"/searchPost\"><input type\"text\" name=\"search\" value=\"").append(query)
                    .append("\" style=\"-webkit-appearance:none;border-bottom:1px solid #999\" placeholder=\"Search\"><button style=\"float:right;background:none\" type=\"submit\"><img style=\"height:18px;border-radius:50%\" src=\"/searchIcon\"></button></form></div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset>");
            if (!query.isEmpty()) {
                out.append("<label>Search Results</label><fieldset>");
                List<App> apps = AppList.searchApps(query, iOS_ver);
                if (apps.isEmpty()) {
                    out.append("<div><div>Couldn't find anything!</div></div><div><div>Make sure you've typed everything correctly, or try shortening your query.</div></div>");
                } else {
                    App app;
                    int s = apps.size();
                    for (int i = 0; i < Math.min(20, s); i++) {
                        app = apps.remove(0);
                        out.append("<a style=\"height:77px\" href=\"/getAppVersions/").append(app.getBundleID())
                                .append("\"><div><div style=\"height:77px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px\" src=\"/getAppIcon/")
                                .append(app.getBundleID()).append("\"><center style=\"line-height:57px\">").append(cutStringTo(app.getName(), 15))
                                .append("</center></div></div></a>");
                    }
                }
                out.append("</fieldset>");
            }
            out.append("<fieldset><a href=\"/\"><div><div>Return to Homepage</div></div></a></fieldset></panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            outgoingHeaders.set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/searchIcon").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            Headers incomingHeaders = exchange.getRequestHeaders();
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            boolean macOS_connection = userAgent.contains("Macintosh");
            boolean modernOS = false;
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                String ver = split2[split2.length - 1].replace("_", ".");
                modernOS = App.isVersionLater("7.0", ver);
            }
            if (macOS_connection) {
                String[] split1 = userAgent.split("AppleWebKit");
                String[] split2 = split1[0].split("\\)")[0].split(" ");
                String ver = split2[split2.length - 1].replace("_", ".");
                modernOS = App.isVersionLater("10.10", ver);
            }
            outgoingHeaders.set("Content-Type", "image/jpeg");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(200, (modernOS ? searchIcon7 : searchIcon).length);
            exchange.getResponseBody().write(modernOS ? searchIcon7 : searchIcon);
            exchange.close();
        });
        server.createContext("/icon").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/png");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(200, mainicon.length);
            exchange.getResponseBody().write(mainicon);
            exchange.close();
        });
        server.createContext("/icon32").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/png");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(200, icon32.length);
            exchange.getResponseBody().write(icon32);
            exchange.close();
        });
        server.createContext("/icon16").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/png");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(200, icon16.length);
            exchange.getResponseBody().write(icon16);
            exchange.close();
        });
        server.createContext("/favicon.ico").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/vnd.microsoft.icon");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(200, favicon.length);
            exchange.getResponseBody().write(favicon);
            exchange.close();
        });
        server.createContext("/getIconMask7").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/svg+xml");
            outgoingHeaders.set("Cache-Control", "max-age=172800,immutable");
            exchange.sendResponseHeaders(200, iconMask7.length);
            exchange.getResponseBody().write(iconMask7);
            exchange.close();
        });
        server.createContext("/reload").setHandler(exchange -> {
            if (!allowReload || (lastReload + 1000 * 60 * 5) > System.currentTimeMillis()) {
                exchange.sendResponseHeaders(202, 0);
                exchange.close();
                return;
            }
            AppList.loadAppDatabaseFile(Main.databaseLocation);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
            lastReload = System.currentTimeMillis();
        });
    }
    
    public void startServer() {
        server.start();
    }
    
    private String cutStringTo(String str, int len) {
        str = str.trim();
        if (str.length() < len) {
            return str;
        }
        return (str.substring(0, len).trim()) + "...";
    }
}
