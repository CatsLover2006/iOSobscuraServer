package ca.litten.ios_obscura_server.parser;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class BinaryTest {
    private static URL multi_arch_encrypted_binary, multi_arch_cracked_binary, multi_arch_64_encrypted_binary, multi_arch_64_cracked_binary;
    
    static {
        try {
            multi_arch_encrypted_binary = new URL("https://archive.org/download/seychells-big-mega-.ipa-collection-ios-apps-and-games/iPhone%204S%2064GB%20Black%20iOS%207.1.2%20Apps%20.ipa%20Collection/Uncracked/8%20701-800/TempleRun%201.6.1.ipa");
            multi_arch_cracked_binary = new URL("https://archive.org/download/seychells-big-mega-.ipa-collection-ios-apps-and-games/iPhone%204S%2064GB%20Black%20iOS%207.1.2%20Apps%20.ipa%20Collection/Cracked/8%20701-800/com.imangi.templerun-iOS4.3-%28Clutch-2.0.4%29.ipa");
            multi_arch_64_encrypted_binary = new URL("https://archive.org/download/seychells-big-mega-.ipa-collection-ios-apps-and-games/iPhone%205S%2016GB%20Space%20Gray%20iOS%209.2%20Apps%20.ipa%20Collection/Uncracked/Roblox%20-%20Uncracked%20-%20AppDoc.ipa");
            multi_arch_64_cracked_binary = new URL("https://archive.org/download/seychells-big-mega-.ipa-collection-ios-apps-and-games/iPhone%205S%2016GB%20Space%20Gray%20iOS%209.2%20Apps%20.ipa%20Collection/Cracked/com.roblox.robloxmobile-iOS7.0-%28Clutch-2.0.4%29.ipa");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void TestEncryptedBinaryParser() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) multi_arch_encrypted_binary.openConnection();
        ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
        ZipEntry entry = zipExtractor.getNextEntry();
        while (entry != null) {
            if (entry.getName().endsWith("TempleRun")) {
                Binary binary = Binary.parseBinary(zipExtractor);
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv6));
                assertTrue(binary.supportsArchitecture(CPUarch.ARMv7));
                assertTrue(binary.architectureEncrypted(CPUarch.ARMv7));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7F));
                assertTrue(binary.supportsArchitecture(CPUarch.ARMv7S));
                assertTrue(binary.architectureEncrypted(CPUarch.ARMv7S));
                assertFalse(binary.supportsArchitecture(CPUarch.ARM64));
                assertFalse(binary.supportsArchitecture(CPUarch.ARM64e));
                assertFalse(binary.supportsArchitecture(CPUarch.i386));
                assertFalse(binary.supportsArchitecture(CPUarch.x86_64));
                return;
            }
            entry = zipExtractor.getNextEntry();
        }
    }
    
    @Test
    public void TestCrackedBinaryParser() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) multi_arch_cracked_binary.openConnection();
        ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
        ZipEntry entry = zipExtractor.getNextEntry();
        while (entry != null) {
            if (entry.getName().endsWith("TempleRun")) {
                Binary binary = Binary.parseBinary(zipExtractor);
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv6));
                assertTrue(binary.supportsArchitecture(CPUarch.ARMv7));
                assertFalse(binary.architectureEncrypted(CPUarch.ARMv7));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7F));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7S));
                assertFalse(binary.supportsArchitecture(CPUarch.ARM64));
                assertFalse(binary.supportsArchitecture(CPUarch.ARM64e));
                assertFalse(binary.supportsArchitecture(CPUarch.i386));
                assertFalse(binary.supportsArchitecture(CPUarch.x86_64));
                return;
            }
            entry = zipExtractor.getNextEntry();
        }
    }
    
    @Test
    public void TestCracked64BinaryParser() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) multi_arch_64_cracked_binary.openConnection();
        ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
        ZipEntry entry = zipExtractor.getNextEntry();
        boolean foundOther = false;
        while (entry != null) {
            if (entry.getName().endsWith("Roblox")) {
                Binary binary = Binary.parseBinary(zipExtractor);
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv6));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7F));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7S));
                assertTrue(binary.supportsArchitecture(CPUarch.ARM64));
                assertFalse(binary.architectureEncrypted(CPUarch.ARM64));
                assertFalse(binary.supportsArchitecture(CPUarch.ARM64e));
                assertFalse(binary.supportsArchitecture(CPUarch.i386));
                assertFalse(binary.supportsArchitecture(CPUarch.x86_64));
                return;
            }
            entry = zipExtractor.getNextEntry();
        }
    }
    
    
    
    @Test
    public void TestEncrypted64BinaryParser() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) multi_arch_64_encrypted_binary.openConnection();
        ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
        ZipEntry entry = zipExtractor.getNextEntry();
        boolean foundOther = false;
        while (entry != null) {
            if (entry.getName().endsWith("Roblox")) {
                Binary binary = Binary.parseBinary(zipExtractor);
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv6));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7F));
                assertFalse(binary.supportsArchitecture(CPUarch.ARMv7S));
                assertTrue(binary.supportsArchitecture(CPUarch.ARM64));
                assertTrue(binary.architectureEncrypted(CPUarch.ARM64));
                assertFalse(binary.supportsArchitecture(CPUarch.ARM64e));
                assertFalse(binary.supportsArchitecture(CPUarch.i386));
                assertFalse(binary.supportsArchitecture(CPUarch.x86_64));
                return;
            }
            entry = zipExtractor.getNextEntry();
        }
    }
}
