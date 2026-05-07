package org.movzx.dibella.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppBackupTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(AppBackup::class.java)

    @Test
    fun `test serialization and deserialization`() {
        val settings =
            AppSettingsBackup(
                nsfw = "X",
                sort = "Most Reacted",
                period = "AllTime",
                type = "all",
                tagIds = null,
                pageLimit = 20,
                gridColumns = 2,
                apiKey = "fake-api-key",
            )

        val favorites =
            listOf(
                FavoriteImageBackup(
                    id = 12345,
                    url = "uuid-1",
                    nsfw = true,
                    type = "image",
                    timestamp = System.currentTimeMillis(),
                )
            )

        val backup = AppBackup(version = 1, settings = settings, favorites = favorites)
        val json = adapter.toJson(backup)

        assertNotNull(json)

        val deserialized = adapter.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(backup.version, deserialized?.version)
        assertEquals(backup.settings?.nsfw, deserialized?.settings?.nsfw)
        assertEquals(backup.favorites.size, deserialized?.favorites?.size)
        assertEquals(backup.favorites[0].id, deserialized?.favorites?.get(0)?.id)
    }
}
