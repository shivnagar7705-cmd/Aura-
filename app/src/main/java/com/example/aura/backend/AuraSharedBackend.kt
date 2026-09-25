package com.example.aura.backend

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.example.aura.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Await extension for Play Services / Firebase Tasks
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

class AuraSharedBackend private constructor(private val context: Context) {

    private val dbHelper = AuraSharedDatabaseHelper.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sessionPrefs = context.getSharedPreferences("aura_user_session", Context.MODE_PRIVATE)
    private var firestore: FirebaseFirestore? = null
    private var songsListener: ListenerRegistration? = null
    private var bannersListener: ListenerRegistration? = null

    // Reactive StateFlows observed by both User App and Admin App
    private val _publishedSongs = MutableStateFlow<List<Song>>(emptyList())
    val publishedSongs: StateFlow<List<Song>> = _publishedSongs.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _banners = MutableStateFlow<List<HomeBanner>>(emptyList())
    val banners: StateFlow<List<HomeBanner>> = _banners.asStateFlow()

    private val _reports = MutableStateFlow<List<ContentReport>>(emptyList())
    val reports: StateFlow<List<ContentReport>> = _reports.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentAdmin = MutableStateFlow<User?>(null)
    val currentAdmin: StateFlow<User?> = _currentAdmin.asStateFlow()

    init {
        restoreUserSession()
    }

    companion object {
        private const val TAG = "AuraSharedBackend"

        @Volatile
        private var instance: AuraSharedBackend? = null

        fun getInstance(context: Context): AuraSharedBackend {
            return instance ?: synchronized(this) {
                instance ?: AuraSharedBackend(context.applicationContext).also {
                    instance = it
                    it.initialize()
                }
            }
        }
    }

    private fun initialize() {
        scope.launch {
            try {
                seedInitialMusicCatalog()
                seedAdalatSongIfNeeded()
                restoreUserSession()
                refreshData()
                initFirebaseRealtimeSync()
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
            }
        }
    }

    fun refreshData() {
        _allSongs.value = queryAllSongs()
        _publishedSongs.value = _allSongs.value.filter { it.status == SongStatus.PUBLISHED }
        _banners.value = queryBanners()
        _reports.value = queryReports()
    }

    // ==========================================
    // CLOUD STORAGE OPERATIONS
    // ==========================================

    fun getAudioStorageDir(): File {
        val dir = File(context.filesDir, "aura_cloud_storage/audio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getImageStorageDir(): File {
        val dir = File(context.filesDir, "aura_cloud_storage/images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveAudioFile(inputStream: InputStream, originalFileName: String): String {
        val extension = originalFileName.substringAfterLast(".", "mp3")
        val fileName = "audio_${UUID.randomUUID()}.$extension"
        val destination = File(getAudioStorageDir(), fileName)
        FileOutputStream(destination).use { out ->
            inputStream.copyTo(out)
        }
        return destination.absolutePath
    }

    fun saveImageFile(inputStream: InputStream, originalFileName: String): String {
        val extension = originalFileName.substringAfterLast(".", "png")
        val fileName = "img_${UUID.randomUUID()}.$extension"
        val destination = File(getImageStorageDir(), fileName)
        FileOutputStream(destination).use { out ->
            inputStream.copyTo(out)
        }
        return destination.absolutePath
    }

    suspend fun uploadFileToFirebaseStorage(
        localFile: File,
        remoteFolder: String = "songs",
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                try {
                    auth.signInAnonymously().awaitTask()
                } catch (authEx: Exception) {
                    Log.w(TAG, "FirebaseAuth anonymous sign-in: ${authEx.message}")
                }
            }

            val storage = FirebaseStorage.getInstance()
            val safeName = localFile.name.replace(" ", "_").filter { it.isLetterOrDigit() || it == '.' || it == '_' }
            val storageRef = storage.reference.child("$remoteFolder/${UUID.randomUUID()}_$safeName")

            val uploadTask = storageRef.putFile(Uri.fromFile(localFile))
            uploadTask.addOnProgressListener { snap ->
                if (snap.totalByteCount > 0) {
                    val pct = ((100.0 * snap.bytesTransferred) / snap.totalByteCount).toInt()
                    onProgress?.invoke(pct)
                }
            }
            uploadTask.awaitTask()
            val downloadUri = storageRef.downloadUrl.awaitTask()
            Log.d(TAG, "Firebase Storage upload succeeded: $downloadUri")
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Firebase Storage: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncLocalSongAudioToCloud(song: Song): Result<Song> {
        val file = File(song.audioUrl)
        if (!file.exists()) {
            return Result.failure(IllegalStateException("Local audio file not found on this device"))
        }
        val uploadResult = uploadFileToFirebaseStorage(file, "songs")
        return if (uploadResult.isSuccess) {
            val cloudUrl = uploadResult.getOrThrow()
            var cloudCoverUrl = song.coverUrl
            val coverFile = File(song.coverUrl)
            if (coverFile.exists() && !song.coverUrl.startsWith("http")) {
                val coverUpload = uploadFileToFirebaseStorage(coverFile, "covers")
                if (coverUpload.isSuccess) {
                    cloudCoverUrl = coverUpload.getOrThrow()
                }
            }
            val updatedSong = song.copy(
                audioUrl = cloudUrl,
                coverUrl = cloudCoverUrl,
                updatedAt = System.currentTimeMillis()
            )
            updateSong(updatedSong)
            Result.success(updatedSong)
        } else {
            Result.failure(uploadResult.exceptionOrNull() ?: Exception("Cloud upload failed"))
        }
    }

    // ==========================================
    // AUTHENTICATION & ROLE-BASED AUTHORIZATION
    // ==========================================

    fun signUp(email: String, username: String, passwordPlain: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedUsername = username.trim()

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (trimmedUsername.length < 2) {
            return Result.failure(IllegalArgumentException("Username must be at least 2 characters."))
        }
        if (passwordPlain.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val db = dbHelper.writableDatabase
        val existing = db.query(
            "users",
            arrayOf("id"),
            "email = ?",
            arrayOf(trimmedEmail),
            null, null, null
        )
        val exists = existing.moveToFirst()
        existing.close()

        if (exists) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            email = trimmedEmail,
            username = trimmedUsername,
            role = "USER",
            passwordHash = AuraSharedDatabaseHelper.hashPassword(passwordPlain),
            profileImageUrl = "",
            isSuspended = false,
            createdAt = System.currentTimeMillis()
        )

        val cv = ContentValues().apply {
            put("id", user.id)
            put("email", user.email)
            put("username", user.username)
            put("role", user.role)
            put("password_hash", user.passwordHash)
            put("profile_image_url", user.profileImageUrl)
            put("is_suspended", if (user.isSuspended) 1 else 0)
            put("created_at", user.createdAt)
        }
        db.insert("users", null, cv)
        _currentUser.value = user
        persistUserSession(user.id)
        return Result.success(user)
    }

    fun login(email: String, passwordPlain: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val hash = AuraSharedDatabaseHelper.hashPassword(passwordPlain)

        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "users",
            null,
            "email = ? AND password_hash = ?",
            arrayOf(trimmedEmail, hash),
            null, null, null
        )

        val user = if (cursor.moveToFirst()) {
            parseUser(cursor)
        } else {
            null
        }
        cursor.close()

        if (user == null) {
            return Result.failure(IllegalArgumentException("Invalid email or password."))
        }
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("This account has been suspended by administration."))
        }

