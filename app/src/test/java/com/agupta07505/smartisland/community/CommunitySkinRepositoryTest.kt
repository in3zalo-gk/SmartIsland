/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package com.agupta07505.smartisland.community

import android.content.Context
import android.content.res.AssetManager
import com.agupta07505.smartisland.model.CommunitySkinParser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class CommunitySkinRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun repository(bundled: Map<String, String> = emptyMap()): CommunitySkinRepository {
        val context = mockk<Context>()
        val assets = mockk<AssetManager>()
        every { context.filesDir } returns temporaryFolder.root
        every { context.assets } returns assets
        every { assets.open(any()) } answers {
            val path = firstArg<String>()
            ByteArrayInputStream(requireNotNull(bundled[path]) { "Unknown test asset: $path" }.toByteArray())
        }
        return CommunitySkinRepository(context)
    }

    private fun skinJson(id: String, name: String = "Skin $id") =
        """{"version":1,"id":"$id","name":"$name","author":"Tester","license":"MIT","baseStyle":"classic","states":{"idle":{"colors":["#112233"]}}}"""

    @Test
    fun importListAndReadPersistJsonInTheCommunityDirectory() = runBlocking {
        val repository = repository()
        val json = skinJson("night-sky")

        val imported = repository.import(json)

        assertTrue(imported.isSuccess)
        assertEquals("night-sky.json", imported.getOrThrow().name)
        assertEquals(listOf("night-sky.json"), repository.list().map(File::getName))
        assertEquals(json, repository.read("night-sky").getOrThrow())
    }

    @Test
    fun duplicateIdDoesNotReplaceTheExistingSkinByDefault() = runBlocking {
        val repository = repository()
        val original = skinJson("same-id", "Original")
        val duplicate = skinJson("same-id", "Duplicate")

        assertTrue(repository.import(original).isSuccess)
        val duplicateResult = repository.import(duplicate)

        assertTrue(duplicateResult.isFailure)
        assertEquals(original, repository.read("same-id").getOrThrow())
    }

    @Test
    fun explicitReplaceOverwritesTheExistingSkin() = runBlocking {
        val repository = repository()
        val original = skinJson("same-id", "Original")
        val replacement = skinJson("same-id", "Replacement")

        assertTrue(repository.import(original).isSuccess)
        assertTrue(repository.import(replacement, replace = true).isSuccess)

        assertEquals(replacement, repository.read("same-id").getOrThrow())
        assertEquals(listOf("same-id.json"), repository.list().map(File::getName))
    }

    @Test
    fun removeDeletesOnlyValidExistingSkinIds() = runBlocking {
        val repository = repository()
        assertTrue(repository.import(skinJson("to-remove")).isSuccess)

        assertTrue(repository.remove("to-remove"))
        assertTrue(repository.list().isEmpty())
        assertFalse(repository.remove("to-remove"))
        assertFalse(repository.remove("../to-remove"))
    }

    @Test
    fun listAndReadRejectFilesOverTheMaximumReadableSize() = runBlocking {
        val repository = repository()
        val directory = File(temporaryFolder.root, "community_skins")
        val tooLarge = File(directory, "too-large.json")
        tooLarge.writeBytes(ByteArray(CommunitySkinParser.MAX_BYTES + 1) { 'x'.code.toByte() })

        assertTrue(repository.list().isEmpty())
        assertTrue(repository.read("too-large").isFailure)
    }

    @Test
    fun importRejectsNewSkinsAfterTheLibraryLimit() = runBlocking {
        val repository = repository()

        repeat(64) { index ->
            assertTrue("skin $index should import", repository.import(skinJson("skin-$index")).isSuccess)
        }

        assertEquals(64, repository.list().size)
        assertTrue(repository.import(skinJson("one-too-many")).isFailure)
        assertEquals(64, repository.list().size)
    }

    @Test
    fun bundledAssetsAreParserValidAndContainTheSixPublicIdleStyleNames() {
        val assetsRoot = sequenceOf(File("app/src/main/assets"), File("src/main/assets"))
            .firstOrNull(File::isDirectory) ?: error("Bundled assets directory is missing")
        val assetDirectory = File(assetsRoot, CommunitySkinRepository.BUNDLED_ASSET_DIRECTORY)
        assertEquals(CommunitySkinRepository.BUNDLED_SKIN_FILES.toSet(), assetDirectory.list().orEmpty().toSet())

        CommunitySkinRepository.BUNDLED_SKIN_FILES.forEach { name ->
            val result = CommunitySkinParser.parse(File(assetDirectory, name).readText())
            assertTrue("$name must be valid: ${result.exceptionOrNull()?.message}", result.isSuccess)
        }
    }

    @Test
    fun bundledInstallRunsOnceAndNeverRestoresRemovedOrImportedSkins() = runBlocking {
        val bundled = CommunitySkinRepository.BUNDLED_SKIN_FILES.associate { fileName ->
            val id = fileName.removeSuffix(".json")
            "${CommunitySkinRepository.BUNDLED_ASSET_DIRECTORY}/$fileName" to skinJson(id)
        }
        val repository = repository(bundled)
        val importedObsidian = skinJson("obsidian", "User choice")

        assertTrue(repository.import(importedObsidian).isSuccess)
        assertEquals(5, repository.installBundledSkinsIfNeeded().getOrThrow())
        assertEquals(importedObsidian, repository.read("obsidian").getOrThrow())
        assertTrue(repository.remove("black-hole"))

        assertEquals(0, repository.installBundledSkinsIfNeeded().getOrThrow())
        assertTrue(repository.read("black-hole").isFailure)
        assertEquals(5, repository.list().size)
    }
}
