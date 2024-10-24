package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.parser.ArchiveListDecoder;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class ArchiveListDecoderTest {
    private static final String url = "https://archive.org/download/iOSObscura/iOSObscura_files.xml";
    
    @Test
    public void DownloadiOSObscuraTest() throws MalformedURLException, URISyntaxException {
        String[] urls = ArchiveListDecoder.getUrlListFromArchiveOrgListing(new URL(url));
        boolean includesA8 = false;
        for (String url : urls) {
            if (url.equals("https://archive.org/download/iOSObscura/iOS 5/com.gameloft.asphalt8/Asphalt 8-(com.gameloft.asphalt8)-1.0.0-(iOS_5.0)-643b45b35269e378a38d549b5260ee52.ipa")) {
                includesA8 = true;
                break;
            }
        }
        assertTrue(includesA8);
    }
}
