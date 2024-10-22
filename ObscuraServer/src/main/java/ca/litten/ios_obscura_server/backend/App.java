package ca.litten.ios_obscura_server.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class App {
    
    private static class Version {
        private final String version;
        private String[] urls;
        private String supportedVersion;
        
        public Version(String version, String[] urls, String supportedVersion) {
            this.version = version;
            this.urls = urls;
            this.supportedVersion = supportedVersion;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(version, Arrays.hashCode(urls), supportedVersion);
        }
        
        public void addUrl(String url) {
            if (Arrays.stream(urls).noneMatch(url::equals)) {
                List<String> urlList = Arrays.stream(urls).collect(Collectors.toList());
                urlList.add(url);
                urls = urlList.toArray(new String[]{});
            }
        }
    }
    
    private String name;
    private boolean usesMetaName = false;
    private final String bundleID;
    private String earliestSupportedVersion = "99999999";
    
    private String earliestArtVersion = earliestSupportedVersion;
    private String artworkURL = "";
    
    private String earliestDevVersion = earliestSupportedVersion;
    private String developer = "Unknown Developer";
    
    private final ArrayList<Version> versions;
    
    public boolean showAppForVersion(String version) {
        return isVersionLater(earliestSupportedVersion, version);
    }
    
    public String getName() {
        return name;
    }
    
    public String getBundleID() {
        return bundleID;
    }
    
    public String getEarliestSupportedVersion() {
        return earliestSupportedVersion;
    }
    
    public String getArtworkURL() {
        return artworkURL;
    }
    
    public String getDeveloper() {
        return developer;
    }
    
    public String getCompatibleVersion(String version) {
        for (Version v : versions) {
            if (v.version.equals(version)) {
                return v.supportedVersion;
            }
        }
        return "69.420";
    }
    
    public App(String name, String bundleID) {
        this.name = name;
        this.bundleID = bundleID;
        versions = new ArrayList<>();
    }
    
    public void addAppVersionNoSort(String version, String[] urls, String supportedVersion) {
        if (isVersionLater(supportedVersion, earliestSupportedVersion))
            earliestSupportedVersion = supportedVersion;
        for (Version otherVersion : versions) {
            if (otherVersion.version.equals(version)) {
                if (isVersionLater(supportedVersion, otherVersion.supportedVersion))
                    otherVersion.supportedVersion = supportedVersion;
                for (String url : urls)
                    otherVersion.addUrl(url);
                return;
            }
        }
        versions.add(new Version(version, urls, supportedVersion));
    }
    
    public void updateArtwork(String version, String url) {
        if (url == null) return;
        if (isVersionLater(version, earliestArtVersion)) {
            earliestArtVersion = version;
            artworkURL = url;
        }
    }
    
    public void updateDeveloper(String version, String dev) {
        if (dev == null) return;
        if (isVersionLater(version, earliestDevVersion)) {
            earliestDevVersion = version;
            developer = dev;
        }
    }
    
    public void sortVersions() {
        versions.sort((o1, o2) -> {
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            if (o1.version.equals(o2.version)) return o1.urls[0].compareTo(o2.urls[0]);
            return (isVersionLater(o1.version, o2.version)) ? -1 : 1;
        });
    }
    
    public void addAppVersion(String version, String[] urls, String supportedVersion) {
        addAppVersionNoSort(version, urls, supportedVersion);
        sortVersions();
    }
    
    public static boolean isVersionLater(String lateVersion, String checkVersion) {
        String[] support = lateVersion.split("\\.");
        String[] check = checkVersion.split("\\.");
        int checkLen = Math.max(check.length, support.length);
        int checkVer;
        int supportVer;
        for (int i = 0; i < checkLen; i++) {
            try {
                try {
                    supportVer = Integer.parseInt(support[i]);
                } catch (IndexOutOfBoundsException e) {
                    supportVer = 0;
                }
                try {
                    checkVer = Integer.parseInt(check[i]);
                } catch (IndexOutOfBoundsException e) {
                    checkVer = 0;
                }
                if (supportVer > checkVer) return false;
                if (supportVer < checkVer) return true;
            } catch (NumberFormatException e) {
                try {
                    int comp = support[i].compareTo(check[i]);
                    if (comp < 0) return false;
                    if (comp > 0) return true;
                } catch (IndexOutOfBoundsException f) {
                    return support.length < check.length;
                }
            }
        }
        return true; // Earliest supported version
    }
    
    public void usedMetaName() {
        usesMetaName = true;
    }
    
    public void updateName(String name) {
        if (usesMetaName) {
            return;
        }
        usesMetaName = true;
        this.name = name;
    }
    
    public String[] getSupportedAppVersions(String version) {
        List<String> halfway = new ArrayList<>();
        for (Version appVer : versions) {
            if (isVersionLater(appVer.supportedVersion, version)) {
                halfway.add(appVer.version);
            }
        }
        halfway.sort((o1, o2) -> (isVersionLater(o1, o2)) ? -1 : 1);
        return halfway.toArray(new String[]{});
    }
    
    public JSONObject getAppJSON() {
        JSONObject appJSON = new JSONObject();
        appJSON.put("name", name);
        appJSON.put("bundle", bundleID);
        JSONArray versionArray = new JSONArray();
        for (Version version : versions) {
            JSONObject versionJSON = new JSONObject();
            versionJSON.put("ver", version.version);
            versionJSON.put("support", version.supportedVersion);
            JSONArray urls = new JSONArray();
            for (String url : version.urls) {
                urls.put(url);
            }
            versionJSON.put("urls", urls);
            versionArray.put(versionJSON);
        }
        appJSON.put("versions", versionArray);
        appJSON.put("art", artworkURL);
        appJSON.put("artver", earliestArtVersion);
        appJSON.put("dev", developer);
        appJSON.put("devVer", earliestDevVersion);
        appJSON.put("nN", usesMetaName);
        return appJSON;
    }
    
    public String[] getUrlsForVersion(String version) {
        for (Version v : versions) {
            if (v.version.equals(version)) {
                return v.urls;
            }
        }
        return new String[]{};
    }
    
    public List<String> getAllUrls() {
        LinkedList<String> list = new LinkedList<>();
        for (Version v : versions) {
            list.addAll(Arrays.asList(v.urls));
        }
        return list;
    }
}
