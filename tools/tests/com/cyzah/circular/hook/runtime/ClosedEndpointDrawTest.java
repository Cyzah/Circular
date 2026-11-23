package com.cyzah.circular.hook.runtime;

import android.graphics.Canvas;
import android.view.View;
import com.cyzah.circular.core.graphics.IconPainter;
import com.cyzah.circular.core.graphics.animation.BatteryTransferState;
import com.cyzah.circular.core.graphics.animation.BatteryTransferTimeline;
import com.cyzah.circular.domain.settings.ContentOffsets;
import com.cyzah.circular.domain.settings.DualStyle;
import com.cyzah.circular.domain.settings.SettingsSchema;
import com.cyzah.circular.domain.settings.SettingsSnapshot;
import com.cyzah.circular.domain.settings.SettingsValues;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import sun.misc.Unsafe;

/** Runs the real private runtime draw method with recorded Android/process boundaries. */
public final class ClosedEndpointDrawTest {
    private static final ContentOffsets OFFSETS = new ContentOffsets(3, -5, 11, -7, -13, 17);
    private static final int TINT = 0xff123abc;
    private static final float SHIFT = 4.25f;
    private static int checks;

    public static void main(String[] args) throws Throwable {
        for (DualStyle style : DualStyle.values()) {
            for (int host : new int[] {5, 6}) {
                for (int phase = 0; phase < 3; phase++) {
                    projectedUserEndpoint(style, host, phase);
                }
            }
            for (boolean rtl : new boolean[] {false, true}) {
                missingHostKeepsPhysicalPixels(style, rtl);
                retainedMergedRowKeepsItsLocalFrame(style, rtl);
            }
            drawFailureRestoresCanvas(style);
        }
        System.out.println("PASS " + checks + " actual closed-endpoint draws: four styles, both hosts, "
                + "normal/waiting/transfer masks, user offsets, projected/local pixels and Canvas cleanup");
    }

    private static void projectedUserEndpoint(DualStyle style, int host, int phase) throws Throwable {
        Fixture f = new Fixture(style, false, phase);
        f.owner.from = host;
        f.draw();
        f.verifyCommon(phase);
        require(f.owner.batteryTransferProjection.calls == 1, "The source endpoint must be projected");
        require(f.owner.batteryTransferProjection.source == f.module.statusHost, "Projection must use host 0");
        require(f.owner.batteryTransferProjection.target == f.owner, "Projection must target the active copy");
        close(SHIFT, f.owner.batteryTransferProjection.canvasShift, "Copied-row transform must be included");
        close(17.25f, f.canvas.translateX, "Projected origin X");
        close(-8.75f, f.canvas.translateY, "Projected origin Y");
        close(.625f, f.canvas.scaleX, "Projected X scale must survive independently");
        close(.8f, f.canvas.scaleY, "Projected Y scale must survive independently");
        close(.375f, f.module.endpoint.reference, "Projected content must retain the source pixel reference");
        checks++;
    }

    private static void missingHostKeepsPhysicalPixels(DualStyle style, boolean rtl) throws Throwable {
        Fixture f = new Fixture(style, false, 1);
        f.module.statusHost = null;
        f.owner.battery.direction = rtl ? View.LAYOUT_DIRECTION_RTL : 0;
        f.draw();
        f.verifyCommon(1);
        require(f.owner.batteryTransferProjection.calls == 1, "A missing source must exercise projection failure");
        f.verifyLocal(rtl);
        checks++;
    }

    private static void retainedMergedRowKeepsItsLocalFrame(DualStyle style, boolean rtl) throws Throwable {
        Fixture f = new Fixture(style, true, 0);
        f.owner.battery.direction = rtl ? View.LAYOUT_DIRECTION_RTL : 0;
        f.draw();
        f.verifyCommon(0);
        require(f.owner.batteryTransferProjection.calls == 0, "Retained merged rows must not adopt host-0 coordinates");
        f.verifyLocal(rtl);
        checks++;
    }

