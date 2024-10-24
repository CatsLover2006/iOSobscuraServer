package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.parser.AppDownloader;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class AppDownloaderTest {
    private static final String url = "https://archive.org/download/iOSObscura/iPhoneOS%202/com.activision.callofduty/COD%20Zombies-%28com.activision.callofduty%29-1.0.0-%28iOS_2.2.1%29-59800a8eb9cfc230169f508ce4e619d4.ipa";
    private static final String url2 = "https://archive.org/download/iOSObscura/iPhoneOS%202/com.activision.callofduty/COD%20Zombies-%28com.activision.callofduty%29-1.1.0-%28iOS_2.2.1%29-1fdf037fdca206105f017a53562ca246.ipa";
    private static final String url3 = "https://archive.org/download/iOSObscura/iPhoneOS%203/com.activision.callofduty/COD%20Zombies-%28com.activision.callofduty%29-1.5.0-%28iOS_3.0%29-8ed226054ca85be7f9146c525cc26b85.ipa";
    
    @Test
    public void AddAppTest() throws MalformedURLException {
        AppDownloader.downloadAndAddApp(new URL(url3));
        AppDownloader.downloadAndAddApp(new URL(url2));
        AppDownloader.downloadAndAddApp(new URL(url));
        App cod = AppList.getAppByBundleID("com.activision.callofduty");
        System.out.println(cod.getSupportedAppVersions("3.0").toString());
        assertNotNull(cod);
        assertEquals("2.2.1", cod.getEarliestSupportedVersion());
        assertEquals("Call of Duty: Zombies", cod.getName());
        assertTrue(AppList.appUrlAlreadyExists(url3));
        assertFalse(AppList.appUrlAlreadyExists("https://archive.org/download/iOSObsc"));
    }
}
