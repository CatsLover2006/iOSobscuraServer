package ca.litten.ios_obscura_server.parser;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Binary {
    
    private HashMap<CPUarch, Boolean> encryptionMatrix;
    
    protected Binary() {
        encryptionMatrix = new HashMap<>();
    }
    
    public boolean supportsArchitecture(CPUarch arch) {
        return encryptionMatrix.containsKey(arch);
    }
    
    public boolean architectureEncrypted(CPUarch arch) {
        if (!encryptionMatrix.containsKey(arch)) return false;
        return encryptionMatrix.get(arch);
    }
    
    public static Binary parseBinary(InputStream binaryStream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(binaryStream);
        InputStream stream = new InputStream() {
            int pos = 0;
            
            @Override
            public int available() {
                return bytes.length - pos;
            }
            
            @Override
            public int read() throws IOException {
                if (pos == bytes.length) {
                    throw new IOException("End of byte stream");
                }
                int read = bytes[pos];
                pos++;
                return read & 0xff;
            }
            
            @Override
            public long skip(long n) {
                if (available() < n) {
                    int toSkip = available();
                    pos = bytes.length;
                    return toSkip;
                }
                pos += (int) n;
                return n;
            }
            
            @Override
            public void reset() {
                pos = 0;
            }
        };
        return resetableParseBinary(stream);
    }
    
    private static Binary resetableParseBinary(InputStream binaryStream) throws IOException {
        int cpuArchMain, cpuArchSub;
        byte[] bytes = new byte[4];
        Binary binary = new Binary();
        switch (binaryStream.read()) {
            case 0xcf: { // 64-bit binary
                if (binaryStream.read() != 0xfa) return null;
                if (binaryStream.read() != 0xed) return null;
                if (binaryStream.read() != 0xfe) return null;
                System.out.println("64-bit binary found!");
                binaryStream.read(bytes);
                cpuArchMain = 0;
                for (byte bit : bytes) {
                    cpuArchMain = (cpuArchMain >> 8) + (bit & 0xFF) * 0x01000000;
                }
                binaryStream.read(bytes);
                cpuArchSub = 0;
                for (byte bit : bytes) {
                    cpuArchSub = (cpuArchSub >> 8) + (bit & 0xFF) * 0x01000000;
                }
                CPUarch arch = CPUarch.UNKNOWN;
                for (CPUarch cpu : CPUarch.values()) {
                    if (cpu.arch_main == cpuArchMain && (cpu.arch_sub == cpuArchSub || cpu.arch_sub == -1)) {
                        arch = cpu;
                        break;
                    }
                }
                System.out.println("This binary is for " + arch.name());
                binary.encryptionMatrix.put(arch, false);
                binaryStream.read(bytes); // File type (irrelevant)
                binaryStream.read(bytes); // Load command count
                binaryStream.read(bytes);
                int totalLoadCommandSize = 0;
                for (byte bit : bytes) {
                    totalLoadCommandSize = (totalLoadCommandSize >> 8) + (bit & 0xFF) * 0x01000000;
                }
                binaryStream.read(bytes); // Flags
                binaryStream.read(bytes); // Padding
                int bytesParsed = 0;
                int workingValue;
                while (bytesParsed < totalLoadCommandSize) {
                    bytesParsed += binaryStream.read(bytes);
                    workingValue = 0;
                    for (byte bit : bytes) {
                        workingValue = (workingValue >> 8) + (bit & 0xFF) * 0x01000000;
                    }
                    if (workingValue == 0x2C) { // LC_ENCRYPTION_INFO_64
                        bytesParsed += binaryStream.read(bytes); // Command Size
                        bytesParsed += binaryStream.read(bytes); // Encrypted Data Offset
                        bytesParsed += binaryStream.read(bytes); // Encrypted Data Size
                        bytesParsed += binaryStream.read(bytes); // Which encryption system
                        workingValue = 0;
                        for (byte bit : bytes) {
                            workingValue = (workingValue >> 8) + (bit & 0xFF) * 0x01000000;
                        }
                        if (workingValue == 0) continue;
                        System.out.println("Binary has encrypted segment.");
                        binary.encryptionMatrix.put(arch, true);
                        return binary;
                    }
                }
                return binary;
            }
            case 0xce: { // 32-bit binary
                if (binaryStream.read() != 0xfa) return null;
                if (binaryStream.read() != 0xed) return null;
                if (binaryStream.read() != 0xfe) return null;
                System.out.println("32-bit binary found!");
                binaryStream.read(bytes);
                cpuArchMain = 0;
                for (byte bit : bytes) {
                    cpuArchMain = (cpuArchMain >> 8) + (bit & 0xFF) * 0x01000000;
                }
                binaryStream.read(bytes);
                cpuArchSub = 0;
                for (byte bit : bytes) {
                    cpuArchSub = (cpuArchSub >> 8) + (bit & 0xFF) * 0x01000000;
                }
                CPUarch arch = CPUarch.UNKNOWN;
                for (CPUarch cpu : CPUarch.values()) {
                    if (cpu.arch_main == cpuArchMain && (cpu.arch_sub == cpuArchSub || cpu.arch_sub == -1)) {
                        arch = cpu;
                        break;
                    }
                }
                System.out.println("This binary is for " + arch.name());
                binary.encryptionMatrix.put(arch, false);
                binaryStream.read(bytes); // File type (irrelevant)
                binaryStream.read(bytes); // Load command count
                binaryStream.read(bytes);
                int totalLoadCommandSize = 0;
                for (byte bit : bytes) {
                    totalLoadCommandSize = (totalLoadCommandSize >> 8) + (bit & 0xFF) * 0x01000000;
                }
                binaryStream.read(bytes); // Flags
                int bytesParsed = 0;
                int workingValue;
                while (bytesParsed < totalLoadCommandSize) {
                    bytesParsed += binaryStream.read(bytes);
                    workingValue = 0;
                    for (byte bit : bytes) {
                        workingValue = (workingValue >> 8) + (bit & 0xFF) * 0x01000000;
                    }
                    if (workingValue == 0x21) { // LC_ENCRYPTION_INFO
                        bytesParsed += binaryStream.read(bytes); // Command Size
                        bytesParsed += binaryStream.read(bytes); // Encrypted Data Offset
                        bytesParsed += binaryStream.read(bytes); // Encrypted Data Size
                        bytesParsed += binaryStream.read(bytes); // Which encryption system
                        workingValue = 0;
                        for (byte bit : bytes) {
                            workingValue = (workingValue >> 8) + (bit & 0xFF) * 0x01000000;
                        }
                        if (workingValue == 0) continue;
                        System.out.println("Binary has encrypted segment.");
                        binary.encryptionMatrix.put(arch, true);
                        return binary;
                    }
                }
                return binary;
            }
            case 0xca: { // multi-architecture
                if (binaryStream.read() != 0xfe) return null;
                if (binaryStream.read() != 0xba) return null;
                if (binaryStream.read() != 0xbe) return null;
                binaryStream.read(bytes);
                int numberBinaries = 0;
                for (byte bit : bytes) {
                    numberBinaries = (numberBinaries << 8) + (bit & 0xFF);
                }
                System.out.println(numberBinaries + " binaries found in multi-architecture binary.");
                HashMap<CPUarch, Integer> fileOffsetMatrix = new HashMap<>();
                int fileOffset;
                for (int i = 0; i < numberBinaries; i++) {
                    CPUarch arch;
                    binaryStream.read(bytes);
                    cpuArchMain = 0;
                    for (byte bit : bytes) {
                        cpuArchMain = (cpuArchMain << 8) + (bit & 0xFF);
                    }
                    binaryStream.read(bytes);
                    cpuArchSub = 0;
                    for (byte bit : bytes) {
                        cpuArchSub = (cpuArchSub << 8) + (bit & 0xFF);
                    }
                    binaryStream.read(bytes);
                    fileOffset = 0;
                    for (byte bit : bytes) {
                        fileOffset = (fileOffset << 8) + (bit & 0xFF);
                    }
                    arch = CPUarch.UNKNOWN;
                    for (CPUarch cpu : CPUarch.values()) {
                        if (cpu.arch_main == cpuArchMain && (cpu.arch_sub == cpuArchSub || cpu.arch_sub == -1)) {
                            arch = cpu;
                            break;
                        }
                    }
                    System.out.println(arch.name() + " binary at " + fileOffset);
                    binaryStream.read(bytes); // Size
                    binaryStream.read(bytes); // Alignment
                    fileOffsetMatrix.put(arch, fileOffset);
                }
                for (CPUarch entry : fileOffsetMatrix.keySet()) {
                    binaryStream.reset();
                    binaryStream.skip(fileOffsetMatrix.get(entry));
                    Binary temp = resetableParseBinary(binaryStream);
                    binary.encryptionMatrix.put(entry, temp.architectureEncrypted(entry));
                }
                return binary;
            }
        }
        return null;
    }
}