    private static void drawFailureRestoresCanvas(DualStyle style) throws Throwable {
        Fixture f = new Fixture(style, false, 1);
        RuntimeException primary = new IllegalStateException("endpoint draw");
        f.module.endpoint.drawFailure = primary;
        Throwable actual = null;
        try { f.draw(); } catch (Throwable error) { actual = error; }
        require(actual == primary, "Drawing must preserve the primary failure");
        require(f.canvas.getSaveCount() == 1 && f.canvas.restores == 1, "Drawing failure must restore the outer Canvas");
        require(f.owner.batteryRingRenderer.calls == 0, "Failed content must not continue into another layer");
        checks++;
    }

    private static final class Fixture {
        final ModuleMain module = new ModuleMain();
        final IconSession owner = new IconSession();
        final Canvas canvas = new Canvas();
        final IconSessionPainter renderer;
        final SessionPaintFrame frame = new SessionPaintFrame();
        final BatteryTransferState transfer;
        final Method draw;
        long now = 1000;

        Fixture(DualStyle style, boolean keep, int phase) throws Exception {
            module.sourceSettings = settings(style, keep);
            module.centerSettings = module.sourceSettings.controlCenter();
            module.statusHost = new IconSession();
            module.state.battery = 82;
            module.state.wifiKnown = module.state.wifiEnabled = module.state.wifiConnected = true;
            module.state.wifiLevel = 3;
            module.state.mobileKnown = module.state.secondaryKnown = true;
            module.state.simPresent = module.state.secondarySimPresent = true;
            module.state.inService = module.state.secondaryInService = true;
            module.state.mobileLevel = 4; module.state.secondaryLevel = 2;
            owner.module = module;
            transfer = new BatteryTransferState(new BatteryTransferTimeline(() -> now));
            transfer.update(0, 0, 0, true, true, true, true, now);
            if (phase > 0) {
                transfer.update(1, 0, 1, true, true, true, false, now);
                transfer.update(0, 0, 0, true, true, true, false, now);
                require(transfer.pointVisible() && !transfer.ringTransferVisible(), "Fixture must reach the held endpoint");
                if (phase == 2) transfer.update(0, 0, 0, true, true, true, true, now);
            }
            frame.currentTextColor = TINT;
            frame.transferState = transfer;
            renderer = allocate(IconSessionPainter.class);
            set(renderer, "frame", frame);
            set(renderer, "canvasRowShift", SHIFT);
            draw = IconSessionPainter.class.getDeclaredMethod("drawClosedControlCenterEndpoint", IconSession.class, Canvas.class);
            draw.setAccessible(true);
        }

        void draw() throws Throwable {
            try { draw.invoke(renderer, owner, canvas); }
            catch (InvocationTargetException error) { throw error.getCause(); }
        }

        void verifyCommon(int phase) {
            IconPainter endpoint = module.endpoint;
            require(endpoint.calls == 1, "Exactly one complete endpoint must be painted");
            require(module.lastRulerSettings == module.sourceSettings, "The endpoint must use user settings, not control-center settings");
            require(endpoint.settings.dualStyle() == module.sourceSettings.dualStyle(), "User style must survive the handoff");
            require(OFFSETS.equals(endpoint.settings.contentOffsets()), "All six user offsets must survive the handoff");
            require(ContentOffsets.DEFAULT.equals(module.centerSettings.contentOffsets()), "Fixture must distinguish the zero-offset control-center template");
            require(endpoint.state == module.state && endpoint.state.showWifi(), "Current Wi-Fi and battery state must reach the full draw");
            require(endpoint.coloured == module.sourceSettings.coloured() && endpoint.tint == TINT, "Tint and colour setting must be forwarded");
            require(endpoint.mask == (phase == 0 ? 7 : 263), "Only the ring may be hidden during waiting/transfer, mask=" + endpoint.mask);
            require(endpoint.motionCalls == 1 && endpoint.motion == 0, "Static endpoint must explicitly clear inherited motion");
            close(0, endpoint.x, "Content-local origin X"); close(0, endpoint.y, "Content-local origin Y");
            close(120, endpoint.size, "Content draw uses the canonical frame"); close(1, endpoint.alpha, "Complete endpoint opacity");
            require(!owner.stackedDrawing && owner.stackedAlpha == 0 && owner.painterMask == 0, "Closed endpoint must discard overlay ownership");
            require(owner.swappedWifiTransfer.resets == 1, "Moving Wi-Fi state must reset");
            require(canvas.translations == 1 && canvas.scales == 1 && canvas.restores == 1 && canvas.getSaveCount() == 1,
                    "The complete draw must share one balanced outer transform");
            require(owner.batteryRingRenderer.calls == (phase == 0 ? 0 : 1), "Waiting point and released trace must both have ring ownership");
            if (phase > 0) {
                require(owner.batteryRingRenderer.geometry == module.ring && owner.batteryRingRenderer.state == transfer,
                        "The transition layer must use the same user ring and live state");
                close(0, owner.batteryRingRenderer.x, "Transfer ring local X");
                close(0, owner.batteryRingRenderer.y, "Transfer ring local Y");
                close(1, owner.batteryRingRenderer.unit, "Transfer ring must share the content's outer scale");
            }
        }

