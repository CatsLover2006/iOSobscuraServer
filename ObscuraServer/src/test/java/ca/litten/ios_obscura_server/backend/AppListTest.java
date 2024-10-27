package ca.litten.ios_obscura_server.backend;

import ca.litten.ios_obscura_server.backend.App;
import org.junit.*;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AppListTest {
    
    @Test
    public void TestApp() {
        App app = new App("Test app", "ca.litten.test");
        app.addAppVersionNoSort("1.0.3", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.0.3.ipa", 0),
                new App.VersionLink(null,"http://backup.litten.ca/test.1.0.3.ipa", 0)}, "5.1.1");
        app.addAppVersionNoSort("1.0", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.0.0.ipa", 0)}, "5.2");
        app.addAppVersionNoSort("1.0.1", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.0.1.ipa", 0)}, "5.2");
        app.addAppVersionNoSort("1.0.2", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.0.2.ipa", 0)}, "5.2");
        app.addAppVersionNoSort("1.1", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.1.ipa", 0),
                new App.VersionLink(null,"http://backup.litten.ca/test.1.1.ipa", 0)}, "6.0");
        app.addAppVersionNoSort("1.1.1", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.1.1.ipa", 0),
                new App.VersionLink(null,"http://backup.litten.ca/test.1.1.1.ipa", 0)}, "6.1.3");
        app.addAppVersionNoSort("1.1.2", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.1.2.ipa", 0),
                new App.VersionLink(null,"http://backup.litten.ca/test.1.1.2.ipa", 0)}, "6.1.3");
        app.addAppVersion("1.1.3", new App.VersionLink[]{new App.VersionLink(null,"http://litten.ca/test.1.1.3.ipa", 0),
                new App.VersionLink(null,"http://backup.litten.ca/test.1.1.3.ipa", 0)}, "6.1");
        assertEquals("5.1.1", app.getEarliestSupportedVersion());
        assertEquals("ca.litten.test", app.getBundleID());
        assertEquals("Test app", app.getName());
        String[] supportedVersions511 = app.getSupportedAppVersions("5.1.1");
        String[] supportedVersions6 = app.getSupportedAppVersions("6.0");
        String[] supportedVersions7 = app.getSupportedAppVersions("7.0");
        assertFalse(Arrays.asList(supportedVersions511).contains("1.0"));
        assertTrue(Arrays.asList(supportedVersions511).contains("1.0.3"));
        assertArrayEquals(new String[]{"http://litten.ca/test.1.0.3.ipa",
                "http://backup.litten.ca/test.1.0.3.ipa"}, app.getUrlsForVersion("1.0.3"));
        assertEquals(1, supportedVersions511.length);
        assertArrayEquals(new String[]{"1.0.3"}, supportedVersions511);
        assertArrayEquals(new String[]{"1.0", "1.0.1", "1.0.2", "1.0.3", "1.1"},
                supportedVersions6);
        assertArrayEquals(new String[]{"1.0", "1.0.1", "1.0.2", "1.0.3", "1.1", "1.1.1", "1.1.2", "1.1.3"},
                supportedVersions7);
        assertTrue(app.showAppForVersion("5.1.1"));
        assertFalse(app.showAppForVersion("5.0"));
    }

    @Test
    public void TestAppList() {
    
    }
}
