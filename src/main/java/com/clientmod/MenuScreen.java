package com.clientmod;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuScreen extends Screen {
    private static final int SB = 104, HEAD = 26;
    private static final int WHITE = 0xFFFFFFFF, MUTED = 0xFF8D98A8, TEXT = 0xFFD5DBE4;
    private static final String[][] NAV = {{"All", "all"}, {"HUD", "hud"}, {"Visual", "visual"}, {"Utility", "util"}};

    private interface Act { void run(double mx, double my); }

    private static final class Hit {
        final int x, y, w, h;
        final Act click, drag;
        final boolean content;

        Hit(int x, int y, int w, int h, Act click, Act drag, boolean content) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.click = click; this.drag = drag; this.content = content;
        }

        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    private int wx, wy, ww, wh, cx, cy, cw, ch;
    private String cat = "All";
    private Module sel;
    private boolean menuPage;
    private double scroll, maxScroll;
    private int accent = 0xFF2DD4BF;

    public MenuScreen() {
        super(Text.literal("Client"));
    }

    @Override
    protected void init() {
        ww = Math.min(width - 16, 430);
        wh = Math.min(height - 16, 250);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        cx = wx + SB + 10;
        cy = wy + HEAD + 6;
        cw = ww - SB - 20;
        ch = wh - HEAD - 12;
    }

    // ------------------------------------------------------------ helpers

    private void txt(DrawContext c, String s, int x, int y, int col) {
        c.drawTextWithShadow(textRenderer, Text.literal(s), x, y, col);
    }

    private void rect(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + h, col);
    }

    private boolean inContent(double mx, double my) {
        return mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
    }

    private void hit(int x, int y, int w, int h, Act click, Act drag) {
        if (y + h < cy || y > cy + ch) return;
        hits.add(new Hit(x, y, w, h, click, drag, true));
    }

    private void fixed(int x, int y, int w, int h, Act click) {
        hits.add(new Hit(x, y, w, h, click, null, false));
    }

    private void toggle(DrawContext c, int x, int y, boolean on) {
        rect(c, x, y, 22, 10, on ? accent : 0x44FFFFFF);
        rect(c, x + (on ? 12 : 1), y + 1, 9, 8, WHITE);
    }

    private void checker(DrawContext c, int x, int y, int w, int h) {
        for (int yy = 0; yy < h; yy += 4) {
            for (int xx = 0; xx < w; xx += 4) {
                boolean a = (((xx >> 2) + (yy >> 2)) & 1) == 0;
                c.fill(x + xx, y + yy, Math.min(x + xx + 4, x + w), Math.min(y + yy + 4, y + h), a ? 0xFF6A6A6A : 0xFF3A3A3A);
            }
        }
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        hits.clear();
        accent = ModuleManager.MENU.c("accent") | 0xFF000000;
        int a = Math.max(0, Math.min(255, Math.round(ModuleManager.MENU.n("opacity") * 2.55f)));

        rect(c, wx - 1, wy - 1, ww + 2, wh + 2, 0x33FFFFFF);
        rect(c, wx, wy, ww, wh, (a << 24) | 0x0A0D14);

        // sidebar
        rect(c, wx, wy, SB, wh, 0x48000000);
        rect(c, wx + SB - 1, wy, 1, wh, 0x1AFFFFFF);
        Icons.draw(c, "box", wx + 10, wy + 8, 2, accent, WHITE);
        txt(c, "CLIENT", wx + 32, wy + 11, WHITE);
        int ny = wy + 34;
        for (String[] n : NAV) {
            boolean on = !menuPage && sel == null && cat.equals(n[0]);
            nav(c, n[1], n[0], ny, on, mx, my, () -> { cat = n[0]; sel = null; menuPage = false; scroll = 0; });
            ny += 24;
        }
        nav(c, "hud", "HUD editor", ny, false, mx, my, () -> client.setScreen(new HudEditorScreen()));
        nav(c, "cog", "Menu", wy + wh - 52, menuPage, mx, my, () -> { menuPage = true; sel = null; scroll = 0; });
        txt(c, "Key: Options >", wx + 8, wy + wh - 24, MUTED);
        txt(c, "Controls", wx + 8, wy + wh - 14, MUTED);

        // header
        rect(c, wx + SB, wy, ww - SB, HEAD, 0x22000000);
        rect(c, wx + SB, wy + HEAD, ww - SB, 1, 0x1AFFFFFF);
        String title = menuPage ? "Menu settings" : sel != null ? sel.name : cat.equals("All") ? "All modules" : cat + " modules";
        txt(c, title, cx, wy + 9, WHITE);
        txt(c, "ESC to close", wx + ww - 74, wy + 9, MUTED);

        // content
        c.enableScissor(cx - 2, cy, cx + cw + 2, cy + ch);
        int y = cy - (int) scroll;
        int end;
        if (menuPage) end = settings(c, ModuleManager.MENU, y, mx, my);
        else if (sel != null) end = detail(c, sel, y, mx, my);
        else end = grid(c, y, mx, my);
        c.disableScissor();

        maxScroll = Math.max(0, (end - (cy - (int) scroll)) - ch);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    private void nav(DrawContext c, String icon, String label, int y, boolean on, int mx, int my, Runnable r) {
        boolean hov = mx >= wx + 6 && mx < wx + SB - 6 && my >= y && my < y + 20;
        if (on || hov) rect(c, wx + 6, y, SB - 12, 20, on ? 0x26FFFFFF : 0x12FFFFFF);
        if (on) rect(c, wx + 6, y, 2, 20, accent);
        Icons.draw(c, icon, wx + 14, y + 2, 2, on ? accent : 0xFFAAB4C2, WHITE);
        txt(c, label, wx + 36, y + 6, on ? WHITE : 0xFFAAB4C2);
        fixed(wx + 6, y, SB - 12, 20, (x, yy) -> r.run());
    }

    // ------------------------------------------------------------ grid

    private int grid(DrawContext c, int y, int mx, int my) {
        int cardW = 118, cardH = 52, gap = 8;
        int cols = Math.max(1, (cw + gap) / (cardW + gap));
        int i = 0;
        for (Module m : ModuleManager.all()) {
            if (!cat.equals("All") && !m.category.equals(cat)) continue;
            int x = cx + (i % cols) * (cardW + gap);
            int yy = y + (i / cols) * (cardH + gap);
            card(c, m, x, yy, cardW, cardH, mx, my);
            i++;
        }
        return y + ((i + cols - 1) / cols) * (cardH + gap);
    }

    private void card(DrawContext c, Module m, int x, int y, int w, int h, int mx, int my) {
        boolean on = m.enabled && m.implemented;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h && inContent(mx, my);
        rect(c, x, y, w, h, on ? 0x2AFFFFFF : hov ? 0x20FFFFFF : 0x14FFFFFF);
        int bc = on ? accent : 0x22FFFFFF;
        rect(c, x, y, w, 1, bc); rect(c, x, y + h - 1, w, 1, bc);
        rect(c, x, y, 1, h, bc); rect(c, x + w - 1, y, 1, h, bc);
        rect(c, x + 8, y + 8, 24, 24, 0x55000000);
        Icons.draw(c, m.id, x + 12, y + 12, 2, on ? accent : m.implemented ? 0xFF9AA5B5 : 0xFF566070, WHITE);
        txt(c, m.name, x + 8, y + 38, m.implemented ? WHITE : MUTED);
        final Module mm = m;
        hit(x, y, w, h, (px, py) -> { sel = mm; scroll = 0; }, null);
        if (m.implemented) {
            toggle(c, x + w - 30, y + 10, m.enabled);
            hit(x + w - 32, y + 6, 28, 18, (px, py) -> mm.enabled = !mm.enabled, null);
        } else {
            txt(c, "SOON", x + w - 34, y + 10, MUTED);
        }
    }

    // ------------------------------------------------------------ detail

    private int detail(DrawContext c, Module m, int y, int mx, int my) {
        txt(c, "< Back", cx, y + 2, MUTED);
        hit(cx, y, 44, 12, (px, py) -> { sel = null; scroll = 0; }, null);
        y += 18;
        Icons.draw(c, m.id, cx, y, 3, m.enabled && m.implemented ? accent : MUTED, WHITE);
        txt(c, m.name, cx + 32, y + 3, WHITE);
        txt(c, m.desc, cx + 32, y + 15, MUTED);
        if (m.implemented) {
            toggle(c, cx + cw - 26, y + 4, m.enabled);
            hit(cx + cw - 28, y, 28, 18, (px, py) -> m.enabled = !m.enabled, null);
            int rx = cx + cw - 70;
            rect(c, rx, y + 2, 38, 14, 0x22FFFFFF);
            txt(c, "Reset", rx + 5, y + 5, TEXT);
            hit(rx, y + 2, 38, 14, (px, py) -> { for (Setting s : m.all()) s.reset(); }, null);
        }
        y += 34;
        if (!m.implemented) {
            txt(c, "This module is planned and not built yet.", cx, y, MUTED);
            return y + 20;
        }
        if (m.id.equals("itf")) {
            ItemStack held = client != null && client.player != null ? client.player.getMainHandStack() : ItemStack.EMPTY;
            if (held.isEmpty()) {
                txt(c, "Hold an item in your hand to edit it.", cx, y + 4, MUTED);
            } else {
                c.drawItem(held, cx, y);
                txt(c, "Editing: " + held.getName().getString(), cx + 22, y + 4, TEXT);
            }
            y += 24;
        }
        return settings(c, m, y, mx, my);
    }

    private int settings(DrawContext c, Module m, int y, int mx, int my) {
        for (Map.Entry<String, List<Setting>> g : m.groups.entrySet()) {
            String name = g.getKey().toUpperCase();
            txt(c, name, cx, y, accent);
            int tw = textRenderer.getWidth(name);
            rect(c, cx + tw + 6, y + 4, cw - tw - 6, 1, 0x22FFFFFF);
            y += 14;
            for (Setting s : g.getValue()) y = row(c, s, y, mx, my);
            y += 6;
        }
        return y;
    }

    private int row(DrawContext c, Setting s, int y, int mx, int my) {
        int right = cx + cw;
        txt(c, s.label, cx + 4, y + 5, TEXT);

        if (s instanceof Setting.Bool b) {
            toggle(c, right - 26, y + 4, b.value);
            hit(right - 30, y, 30, 18, (px, py) -> b.value = !b.value, null);
            return y + 18;
        }
        if (s instanceof Setting.Num n) {
            int tw = 100, tx = right - tw - 56, ty = y + 8;
            rect(c, tx, ty, tw, 3, 0x33FFFFFF);
            int fw = Math.round(tw * Math.max(0f, Math.min(1f, n.fraction())));
            rect(c, tx, ty, fw, 3, accent);
            rect(c, tx + fw - 2, ty - 3, 4, 9, WHITE);
            txt(c, n.fmt(), tx + tw + 8, y + 5, accent);
            Act set = (px, py) -> n.set(n.min + (float) Math.max(0, Math.min(1, (px - tx) / tw)) * (n.max - n.min));
            hit(tx - 3, y + 2, tw + 6, 14, set, set);
            return y + 18;
        }
        if (s instanceof Setting.Choice ch2) {
            int total = 0;
            for (String o : ch2.opts) total += textRenderer.getWidth(o) + 10 + 3;
            int x = right - total + 3;
            for (int i = 0; i < ch2.opts.length; i++) {
                int pw = textRenderer.getWidth(ch2.opts[i]) + 10;
                boolean on = ch2.value == i;
                rect(c, x, y + 3, pw, 12, on ? accent : 0x22FFFFFF);
                txt(c, ch2.opts[i], x + 5, y + 5, on ? 0xFF06312B : TEXT);
                final int idx = i;
                hit(x, y + 3, pw, 12, (px, py) -> ch2.value = idx, null);
                x += pw + 3;
            }
            return y + 18;
        }
        if (s instanceof Setting.Color col) {
            int sx = right - 32;
            checker(c, sx, y + 3, 28, 12);
            rect(c, sx, y + 3, 28, 12, col.argb());
            rect(c, sx - 1, y + 2, 30, 1, 0x88FFFFFF); rect(c, sx - 1, y + 15, 30, 1, 0x88FFFFFF);
            rect(c, sx - 1, y + 2, 1, 14, 0x88FFFFFF); rect(c, sx + 28, y + 2, 1, 14, 0x88FFFFFF);
            String hex = String.format("#%08X", col.argb());
            txt(c, hex, sx - 8 - textRenderer.getWidth(hex), y + 5, MUTED);
            hit(sx - 90, y, 122, 18, (px, py) -> col.open = !col.open, null);
            y += 18;
            if (col.open) y = picker(c, col, y);
            return y;
        }
        return y + 18;
    }

    // ------------------------------------------------------------ color picker

    private int picker(DrawContext c, Setting.Color col, int y) {
        int px = cx + 6, pw = 168, svh = 72;
        int top = y + 2;
        rect(c, px - 4, top - 2, pw + 8 + 58, svh + 98, 0x30000000);

        // saturation / value square
        for (int i = 0; i < pw; i += 2) {
            float s = i / (float) (pw - 1);
            int topCol = 0xFF000000 | Setting.Color.hsv(col.chroma ? col.h : col.h, s, 1f);
            c.fillGradient(px + i, top, px + i + 2, top + svh, topCol, 0xFF000000);
        }
        int mxp = px + Math.round(col.s * (pw - 1)), myp = top + Math.round((1 - col.v) * (svh - 1));
        rect(c, mxp - 3, myp - 3, 7, 1, WHITE); rect(c, mxp - 3, myp + 3, 7, 1, WHITE);
        rect(c, mxp - 3, myp - 3, 1, 7, WHITE); rect(c, mxp + 3, myp - 3, 1, 7, WHITE);
        final int ftop = top;
        Act sv = (mx, my) -> {
            col.s = (float) Math.max(0, Math.min(1, (mx - px) / (pw - 1)));
            col.v = 1f - (float) Math.max(0, Math.min(1, (my - ftop) / (svh - 1)));
        };
        hit(px, top, pw, svh, sv, sv);

        // hue bar
        int hy = top + svh + 6;
        for (int i = 0; i < pw; i++) c.fill(px + i, hy, px + i + 1, hy + 8, 0xFF000000 | Setting.Color.hsv(i * 360f / pw, 1f, 1f));
        int hx = px + Math.round(col.h / 360f * (pw - 1));
        rect(c, hx - 1, hy - 2, 3, 12, WHITE);
        Act hue = (mx, my) -> col.h = (float) Math.max(0, Math.min(1, (mx - px) / (pw - 1))) * 359.9f;
        hit(px, hy - 2, pw, 12, hue, hue);

        // opacity bar
        int oy = hy + 14;
        checker(c, px, oy, pw, 8);
        int rgb = col.rgb();
        for (int i = 0; i < pw; i += 2) {
            int al = Math.round(i / (float) (pw - 1) * 255f);
            c.fill(px + i, oy, px + i + 2, oy + 8, (al << 24) | rgb);
        }
        int ox = px + Math.round(col.a * (pw - 1));
        rect(c, ox - 1, oy - 2, 3, 12, WHITE);
        Act op = (mx, my) -> col.a = (float) Math.max(0, Math.min(1, (mx - px) / (pw - 1)));
        hit(px, oy - 2, pw, 12, op, op);
        txt(c, Math.round(col.a * 100) + " %", px + pw + 8, oy, TEXT);

        // preview + chroma + hex
        checker(c, px + pw + 8, top, 40, 40);
        rect(c, px + pw + 8, top, 40, 40, col.argb());
        int ry = oy + 16;
        txt(c, "Chroma", px, ry + 1, TEXT);
        toggle(c, px + 56, ry, col.chroma);
        hit(px + 54, ry - 2, 28, 14, (mx, my) -> col.chroma = !col.chroma, null);
        txt(c, String.format("#%08X", col.argb()), px + 100, ry + 1, MUTED);
        return ry + 22;
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double mx = click.x(), my = click.y();
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (!h.in(mx, my)) continue;
                if (h.content && !inContent(mx, my)) continue;
                h.click.run(mx, my);
                if (h.drag != null) dragging = h;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            dragging.drag.run(click.x(), click.y());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 14));
        return true;
    }

    @Override
    public void removed() {
        ModuleManager.save();
        ClientModMain.oneConfigPush();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