        void verifyLocal(boolean rtl) {
            // Size 28dp at density 2 is clipped to the local host's 48px height.
            close((rtl ? 0 : 160 - 48) + 2.5f - SHIFT, canvas.translateX, "Fallback local origin X");
            close(-1.5f, canvas.translateY, "Fallback local origin Y");
            close(.4f, canvas.scaleX, "Fallback local X scale");
            close(.4f, canvas.scaleY, "Fallback local Y scale");
            close(.4f, module.endpoint.reference, "Fallback pixel reference must match its actual clipped frame");
        }
    }

    private static SessionSettings settings(DualStyle style, boolean keep) {
        MapValues values = new MapValues();
        values.data.put(SettingsSchema.MODE, 2); values.data.put(SettingsSchema.SIZE, 28);
        values.data.put(SettingsSchema.COLOUR, style.ordinal() % 2 == 0);
        values.data.put(SettingsSchema.GROUPED_DOTS, style == DualStyle.GROUPED);
        values.data.put(SettingsSchema.CAPSULE_DUAL, style == DualStyle.CAPSULE);
        values.data.put(SettingsSchema.DUO_DUAL, style == DualStyle.DUO);
        values.data.put(SettingsSchema.KEEP_MERGED_SHADE, keep);
        values.data.put(SettingsSchema.TOP_CONTENT_OFFSET_X_HALF_PX, OFFSETS.topX);
        values.data.put(SettingsSchema.TOP_CONTENT_OFFSET_Y_HALF_PX, OFFSETS.topY);
        values.data.put(SettingsSchema.CENTER_CONTENT_OFFSET_X_HALF_PX, OFFSETS.centerX);
        values.data.put(SettingsSchema.CENTER_CONTENT_OFFSET_Y_HALF_PX, OFFSETS.centerY);
        values.data.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_X_HALF_PX, OFFSETS.bottomX);
        values.data.put(SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_HALF_PX, OFFSETS.bottomY);
        return SessionSettings.from(SettingsSnapshot.from(values), true);
    }

    private static final class MapValues implements SettingsValues {
        final Map<String, Object> data = new HashMap<>();
        public boolean contains(String key) { return data.containsKey(key); }
        public int getInt(String key, int fallback) { Object value = data.get(key); return value instanceof Number ? ((Number) value).intValue() : fallback; }
        public boolean getBoolean(String key, boolean fallback) { Object value = data.get(key); return value instanceof Boolean ? (Boolean) value : fallback; }
        public String getString(String key, String fallback) { Object value = data.get(key); return value instanceof String ? (String) value : fallback; }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }

    private static void close(float expected, float actual, String label) {
        if (!(Math.abs(expected - actual) <= .0001f)) {
            throw new AssertionError(label + "; expected=" + expected + " actual=" + actual);
        }
    }

    private static void require(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
}
