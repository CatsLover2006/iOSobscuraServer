package ca.litten.ios_obscura_server.backend;

import ca.litten.ios_obscura_server.parser.Binary;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AppList {
    private static final ArrayList<App> apps = new ArrayList<>();

    public static void loadAppDatabaseFile(File file) {
        loadAppDatabaseFile(file, false, false, false, false, false);
    }
    
    public static void loadAppDatabaseFile(File file, boolean skipEmptyIcons, boolean skipDataIcons, boolean checkUrls, boolean singleThreadedLoad, boolean skipNameless) {
        try {
            {
                System.out.println("Loading database at: " + file.getPath());
                System.out.print("Loading flags:");
                String loadingFlags = "";
                if (skipEmptyIcons) loadingFlags += " skip apps with no icon,";
                if (skipDataIcons) loadingFlags += " skip apps with embedded icon,";
                if (checkUrls) loadingFlags += " skip broken links,";
                if (singleThreadedLoad) loadingFlags += " load on single thread,";
                if (skipNameless) loadingFlags += " skip apps with no name,";
                if (loadingFlags.isEmpty()) loadingFlags = " none,";
                loadingFlags = loadingFlags.substring(0, loadingFlags.length() - 1);
                System.out.println(loadingFlags);
            } // Initial message, in curly braces for memory management
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
            apps.clear();
            AtomicInteger appCount = new AtomicInteger();
            AtomicInteger versions = new AtomicInteger();
            AtomicInteger links = new AtomicInteger();
            AtomicInteger appsLoaded = new AtomicInteger();
            AtomicInteger versionsLoaded = new AtomicInteger();
            AtomicInteger linksLoaded = new AtomicInteger();
            ThreadPoolExecutor executor;
            if (!singleThreadedLoad) executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors() * 4,
                    100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
            else executor = new ThreadPoolExecutor(1,
                    1, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
            for (Object appObject : appArray) {
                executor.execute(() -> {
                    try {
                        appCount.getAndIncrement();
                        JSONObject appJSON = (JSONObject) appObject;
                        if (skipNameless && appJSON.getString("name").isEmpty()) return;
                        if (skipEmptyIcons && appJSON.getString("art").isEmpty()) return;
                        if (skipDataIcons && appJSON.getString("art").startsWith("data")) return;
                        String bundleID = appJSON.getString("bundle").toLowerCase();
                        App app = new App(appJSON.getString("name"), bundleID);
                        if (singleThreadedLoad && getAppByBundleID(bundleID) != null) app = getAppByBundleID(bundleID);
                        else apps.add(app);
                        for (Object versionObject : appJSON.getJSONArray("versions")) {
                            JSONObject versionJSON = (JSONObject) versionObject;
                            versions.getAndIncrement();
                            JSONArray array = versionJSON.getJSONArray("urls");
                            LinkedList<App.VersionLink> versionLinks = new LinkedList<>();
                            for (Object objectStd : array) {
                                links.getAndIncrement();
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
                                linksLoaded.getAndIncrement();
                                versionLinks.add(new App.VersionLink(Binary.fromJSON(binary), object.getString("url"), object.getString("bv"), object.getLong("fs")));
                            }
                            if (versionLinks.isEmpty()) continue;
                            versionsLoaded.getAndIncrement();
                            app.addAppVersionNoSort(versionJSON.getString("ver"),
                                    versionLinks.toArray(new App.VersionLink[]{}),
                                    versionJSON.getString("support"));
                        }
                        if (app.getAllUrls().isEmpty()) return;
                        app.updateArtwork(appJSON.getString("artver"), appJSON.getString("art"));
                        app.updateDeveloper(appJSON.getString("devVer"), appJSON.getString("dev"));
                        if (appJSON.getBoolean("nN")) {
                            app.usedMetaName();
                        }
                        app.sortVersions();
                        appsLoaded.getAndIncrement();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            System.out.println("Finished sending out parser threads, waiting for them to finish...");
            executor.shutdown();
            while (executor.isTerminating()) {
                executor.awaitTermination(10, TimeUnit.SECONDS);
                if (executor.isTerminating()) System.out.println("Seen " + links + " links");
            }
            System.out.println("Saw " + appCount + " apps, loaded " + appsLoaded);
            System.out.println("Saw " + versions + " versions, loaded " + versionsLoaded);
            System.out.println("Saw " + links + " links, loaded " + linksLoaded);
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
    
    public static List<App> listAppsThatSupportVersion(String version) {
        return apps.parallelStream().filter(app -> app.showAppForVersion(version)).collect(Collectors.toList());
    }
    
    public static App getAppByBundleID(String bundleID) {
        List<App> theApp = apps.parallelStream().filter(app -> (app.getBundleID().equals(bundleID))).collect(Collectors.toList());
        if (theApp.isEmpty()) return null;
        return theApp.get(0);
    }
    
    public static void addApp(App app) {
        if (getAppByBundleID(app.getBundleID()) == null) {
            apps.add(app);
        }
    }
    
    private static final double BundleMatchBonus = Math.sqrt(Math.sqrt(Math.pow(Math.PI, Math.pow(Math.PI, Math.PI))));
    
    protected static class SearchResult {
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
                    .trim().equals(query))
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
            if (app.getBundleID().contains("com.google")) System.out.println(app.getBundleID() + ": " + resultPossibility);
        }
    }
    
    public static final double AppRelevanceCutoff = 46.123456789;
    
    public static List<App> searchApps(String query, String version) {
        return apps.parallelStream().filter(app -> app.showAppForVersion(version))
                .map(app -> new SearchResult(app, query))
                .filter(app -> app.resultPossibility >= query.length() * AppRelevanceCutoff)
                .sorted(Comparator.comparingDouble(app -> -app.resultPossibility))
                .map(app -> app.app).collect(Collectors.toList());
    }
    
    public static List<App> searchApps(String query) {
        return apps.parallelStream().map(app -> new SearchResult(app, query))
                .filter(app -> app.resultPossibility >= query.length() * AppRelevanceCutoff)
                .sorted(Comparator.comparingDouble(app -> -app.resultPossibility))
                .map(app -> app.app).collect(Collectors.toList());
    }
    
    public static boolean appUrlAlreadyExists(String url) {
        return !apps.parallelStream().filter(app -> !app.getAllUrls().parallelStream().filter(string -> string.equals(url))
                        .collect(Collectors.toList()).isEmpty()).collect(Collectors.toList()).isEmpty();
    }
}
