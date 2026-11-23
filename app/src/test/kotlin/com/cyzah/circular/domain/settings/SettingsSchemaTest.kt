package com.cyzah.circular.domain.settings

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable

class SettingsSchemaTest {
    @Test
    fun rowAssignmentsFollowColumnNamesAcrossSchemaReordering() {
        val definitions = Arrays.asList(
            SettingsSchema.Definition("second", "second", SettingsSchema.Kind.INTEGER),
            SettingsSchema.Definition("first", "first", SettingsSchema.Kind.INTEGER)
        )
        val row = SettingsSchema.Row(definitions)
        row.put("first", 37)
        row.put("second", 83)

        Assert.assertArrayEquals(arrayOf<Any>(83, 37), row.build())
    }

    @Test
    fun addedColumnRequiresAnExplicitAssignment() {
        val definitions = Arrays.asList(
            SettingsSchema.Definition("first", "first", SettingsSchema.Kind.STRING),
            SettingsSchema.Definition("added", "added", SettingsSchema.Kind.INTEGER)
        )
        val row = SettingsSchema.Row(definitions)
        row.put("first", null)

        Assert.assertThrows<IllegalStateException?>(IllegalStateException::class.java, ThrowingRunnable { row.build() })
        row.put("added", 0)
        Assert.assertArrayEquals(arrayOf<Any?>(null, 0), row.build())
    }

    @Test
    fun unknownAndDuplicateColumnAssignmentsFailFast() {
        val row = SettingsSchema.Row(SettingsSchema.DEFINITIONS)
        Assert.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            ThrowingRunnable { row.put("typo", 7) })
        row.put("mode", 2)
        Assert.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            ThrowingRunnable { row.put("mode", 0) })
    }

    @Test
    fun duplicateSchemaColumnsCannotAliasAnAssignment() {
        val definitions = Arrays.asList(
            SettingsSchema.Definition("first", "same", SettingsSchema.Kind.INTEGER),
            SettingsSchema.Definition("second", "same", SettingsSchema.Kind.INTEGER)
        )

        Assert.assertThrows<IllegalArgumentException?>(
            IllegalArgumentException::class.java,
            ThrowingRunnable { SettingsSchema.Row(definitions) })
    }

    @Test
    fun cursorColumnsAreUnique() {
        val unique: MutableSet<String?> = HashSet<String?>()
        for (column in SettingsSchema.CURSOR_COLUMNS) {
            unique.add(column)
        }
        Assert.assertEquals(SettingsSchema.CURSOR_COLUMNS.size.toLong(), unique.size.toLong())
    }

    @Test
    fun everyCursorColumnHasOneCanonicalDefinition() {
        val columns: MutableList<String?> = ArrayList<String?>()
        for (definition in SettingsSchema.DEFINITIONS) {
            if (definition.cursorColumn() != null) {
                columns.add(definition.cursorColumn())
            }
        }

        Assert.assertEquals(SettingsSchema.CURSOR_COLUMNS.size.toLong(), columns.size.toLong())
        Assert.assertEquals(Arrays.asList<String?>(*SettingsSchema.CURSOR_COLUMNS), columns)
    }

    @Test
    fun preferenceKeysAreUniqueAcrossCurrentAndLegacySettings() {
        val keys: MutableSet<String?> = HashSet<String?>()
        for (definition in SettingsSchema.DEFINITIONS) {
            if (definition.preferenceKey() == null) {
                continue
            }
            Assert.assertEquals(
                "Duplicate key: " + definition.preferenceKey(),
                true,
                keys.add(definition.preferenceKey())
            )
        }
    }
}
