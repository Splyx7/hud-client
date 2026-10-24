package com.clientmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    /** Last drawn bounds per module: {x, y, w, h} in scaled GUI pixels (used by the HUD editor). */
    public static final java.util.Map<String, float[]> BOUNDS = new java.util.HashMap<>();

    private HudRenderer() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BOUNDS.clear();
        if (mc.player == null || mc.options.hudHidden) return;

        float[] used = new float[4];
        for (Module m : ModuleManager.all()) {
            if (!m.enabled || !m.implemented || !m.category.equals("HUD")) continue;
            if (m.id.equals("arm")) {
                armor(ctx, mc, m);
                continue;
            }
            List<String> lines = lines(m, mc);
            if (!lines.isEmpty()) drawLines(ctx, mc, m, lines, used);
        }
    }

    private static String line(Module m, String label, String value) {
        String s = m.b("label") ? label + ": " + value : value;
        return m.b("brackets") ? "[" + s + "]" : s;
    }

    private static List<String> lines(Module m, MinecraftClient mc) {
        List<String> out = new ArrayList<>();
        switch (m.id) {
            case "fps" -> out.add(line(m, "FPS", String.valueOf(mc.getCurrentFps())));
            case "cps" -> {
                String sep = new String[]{" | ", " / ", " - "}[m.ch("sep")];
                String v = String.valueOf(ClientModMain.cps(true));
                if (m.b("right")) v += sep + ClientModMain.cps(false);
                out.add(line(m, "CPS", v));
            }
            case "ping" -> {
                int ping = 0;
                if (mc.getNetworkHandler() != null) {
                    PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (e != null) ping = e.getLatency();
                }
                out.add(line(m, "Ping", ping + (m.b("unit") ? " ms" : "")));
            }
            case "xyz" -> {
                int dec = Math.round(m.n("dec"));
                String f = "%." + dec + "f";
                String x = String.format(Locale.ROOT, f, mc.player.getX());
                String y = String.format(Locale.ROOT, f, mc.player.getY());
                String z = String.format(Locale.ROOT, f, mc.player.getZ());
                String dir = "";
                if (m.b("dir")) {
                    String n = mc.player.getHorizontalFacing().name();
                    dir = n.charAt(0) + n.substring(1).toLowerCase(Locale.ROOT);
                }
                if (m.ch("layout") == 0) {
                    out.add(line(m, "XYZ", x + ", " + y + ", " + z + (dir.isEmpty() ? "" : " (" + dir + ")")));
                } else {
                    out.add(m.b("label") ? "X: " + x : x);
                    out.add(m.b("label") ? "Y: " + y : y);
                    out.add(m.b("label") ? "Z: " + z : z);
                    if (!dir.isEmpty()) out.add(dir);
                }
            }
            case "reach" -> {
                long age = System.currentTimeMillis() - ClientModMain.lastReachTime;
                String v = "-";
                if (ClientModMain.lastReach >= 0 && age < m.n("hold") * 1000f) {
                    v = String.format(Locale.ROOT, "%." + Math.round(m.n("dec")) + "f", ClientModMain.lastReach);
                }
                out.add(line(m, "Reach", v));
            }
            default -> { }
        }
        return out;
    }

    private static void drawLines(DrawContext ctx, MinecraftClient mc, Module m, List<String> lines, float[] used) {
        TextRenderer tr = mc.textRenderer;
        float sc = m.n("scale");
        int pad = Math.round(m.n("padding"));
        int anchor = m.ch("anchor");
        int w = 0;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int bw = w + pad * 2;
        int bh = lines.size() * 10 - 1 + pad * 2;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        float px = (anchor == 1 || anchor == 3) ? sw - bw * sc - 4 : 4;
        float py = (anchor >= 2) ? sh - bh * sc - 4 - used[anchor] : 4 + used[anchor];
        px += m.n("offX");
        py += m.n("offY");
        used[anchor] += bh * sc + 2;
        BOUNDS.put(m.id, new float[]{px, py, bw * sc, bh * sc});

        int tc = m.c("textColor");
        var mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(px, py);
        mat.scale(sc, sc);
        int bg = m.c("bgColor");
        if (((bg >>> 24) & 255) > 0) ctx.fill(0, 0, bw, bh, bg);
        if (m.b("outline")) {
            ctx.fill(0, 0, bw, 1, tc);
            ctx.fill(0, bh - 1, bw, bh, tc);
            ctx.fill(0, 0, 1, bh, tc);
            ctx.fill(bw - 1, 0, bw, bh, tc);
        }
        boolean shadow = m.b("shadow");
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(tr, Text.literal(lines.get(i)), pad, pad + i * 10, tc, shadow);
        }
        mat.popMatrix();
    }

    // ---------------------------------------------------------------- armor

    private static void armor(DrawContext ctx, MinecraftClient mc, Module m) {
        TextRenderer tr = mc.textRenderer;
        PlayerEntity p = mc.player;
        List<ItemStack> st = new ArrayList<>();
        st.add(p.getEquippedStack(EquipmentSlot.HEAD));
        st.add(p.getEquippedStack(EquipmentSlot.CHEST));
        st.add(p.getEquippedStack(EquipmentSlot.LEGS));
        st.add(p.getEquippedStack(EquipmentSlot.FEET));
        if (m.b("held")) st.add(p.getMainHandStack());

        boolean vert = m.ch("orient") == 1;
        boolean nums = m.b("nums"), bars = m.b("bars"), frame = m.b("frame");
        int gap = Math.round(m.n("gap"));
        final int slot = 20;
        int n = st.size();
        int numW = 0;
        if (nums && vert) {
            for (ItemStack s : st) if (s.isDamageable()) numW = Math.max(numW, tr.getWidth(String.valueOf(s.getMaxDamage() - s.getDamage())));
            numW += 4;
        }
        int cw = vert ? slot + numW : n * slot + (n - 1) * gap;
        int chh = vert ? n * slot + (n - 1) * gap : slot + (nums ? 11 : 0);
        int pad = frame ? 3 : 0;
        int bw = cw + pad * 2, bh = chh + pad * 2;

        float sc = m.n("scale");
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int anchor = m.ch("anchor");
        float px = (anchor == 1 || anchor == 3) ? sw - bw * sc : 0;
        float py = (anchor >= 2) ? sh - bh * sc : 0;
        px += m.n("offX");
        py += m.n("offY");
        BOUNDS.put(m.id, new float[]{px, py, bw * sc, bh * sc});

        var mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(px, py);
        mat.scale(sc, sc);

        if (frame) {
            ctx.fill(0, 0, bw, bh, 0xDD1A1A1A);
            ctx.fill(0, 0, bw, 1, 0xFF3C3C3C);
            ctx.fill(0, bh - 1, bw, bh, 0xFF3C3C3C);
            ctx.fill(0, 0, 1, bh, 0xFF3C3C3C);
            ctx.fill(bw - 1, 0, bw, bh, 0xFF3C3C3C);
        }
        int numColor = m.c("numColor");
        int baseColor = m.c("barColor");
        for (int i = 0; i < n; i++) {
            ItemStack s = st.get(i);
            int sx = pad + (vert ? 0 : i * (slot + gap));
            int sy = pad + (vert ? i * (slot + gap) : (nums ? 11 : 0));
            ctx.fill(sx, sy, sx + slot, sy + slot, 0xFF8B8B8B);
            ctx.fill(sx + 1, sy + 1, sx + slot - 1, sy + slot - 1, 0xFF555555);
            ctx.fill(sx, sy, sx + slot - 1, sy + 1, 0xFF2A2A2A);
            ctx.fill(sx, sy, sx + 1, sy + slot - 1, 0xFF2A2A2A);
            if (s.isEmpty()) continue;
            ctx.drawItem(s, sx + 2, sy + 2);
            if (s.isDamageable()) {
                int left = s.getMaxDamage() - s.getDamage();
                float frac = Math.max(0f, Math.min(1f, left / (float) s.getMaxDamage()));
                if (bars) {
                    int col = m.b("byDur") ? (0xFF000000 | Setting.Color.hsv(frac * 120f, 1f, 0.85f)) : baseColor;
                    ctx.fill(sx + 2, sy + slot - 4, sx + slot - 2, sy + slot - 2, 0xFF1A1A1A);
                    ctx.fill(sx + 2, sy + slot - 4, sx + 2 + Math.round((slot - 4) * frac), sy + slot - 2, col);
                }
                if (nums) {
                    String t = String.valueOf(left);
                    if (vert) ctx.drawText(tr, Text.literal(t), sx + slot + 3, sy + 6, numColor, true);
                    else ctx.drawText(tr, Text.literal(t), sx + slot / 2 - tr.getWidth(t) / 2, sy - 10, numColor, true);
                }
            }
        }
        mat.popMatrix();
    }
}
