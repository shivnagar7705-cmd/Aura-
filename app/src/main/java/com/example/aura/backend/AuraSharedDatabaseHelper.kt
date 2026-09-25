package com.example.aura.backend

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.aura.model.*
import java.security.MessageDigest

class AuraSharedDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "aura_shared_production.db",
    null,
    1
) {

    companion object {
        @Volatile
        private var instance: AuraSharedDatabaseHelper? = null

        fun getInstance(context: Context): AuraSharedDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AuraSharedDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }

        fun hashPassword(password: String): String {
            val salt = "AURA_BEAT_2026_SALT"
            val bytes = (password + salt).toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE,
                username TEXT,
                role TEXT,
                password_hash TEXT,
                profile_image_url TEXT,
                is_suspended INTEGER,
                created_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS songs (
                id TEXT PRIMARY KEY,
                title TEXT,
                artist TEXT,
                artist_id TEXT,
                album TEXT,
                album_id TEXT,
                genre TEXT,
                release_date TEXT,
                description TEXT,
                lyrics TEXT,
                duration_ms INTEGER,
                audio_url TEXT,
                cover_url TEXT,
                download_permitted INTEGER,
                copyright_notice TEXT,
                status TEXT,
                plays_count INTEGER,
                likes_count INTEGER,
                is_featured INTEGER,
                is_trending INTEGER,
                is_new_release INTEGER,
                is_recommended INTEGER,
                created_at INTEGER,
                updated_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS artists (
                id TEXT PRIMARY KEY,
                name TEXT,
                image_url TEXT,
                bio TEXT,
                genre TEXT,
                monthly_listeners INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS albums (
                id TEXT PRIMARY KEY,
                title TEXT,
                artist TEXT,
                artist_id TEXT,
                cover_url TEXT,
                release_year TEXT,
                genre TEXT,
                track_ids TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS playlists (
                id TEXT PRIMARY KEY,
                title TEXT,
                description TEXT,
                cover_url TEXT,
                created_by TEXT,
                creator_name TEXT,
                is_featured INTEGER,
                song_ids TEXT,
                created_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS likes (
                user_id TEXT,
                song_id TEXT,
                created_at INTEGER,
                PRIMARY KEY(user_id, song_id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recently_played (
                user_id TEXT,
                song_id TEXT,
                played_at INTEGER,
                PRIMARY KEY(user_id, song_id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS downloads (
                id TEXT PRIMARY KEY,
                user_id TEXT,
                song_id TEXT,
                local_file_path TEXT,
                downloaded_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reports (
                id TEXT PRIMARY KEY,
                song_id TEXT,
                song_title TEXT,
                reporter_email TEXT,
                reason TEXT,
                details TEXT,
                type TEXT,
                status TEXT,
                created_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS banners (
                id TEXT PRIMARY KEY,
                title TEXT,
                subtitle TEXT,
                image_url TEXT,
                tag TEXT,
                target_song_id TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS admin_users (
                email TEXT PRIMARY KEY,
                password_hash TEXT,
                full_name TEXT,
                permissions TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Not needed for v1
    }
}
