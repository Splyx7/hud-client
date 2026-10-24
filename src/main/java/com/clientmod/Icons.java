package com.clientmod;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/** Hand-drawn 8x8 pixel icons. 'x' = main color, 'a' = accent color. */
public final class Icons {
    private static final Map<String, String[]> M = new HashMap<>();

    private static void i(String n, String... rows) { M.put(n, rows); }

    static {
        i("all", "xxx.xxx.", "xxx.xxx.", "xxx.xxx.", "........", "xxx.xxx.", "xxx.xxx.", "xxx.xxx.", "........");
        i("hud", "xxxxxxxx", "x......x", "x.xx...x", "x......x", "x...xx.x", "x......x", "xxxxxxxx", "..xxxx..");
        i("visual", "........", "..xxxx..", ".x.aa.x.", "xx.aa.xx", ".x.aa.x.", "..xxxx..", "........", "........");
        i("util", "..xxx...", ".x..xx..", ".x..xx..", "..xxxx..", "...xx...", "...xx...", "..xx....", "..x.....");
        i("cog", "...xx...", ".x.xx.x.", "..xxxx..", "xxx..xxx", "xxx..xxx", "..xxxx..", ".x.xx.x.", "...xx...");
        i("fps", ".....xx.", "....xx..", "...xx...", "..xxxxx.", "....xx..", "...xx...", "..xx....", "..a.....");
        i("cps", "..xxxx..", ".xx.axx.", ".xx.axx.", ".xxxxxx.", ".xxxxxx.", ".xxxxxx.", ".xxxxxx.", "..xxxx..");
        i("ping", "........", "......xx", "......xx", "....xxxx", "....xxxx", "..xxxxxx", "..xxxxxx", "xxxxxxxx");
        i("xyz", "..xxxx..", ".xxxxxx.", ".xx..xx.", ".xxxxxx.", "..xxxx..", "...xx...", "...xx...", "....a...");
        i("arm", "xx....xx", "xxx..xxx", "xxxxxxxx", "xxxxxxxx", ".xxxxxx.", ".xxaaxx.", ".xxxxxx.", "..xxxx..");
        i("reach", "........", "..x..x..", ".xx..xx.", "xxxxxxxx", ".xx..xx.", "..x..x..", "........", "........");
        i("full", "x..xx..x", ".x.xx.x.", "..xxxx..", "xxxxxxxx", "xxxxxxxx", "..xxxx..", ".x.xx.x.", "x..xx..x");
        i("sat", "...xx...", "...xx...", "..xxxx..", ".xxxxxx.", "xxxxaxxx", "xxxxaxxx", ".xxxxxx.", "..xxxx..");
        i("blur", "........", ".xxxxx..", "........", "xxxxxxx.", "........", "..xxxxxx", "........", ".xxxx...");
        i("fog", "........", "..xxx...", ".xxxxxx.", "xxxxxxxx", "xxxxxxxx", ".xxxxxx.", "........", "........");
        i("hit", "...xx...", ".x.xx.x.", "..xxxx..", "xxxaaxxx", "xxxaaxxx", "..xxxx..", ".x.xx.x.", "...xx...");
        i("shield", "xxxxxxxx", "xxxxxxxx", "xxxaaxxx", "xxxaaxxx", ".xxaaxx.", ".xxxxxx.", "..xxxx..", "...xx...");
        i("out", "xx....xx", "x......x", "........", "........", "........", "........", "x......x", "xx....xx");
        i("box", "xxxxxxxx", "x......x", "x.xxxx.x", "x.x..x.x", "x.x..x.x", "x.xxxx.x", "x......x", "xxxxxxxx");
        i("tw", "..xxx...", ".xx.....", "xx......", "xx......", "xx......", ".xx...a.", "..xxxx..", "........");
        i("itf", "......xx", ".....xxx", "....xxx.", "x..xxx..", "xxxxx...", ".xxx....", "xx.xx...", "x...x...");
        i("zoom", ".xxxx...", "x....x..", "x....x..", "x....x..", ".xxxx...", "....xx..", ".....xx.", "......xx");
        i("spr", ".x..x...", "..x..x..", "...x..x.", "....x..x", "...x..x.", "..x..x..", ".x..x...", "........");
        i("wp", "xxxxxx..", "xxxxxxx.", "xxxxxx..", "xx......", "x.......", "x.......", "x.......", "x.......");
        i("rec", "..xxxx..", ".x...xxx", "x.....xx", "x.......", ".......x", "xx.....x", "xxx...x.", "..xxxx..");
    }

    public static void draw(DrawContext c, String name, int x, int y, int px, int main, int acc) {
        String[] rows = M.getOrDefault(name, M.get("cog"));
        for (int r = 0; r < rows.length; r++) {
            for (int k = 0; k < 8 && k < rows[r].length(); k++) {
                char ch = rows[r].charAt(k);
                if (ch == '.') continue;
                c.fill(x + k * px + 1, y + r * px + 1, x + (k + 1) * px + 1, y + (r + 1) * px + 1, 0x66000000);
            }
        }
        for (int r = 0; r < rows.length; r++) {
            for (int k = 0; k < 8 && k < rows[r].length(); k++) {
                char ch = rows[r].charAt(k);
                if (ch == '.') continue;
                c.fill(x + k * px, y + r * px, x + (k + 1) * px, y + (r + 1) * px, ch == 'a' ? acc : main);
            }
        }
    }
}