        _currentUser.value = user
        persistUserSession(user.id)
        return Result.success(user)
    }

    fun loginWithSocial(
        provider: String,
        email: String,
        displayName: String,
        profileImageUrl: String = ""
    ): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email is required for social login."))
        }

        val db = dbHelper.writableDatabase
        val cursor = db.query(
            "users",
            null,
            "email = ?",
            arrayOf(trimmedEmail),
            null, null, null
        )

        val existing = if (cursor.moveToFirst()) parseUser(cursor) else null
        cursor.close()

        val user = if (existing != null) {
            if (existing.isSuspended) {
                return Result.failure(IllegalStateException("This account has been suspended."))
            }
            val targetImg = if (existing.profileImageUrl.isEmpty()) profileImageUrl else existing.profileImageUrl
            if (targetImg.isNotEmpty() && targetImg != existing.profileImageUrl) {
                val cv = ContentValues().apply { put("profile_image_url", targetImg) }
                db.update("users", cv, "id = ?", arrayOf(existing.id))
            }
            existing.copy(profileImageUrl = targetImg)
        } else {
            val newUser = User(
                id = "user_${UUID.randomUUID()}",
                email = trimmedEmail,
                username = displayName.trim().ifBlank { trimmedEmail.substringBefore("@") },
                role = "USER",
                passwordHash = AuraSharedDatabaseHelper.hashPassword("social_${provider}_${UUID.randomUUID()}"),
                profileImageUrl = profileImageUrl,
                isSuspended = false,
                createdAt = System.currentTimeMillis()
            )
            val cv = ContentValues().apply {
                put("id", newUser.id)
                put("email", newUser.email)
                put("username", newUser.username)
                put("role", newUser.role)
                put("password_hash", newUser.passwordHash)
                put("profile_image_url", newUser.profileImageUrl)
                put("is_suspended", 0)
                put("created_at", newUser.createdAt)
            }
            db.insert("users", null, cv)
            newUser
        }

        _currentUser.value = user
        persistUserSession(user.id)
        return Result.success(user)
    }

    private fun persistUserSession(userId: String?) {
        if (userId == null) {
            sessionPrefs.edit().remove("active_user_id").apply()
        } else {
            sessionPrefs.edit().putString("active_user_id", userId).apply()
        }
    }

    private fun restoreUserSession() {
        try {
            val activeId = sessionPrefs.getString("active_user_id", null)
            val db = dbHelper.readableDatabase
            if (!activeId.isNullOrEmpty()) {
                val cursor = db.query("users", null, "id = ?", arrayOf(activeId), null, null, null)
                if (cursor.moveToFirst()) {
                    val user = parseUser(cursor)
                    if (!user.isSuspended) {
                        _currentUser.value = user
                        Log.d(TAG, "Restored active user session: ${user.username} (${user.email})")
                        cursor.close()
                        return
                    }
                }
                cursor.close()
            }

            // No active session found. Keep _currentUser as null so AuthScreen (Login/Signup) is presented.
            Log.d(TAG, "No active user session. Showing Auth Screen.")
        } catch (e: Exception) {
            Log.w(TAG, "Error restoring user session: ${e.message}")
        }
    }

    fun adminLogin(email: String, passwordPlain: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val hash = AuraSharedDatabaseHelper.hashPassword(passwordPlain)

        val db = dbHelper.readableDatabase

        // First check users table for role == 'ADMIN'
        var cursor = db.query(
            "users",
            null,
            "email = ? AND password_hash = ? AND role = 'ADMIN'",
            arrayOf(trimmedEmail, hash),
            null, null, null
        )

        var adminUser: User? = null
        if (cursor.moveToFirst()) {
            adminUser = parseUser(cursor)
        }
        cursor.close()

        // Also check admin_users table for system administrator
        if (adminUser == null) {
            cursor = db.query(
                "admin_users",
                null,
                "email = ? AND password_hash = ?",
                arrayOf(trimmedEmail, hash),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                val fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"))
                adminUser = User(
                    id = "admin_root",
                    email = trimmedEmail,
                    username = fullName,
                    role = "ADMIN",
                    passwordHash = hash,
                    profileImageUrl = "",
                    isSuspended = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            cursor.close()
        }

        if (adminUser == null) {
            return Result.failure(SecurityException("Access Denied: Invalid administrator credentials or role."))
        }

        _currentAdmin.value = adminUser
        return Result.success(adminUser)
    }

    fun logoutUser() {
        _currentUser.value = null
        persistUserSession(null)
    }

    fun logoutAdmin() {
        _currentAdmin.value = null
    }

    fun updateProfile(userId: String, newUsername: String, newProfileImageUrl: String): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("username", newUsername)
            put("profile_image_url", newProfileImageUrl)
        }
        val rows = db.update("users", cv, "id = ?", arrayOf(userId))
        if (rows > 0) {
            _currentUser.value?.let { current ->
                if (current.id == userId) {
                    _currentUser.value = current.copy(
                        username = newUsername,
                        profileImageUrl = newProfileImageUrl
                    )
                }
            }
            return true
        }
        return false
    }

    fun changePassword(userId: String, oldPasswordPlain: String, newPasswordPlain: String): Result<Unit> {
        if (newPasswordPlain.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        val oldHash = AuraSharedDatabaseHelper.hashPassword(oldPasswordPlain)
        val newHash = AuraSharedDatabaseHelper.hashPassword(newPasswordPlain)

        val db = dbHelper.writableDatabase
        val cursor = db.query("users", arrayOf("id"), "id = ? AND password_hash = ?", arrayOf(userId, oldHash), null, null, null)
        val matches = cursor.moveToFirst()
        cursor.close()

        if (!matches) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }

        val cv = ContentValues().apply { put("password_hash", newHash) }
        db.update("users", cv, "id = ?", arrayOf(userId))
        return Result.success(Unit)
    }

    fun deleteUserAccount(userId: String): Boolean {
        val db = dbHelper.writableDatabase
        db.delete("likes", "user_id = ?", arrayOf(userId))
        db.delete("recently_played", "user_id = ?", arrayOf(userId))
        db.delete("downloads", "user_id = ?", arrayOf(userId))
        db.delete("playlists", "created_by = ?", arrayOf(userId))
        val rows = db.delete("users", "id = ?", arrayOf(userId))
        if (rows > 0) {
            _currentUser.value = null
            return true
        }
        return false
    }

    // ==========================================
    // SONG REPOSITORY (SHARED WITH ADMIN & USER)
    // ==========================================

    fun uploadSong(
        title: String,
        artist: String,
        album: String,
        genre: String,
        releaseDate: String,
        description: String,
        lyrics: String?,
        audioFilePath: String,
        coverFilePath: String,
        downloadPermitted: Boolean,
        copyrightNotice: String,
        status: SongStatus,
        durationMs: Long = 214000L
    ): Song {
        val song = Song(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            artist = artist.trim(),
            album = album.trim(),
            genre = genre.trim(),
            releaseDate = releaseDate.trim(),
            description = description.trim(),
            lyrics = lyrics?.trim(),
            durationMs = if (durationMs > 0) durationMs else 180000L,
            audioUrl = audioFilePath,
            coverUrl = coverFilePath,
            downloadPermitted = downloadPermitted,
            copyrightNotice = copyrightNotice.trim(),
            status = status,
            playsCount = 0L,
            likesCount = 0L,
            isFeatured = status == SongStatus.PUBLISHED,
            isTrending = true,
            isNewRelease = true,
            isRecommended = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val db = dbHelper.writableDatabase
        val cv = songToContentValues(song)
        db.insert("songs", null, cv)

        refreshData()
        syncSongToFirestore(song)
        return song
    }

    fun updateSong(song: Song): Boolean {
        val db = dbHelper.writableDatabase
        val cv = songToContentValues(song.copy(updatedAt = System.currentTimeMillis()))
        val rows = db.update("songs", cv, "id = ?", arrayOf(song.id))
        if (rows > 0) {
            refreshData()
            syncSongToFirestore(song)
            return true
        }
        return false
    }

    fun setSongStatus(songId: String, newStatus: SongStatus): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("status", newStatus.name)
            put("updated_at", System.currentTimeMillis())
        }
        val rows = db.update("songs", cv, "id = ?", arrayOf(songId))
        if (rows > 0) {
            refreshData()
            updateSongStatusInFirestore(songId, newStatus)
            return true
        }
        return false
    }

    fun deleteSong(songId: String): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("songs", "id = ?", arrayOf(songId))
        if (rows > 0) {
            refreshData()
            deleteSongFromFirestore(songId)
            return true
        }
        return false
    }

    fun recordPlay(songId: String, userId: String?) {
        scope.launch {
            val db = dbHelper.writableDatabase
            db.execSQL("UPDATE songs SET plays_count = plays_count + 1 WHERE id = ?", arrayOf(songId))
            if (!userId.isNullOrEmpty()) {
                val cv = ContentValues().apply {
                    put("user_id", userId)
                    put("song_id", songId)
                    put("played_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict("recently_played", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            }
            refreshData()
        }
    }

    // ==========================================
    // LIKES & PLAYLISTS
    // ==========================================

    fun toggleLike(userId: String, songId: String): Boolean {
        val db = dbHelper.writableDatabase
        val cursor = db.query("likes", arrayOf("song_id"), "user_id = ? AND song_id = ?", arrayOf(userId, songId), null, null, null)
        val isLiked = cursor.moveToFirst()
        cursor.close()

        val nowLiked: Boolean
        if (isLiked) {
            db.delete("likes", "user_id = ? AND song_id = ?", arrayOf(userId, songId))
            db.execSQL("UPDATE songs SET likes_count = MAX(0, likes_count - 1) WHERE id = ?", arrayOf(songId))
            nowLiked = false
        } else {
            val cv = ContentValues().apply {
                put("user_id", userId)
                put("song_id", songId)
                put("created_at", System.currentTimeMillis())
            }
            db.insert("likes", null, cv)
            db.execSQL("UPDATE songs SET likes_count = likes_count + 1 WHERE id = ?", arrayOf(songId))
            nowLiked = true
        }
        refreshData()
        return nowLiked
    }

    fun isSongLiked(userId: String, songId: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query("likes", arrayOf("song_id"), "user_id = ? AND song_id = ?", arrayOf(userId, songId), null, null, null)
        val liked = cursor.moveToFirst()
        cursor.close()
        return liked
    }

    fun getLikedSongs(userId: String): List<Song> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT s.* FROM songs s
            INNER JOIN likes l ON s.id = l.song_id
            WHERE l.user_id = ? AND s.status = 'PUBLISHED'
            ORDER BY l.created_at DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId))
        val list = mutableListOf<Song>()
        while (cursor.moveToNext()) {
            list.add(parseSong(cursor))
        }
        cursor.close()
        return list
    }

    fun getRecentlyPlayed(userId: String): List<Song> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT s.* FROM songs s
            INNER JOIN recently_played r ON s.id = r.song_id
            WHERE r.user_id = ? AND s.status = 'PUBLISHED'
            ORDER BY r.played_at DESC LIMIT 20
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId))
        val list = mutableListOf<Song>()
        while (cursor.moveToNext()) {
            list.add(parseSong(cursor))
        }
        cursor.close()
        return list
    }

    fun getUserPlaylists(userId: String): List<Playlist> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("playlists", null, "created_by = ?", arrayOf(userId), null, null, "created_at DESC")
        val list = mutableListOf<Playlist>()
        while (cursor.moveToNext()) {
            list.add(parsePlaylist(cursor))
        }
        cursor.close()
        return list
    }

    fun getFeaturedPlaylists(): List<Playlist> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("playlists", null, "is_featured = 1", null, null, null, "created_at DESC")
        val list = mutableListOf<Playlist>()
        while (cursor.moveToNext()) {
            list.add(parsePlaylist(cursor))
        }
        cursor.close()
        return list
    }

    fun createPlaylist(userId: String, userName: String, title: String, description: String, coverUrl: String): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            coverUrl = coverUrl,
            createdBy = userId,
            creatorName = userName,
            isFeatured = false,
            songIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", playlist.id)
            put("title", playlist.title)
            put("description", playlist.description)
            put("cover_url", playlist.coverUrl)
            put("created_by", playlist.createdBy)
            put("creator_name", playlist.creatorName)
            put("is_featured", if (playlist.isFeatured) 1 else 0)
            put("song_ids", "")
            put("created_at", playlist.createdAt)
        }
        db.insert("playlists", null, cv)
        return playlist
    }

    fun addSongToPlaylist(playlistId: String, songId: String): Boolean {
        val db = dbHelper.writableDatabase
        val cursor = db.query("playlists", arrayOf("song_ids"), "id = ?", arrayOf(playlistId), null, null, null)
        if (cursor.moveToFirst()) {
            val raw = cursor.getString(0) ?: ""
            cursor.close()
            val ids = if (raw.isEmpty()) mutableListOf() else raw.split(",").toMutableList()
            if (!ids.contains(songId)) {
                ids.add(songId)
                val cv = ContentValues().apply { put("song_ids", ids.joinToString(",")) }
                db.update("playlists", cv, "id = ?", arrayOf(playlistId))
                return true
            }
        } else {
            cursor.close()
        }
        return false
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean {
        val db = dbHelper.writableDatabase
        val cursor = db.query("playlists", arrayOf("song_ids"), "id = ?", arrayOf(playlistId), null, null, null)
        if (cursor.moveToFirst()) {
            val raw = cursor.getString(0) ?: ""
            cursor.close()
            val ids = raw.split(",").filter { it.isNotEmpty() && it != songId }
            val cv = ContentValues().apply { put("song_ids", ids.joinToString(",")) }
            db.update("playlists", cv, "id = ?", arrayOf(playlistId))
            return true
        }
        cursor.close()
        return false
    }

    fun renamePlaylist(playlistId: String, newTitle: String): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply { put("title", newTitle.trim()) }
        return db.update("playlists", cv, "id = ?", arrayOf(playlistId)) > 0
    }

    fun deletePlaylist(playlistId: String): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete("playlists", "id = ?", arrayOf(playlistId)) > 0
    }

    // ==========================================
    // DOWNLOADS (LEGAL PERMITTED ONLY)
    // ==========================================

    fun downloadSong(userId: String, song: Song): Result<String> {
        if (!song.downloadPermitted) {
            return Result.failure(IllegalStateException("This track is not licensed for offline download."))
        }
        val source = File(song.audioUrl)
        if (!source.exists()) {
            return Result.failure(IllegalStateException("Source audio is currently unavailable."))
        }

        val offlineDir = File(context.filesDir, "aura_offline_downloads")
        if (!offlineDir.exists()) offlineDir.mkdirs()
        val destFile = File(offlineDir, "offline_${song.id}.wav")

        source.copyTo(destFile, overwrite = true)

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", UUID.randomUUID().toString())
            put("user_id", userId)
            put("song_id", song.id)
            put("local_file_path", destFile.absolutePath)
            put("downloaded_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("downloads", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return Result.success(destFile.absolutePath)
    }

    fun getDownloadedSongs(userId: String): List<Song> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT s.* FROM songs s
            INNER JOIN downloads d ON s.id = d.song_id
            WHERE d.user_id = ?
            ORDER BY d.downloaded_at DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId))
        val list = mutableListOf<Song>()
        while (cursor.moveToNext()) {
            list.add(parseSong(cursor))
        }
        cursor.close()
        return list
    }

    fun isSongDownloaded(userId: String, songId: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query("downloads", arrayOf("id"), "user_id = ? AND song_id = ?", arrayOf(userId, songId), null, null, null)
        val downloaded = cursor.moveToFirst()
        cursor.close()
        return downloaded
    }

    fun removeDownload(userId: String, songId: String): Boolean {
        val db = dbHelper.writableDatabase
        val cursor = db.query("downloads", arrayOf("local_file_path"), "user_id = ? AND song_id = ?", arrayOf(userId, songId), null, null, null)
        if (cursor.moveToFirst()) {
            val path = cursor.getString(0)
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }
        cursor.close()
        return db.delete("downloads", "user_id = ? AND song_id = ?", arrayOf(userId, songId)) > 0
    }

    // ==========================================
    // REPORTS & MODERATION (ADMIN & USER)
    // ==========================================

    fun submitReport(
        songId: String,
        songTitle: String,
        reporterEmail: String,
        reason: String,
        details: String,
        type: String
    ): ContentReport {
        val report = ContentReport(
            id = UUID.randomUUID().toString(),
            songId = songId,
            songTitle = songTitle,
            reporterEmail = reporterEmail,
            reason = reason,
            details = details,
            type = type,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", report.id)
            put("song_id", report.songId)
            put("song_title", report.songTitle)
            put("reporter_email", report.reporterEmail)
            put("reason", report.reason)
            put("details", report.details)
            put("type", report.type)
            put("status", report.status)
            put("created_at", report.createdAt)
        }
        db.insert("reports", null, cv)
        refreshData()
        return report
    }

    fun resolveReport(reportId: String, newStatus: String): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply { put("status", newStatus) }
        val rows = db.update("reports", cv, "id = ?", arrayOf(reportId))
        if (rows > 0) {
            refreshData()
            return true
        }
        return false
    }

    fun unpublishAndResolveReport(reportId: String, songId: String): Boolean {
        setSongStatus(songId, SongStatus.UNPUBLISHED)
        return resolveReport(reportId, "RESOLVED")
    }

    // ==========================================
    // ARTISTS & ALBUMS
    // ==========================================

    fun getAllArtists(): List<Artist> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("artists", null, null, null, null, null, "monthly_listeners DESC")
        val list = mutableListOf<Artist>()
        while (cursor.moveToNext()) {
            list.add(
                Artist(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url")),
                    bio = cursor.getString(cursor.getColumnIndexOrThrow("bio")),
                    genre = cursor.getString(cursor.getColumnIndexOrThrow("genre")),
                    monthlyListeners = cursor.getLong(cursor.getColumnIndexOrThrow("monthly_listeners"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getAllAlbums(): List<Album> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("albums", null, null, null, null, null, "release_year DESC")
        val list = mutableListOf<Album>()
        while (cursor.moveToNext()) {
            val trackIdsRaw = cursor.getString(cursor.getColumnIndexOrThrow("track_ids")) ?: ""
            list.add(
                Album(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                    artistId = cursor.getString(cursor.getColumnIndexOrThrow("artist_id")),
                    coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url")),
                    releaseYear = cursor.getString(cursor.getColumnIndexOrThrow("release_year")),
                    genre = cursor.getString(cursor.getColumnIndexOrThrow("genre")),
                    trackIds = if (trackIdsRaw.isEmpty()) emptyList() else trackIdsRaw.split(",")
                )
            )
        }
        cursor.close()
        return list
    }

    fun getAllUsers(): List<User> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("users", null, null, null, null, null, "created_at DESC")
        val list = mutableListOf<User>()
        while (cursor.moveToNext()) {
            list.add(parseUser(cursor))
        }
        cursor.close()
        return list
    }

    fun toggleUserSuspension(userId: String): Boolean {
        val db = dbHelper.writableDatabase
        val cursor = db.query("users", arrayOf("is_suspended"), "id = ?", arrayOf(userId), null, null, null)
        if (cursor.moveToFirst()) {
            val isSuspended = cursor.getInt(0) == 1
            cursor.close()
            val cv = ContentValues().apply { put("is_suspended", if (isSuspended) 0 else 1) }
            db.update("users", cv, "id = ?", arrayOf(userId))
            return true
        }
        cursor.close()
        return false
    }

    fun getListeningStats(): ListeningStats {
        val db = dbHelper.readableDatabase
        val all = queryAllSongs()
        val published = all.count { it.status == SongStatus.PUBLISHED }
        val unpublished = all.count { it.status != SongStatus.PUBLISHED }

        val totalStreams = all.sumOf { it.playsCount }

        var totalUsers = 0L
        val userCur = db.rawQuery("SELECT COUNT(*) FROM users", null)
        if (userCur.moveToFirst()) totalUsers = userCur.getLong(0)
        userCur.close()

        var totalPlaylists = 0L
        val plCur = db.rawQuery("SELECT COUNT(*) FROM playlists", null)
        if (plCur.moveToFirst()) totalPlaylists = plCur.getLong(0)
        plCur.close()

        var totalReports = 0L
        val repCur = db.rawQuery("SELECT COUNT(*) FROM reports WHERE status = 'PENDING'", null)
        if (repCur.moveToFirst()) totalReports = repCur.getLong(0)
        repCur.close()

        return ListeningStats(
            totalUsers = totalUsers,
            totalSongs = all.size.toLong(),
            publishedSongs = published.toLong(),
            unpublishedSongs = unpublished.toLong(),
            totalPlaylists = totalPlaylists,
            totalReports = totalReports,
            totalStreams = totalStreams
        )
    }

    // ==========================================
    // SEEDING INITIAL REAL CATALOG
    // ==========================================

    private fun seedInitialMusicCatalog() {
        val db = dbHelper.writableDatabase
        val countCur = db.rawQuery("SELECT COUNT(*) FROM songs", null)
        val hasSongs = if (countCur.moveToFirst()) countCur.getInt(0) > 0 else false
        countCur.close()

        if (hasSongs) return

        // 1. Generate real PCM audio files for streaming
        val track1 = WavSynthHelper.createAmbientWavTrack(context, "midnight_pulse.wav", 130.81, bpm = 118, durationSeconds = 180)
        val track2 = WavSynthHelper.createAmbientWavTrack(context, "ethereal_echoes.wav", 146.83, bpm = 124, durationSeconds = 210)
        val track3 = WavSynthHelper.createAmbientWavTrack(context, "neon_horizons.wav", 164.81, bpm = 128, durationSeconds = 195)
        val track4 = WavSynthHelper.createAmbientWavTrack(context, "lunar_drift.wav", 110.0, bpm = 105, durationSeconds = 240)
        val track5 = WavSynthHelper.createAmbientWavTrack(context, "deep_resonance.wav", 98.0, bpm = 95, durationSeconds = 220)

        // 2. Generate artwork
        val art1 = ArtworkHelper.createCoverArtwork(context, "art_midnight_pulse.png", "Midnight Pulse", "Kaelen Voss", "#0A1128", "#1C2541")
        val art2 = ArtworkHelper.createCoverArtwork(context, "art_ethereal_echoes.png", "Ethereal Echoes", "Aura Sound Lab", "#2E0854", "#6B2D5C")
        val art3 = ArtworkHelper.createCoverArtwork(context, "art_neon_horizons.png", "Neon Horizons", "Sora & The Synthetics", "#0F2027", "#2C5364")
        val art4 = ArtworkHelper.createCoverArtwork(context, "art_lunar_drift.png", "Lunar Drift", "Nova Ray", "#141E30", "#243B55")
        val art5 = ArtworkHelper.createCoverArtwork(context, "art_deep_resonance.png", "Deep Resonance", "Kaelen Voss", "#16222F", "#360033")

        // 3. Seed Artists
        val artistList = listOf(
            Artist("art_1", "Kaelen Voss", art1.absolutePath, "Atmospheric electronic and synthwave pioneer creating immersive soundscapes.", "Electronic", 420000L),
            Artist("art_2", "Aura Sound Lab", art2.absolutePath, "Official sonic research collective developing next-generation spatial harmonies.", "Ambient", 680000L),
            Artist("art_3", "Sora & The Synthetics", art3.absolutePath, "Futuristic cyber-pop group known for high-energy synth pulses.", "Cyber Synth", 310000L),
            Artist("art_4", "Nova Ray", art4.absolutePath, "Deep-space chillout and downtempo composer.", "Downtempo", 195000L)
        )
        for (artist in artistList) {
            val cv = ContentValues().apply {
                put("id", artist.id)
                put("name", artist.name)
                put("image_url", artist.imageUrl)
                put("bio", artist.bio)
                put("genre", artist.genre)
                put("monthly_listeners", artist.monthlyListeners)
            }
            db.insert("artists", null, cv)
        }

        // 4. Seed Songs
        val seedSongs = listOf(
            Song(
                id = "song_1",
                title = "Midnight Pulse",
                artist = "Kaelen Voss",
                artistId = "art_1",
                album = "Chronicles of Night",
                genre = "Electronic",
                releaseDate = "2026",
                description = "Driving bass pulses paired with crystalline analog arpeggios that capture the rhythm of late night metropolis.",
                lyrics = "[Verse 1]\nNeon lights bleed into rain\nEchoes humming through my veins\nFeel the current, take control\nMidnight pulse inside my soul.",
                durationMs = 180000L,
                audioUrl = track1.absolutePath,
                coverUrl = art1.absolutePath,
                downloadPermitted = true,
                copyrightNotice = "© 2026 Kaelen Voss / AURA Records. Licensed worldwide.",
                status = SongStatus.PUBLISHED,
                playsCount = 42800L,
                likesCount = 3120L,
                isFeatured = true,
                isTrending = true,
                isNewRelease = false,
                isRecommended = true
            ),
            Song(
                id = "song_2",
                title = "Ethereal Echoes",
                artist = "Aura Sound Lab",
                artistId = "art_2",
                album = "Harmonic Dimensions",
                genre = "Ambient",
                releaseDate = "2026",
                description = "Multi-layered vocal pads harmonized with modular analog synthesis for deep meditation and focus.",
                lyrics = "[Instrumental Ambient Masterpiece]",
                durationMs = 210000L,
                audioUrl = track2.absolutePath,
                coverUrl = art2.absolutePath,
                downloadPermitted = true,
                copyrightNotice = "© 2026 Aura Sound Lab. Distributed under open creative license.",
                status = SongStatus.PUBLISHED,
                playsCount = 89400L,
                likesCount = 6750L,
                isFeatured = true,
                isTrending = true,
                isNewRelease = true,
                isRecommended = true
            ),
            Song(
                id = "song_3",
                title = "Neon Horizons",
                artist = "Sora & The Synthetics",
                artistId = "art_3",
                album = "Future Velocity",
                genre = "Cyber Synth",
                releaseDate = "2026",
                description = "Upbeat tempo infused with energetic brass stabs and retrofuturistic drum machines.",
                lyrics = "[Chorus]\nChasing horizons made of light\nSpeeding into the purple night\nNothing can slow us down today\nWe found our own frequency.",
                durationMs = 195000L,
                audioUrl = track3.absolutePath,
                coverUrl = art3.absolutePath,
                downloadPermitted = true,
                copyrightNotice = "© 2026 Sora & The Synthetics / Warp Velocity Music.",
                status = SongStatus.PUBLISHED,
                playsCount = 26500L,
                likesCount = 1890L,
                isFeatured = false,
                isTrending = true,
                isNewRelease = true,
                isRecommended = false
            ),
            Song(
                id = "song_4",
                title = "Lunar Drift",
                artist = "Nova Ray",
                artistId = "art_4",
                album = "Quiet Satellites",
                genre = "Downtempo",
                releaseDate = "2026",
                description = "Warm rhodes keys floating over subtle 90s hip-hop groove and tape-saturated acoustic textures.",
                lyrics = "[Instrumental Chill]",
                durationMs = 240000L,
                audioUrl = track4.absolutePath,
                coverUrl = art4.absolutePath,
                downloadPermitted = true,
                copyrightNotice = "© 2026 Nova Ray. Licensed exclusively for AURA streaming.",
                status = SongStatus.PUBLISHED,
                playsCount = 14200L,
                likesCount = 940L,
                isFeatured = false,
                isTrending = false,
                isNewRelease = true,
                isRecommended = true
            ),
            Song(
                id = "song_5",
                title = "Deep Resonance",
                artist = "Kaelen Voss",
                artistId = "art_1",
                album = "Chronicles of Night",
                genre = "Electronic",
                releaseDate = "2026",
                description = "Sub-bass exploration designed to test acoustic fidelity and soundstage separation.",
                lyrics = "[Ambient Sub-bass Resonance]",
                durationMs = 220000L,
                audioUrl = track5.absolutePath,
                coverUrl = art5.absolutePath,
                downloadPermitted = false, // Demonstrates legal download permission restrictions!
                copyrightNotice = "© 2026 Kaelen Voss. Streaming only.",
                status = SongStatus.PUBLISHED,
                playsCount = 9800L,
                likesCount = 760L,
                isFeatured = false,
                isTrending = false,
                isNewRelease = false,
                isRecommended = true
            )
        )

        for (song in seedSongs) {
            db.insert("songs", null, songToContentValues(song))
        }

        // 5. Seed Albums
        val album1 = Album(
            id = "alb_1",
            title = "Chronicles of Night",
            artist = "Kaelen Voss",
            artistId = "art_1",
            coverUrl = art1.absolutePath,
            releaseYear = "2026",
            genre = "Electronic",
            trackIds = listOf("song_1", "song_5")
        )
        val album2 = Album(
            id = "alb_2",
            title = "Harmonic Dimensions",
            artist = "Aura Sound Lab",
            artistId = "art_2",
            coverUrl = art2.absolutePath,
            releaseYear = "2026",
            genre = "Ambient",
            trackIds = listOf("song_2")
        )
        db.insert("albums", null, ContentValues().apply {
            put("id", album1.id)
            put("title", album1.title)
            put("artist", album1.artist)
            put("artist_id", album1.artistId)
            put("cover_url", album1.coverUrl)
            put("release_year", album1.releaseYear)
            put("genre", album1.genre)
            put("track_ids", album1.trackIds.joinToString(","))
        })
        db.insert("albums", null, ContentValues().apply {
            put("id", album2.id)
            put("title", album2.title)
            put("artist", album2.artist)
            put("artist_id", album2.artistId)
            put("cover_url", album2.coverUrl)
            put("release_year", album2.releaseYear)
            put("genre", album2.genre)
            put("track_ids", album2.trackIds.joinToString(","))
        })

        // 6. Seed Playlists
        val playlist = Playlist(
            id = "pl_featured_1",
            title = "Midnight Focus & Flow",
            description = "Deep harmonic soundscapes to unlock peak cognitive clarity and effortless concentration.",
            coverUrl = art2.absolutePath,
            createdBy = "admin_root",
            creatorName = "AURA Editorial",
            isFeatured = true,
            songIds = listOf("song_2", "song_1", "song_4"),
            createdAt = System.currentTimeMillis()
        )
        db.insert("playlists", null, ContentValues().apply {
            put("id", playlist.id)
            put("title", playlist.title)
            put("description", playlist.description)
            put("cover_url", playlist.coverUrl)
            put("created_by", playlist.createdBy)
            put("creator_name", playlist.creatorName)
            put("is_featured", 1)
            put("song_ids", playlist.songIds.joinToString(","))
            put("created_at", playlist.createdAt)
        })

        // 7. Seed Home Banners
        val banners = listOf(
            HomeBanner(
                id = "ban_1",
                title = "FEEL EVERY BEAT",
                subtitle = "Spatial High-Fidelity Streaming on AURA",
                imageUrl = art1.absolutePath,
                tag = "FEATURED",
                targetSongId = "song_1"
            ),
            HomeBanner(
                id = "ban_2",
                title = "HARMONIC DIMENSIONS",
                subtitle = "New Album by Aura Sound Lab is Out Now",
                imageUrl = art2.absolutePath,
                tag = "NEW RELEASE",
                targetSongId = "song_2"
            )
        )
        for (b in banners) {
            db.insert("banners", null, ContentValues().apply {
                put("id", b.id)
                put("title", b.title)
                put("subtitle", b.subtitle)
                put("image_url", b.imageUrl)
                put("tag", b.tag)
                put("target_song_id", b.targetSongId)
            })
        }
    }

    private fun seedAdalatSongIfNeeded() {
        val db = dbHelper.writableDatabase
        val checkCur = db.rawQuery("SELECT id FROM songs WHERE id = 'song_adalat'", null)
        val exists = checkCur.moveToFirst()
        checkCur.close()
        if (exists) return

        // 1. Generate audio track and cover art
        val adalatAudio = WavSynthHelper.createDrillRapTrack(context, "adalat_bantai.wav", bpm = 138, durationSeconds = 165)
        val adalatCover = ArtworkHelper.createCoverArtwork(
            context,
            "art_adalat.png",
            "Adalat",
            "Bantai",
            "#220000",
            "#880000"
        )

        // 2. Insert or update Artist Bantai
        val artCur = db.rawQuery("SELECT id FROM artists WHERE id = 'art_bantai'", null)
        val artistExists = artCur.moveToFirst()
        artCur.close()
        if (!artistExists) {
            val cvArt = ContentValues().apply {
                put("id", "art_bantai")
                put("name", "Bantai")
                put("image_url", adalatCover.absolutePath)
                put("bio", "Raw Bombay street rap powerhouse delivering fierce flows, hard-hitting drill beats, and anthemic underground verses.")
                put("genre", "Desi Hip-Hop")
                put("monthly_listeners", 1450000L)
            }
            db.insert("artists", null, cvArt)
        }

        // 3. Insert Album Bombay Law
        val albCur = db.rawQuery("SELECT id FROM albums WHERE id = 'alb_bombay_law'", null)
        val albumExists = albCur.moveToFirst()
        albCur.close()
        if (!albumExists) {
            val cvAlb = ContentValues().apply {
                put("id", "alb_bombay_law")
                put("title", "Bombay Law")
                put("artist", "Bantai")
                put("artist_id", "art_bantai")
                put("cover_url", adalatCover.absolutePath)
                put("release_year", "2026")
                put("genre", "Desi Hip-Hop")
                put("track_ids", "song_adalat")
            }
            db.insert("albums", null, cvAlb)
        }

        // 4. Insert Adalat Song
        val adalatLyrics = """[Intro]
Yeah...
Uh...
Woah...

[Verse 1]
देख तेरा होश उड़ा कहानी शुरू है बनताइ
कठघरे में खड़ा मैं पर आंखें मेरी बोलती बनताइ
वकील तेरा कांपे उसकी फाइलें सारी खोल दी बनताइ
बैठा ऊपर पर कानून हमारा चलता है
यहाँ पे सच बिकता और हर एक अपना चलता है
काला कोट पहने पर तेरा दिल पूरा काला है
मेरे एक इशारे पे यहाँ बचता ना साला है
गवाह तेरे सारे अब डर से मुँह छुपाते हैं
जो भी मेरे खिलाफ बोले सीधा ऊपर जाते हैं
सब करके रूल्स यहाँ मेरे सामने फीके हैं
हमने तो बचपन से ही मौत के गुर सीखे हैं
पुलिस वाले बाहर खड़े पर किस्मत तो मेरी है
फैसला सुनाने में क्यों कर रहा देरी है
कलम उठा साइन कर और किस्सा खत्म कर चल
यहाँ तेरी सत्ता का हर एक दिन है निष्फल
मेरा एक ही उसूल है गोली पहले चलती है
दुश्मन की हर एक सांस गलती से बचती है

[Chorus]
अदालत मेरी फैसला मेरा बनताइ
हुकूमत मेरी रास्ता मेरा बनताइ
मौत भी यहाँ आ कर गले मिले
इंसाफ मेरे हाथ में है अब
डर से तेरा काँपेगा हर रग
खामोश खड़ा हर एक रास्ता यहाँ
जीतेगा सिर्फ वही है जिसमें जान!
सिर्फ वही है जिसमें जान!

[Verse 2]
ये काली रात मेरा ही तो साथ देती बनताइ
तेरी हर एक चाल मुझे साफ दिखती बनताइ
कागज़ों पे लिखे तेरे झूठे जहाँ सारे
मैंने एक ही झटके में तेरे सारे ख्वाब मारे
अब गीला हो रहा है तेरा ये सफेद कुरता
यहाँ देखा है मैंने हर एक शेर को मुड़ता
जुबान पे मेरे सच नहीं आग उबलती है
यहाँ दुश्मन की किस्मत हर एक पल ढलती है
पता है मुझे कौन है तेरा मास्टरमाइंड यहाँ
लेकिन बच नहीं सकता कोई भी गुनाह किया
मेरा हर एक लफ्ज़ एक नई मौत लाता है
जो भी सुनेगा वो तो सीधा थरथराता है
हथकड़ियां तेरी सिर्फ एक दिखावा है बनताइ
मैंने पूरे सिस्टम को ही हिलाया है बनताइ
चल हाथ हटा और मुझे बाहर जाने दे
मेरा खुद का कानून मुझे आज बनाने दे!

[Chorus]
अदालत मेरी फैसला मेरा बनताइ
हुकूमत मेरी रास्ता मेरा बनताइ
कानून तेरा ठुक पे मेरे चले
मौत भी यहाँ आ कर गले मिले
इंसाफ मेरे हाथ में है अब
डर से तेरा काँपेगा हर रग
खामोश खड़ा हर एक रास्ता यहाँ
जीतेगा सिर्फ वही है जिसमें जान!

[Outro]
किस्सा खत्म अब फैसला मेरा हो चुका है...
जो भी था झूठ वो इस मिट्टी में सो चुका है...
बॉम्बे का कानून बनताइ हमेशा ही चलेगा...
जो भी मेरे रास्ते में आएगा... वो तो जलेगा।"""

        val adalatSong = Song(
            id = "song_adalat",
            title = "Adalat",
            artist = "Bantai",
            artistId = "art_bantai",
            album = "Bombay Law",
            genre = "Desi Hip-Hop",
            releaseDate = "2026",
            description = "Hard-hitting Bombay drill street anthem featuring relentless rhymes, courtroom imagery, and unstoppable energy by Bantai.",
            lyrics = adalatLyrics,
            durationMs = 165000L,
            audioUrl = adalatAudio.absolutePath,
            coverUrl = adalatCover.absolutePath,
            downloadPermitted = true,
            copyrightNotice = "© 2026 Bantai / AURA Music. All Rights Reserved.",
            status = SongStatus.PUBLISHED,
            playsCount = 285400L,
            likesCount = 38900L,
            isFeatured = true,
            isTrending = true,
            isNewRelease = true,
            isRecommended = true
        )
        db.insert("songs", null, songToContentValues(adalatSong))

        // 5. Insert Spotlight Home Banner for Adalat
        val banCur = db.rawQuery("SELECT id FROM banners WHERE id = 'ban_adalat'", null)
        val banExists = banCur.moveToFirst()
        banCur.close()
        if (!banExists) {
            val cvBan = ContentValues().apply {
                put("id", "ban_adalat")
                put("title", "ADALAT • BANTAI")
                put("subtitle", "The hard-hitting Bombay drill anthem is now streaming on AURA")
                put("image_url", adalatCover.absolutePath)
                put("tag", "NEW DROP")
                put("target_song_id", "song_adalat")
            }
            db.insert("banners", null, cvBan)
        }
    }

    // ==========================================
    // PARSERS & HELPERS
    // ==========================================

    private fun songToContentValues(song: Song): ContentValues {
        return ContentValues().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("artist_id", song.artistId)
            put("album", song.album)
            put("album_id", song.albumId)
            put("genre", song.genre)
            put("release_date", song.releaseDate)
            put("description", song.description)
            put("lyrics", song.lyrics)
            put("duration_ms", song.durationMs)
            put("audio_url", song.audioUrl)
            put("cover_url", song.coverUrl)
            put("download_permitted", if (song.downloadPermitted) 1 else 0)
            put("copyright_notice", song.copyrightNotice)
            put("status", song.status.name)
            put("plays_count", song.playsCount)
            put("likes_count", song.likesCount)
            put("is_featured", if (song.isFeatured) 1 else 0)
            put("is_trending", if (song.isTrending) 1 else 0)
            put("is_new_release", if (song.isNewRelease) 1 else 0)
            put("is_recommended", if (song.isRecommended) 1 else 0)
            put("created_at", song.createdAt)
            put("updated_at", song.updatedAt)
        }
    }

    private fun parseSong(cursor: Cursor): Song {
        return Song(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
            artistId = cursor.getString(cursor.getColumnIndexOrThrow("artist_id")) ?: "",
            album = cursor.getString(cursor.getColumnIndexOrThrow("album")) ?: "",
            albumId = cursor.getString(cursor.getColumnIndexOrThrow("album_id")) ?: "",
            genre = cursor.getString(cursor.getColumnIndexOrThrow("genre")) ?: "Electronic",
            releaseDate = cursor.getString(cursor.getColumnIndexOrThrow("release_date")) ?: "2026",
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
            lyrics = cursor.getString(cursor.getColumnIndexOrThrow("lyrics")),
            durationMs = cursor.getLong(cursor.getColumnIndexOrThrow("duration_ms")),
            audioUrl = cursor.getString(cursor.getColumnIndexOrThrow("audio_url")),
            coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url")),
            downloadPermitted = cursor.getInt(cursor.getColumnIndexOrThrow("download_permitted")) == 1,
            copyrightNotice = cursor.getString(cursor.getColumnIndexOrThrow("copyright_notice")) ?: "",
            status = try {
                SongStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status")))
            } catch (_: Exception) {
                SongStatus.PUBLISHED
            },
            playsCount = cursor.getLong(cursor.getColumnIndexOrThrow("plays_count")),
            likesCount = cursor.getLong(cursor.getColumnIndexOrThrow("likes_count")),
            isFeatured = cursor.getInt(cursor.getColumnIndexOrThrow("is_featured")) == 1,
            isTrending = cursor.getInt(cursor.getColumnIndexOrThrow("is_trending")) == 1,
            isNewRelease = cursor.getInt(cursor.getColumnIndexOrThrow("is_new_release")) == 1,
            isRecommended = cursor.getInt(cursor.getColumnIndexOrThrow("is_recommended")) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
        )
    }

    private fun parseUser(cursor: Cursor): User {
        return User(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
            username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
            role = cursor.getString(cursor.getColumnIndexOrThrow("role")),
            passwordHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash")),
            profileImageUrl = cursor.getString(cursor.getColumnIndexOrThrow("profile_image_url")) ?: "",
            isSuspended = cursor.getInt(cursor.getColumnIndexOrThrow("is_suspended")) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        )
    }

    private fun parsePlaylist(cursor: Cursor): Playlist {
        val raw = cursor.getString(cursor.getColumnIndexOrThrow("song_ids")) ?: ""
        return Playlist(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
            coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url")) ?: "",
            createdBy = cursor.getString(cursor.getColumnIndexOrThrow("created_by")),
            creatorName = cursor.getString(cursor.getColumnIndexOrThrow("creator_name")) ?: "AURA User",
            isFeatured = cursor.getInt(cursor.getColumnIndexOrThrow("is_featured")) == 1,
            songIds = if (raw.isEmpty()) emptyList() else raw.split(",").filter { it.isNotEmpty() },
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        )
    }

    private fun queryAllSongs(): List<Song> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("songs", null, null, null, null, null, "created_at DESC")
        val list = mutableListOf<Song>()
        while (cursor.moveToNext()) {
            list.add(parseSong(cursor))
        }
        cursor.close()
        return list
    }

    private fun queryBanners(): List<HomeBanner> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("banners", null, null, null, null, null, null)
        val list = mutableListOf<HomeBanner>()
        while (cursor.moveToNext()) {
            list.add(
                HomeBanner(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    subtitle = cursor.getString(cursor.getColumnIndexOrThrow("subtitle")),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url")),
                    tag = cursor.getString(cursor.getColumnIndexOrThrow("tag")),
                    targetSongId = cursor.getString(cursor.getColumnIndexOrThrow("target_song_id"))
                )
            )
        }
        cursor.close()
        return list
    }

    private fun queryReports(): List<ContentReport> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("reports", null, null, null, null, null, "created_at DESC")
        val list = mutableListOf<ContentReport>()
        while (cursor.moveToNext()) {
            list.add(
                ContentReport(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    songId = cursor.getString(cursor.getColumnIndexOrThrow("song_id")),
                    songTitle = cursor.getString(cursor.getColumnIndexOrThrow("song_title")),
                    reporterEmail = cursor.getString(cursor.getColumnIndexOrThrow("reporter_email")),
                    reason = cursor.getString(cursor.getColumnIndexOrThrow("reason")),
                    details = cursor.getString(cursor.getColumnIndexOrThrow("details")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        return list
    }

    // ==========================================
    // FIREBASE REAL-TIME CLOUD SYNCHRONIZATION
    // ==========================================

    private fun initFirebaseRealtimeSync() {
        try {
            val db = FirebaseFirestore.getInstance()
            firestore = db

            // 1. Real-time snapshot listener for songs across all devices
            songsListener = db.collection("songs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.d(TAG, "Firestore snapshot listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            for (change in snapshot.documentChanges) {
                                val song = firestoreDocToSong(change.document.data)
                                if (song != null) {
                                    when (change.type) {
                                        DocumentChange.Type.ADDED,
                                        DocumentChange.Type.MODIFIED -> {
                                            upsertSongLocal(song)
                                        }
                                        DocumentChange.Type.REMOVED -> {
                                            deleteSongLocal(song.id)
                                        }
                                    }
                                }
                            }
                            refreshData()
                        }
                    }
                }

            // 2. Real-time snapshot listener for featured home banners
            bannersListener = db.collection("banners")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        scope.launch {
                            val writableDb = dbHelper.writableDatabase
                            for (change in snapshot.documentChanges) {
                                val data = change.document.data
                                val id = data["id"] as? String ?: continue
                                when (change.type) {
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val cv = ContentValues().apply {
                                            put("id", id)
                                            put("title", data["title"] as? String ?: "")
                                            put("subtitle", data["subtitle"] as? String ?: "")
                                            put("image_url", data["image_url"] as? String ?: "")
                                            put("tag", data["tag"] as? String ?: "")
                                            put("target_song_id", data["target_song_id"] as? String ?: "")
                                        }
                                        writableDb.insertWithOnConflict("banners", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        writableDb.delete("banners", "id = ?", arrayOf(id))
                                    }
                                }
                            }
                            refreshData()
                        }
                    }
                }

            // 3. One-time seed cloud upload if cloud songs collection is empty
            db.collection("songs").limit(1).get().addOnSuccessListener { qs ->
                if (qs.isEmpty) {
                    scope.launch {
                        val local = queryAllSongs()
                        local.forEach { syncSongToFirestore(it) }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase sync offline or initializing: ${t.message}")
        }
    }

    fun syncSongToFirestore(song: Song) {
        scope.launch {
            try {
                firestore?.collection("songs")?.document(song.id)
                    ?.set(songToFirestoreMap(song), SetOptions.merge())
                    ?.addOnSuccessListener {
                        Log.d(TAG, "Song '${song.title}' synced to Firebase Firestore successfully")
                    }
                    ?.addOnFailureListener { err ->
                        Log.w(TAG, "Could not sync song to Firestore: ${err.message}")
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "Firestore sync error: ${t.message}")
            }
        }
    }

    private fun updateSongStatusInFirestore(songId: String, newStatus: SongStatus) {
        scope.launch {
            try {
                firestore?.collection("songs")?.document(songId)
                    ?.update("status", newStatus.name, "updated_at", System.currentTimeMillis())
            } catch (_: Throwable) {}
        }
    }

    private fun deleteSongFromFirestore(songId: String) {
        scope.launch {
            try {
                firestore?.collection("songs")?.document(songId)?.delete()
            } catch (_: Throwable) {}
        }
    }

    private fun upsertSongLocal(song: Song) {
        try {
            val db = dbHelper.writableDatabase
            val cv = songToContentValues(song)
            db.insertWithOnConflict("songs", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting song into SQLite: ${e.message}")
        }
    }

    private fun deleteSongLocal(songId: String) {
        try {
            val db = dbHelper.writableDatabase
            db.delete("songs", "id = ?", arrayOf(songId))
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting song from SQLite: ${e.message}")
        }
    }

    private fun songToFirestoreMap(song: Song): Map<String, Any?> {
        return mapOf(
            "id" to song.id,
            "title" to song.title,
            "artist" to song.artist,
            "artist_id" to song.artistId,
            "album" to song.album,
            "album_id" to song.albumId,
            "genre" to song.genre,
            "release_date" to song.releaseDate,
            "description" to song.description,
            "lyrics" to (song.lyrics ?: ""),
            "duration_ms" to song.durationMs,
            "audio_url" to song.audioUrl,
            "cover_url" to song.coverUrl,
            "download_permitted" to song.downloadPermitted,
            "copyright_notice" to song.copyrightNotice,
            "status" to song.status.name,
            "plays_count" to song.playsCount,
            "likes_count" to song.likesCount,
            "is_featured" to song.isFeatured,
            "is_trending" to song.isTrending,
            "is_new_release" to song.isNewRelease,
            "is_recommended" to song.isRecommended,
            "created_at" to song.createdAt,
            "updated_at" to song.updatedAt
        )
    }

    private fun firestoreDocToSong(data: Map<String, Any?>): Song? {
        val id = data["id"] as? String ?: return null
        val title = data["title"] as? String ?: return null
        val artist = data["artist"] as? String ?: return null
        val statusStr = data["status"] as? String ?: "PUBLISHED"
        val status = try { SongStatus.valueOf(statusStr) } catch (_: Exception) { SongStatus.PUBLISHED }

        return Song(
            id = id,
            title = title,
            artist = artist,
            artistId = data["artist_id"] as? String ?: "",
            album = data["album"] as? String ?: "",
            albumId = data["album_id"] as? String ?: "",
            genre = data["genre"] as? String ?: "Hip-Hop",
            releaseDate = data["release_date"] as? String ?: "2026",
            description = data["description"] as? String ?: "",
            lyrics = (data["lyrics"] as? String)?.takeIf { it.isNotEmpty() },
            durationMs = (data["duration_ms"] as? Number)?.toLong() ?: 180000L,
            audioUrl = data["audio_url"] as? String ?: "",
            coverUrl = data["cover_url"] as? String ?: "",
            downloadPermitted = (data["download_permitted"] as? Boolean) ?: true,
            copyrightNotice = data["copyright_notice"] as? String ?: "",
            status = status,
            playsCount = (data["plays_count"] as? Number)?.toLong() ?: 0L,
            likesCount = (data["likes_count"] as? Number)?.toLong() ?: 0L,
            isFeatured = (data["is_featured"] as? Boolean) ?: false,
            isTrending = (data["is_trending"] as? Boolean) ?: false,
            isNewRelease = (data["is_new_release"] as? Boolean) ?: false,
            isRecommended = (data["is_recommended"] as? Boolean) ?: false,
            createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
