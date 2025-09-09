// Source: https://stackoverflow.com/a/53538597

package electrodynamics.util;
import java.awt.*;
import java.lang.reflect.Method;

public final class Device {
    private enum JavaVersion {
        V8,
        V9
    }

    private static final JavaVersion JAVA_VERSION = getJavaVersion();

    private static JavaVersion getJavaVersion() {
        final String versionString = System.getProperty("java.version");
        if (versionString.startsWith("1.8")) return JavaVersion.V8;
        if (versionString.startsWith("9.")) return JavaVersion.V9;
        throw new RuntimeException("Unsupported Java version");
    }

    public static GraphicsConfiguration getCurrentConfiguration(final Component component) {
        final GraphicsConfiguration graphicsConfiguration = component.getGraphicsConfiguration();
        if (graphicsConfiguration == null) {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        } else {
            return graphicsConfiguration;
        }
    }

    public static GraphicsDevice getCurrentDevice(final Component component) {
        final GraphicsConfiguration graphicsConfiguration = component.getGraphicsConfiguration();
        if (graphicsConfiguration == null) {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        } else {
            return graphicsConfiguration.getDevice();
        }
    }

    public static boolean isOnRetinaDisplay(final Component component) {
        switch (JAVA_VERSION) {
            case V8: return isOnRetinaDisplayJava8(component);
            case V9: return isOnRetinaDisplayJava9(component);
            default: throw new AssertionError("Unreachable");
        }
    }

    public static double getDisplayScalingFactor(final Component component) {
        switch (JAVA_VERSION) {
            case V8: return getDisplayScalingFactorJava8(component);
            case V9: return getDisplayScalingFactorJava9(component);
            default: throw new AssertionError("Unreachable");
        }
    }

    private static boolean isOnRetinaDisplayJava8(final Component component) {
        final GraphicsDevice device = getCurrentDevice(component);
        try {
            final Method getScaleFactorMethod = device.getClass().getMethod("getScaleFactor");
            final Object scale = getScaleFactorMethod.invoke(device);
            return scale instanceof Integer && ((Integer) scale).intValue() == 2;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean isOnRetinaDisplayJava9(final Component component) {
        return ! getCurrentConfiguration(component).getDefaultTransform().isIdentity();
    }

    private static double getDisplayScalingFactorJava8(final Component component) {
        return isOnRetinaDisplayJava8(component) ? 2.0 : 1.0;
    }

    private static double getDisplayScalingFactorJava9(final Component component) {
        return getCurrentConfiguration(component).getDefaultTransform().getScaleX();
    }
}