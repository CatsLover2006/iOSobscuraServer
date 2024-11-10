package ca.litten.ios_obscura_server;

import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.frontend.Server;
import ca.litten.ios_obscura_server.parser.AppDownloader;
import ca.litten.ios_obscura_server.parser.ArchiveListDecoder;
import com.google.common.escape.Escaper;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

public class Main {
    
    private static class ArchiveParser extends Thread {
        @Override
        public void run() {
            System.out.println("Starting parser...");
            Escaper escaper = urlPathSegmentEscaper();
            LinkedList<String> urlist = new LinkedList<>();
            FileReader reader = null;
            try {
                reader = new FileReader("config.json");
                StringBuilder out = new StringBuilder();
                char[] buf = new char[4096];
                int read;
                while (reader.ready()) {
                    read = reader.read(buf);
                    for (int i = 0; i < read; i++)
                        out.append(buf[i]);
                }
                JSONObject object = new JSONObject(out.toString());
                System.out.println("Adding individual files to url list...");
                for (Object o : object.getJSONArray("network_files")) {
                    urlist.add(o.toString());
                }
                for (Object o : object.getJSONArray("archive_org_archives")) {
                    System.out.println("Adding files in archive.org item " + o.toString() + " to url list...");
                    urlist.addAll(Arrays.asList(ArchiveListDecoder
                            .getUrlListFromArchiveOrgListing(new URL("https://archive.org/download/"
                                    + o.toString() + "/" + o.toString() + "_files.xml"))));
                }
            } catch (Exception e) {
                return;
            }
            Thread task;
            Thread[] tasks = new Thread[Runtime.getRuntime().availableProcessors()];
            task = new Thread(() -> {});
            Arrays.fill(tasks, task);
            System.out.println("Started parser!");
            for (String temp : urlist) {
                String[] urlfrags = temp.split("/");
                String url = "";
                for (String frag : urlfrags) {
                    url += escaper.escape(frag) + "/";
                }
                url = url.substring(0, url.length() - 1);
                boolean good;
                while (true) {
                    try {
                        good = AppList.appUrlAlreadyExists(url);
                        break;
                    } catch (ConcurrentModificationException e) {
                        // Do nothing
                    }
                }
                if (good)
                    continue;
                String finalUrl = url;
                task = new Thread(() -> {
                    try {
                        AppDownloader.downloadAndAddApp(new URL(finalUrl));
                        System.out.println("Parsed: " + finalUrl);
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                });
                while (!task.isAlive()) {
                    for (int i = 0; i < tasks.length; i++) {
                        if (!tasks[i].isAlive()) {
                            tasks[i] = task;
                            task.start();
                            break;
                        }
                    }
                }
            }
            for (Thread thread : tasks) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // LOL
                }
            }
        }
    }
    
    private static Server server;
    private static File databaseLocation;
    
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--help")) {
            System.out.println("--skipNoAppIcon: skips apps with no app icon when loading the database");
            System.out.println("--noParse: disable app parsing to save RAM");
            System.out.println("--help: display this message");
            return;
        }
        System.out.println("Loading config...");
        try {
            File file = new File("config.json");
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
            databaseLocation = new File(object.getString("database_location"));
        } catch (Exception e) {
            System.out.println("Error occurred while loading config!");
            throw new RuntimeException(e);
        }
        System.out.println("Loading database...");
        AppList.loadAppDatabaseFile(databaseLocation, Arrays.asList(args).contains("--skipNoAppIcon"));
        System.out.println("Starting server...");
        try {
            server = new Server();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.startServer();
        System.out.println("Started server.");
        while (true) {
            Server.allowReload = false;
            if (Arrays.stream(args).noneMatch(a -> a.equals("--noParse"))) {
                ArchiveParser archiveParser = new ArchiveParser();
                archiveParser.start();
                while (archiveParser.isAlive()) {
                    try {
                        Thread.sleep(1000 * 60 * 2);
                    } catch (InterruptedException e) {
                        System.out.println("Saving database...");
                        AppList.saveAppDatabaseFile(databaseLocation);
                        break;
                    }
                    System.out.println("Saving database...");
                    AppList.saveAppDatabaseFile(databaseLocation);
                }
                System.out.println("Finished parsing!");
            }
            Server.allowReload = true;
            try {
                Thread.sleep(1000 * 60 * 60 * 24);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
