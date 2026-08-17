/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package comart.tools.jdbgen.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The release description of GitHub carries more than the distribution
 * archive, and an archive is not to be trusted with the paths it contains.
 */
public class UpdateManagerTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    /** a zip whose entries are the keys of <code>entries</code>. */
    private static File zipOf(Path dir, String name, Map<String, String> entries) throws IOException {
        File zip = dir.resolve(name).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (Map.Entry<String, String> e: entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                if (e.getValue() != null)
                    zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zip;
    }

    @Test
    public void theDistributionArchiveIsPickedOutOfTheAssets() {
        JsonObject release = json("{\"tag_name\":\"v0.3.1\",\"assets\":["
                + "{\"name\":\"checksums.txt\",\"browser_download_url\":\"https://x/checksums.txt\",\"size\":12},"
                + "{\"name\":\"jdbgen-0.3.1-sources.jar\",\"browser_download_url\":\"https://x/src.jar\",\"size\":34},"
                + "{\"name\":\"other-0.3.1.zip\",\"browser_download_url\":\"https://x/other.zip\",\"size\":56},"
                + "{\"name\":\"jdbgen-0.3.1.zip\",\"browser_download_url\":\"https://x/jdbgen-0.3.1.zip\",\"size\":4711}"
                + "]}");

        Optional<UpdateManager.ReleaseAsset> asset = UpdateManager.selectZipAsset(release);

        assertTrue(asset.isPresent(), "the release ships a jdbgen zip");
        assertEquals("jdbgen-0.3.1.zip", asset.get().getName());
        assertEquals("https://x/jdbgen-0.3.1.zip", asset.get().getUrl());
        assertEquals(4711L, asset.get().getSize(), "the size drives the progress bar");
    }

    @Test
    public void aReleaseWithoutAnArchiveCannotBeInstalled() {
        assertFalse(UpdateManager.selectZipAsset(json("{\"tag_name\":\"v0.3.1\",\"assets\":["
                + "{\"name\":\"notes.md\",\"browser_download_url\":\"https://x/notes.md\"}]}"))
                .isPresent());
        assertFalse(UpdateManager.selectZipAsset(json("{\"tag_name\":\"v0.3.1\",\"assets\":[]}"))
                .isPresent());
        assertFalse(UpdateManager.selectZipAsset(json("{\"tag_name\":\"v0.3.1\"}")).isPresent(),
                "a release without an assets array is no reason to fail");
        assertFalse(UpdateManager.selectZipAsset(null).isPresent());
    }

    @Test
    public void anAssetWithoutASizeStillCountsAsTheArchive() {
        Optional<UpdateManager.ReleaseAsset> asset = UpdateManager.selectZipAsset(
                json("{\"assets\":[{\"name\":\"jdbgen-1.0.zip\","
                        + "\"browser_download_url\":\"https://x/jdbgen-1.0.zip\"}]}"));

        assertTrue(asset.isPresent());
        assertEquals(0L, asset.get().getSize(), "an unknown size shows an indeterminate bar");
    }

    @Test
    public void theVersionDirectoryOfTheArchiveIsStripped(@TempDir Path dir) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("jdbgen-0.3.1/", null);
        entries.put("jdbgen-0.3.1/jdbgen-0.3.1.jar", "jar");
        entries.put("jdbgen-0.3.1/lib/gson.jar", "lib");
        entries.put("jdbgen-0.3.1/templates/java_model.hbs", "template");
        entries.put("stray.txt", "dropped");
        File zip = zipOf(dir, "jdbgen-0.3.1.zip", entries);
        File dest = dir.resolve("extracted").toFile();

        UpdateManager.extractZip(zip, dest);

        assertTrue(new File(dest, "jdbgen-0.3.1.jar").isFile(),
                "the content lines up with the installation directory");
        assertEquals("lib", Files.readString(dest.toPath().resolve("lib/gson.jar")));
        assertEquals("template", Files.readString(dest.toPath().resolve("templates/java_model.hbs")));
        assertFalse(new File(dest, "jdbgen-0.3.1").exists(), "the version directory is gone");
        assertFalse(new File(dest, "stray.txt").exists(),
                "an entry without a directory part has nowhere to go");
    }

    @Test
    public void anEntryPointingOutOfTheTargetIsRefused(@TempDir Path dir) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("jdbgen-0.3.1/../../evil.txt", "owned");
        File zip = zipOf(dir, "escaping.zip", entries);
        File dest = dir.resolve("extracted").toFile();

        assertThrows(IOException.class, () -> UpdateManager.extractZip(zip, dest));
        assertFalse(dir.resolve("evil.txt").toFile().exists());
        assertFalse(dir.getParent().resolve("evil.txt").toFile().exists());
    }

    @Test
    public void aRelativeEntryIsRefusedBeforeItIsStripped(@TempDir Path dir) throws Exception {
        // stripping the first segment would turn "../evil.txt" into a harmless
        // name, so the entry has to be rejected as it is
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("../evil.txt", "owned");
        File zip = zipOf(dir, "escaping.zip", entries);
        File dest = dir.resolve("extracted").toFile();

        assertThrows(IOException.class, () -> UpdateManager.extractZip(zip, dest));
        assertFalse(new File(dest, "evil.txt").exists());
    }

    @Test
    public void onlyTheInstallationOfThisRunIsListed(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("jdbgen-0.3.0.jar"), new byte[0]);
        Files.write(dir.resolve("jdbgen-0.2.0.jar"), new byte[0]);
        Files.write(dir.resolve("something.jar"), new byte[0]);
        Files.createDirectory(dir.resolve("jdbgen-dir.jar"));

        List<File> jars = UpdateManager.listJars(dir.toFile());

        assertEquals(2, jars.size(), "only the jars of the application count");
        assertEquals("jdbgen-0.2.0.jar", jars.get(0).getName());
        assertEquals("jdbgen-0.3.0.jar", jars.get(1).getName());
    }

    @Test
    public void anAssetIsRecognizedRegardlessOfItsLetterCase() {
        Optional<UpdateManager.ReleaseAsset> asset = UpdateManager.selectZipAsset(
                json("{\"assets\":[{\"name\":\"JDBGen-0.3.1.ZIP\","
                        + "\"browser_download_url\":\"https://x/JDBGen-0.3.1.ZIP\"}]}"));

        assertTrue(asset.isPresent());
        assertEquals("JDBGen-0.3.1.ZIP", asset.get().getName(),
                "the name is downloaded as it is published");
    }

    @Test
    public void anIncompleteAssetIsSkippedRatherThanUsed() {
        // an asset without a download url cannot be fetched, and the release
        // may well carry a usable one behind it
        Optional<UpdateManager.ReleaseAsset> asset = UpdateManager.selectZipAsset(
                json("{\"assets\":["
                        + "\"not an object\","
                        + "{\"name\":\"jdbgen-0.3.1.zip\"},"
                        + "{\"browser_download_url\":\"https://x/anonymous.zip\"},"
                        + "{\"name\":\"jdbgen-0.3.1.zip\",\"browser_download_url\":\"https://x/ok.zip\"}"
                        + "]}"));

        assertTrue(asset.isPresent());
        assertEquals("https://x/ok.zip", asset.get().getUrl());
    }

    @Test
    public void anAssetSizeThatIsNoNumberOnlyCostsTheProgressBar() {
        Optional<UpdateManager.ReleaseAsset> asset = UpdateManager.selectZipAsset(
                json("{\"assets\":[{\"name\":\"jdbgen-1.0.zip\",\"size\":\"huge\","
                        + "\"browser_download_url\":\"https://x/jdbgen-1.0.zip\"}]}"));

        assertTrue(asset.isPresent());
        assertEquals(0L, asset.get().getSize());
    }

    @Test
    public void theFirstSegmentOfAnEntryNameIsWhatIsStripped() {
        assertEquals("lib/gson.jar", UpdateManager.stripFirstSegment("jdbgen-0.3.1/lib/gson.jar"));
        assertEquals("lib/gson.jar", UpdateManager.stripFirstSegment("/jdbgen-0.3.1/lib/gson.jar"),
                "an absolute entry name is no reason to keep the version directory");
        assertEquals("templates/", UpdateManager.stripFirstSegment("jdbgen-0.3.1/templates/"));
        assertEquals("", UpdateManager.stripFirstSegment("jdbgen-0.3.1/"),
                "the version directory itself has nothing left to create");
        assertEquals("", UpdateManager.stripFirstSegment("readme.txt"),
                "an entry outside of the version directory is dropped");
        assertEquals("", UpdateManager.stripFirstSegment(""));
    }

    @Test
    public void anEmptyArchiveLeavesAnEmptyDirectoryBehind(@TempDir Path dir) throws Exception {
        File zip = zipOf(dir, "jdbgen-0.3.1.zip", new LinkedHashMap<>());
        File dest = dir.resolve("extracted").toFile();

        UpdateManager.extractZip(zip, dest);

        assertTrue(dest.isDirectory(), "the target is created even when nothing is unpacked");
        assertEquals(0, dest.list().length);
    }

    @Test
    public void aDirectoryEntryOfTheArchiveIsCreatedEvenWhenItIsEmpty(@TempDir Path dir)
            throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("jdbgen-0.3.1/drivers/", null);
        File zip = zipOf(dir, "jdbgen-0.3.1.zip", entries);
        File dest = dir.resolve("extracted").toFile();

        UpdateManager.extractZip(zip, dest);

        assertTrue(new File(dest, "drivers").isDirectory());
    }

    @Test
    public void aWindowsStyleEntryNameIsUnderstood(@TempDir Path dir) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("jdbgen-0.3.1\\lib\\gson.jar", "lib");
        File zip = zipOf(dir, "jdbgen-0.3.1.zip", entries);
        File dest = dir.resolve("extracted").toFile();

        UpdateManager.extractZip(zip, dest);

        assertEquals("lib", Files.readString(dest.toPath().resolve("lib/gson.jar")));
    }

    @Test
    public void aDirectoryWithoutAnyJarOfTheApplicationIsNoInstallation(@TempDir Path dir)
            throws Exception {
        Files.write(dir.resolve("something.jar"), new byte[0]);

        assertTrue(UpdateManager.listJars(dir.toFile()).isEmpty());
        assertTrue(UpdateManager.listJars(new File(dir.toFile(), "nowhere")).isEmpty(),
                "a directory that is not there holds no jars either");
        assertTrue(UpdateManager.listJars(null).isEmpty());
    }

    @Test
    public void theStagingDirectoryIsRemovedWithEverythingInIt(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("staging/extracted/lib"));
        try (OutputStream os = Files.newOutputStream(dir.resolve("staging/extracted/lib/a.jar"))) {
            os.write(1);
        }

        UpdateManager.deleteRecursively(dir.resolve("staging").toFile());

        assertFalse(dir.resolve("staging").toFile().exists());
    }
}
