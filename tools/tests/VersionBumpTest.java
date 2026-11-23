import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class VersionBumpTest {
    private VersionBumpTest() {
    }

    public static void main(String[] args) throws Exception {
        codeAdvancesWithoutCreatingDisplayBuild();
        previousBuildNumberIsIgnoredAndRemoved();
        missingVersionCodeIsRejectedWithoutModification();
        zeroVersionCodeIsRejectedWithoutModification();
        versionCodeOverflowIsRejectedWithoutModification();
        patchTenDoesNotCarry();
        patchTwentySixCarriesIntoMinor();
        minorTwentySixCarriesIntoMajor();
        System.out.println("VersionBumpTest passed");
    }

    private static void codeAdvancesWithoutCreatingDisplayBuild() throws Exception {
        Path fixture = fixture();
        try {
            Files.writeString(fixture, "major=3\nminor=0\npatch=4\nversionCode=68\n");
            VersionBump.main(new String[] {fixture.toString()});
            Properties next = read(fixture);
            assertEquals(5, Integer.parseInt(next.getProperty("patch")), "patch advances");
            assertEquals(69, Integer.parseInt(next.getProperty("versionCode")), "code advances from 68");
            if (next.containsKey("build")) throw new AssertionError("display build was added");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void previousBuildNumberIsIgnoredAndRemoved() throws Exception {
        Path fixture = fixture();
        try {
            Files.writeString(fixture, "major=3\nminor=0\npatch=5\nbuild=900\nversionCode=69\n");
            VersionBump.main(new String[] {fixture.toString()});
            Properties next = read(fixture);
            assertEquals(6, Integer.parseInt(next.getProperty("patch")), "patch advances");
            assertEquals(70, Integer.parseInt(next.getProperty("versionCode")), "code advances from 69");
            if (next.containsKey("build")) throw new AssertionError("legacy build property was retained");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void missingVersionCodeIsRejectedWithoutModification() throws Exception {
        Path fixture = fixture();
        String original = "major=3\nminor=0\npatch=5\nbuild=900\n";
        try {
            Files.writeString(fixture, original);
            expectRejectedWithoutModification(fixture, original, "missing");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void zeroVersionCodeIsRejectedWithoutModification() throws Exception {
        Path fixture = fixture();
        String original = "major=3\nminor=0\npatch=5\nversionCode=0\n";
        try {
            Files.writeString(fixture, original);
            expectRejectedWithoutModification(fixture, original, "positive");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void versionCodeOverflowIsRejectedWithoutModification() throws Exception {
        Path fixture = fixture();
        String original = "major=3\nminor=0\npatch=5\nversionCode=2147483647\n";
        try {
            Files.writeString(fixture, original);
            expectRejectedWithoutModification(fixture, original, "overflow");
        } finally {
            Files.deleteIfExists(fixture);
        }
    }

    private static void expectRejectedWithoutModification(Path fixture, String original, String reason)
            throws Exception {
        try {
            VersionBump.main(new String[] {fixture.toString()});
            throw new AssertionError("invalid versionCode was accepted: " + reason);
        } catch (IllegalArgumentException | java.util.NoSuchElementException expected) {
            if (!expected.getMessage().contains(reason)) {
                throw new AssertionError("wrong validation reason: " + expected.getMessage(), expected);
            }
        }
        if (!original.equals(Files.readString(fixture))) {
            throw new AssertionError("rejected file must not be modified");
        }
    }

    private static Path fixture() throws Exception {
        String directory = System.getProperty(
                "version.bump.test.dir",
                System.getProperty("java.io.tmpdir"));
        if (directory == null) throw new IllegalStateException("version test directory unavailable");
        return Files.createTempFile(Path.of(directory), "version-", ".properties");
    }

    private static Properties read(Path fixture) throws Exception {
        Properties next = new Properties();
        try (var input = Files.newInputStream(fixture)) { next.load(input); }
        return next;
    }

    private static void patchTenDoesNotCarry() {
        VersionBump.Version version = VersionBump.bump(3, 0, 9);
        assertEquals(3, version.major, "major");
        assertEquals(0, version.minor, "minor unchanged before 26");
        assertEquals(10, version.patch, "patch continues after 9");
    }

    private static void patchTwentySixCarriesIntoMinor() {
        VersionBump.Version version = VersionBump.bump(3, 0, 25);
        assertEquals(3, version.major, "major");
        assertEquals(1, version.minor, "minor carry");
        assertEquals(0, version.patch, "patch reset");
    }

    private static void minorTwentySixCarriesIntoMajor() {
        VersionBump.Version version = VersionBump.bump(3, 25, 25);
        assertEquals(4, version.major, "major carry");
        assertEquals(0, version.minor, "minor reset");
        assertEquals(0, version.patch, "patch reset");
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
