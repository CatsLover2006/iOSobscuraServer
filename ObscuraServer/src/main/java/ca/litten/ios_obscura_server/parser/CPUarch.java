package ca.litten.ios_obscura_server.parser;

public enum CPUarch {
    ARMv6(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 6}),
    ARMv7(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 9}),
    ARMv7F(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 10}),
    ARMv7S(new byte[]{0, 0, 0, 12}, new byte[]{0, 0, 0, 11}),
    ARM64e_legacy(new byte[]{1, 0, 0, 12}, new byte[]{0, 0, 0, 2}),
    ARM64e(new byte[]{1, 0, 0, 12}, new byte[]{(byte) 0x80, 0, 0, 2}),
    ARM64v8(new byte[]{1, 0, 0, 12}, new byte[]{0, 0, 0, 1}),
    ARM32(new byte[]{0, 0, 0, 12}, null),
    ARM64(new byte[]{1, 0, 0, 12}, null),
    i386(new byte[]{0, 0, 0, 7}, null),
    x86_64(new byte[]{1, 0, 0, 7}, null),
    UNKNOWN(null, null);
    
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
