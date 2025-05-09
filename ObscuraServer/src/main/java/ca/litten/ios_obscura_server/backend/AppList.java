package ca.litten.ios_obscura_server.backend;

import ca.litten.ios_obscura_server.parser.Binary;
import ca.litten.ios_obscura_server.parser.CPUarch;
import com.sun.xml.internal.ws.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AppList {
    private static final ArrayList<App> apps = new ArrayList<>();
    private static final ArrayList<App> loadingApps = new ArrayList<>(1);
    private static boolean loading = true;

    public static void loadAppDatabaseFile(File file) {
        loadAppDatabaseFile(file, false, false, false, false, false, false);
    }
    
    private static String loadingImg = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Zz48Y2lyY2xlIGN4PSIxMiIgY3k9I" +
            "jMiIHI9IjEiPjxhbmltYXRlIGlkPSJzcGlubmVyXzdaNzMiIGJlZ2luPSIwO3NwaW5uZXJfdEtzdS5lbmQtMC41cyIgYXR0cmlidXRlTmFtZT0iciIgY2FsY01vZGU9InNwbGluZSIgZHVyPSIwLjZzIiB2YWx1ZXM9IjE7MjsxIiBrZXlTcGxpbmVzPSIuMjc" +
            "sLjQyLC4zNywuOTk7LjUzLDAsLjYxLC43MyIvPjwvY2lyY2xlPjxjaXJjbGUgY3g9IjE2LjUwIiBjeT0iNC4yMSIgcj0iMSI+PGFuaW1hdGUgaWQ9InNwaW5uZXJfV2Q4NyIgYmVnaW49InNwaW5uZXJfN1o3My5iZWdpbiswLjFzIiBhdHRyaWJ1dGVOYW1lP" +
            "SJyIiBjYWxjTW9kZT0ic3BsaW5lIiBkdXI9IjAuNnMiIHZhbHVlcz0iMTsyOzEiIGtleVNwbGluZXM9Ii4yNywuNDIsLjM3LC45OTsuNTMsMCwuNjEsLjczIi8+PC9jaXJjbGU+PGNpcmNsZSBjeD0iNy41MCIgY3k9IjQuMjEiIHI9IjEiPjxhbmltYXRlIGl" +
            "kPSJzcGlubmVyX3RLc3UiIGJlZ2luPSJzcGlubmVyXzlRbGMuYmVnaW4rMC4xcyIgYXR0cmlidXRlTmFtZT0iciIgY2FsY01vZGU9InNwbGluZSIgZHVyPSIwLjZzIiB2YWx1ZXM9IjE7MjsxIiBrZXlTcGxpbmVzPSIuMjcsLjQyLC4zNywuOTk7LjUzLDAsL" +
            "jYxLC43MyIvPjwvY2lyY2xlPjxjaXJjbGUgY3g9IjE5Ljc5IiBjeT0iNy41MCIgcj0iMSI+PGFuaW1hdGUgaWQ9InNwaW5uZXJfbE1NTyIgYmVnaW49InNwaW5uZXJfV2Q4Ny5iZWdpbiswLjFzIiBhdHRyaWJ1dGVOYW1lPSJyIiBjYWxjTW9kZT0ic3BsaW5" +
            "lIiBkdXI9IjAuNnMiIHZhbHVlcz0iMTsyOzEiIGtleVNwbGluZXM9Ii4yNywuNDIsLjM3LC45OTsuNTMsMCwuNjEsLjczIi8+PC9jaXJjbGU+PGNpcmNsZSBjeD0iNC4yMSIgY3k9IjcuNTAiIHI9IjEiPjxhbmltYXRlIGlkPSJzcGlubmVyXzlRbGMiIGJlZ" +
            "2luPSJzcGlubmVyX0toeHYuYmVnaW4rMC4xcyIgYXR0cmlidXRlTmFtZT0iciIgY2FsY01vZGU9InNwbGluZSIgZHVyPSIwLjZzIiB2YWx1ZXM9IjE7MjsxIiBrZXlTcGxpbmVzPSIuMjcsLjQyLC4zNywuOTk7LjUzLDAsLjYxLC43MyIvPjwvY2lyY2xlPjx" +
            "jaXJjbGUgY3g9IjIxLjAwIiBjeT0iMTIuMDAiIHI9IjEiPjxhbmltYXRlIGlkPSJzcGlubmVyXzVMOXQiIGJlZ2luPSJzcGlubmVyX2xNTU8uYmVnaW4rMC4xcyIgYXR0cmlidXRlTmFtZT0iciIgY2FsY01vZGU9InNwbGluZSIgZHVyPSIwLjZzIiB2YWx1Z" +
            "XM9IjE7MjsxIiBrZXlTcGxpbmVzPSIuMjcsLjQyLC4zNywuOTk7LjUzLDAsLjYxLC43MyIvPjwvY2lyY2xlPjxjaXJjbGUgY3g9IjMuMDAiIGN5PSIxMi4wMCIgcj0iMSI+PGFuaW1hdGUgaWQ9InNwaW5uZXJfS2h4diIgYmVnaW49InNwaW5uZXJfbGQ2UC5" +
            "iZWdpbiswLjFzIiBhdHRyaWJ1dGVOYW1lPSJyIiBjYWxjTW9kZT0ic3BsaW5lIiBkdXI9IjAuNnMiIHZhbHVlcz0iMTsyOzEiIGtleVNwbGluZXM9Ii4yNywuNDIsLjM3LC45OTsuNTMsMCwuNjEsLjczIi8+PC9jaXJjbGU+PGNpcmNsZSBjeD0iMTkuNzkiI" +
            "GN5PSIxNi41MCIgcj0iMSI+PGFuaW1hdGUgaWQ9InNwaW5uZXJfQmZURCIgYmVnaW49InNwaW5uZXJfNUw5dC5iZWdpbiswLjFzIiBhdHRyaWJ1dGVOYW1lPSJyIiBjYWxjTW9kZT0ic3BsaW5lIiBkdXI9IjAuNnMiIHZhbHVlcz0iMTsyOzEiIGtleVNwbGl" +
            "uZXM9Ii4yNywuNDIsLjM3LC45OTsuNTMsMCwuNjEsLjczIi8+PC9jaXJjbGU+PGNpcmNsZSBjeD0iNC4yMSIgY3k9IjE2LjUwIiByPSIxIj48YW5pbWF0ZSBpZD0ic3Bpbm5lcl9sZDZQIiBiZWdpbj0ic3Bpbm5lcl9YeUJzLmJlZ2luKzAuMXMiIGF0dHJpY" +
            "nV0ZU5hbWU9InIiIGNhbGNNb2RlPSJzcGxpbmUiIGR1cj0iMC42cyIgdmFsdWVzPSIxOzI7MSIga2V5U3BsaW5lcz0iLjI3LC40MiwuMzcsLjk5Oy41MywwLC42MSwuNzMiLz48L2NpcmNsZT48Y2lyY2xlIGN4PSIxNi41MCIgY3k9IjE5Ljc5IiByPSIxIj4" +
            "8YW5pbWF0ZSBpZD0ic3Bpbm5lcl83Z0FLIiBiZWdpbj0ic3Bpbm5lcl9CZlRELmJlZ2luKzAuMXMiIGF0dHJpYnV0ZU5hbWU9InIiIGNhbGNNb2RlPSJzcGxpbmUiIGR1cj0iMC42cyIgdmFsdWVzPSIxOzI7MSIga2V5U3BsaW5lcz0iLjI3LC40MiwuMzcsL" +
            "jk5Oy41MywwLC42MSwuNzMiLz48L2NpcmNsZT48Y2lyY2xlIGN4PSI3LjUwIiBjeT0iMTkuNzkiIHI9IjEiPjxhbmltYXRlIGlkPSJzcGlubmVyX1h5QnMiIGJlZ2luPSJzcGlubmVyX0hpU2wuYmVnaW4rMC4xcyIgYXR0cmlidXRlTmFtZT0iciIgY2FsY01" +
            "vZGU9InNwbGluZSIgZHVyPSIwLjZzIiB2YWx1ZXM9IjE7MjsxIiBrZXlTcGxpbmVzPSIuMjcsLjQyLC4zNywuOTk7LjUzLDAsLjYxLC43MyIvPjwvY2lyY2xlPjxjaXJjbGUgY3g9IjEyIiBjeT0iMjEiIHI9IjEiPjxhbmltYXRlIGlkPSJzcGlubmVyX0hpU" +
            "2wiIGJlZ2luPSJzcGlubmVyXzdnQUsuYmVnaW4rMC4xcyIgYXR0cmlidXRlTmFtZT0iciIgY2FsY01vZGU9InNwbGluZSIgZHVyPSIwLjZzIiB2YWx1ZXM9IjE7MjsxIiBrZXlTcGxpbmVzPSIuMjcsLjQyLC4zNywuOTk7LjUzLDAsLjYxLC43MyIvPjwvY2l" +
            "yY2xlPjxhbmltYXRlVHJhbnNmb3JtIGF0dHJpYnV0ZU5hbWU9InRyYW5zZm9ybSIgdHlwZT0icm90YXRlIiBkdXI9IjZzIiB2YWx1ZXM9IjM2MCAxMiAxMjswIDEyIDEyIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPjwvZz48L3N2Zz4="; // Yes it's long
    
    static {
        App app = new App("Loading...", "nil");
        app.updateArtwork("0.0", loadingImg);
        app.addAppVersionNoSort("nil",
                new App.VersionLink[]{new App.VersionLink(null, "/", "nil", 0, "nil")}, "0");
        loadingApps.add(app);
    }
    
    public static void loadAppDatabaseFile(File file, boolean skipEmptyIcons, boolean skipDataIcons, boolean checkUrls, boolean singleThreadedLoad, boolean skipNameless, boolean skipArm32) {
        try {
            {
                System.out.println("Loading database at: " + file.getPath());
                System.out.print("Loading flags:");
                String loadingFlags = "";
                if (skipEmptyIcons) loadingFlags += " skip apps with no icon,";
                if (skipDataIcons) loadingFlags += " skip apps with embedded icon,";
                if (checkUrls) loadingFlags += " skip broken links,";
                if (skipNameless) loadingFlags += " skip apps with no name,";
                if (skipArm32) loadingFlags += " skip binaries with improperly-scanned binaries,";
                if (singleThreadedLoad) loadingFlags += " load on single thread,";
                if (loadingFlags.isEmpty()) loadingFlags = " none,";
                loadingFlags = loadingFlags.substring(0, loadingFlags.length() - 1);
                System.out.println(loadingFlags);
            } // Initial message, in curly braces for memory management
            loading = true;
            FileReader reader = new FileReader(file);
            StringBuilder out = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while (reader.ready()) {
                read = reader.read(buf);
                for (int i = 0; i < read; i++)
                    out.append(buf[i]);
            }
            JSONArray appArray = new JSONArray(out.toString());
            Stream<Object> appStream = StreamSupport.stream(appArray.spliterator(), !singleThreadedLoad);
            List<App> appList = appStream.map(o -> {
                JSONObject appJSON = (JSONObject) o;
                if (skipNameless && appJSON.getString("name").isEmpty()) return null;
                if (skipEmptyIcons && appJSON.getString("art").isEmpty()) return null;
                if (skipDataIcons && appJSON.getString("art").startsWith("data")) return null;
                String bundleID = appJSON.getString("bundle").toLowerCase();
                App app = new App(appJSON.getString("name"), bundleID);
                for (Object versionObject : appJSON.getJSONArray("versions")) {
                    JSONObject versionJSON = (JSONObject) versionObject;
                    JSONArray array = versionJSON.getJSONArray("urls");
                    LinkedList<App.VersionLink> versionLinks = new LinkedList<>();
                    for (Object objectStd : array) {
                        JSONObject object = (JSONObject) objectStd;
                        if (checkUrls) {
                            try {
                                boolean skip = false;
                                {
                                    URL tURL = new URL(object.getString("url"));
                                    ;
                                    HttpURLConnection connection;
                                    boolean keepGoing = true;
                                    int redirects = 0;
                                    int rCode = 0;
                                    while (keepGoing) {
                                        connection = (HttpURLConnection) tURL.openConnection();
                                        connection.setInstanceFollowRedirects(false);
                                        connection.setRequestMethod("HEAD");
                                        connection.connect();
                                        switch (connection.getResponseCode() / 100) {
                                            case 2: { // Success
                                                if (connection.getResponseCode() == 204) {
                                                    skip = true;
                                                }
                                                keepGoing = false;
                                                break;
                                            }
                                            case 3: { // Redirect
                                                String location = connection.getHeaderField("Location");
                                                tURL = new URL(tURL, location);
                                                redirects++;
                                                if (redirects > 10) {
                                                    skip = true;
                                                    keepGoing = false;
                                                }
                                                break;
                                            }
                                            case 4:    // Client error (mostly for 404s)
                                            case 5:    // Server error
                                            default: { // Catchall for other errors
                                                skip = true;
                                                keepGoing = false;
                                                break;
                                            }
                                        }
                                        rCode = connection.getResponseCode();
                                        connection.disconnect();
                                    }
                                } // Check URL
                                if (skip) continue;
                            } catch (Exception e) {
                                System.out.println("Error while connecting, assuming bad URL...");
                                continue;
                            }
                        }
                        JSONObject binary = null;
                        if (object.has("bin")) {
                            binary = object.getJSONObject("bin");
                        }
                        Binary bin = Binary.fromJSON(binary);
                        if (skipArm32 && (bin.getEncryptionMatrix().isEmpty() || // Long condition, I know
                                (bin.supportsArchitecture(CPUarch.ARM32) && bin.getEncryptionMatrix().size() == 1) ||
                                (bin.supportsArchitecture(CPUarch.UNKNOWN) && bin.getEncryptionMatrix().size() == 1) ||
                                (bin.supportsArchitecture(CPUarch.ARM32) && bin.supportsArchitecture(CPUarch.UNKNOWN)
                                        && bin.getEncryptionMatrix().size() == 2))) continue;
                        versionLinks.add(new App.VersionLink(bin, object.getString("url"), object.getString("bv"), object.getLong("fs"), object.getString("bID")));
                    }
                    if (versionLinks.isEmpty()) continue;
                    app.addAppVersionNoSort(versionJSON.getString("ver"),
                            versionLinks.toArray(new App.VersionLink[]{}),
                            versionJSON.getString("support"));
                }
                if (app.getAllUrls().isEmpty()) {
                    return null;
                }
                app.updateArtwork(appJSON.getString("artver"), appJSON.getString("art"));
                app.updateDeveloper(appJSON.getString("devVer"), appJSON.getString("dev"));
                if (appJSON.getBoolean("nN")) {
                    app.usedMetaName();
                }
                app.sortVersions();
                return app;
            }).filter(app -> app != null).collect(Collectors.toList());
            List<String> bundleList = (singleThreadedLoad ? appList.stream() : appList.parallelStream())
                    .map(app -> app.getBundleID().toLowerCase()).collect(Collectors.toList());
            apps.clear();
            apps.addAll((singleThreadedLoad ? appList.stream() : appList.parallelStream())
                    .filter(app -> bundleList.indexOf(app.getBundleID().toLowerCase())
                    == bundleList.lastIndexOf(app.getBundleID().toLowerCase())).collect(Collectors.toList()));
            loading = false;
        } catch (FileNotFoundException e) {
            System.err.println("File not found! Not importing anything.");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    public static void saveAppDatabaseFile(File file) {
        JSONArray appArray = new JSONArray();
        for (App app : apps) {
            appArray.put(app.getAppJSON());
        }
        try {
            FileWriter writer = new FileWriter(file, false);
            writer.write(appArray.toString());
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to write to file!");
        }
    }
    
    private static ArrayList<App> getApps() {
        if (loading && apps.isEmpty()) return loadingApps;
        return apps;
    }
    
    public static List<App> listAppsThatSupportVersion(String version) {
        return getApps().parallelStream().filter(app -> app.showAppForVersion(version)).collect(Collectors.toList());
    }
    
    public static App getAppByBundleID(String bundleID) {
        List<App> theApp = getApps().parallelStream().filter(app -> (app.getBundleID().equals(bundleID))).collect(Collectors.toList());
        if (theApp.isEmpty()) return null;
        return theApp.get(0);
    }
    
    public static void addApp(App app) {
        if (getAppByBundleID(app.getBundleID()) == null) {
            apps.add(app);
        }
    }
    
    private static final double BundleMatchBonus = Math.sqrt(Math.sqrt(Math.pow(Math.PI, Math.pow(Math.PI, Math.PI))));
    
    public static class SearchResult {
        public double resultPossibility;
        public App app;
        
        public SearchResult(App app, String query) {
            this.app = app;
            resultPossibility = 0;
            query = query.toLowerCase(); // Ignore case
            String appName = app.getName().toLowerCase();
            String bundleName = app.getBundleID();
            int val = 0;
            int lastOffset = 0;
            String[] querySplitBySpaces = query.split(" ");
            String[] appNameSplitBySpaces = appName.split(" ");
            for (int i = 0; i < query.length(); i++) {
                val = appName.indexOf(query.charAt(i), val);
                if (val == -1) val = 0;
                resultPossibility += Math.max(0, 30 - Math.sqrt(Math.abs((i - val) - lastOffset)));
                lastOffset = val;
            }
            int min = Math.min(appNameSplitBySpaces.length, querySplitBySpaces.length);
            int min2;
            for (int i = 0; i < min; i++) {
                if (querySplitBySpaces[i].equals(appNameSplitBySpaces[i])) resultPossibility += querySplitBySpaces[i].length() * 40;
                else {
                    min2 = Math.min(querySplitBySpaces[i].length(), appNameSplitBySpaces[i].length());
                    for (int j = 0; j < min2; j++) {
                        if (querySplitBySpaces[i].charAt(j) == appNameSplitBySpaces[i].charAt(j)) resultPossibility += 30;
                        else if ((j != 0 && querySplitBySpaces[i].charAt(j) == appNameSplitBySpaces[i].charAt(j - 1))
                                || (j < appNameSplitBySpaces[i].length() - 1 && querySplitBySpaces[i].charAt(j) == appNameSplitBySpaces[i].charAt(j + 1))
                                || (j < querySplitBySpaces[i].length() - 1 && querySplitBySpaces[i].charAt(j + 1) == appNameSplitBySpaces[i].charAt(j))) resultPossibility += 15;
                    }
                }
            }
            if (appName.contains(query)) resultPossibility += query.length() * 20;
            if (appName.startsWith(query)) resultPossibility += query.length() * 30;
            if (((appName.contains(":")||appName.contains("–"))?
                    appName.substring(0, Math.max(appName.lastIndexOf(":"), appName.lastIndexOf("–"))):appName)
                    .trim().equals(query) || query.equals(appName))
                resultPossibility += query.length() * 123;
            for (int i = 0; i < query.length(); i++) {
                val = bundleName.indexOf(query.charAt(i), val);
                if (val == -1) val = 0;
                resultPossibility += Math.max(0, 40 - Math.sqrt(Math.abs((i - val) - lastOffset))) / 50;
                lastOffset = val;
            }
            if (bundleName.contains(query)) resultPossibility += query.length() * 70;
            if (bundleName.endsWith(query)) resultPossibility += query.length() * 90;
            if (bundleName.equals(query)) resultPossibility += BundleMatchBonus * query.length();
            //if (app.getBundleID().contains("com.google")) System.out.println(app.getBundleID() + ": " + resultPossibility);
        }
    }
    
    public static final double AppRelevanceCutoff = 46.123456789;
    
    public static List<App> searchApps(String query, String version) {
        return getApps().parallelStream().filter(app -> app.showAppForVersion(version))
                .map(app -> new SearchResult(app, query))
                .filter(app -> app.resultPossibility >= query.length() * AppRelevanceCutoff)
                .sorted(Comparator.comparingDouble(app -> -app.resultPossibility))
                .map(app -> app.app).collect(Collectors.toList());
    }
    public static List<SearchResult> searchAppsWithWeights(String query, String version) {
        return getApps().parallelStream().filter(app -> app.showAppForVersion(version))
                .map(app -> new SearchResult(app, query))
                .filter(app -> app.resultPossibility >= query.length() * AppRelevanceCutoff)
                .sorted(Comparator.comparingDouble(app -> -app.resultPossibility)).collect(Collectors.toList());
    }
    
    public static List<App> searchApps(String query) {
        return getApps().parallelStream().map(app -> new SearchResult(app, query))
                .filter(app -> app.resultPossibility >= query.length() * AppRelevanceCutoff)
                .sorted(Comparator.comparingDouble(app -> -app.resultPossibility))
                .map(app -> app.app).collect(Collectors.toList());
    }
    
    public static double getSearchRelevanceCutoff(String query) {
        return query.length() * AppRelevanceCutoff;
    }
    
    public static List<SearchResult> searchAppsWithWeights(String query) {
        return getApps().parallelStream().map(app -> new SearchResult(app, query))
                .filter(app -> app.resultPossibility >= query.length() * AppRelevanceCutoff)
                .sorted(Comparator.comparingDouble(app -> -app.resultPossibility)).collect(Collectors.toList());
    }
    
    public static boolean appUrlAlreadyExists(String url) {
        return !getApps().parallelStream().filter(app -> !app.getAllUrls().parallelStream().filter(string -> string.equals(url))
                        .collect(Collectors.toList()).isEmpty()).collect(Collectors.toList()).isEmpty();
    }
}
