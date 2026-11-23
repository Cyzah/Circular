package com.cyzah.circular.framework.export

import java.io.IOException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.rules.TemporaryFolder

class ExportPolicyTest {
    @JvmField @Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun archiveNameKeepsDeviceIdentityAndDate() {
        Assert.assertEquals(
            "SystemUI_Xiaomi_25098PN5AC_20261028.zip",
            ExportPolicy.archiveName("Xiaomi", "25098PN5AC", "20261028")
        )
    }

    @Test
    fun archiveNameSanitizesUnsafeDeviceText() {
        Assert.assertEquals(
            "SystemUI_Xiaomi_Redmi_Note_20261028.zip",
            ExportPolicy.archiveName("Xiaomi/", "Redmi Note", "20261028")
        )
    }

    @Test
    @Throws(Exception::class)
    fun sharedFileAcceptsOnlyDirectExistingArchives() {
        val archive = temporaryFolder.newFile("SystemUI_Xiaomi_25098PN5AC_20261028.zip")

        Assert.assertEquals(
            archive.getCanonicalFile(),
            ExportPolicy.sharedFile(
                temporaryFolder.getRoot(),
                archive.getName()
            )
        )
        Assert.assertThrows<IOException?>(
            IOException::class.java,
            ThrowingRunnable {
                ExportPolicy.sharedFile(
                    temporaryFolder.getRoot(),
                    "../" + archive.getName()
                )
            })
        Assert.assertThrows<IOException?>(
            IOException::class.java,
            ThrowingRunnable {
                ExportPolicy.sharedFile(
                    temporaryFolder.getRoot(),
                    "nested/" + archive.getName()
                )
            })
        Assert.assertThrows<IOException?>(
            IOException::class.java,
            ThrowingRunnable {
                ExportPolicy.sharedFile(
                    temporaryFolder.getRoot(),
                    "SystemUI_missing_20261028.zip"
                )
            })
    }

    @Test
    @Throws(Exception::class)
    fun sharedFileRejectsBrokenCanonicalIdentity() {
        val archive = temporaryFolder.newFile("SystemUI_Xiaomi_25098PN5AC_20261028.zip")
        val folder = temporaryFolder.newFolder("not-a-file.zip")

        Assert.assertTrue(archive.isFile())
        Assert.assertThrows<IOException?>(
            IOException::class.java,
            ThrowingRunnable {
                ExportPolicy.sharedFile(
                    temporaryFolder.getRoot(),
                    folder.getName()
                )
            })
    }
}
