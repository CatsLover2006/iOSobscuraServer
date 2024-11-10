package ca.litten.ios_obscura_server.backend;

import ca.litten.ios_obscura_server.parser.Binary;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class App {
    
    public static class VersionLink {
        private final Binary binary;
        private final String url;
        private final long size;
        
        public VersionLink(Binary binary, String url, long size) {
            this.binary = binary;
            this.url = url;
            this.size = size;
        }
        
        public JSONObject toJSON() {
            JSONObject object = new JSONObject();
            object.put("url", url);
            if (binary != null) {
                object.put("bin", binary.toJSON());
            }
            object.put("fs", size);
            return object;
        }
        
        public String getUrl() {
            return url;
        }
        
        public Binary getBinary() {
            return binary;
        }
        
        public String getSize() {
            char[] prefixes = new char[]{'k', 'M', 'G', 'T'};
            int prefixIndex = -1;
            while ((size / Math.pow(1024, prefixIndex + 1)) >= 1024) {
                prefixIndex++;
            }
            if (prefixIndex == -1) {
                return size + " B";
            }
            char prefix = prefixes[prefixIndex];
            return (Math.round(Math.floor(size / Math.pow(1024, prefixIndex)) / 102.4) / 10.0) + (prefix + "B");
        }
    }
    
    private static class Version {
        private final String version;
        private VersionLink[] links;
        private String supportedVersion;
        
        public Version(String version, VersionLink[] links, String supportedVersion) {
            this.version = version;
            this.links = links;
            this.supportedVersion = supportedVersion;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(version, Arrays.hashCode(links), supportedVersion);
        }
        
        public void addUrl(VersionLink url) {
            if (Arrays.stream(links).noneMatch(url::equals)) {
                List<VersionLink> urlList = Arrays.stream(links).collect(Collectors.toList());
                urlList.add(url);
                links = urlList.toArray(new VersionLink[]{});
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
    
    public void addAppVersionNoSort(String version, VersionLink[] urls, String supportedVersion) {
        if (isVersionLater(supportedVersion, earliestSupportedVersion))
            earliestSupportedVersion = supportedVersion;
        for (Version otherVersion : versions) {
            if (otherVersion.version.equals(version)) {
                if (isVersionLater(supportedVersion, otherVersion.supportedVersion))
                    otherVersion.supportedVersion = supportedVersion;
                for (VersionLink url : urls)
                    otherVersion.addUrl(url);
                return;
            }
        }
        versions.add(new Version(version, urls, supportedVersion));
    }
    
    public void updateArtwork(String version, String url) {
        if (url == null) return;
        if (artworkURL.isEmpty()) {
            earliestArtVersion = version;
            artworkURL = url;
        }
        if (url.startsWith("data:")) {
            if (!artworkURL.startsWith("data:")) return;
        } else if (artworkURL.startsWith("data:")) {
            earliestArtVersion = version;
            artworkURL = url;
            return;
        }
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
            if (o1.version.equals(o2.version)) return o1.links[0].url.compareTo(o2.links[0].url);
            return (isVersionLater(o1.version, o2.version)) ? -1 : 1;
        });
    }
    
    public void addAppVersion(String version, VersionLink[] urls, String supportedVersion) {
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
            for (VersionLink url : version.links) {
                urls.put(url.toJSON());
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
                return (String[]) Arrays.stream(v.links).map(link -> link.url).toArray();
            }
        }
        return new String[]{};
    }
    
    public VersionLink[] getLinksForVersion(String version) {
        for (Version v : versions) {
            if (v.version.equals(version)) {
                return v.links;
            }
        }
        return new VersionLink[]{};
    }
    
    public List<String> getAllUrls() {
        LinkedList<String> list = new LinkedList<>();
        for (Version v : versions) {
            list.addAll(Arrays.stream(v.links).map(link -> link.url).collect(Collectors.toList()));
        }
        return list;
    }
}
