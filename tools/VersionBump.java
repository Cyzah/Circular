import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class VersionBump {
    private static final int RADIX = 26;
    public static final class Version {
        public final int major;
        public final int minor;
        public final int patch;

        Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }
    }

    private VersionBump() {
    }

    public static Version bump(int major, int minor, int patch) {
        int nextPatch = patch + 1;
        int nextMinor = minor;
        int nextMajor = major;
        if (nextPatch >= RADIX) {
            nextPatch = 0;
            nextMinor++;
            if (nextMinor >= RADIX) {
                nextMinor = 0;
                nextMajor++;
            }
        }
        return new Version(nextMajor, nextMinor, nextPatch);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VersionBump <version.properties>");
        }
        Path path = Path.of(args[0]);
        Properties properties = new Properties();
        try (var input = Files.newInputStream(path)) {
            properties.load(input);
        }
        Version next = bump(
                integer(properties, "major"),
                integer(properties, "minor"),
                integer(properties, "patch"));
        int currentVersionCode = integer(properties, "versionCode");
        if (currentVersionCode <= 0) {
            throw new IllegalArgumentException("versionCode must be positive");
        }
        if (currentVersionCode == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("versionCode overflow");
        }
        int nextVersionCode = currentVersionCode + 1;
        String text = "major=" + next.major + "\n"
                + "minor=" + next.minor + "\n"
                + "patch=" + next.patch + "\n"
                + "versionCode=" + nextVersionCode + "\n";
        Files.writeString(path, text, StandardCharsets.UTF_8);
        System.out.println(next.major + "." + next.minor + "." + next.patch);
    }

    private static int integer(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("missing version property: " + key);
        }
        int parsed = Integer.parseInt(value.trim());
        if (parsed < 0) {
            throw new IllegalArgumentException("negative version property: " + key);
        }
        return parsed;
    }
}
