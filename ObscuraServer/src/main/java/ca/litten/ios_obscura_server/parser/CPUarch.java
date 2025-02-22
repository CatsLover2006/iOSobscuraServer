package ca.litten.ios_obscura_server.parser;

public enum CPUarch {

    // ARM32 Types
    ARMv6(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 6}),
    ARMv7(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 9}),
    ARMv7F(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 10}),
    ARMv7S(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 11}),
    ARMv7K(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 12}),
    ARMv8(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 13}), // Supposed to be unused
    ARMv6M(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 14}), // Supposed to be unused
    ARMv7M(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 15}), // Supposed to be unused
    ARMv7EM(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 16}), // Supposed to be unused
    ARMv8M(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 17}), // Supposed to be unused
    ARM32(new byte[]{0, 0, 0, 12}, null), // Supposed to be unused

    // ARM64 Types
    ARM64e(new byte[]{1, 0, 0, 12}, new byte[]{(byte) 0x80, 0, 0, 2}),
    ARM64e_legacy(new byte[]{1, 0, 0, 12}, new byte[]{0, 0, 0, 2}), // Supposed to be unused
    ARM64v8(new byte[]{1, 0, 0, 12}, new byte[]{0, 0, 0, 1}), // Supposed to be unused
    ARM64(new byte[]{1, 0, 0, 12}, null),

    // ARM64_32 Types
    ARM64_32v8(new byte[]{2, 0, 0, 12}, new byte[]{0, 0, 0, 1}), // Supposed to be unused
    ARM64_32(new byte[]{2, 0, 0, 12}, null),

    // Intel Types
    i386(new byte[]{0, 0, 0, 7}, null), // Supposed to be unused
    x86_64(new byte[]{1, 0, 0, 7}, null), // Supposed to be unused

    // PowerPC Types
    PPC(new byte[]{0, 0, 0, 18}, null), // Supposed to be unused
    PPC64(new byte[]{1, 0, 0, 18}, null), // Supposed to be unused

    // Remaining Types
    UNKNOWN(null, null); // Supposed to be unused
    
    public final int arch_main;
    public final int arch_sub;
    
    CPUarch(byte[] archMain, byte[] archSub) {
        if (archMain == null) {
            arch_main = -1;
        } else {
            int archMainGen = 0;
            for (byte bit : archMain) {
                archMainGen = (archMainGen << 8) + (bit & 0xFF);
            }
            arch_main = archMainGen;
        }
        if (archSub == null) {
            arch_sub = -1;
        } else {
            int archSubGen = 0;
            for (byte bit : archSub) {
                archSubGen = (archSubGen << 8) + (bit & 0xFF);
            }
            arch_sub = archSubGen;
        }
    }
}
