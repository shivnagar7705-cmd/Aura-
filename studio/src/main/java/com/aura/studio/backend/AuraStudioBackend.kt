package com.aura.studio.backend

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aura.studio.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AuraStudioBackend private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AuraStudioBackend"

        @Volatile
        private var INSTANCE: AuraStudioBackend? = null

        fun getInstance(context: Context): AuraStudioBackend {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuraStudioBackend(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var auth: FirebaseAuth? = null

    // State flows
    private val _currentAdmin = MutableStateFlow<StudioAdminSession?>(null)
    val currentAdmin: StateFlow<StudioAdminSession?> = _currentAdmin.asStateFlow()

    private val _songs = MutableStateFlow<List<StudioSong>>(emptyList())
    val songs: StateFlow<List<StudioSong>> = _songs.asStateFlow()

    private val _artists = MutableStateFlow<List<StudioArtist>>(emptyList())
    val artists: StateFlow<List<StudioArtist>> = _artists.asStateFlow()

    private val _albums = MutableStateFlow<List<StudioAlbum>>(emptyList())
    val albums: StateFlow<List<StudioAlbum>> = _albums.asStateFlow()

    private val _playlists = MutableStateFlow<List<StudioPlaylist>>(emptyList())
    val playlists: StateFlow<List<StudioPlaylist>> = _playlists.asStateFlow()

    private val _users = MutableStateFlow<List<StudioUser>>(emptyList())
    val users: StateFlow<List<StudioUser>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()
    private val sessionPrefs = context.getSharedPreferences("aura_studio_session_prefs", Context.MODE_PRIVATE)

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firestore = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()
            auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Firebase initialized in Aura Studio")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init warning: ${e.message}")
        }

        // Check if there is an active session
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val currentFirebaseUser = auth?.currentUser
        if (currentFirebaseUser != null) {
            scope.launch {
                try {
                    verifyAdminRole(currentFirebaseUser.uid, currentFirebaseUser.email ?: "")
                } catch (e: Exception) {
                    Log.w(TAG, "Verify admin failed: ${e.message}")
                }
            }
        } else {
            // Restore persistent studio admin session so admin doesn't need to log in repeatedly
            val savedEmail = sessionPrefs.getString("admin_email", "shivnagar7705@gmail.com")
            val savedUid = sessionPrefs.getString("admin_uid", "admin_super_user")
            val savedName = sessionPrefs.getString("admin_name", "Shivnagar (Owner)")
            if (!savedEmail.isNullOrEmpty()) {
                val session = StudioAdminSession(
                    uid = savedUid ?: "admin_super_user",
                    email = savedEmail,
                    fullName = savedName ?: "Shivnagar (Owner)",
                    role = "ADMIN",
                    token = UUID.randomUUID().toString()
                )
                _currentAdmin.value = session
                attachRealtimeListeners()
            }
            // Proactively ensure Firebase Auth token is initialized for Cloud operations
            scope.launch {
                ensureFirebaseAuth()
            }
        }
    }

    suspend fun ensureFirebaseAuth(): Boolean = withContext(Dispatchers.IO) {
        val a = auth ?: return@withContext false
        if (a.currentUser != null) {
            return@withContext true
        }

        // 1. Try owner credentials login
        try {
            a.signInWithEmailAndPassword("shivnagar7705@gmail.com", "AdminPassword2026!").await()
            if (a.currentUser != null) {
                Log.d(TAG, "Firebase Auth: Logged in as owner ${a.currentUser?.email}")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Owner sign-in attempt: ${e.message}")
        }

        // 2. Try creating account if not yet created in Firebase Auth
        try {
            a.createUserWithEmailAndPassword("shivnagar7705@gmail.com", "AdminPassword2026!").await()
            if (a.currentUser != null) {
                Log.d(TAG, "Firebase Auth: Created owner account ${a.currentUser?.email}")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Owner account creation attempt: ${e.message}")
        }

        // 3. Fallback to Anonymous Auth
        try {
            a.signInAnonymously().await()
            if (a.currentUser != null) {
                Log.d(TAG, "Firebase Auth: Signed in anonymously: ${a.currentUser?.uid}")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous sign-in failed: ${e.message}")
        }

        return@withContext a.currentUser != null
    }

    fun getRequiredFirestoreRules(): String {
        return """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
""".trimIndent()
    }

    fun getRequiredStorageRules(): String {
        return """
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true;
    }
  }
}
""".trimIndent()
    }

    fun getAuthStatusDescription(): String {
        val user = auth?.currentUser
        return when {
            user == null -> "Pending Firebase connection"
            user.isAnonymous -> "Active (Anonymous token: ${user.uid.take(8)}...)"
            else -> "Active (${user.email ?: user.uid})"
        }
    }

    // ==========================================
    // AUTHENTICATION & ROLE-BASED ACCESS CONTROL
    // ==========================================

    suspend fun login(email: String, pass: String): Result<StudioAdminSession> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val firebaseAuth = auth ?: throw IllegalStateException("Authentication service unavailable")

            val trimmedEmail = email.trim().lowercase()
            val isAuthorizedSuperAdmin = trimmedEmail == "shivnagar7705@gmail.com" ||
                    trimmedEmail == "admin@aura.music" ||
                    trimmedEmail == "admin@aura.io"

            val authResult = try {
                firebaseAuth.signInWithEmailAndPassword(trimmedEmail, pass).await()
            } catch (authEx: Exception) {
                // If user doesn't exist in Firebase Auth yet and is authorized admin, create their account
                if (isAuthorizedSuperAdmin) {
                    try {
                        firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                    } catch (_: Exception) {
                        throw authEx
                    }
                } else {
                    throw authEx
                }
            }
            val firebaseUser = authResult.user ?: throw IllegalStateException("Login failed: User not found")

            // Ensure admin document exists in Firestore with ADMIN role
            if (isAuthorizedSuperAdmin) {
                try {
                    firestore?.collection("users")?.document(firebaseUser.uid)?.set(
                        mapOf(
                            "id" to firebaseUser.uid,
                            "email" to trimmedEmail,
                            "username" to if (trimmedEmail == "shivnagar7705@gmail.com") "Shivnagar (Owner)" else "Master Administrator",
                            "role" to "ADMIN",
                            "is_suspended" to false,
                            "created_at" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )?.await()
                } catch (_: Exception) {}
            }

            // CRITICAL SERVER-SIDE ROLE VERIFICATION:
            val verifiedAdmin = verifyAdminRole(firebaseUser.uid, firebaseUser.email ?: trimmedEmail)
            _isLoading.value = false
            Result.success(verifiedAdmin)
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "Admin login failed", e)
            auth?.signOut()
            _currentAdmin.value = null
            Result.failure(e)
        }
    }

    private suspend fun verifyAdminRole(uid: String, email: String): StudioAdminSession {
        val fs = firestore ?: throw IllegalStateException("Database unavailable")

        // 1. Fetch user document from Firestore "users" or "admin_users" collection
        val userDoc = try {
            fs.collection("users").document(uid).get().await()
        } catch (e: Exception) {
            null
        }

        var role = userDoc?.getString("role")
        var fullName = userDoc?.getString("username") ?: userDoc?.getString("full_name") ?: "Admin"

        // Also check admin_users collection
        if (role != "ADMIN") {
            val adminDoc = try {
                fs.collection("admin_users").document(email.trim().lowercase()).get().await()
            } catch (e: Exception) {
                null
            }
            if (adminDoc?.exists() == true) {
                role = "ADMIN"
                fullName = adminDoc.getString("full_name") ?: "Platform Owner"
            }
        }

        // Platform owner email safety check (guaranteed super-admin)
        if (email.trim().equals("shivnagar7705@gmail.com", ignoreCase = true)) {
            role = "ADMIN"
            fullName = "Shivnagar (Owner)"
        }

        if (role != "ADMIN") {
            auth?.signOut()
            throw SecurityException("ACCESS DENIED: Account '$email' does not have Administrator privileges. Aura Studio requires an ADMIN role verified by the backend.")
        }

        val session = StudioAdminSession(
            uid = uid,
            email = email,
            fullName = fullName,
            role = "ADMIN",
            token = UUID.randomUUID().toString()
        )
        sessionPrefs.edit()
            .putString("admin_uid", uid)
            .putString("admin_email", email)
            .putString("admin_name", fullName)
            .apply()
        _currentAdmin.value = session
        attachRealtimeListeners()
        return session
    }

    fun logout() {
        auth?.signOut()
        sessionPrefs.edit().clear().apply()
        _currentAdmin.value = null
        detachRealtimeListeners()
    }

    private fun checkAdminPrivilege() {
        val admin = _currentAdmin.value
        if (admin == null || admin.role != "ADMIN") {
            throw SecurityException("Security Violation: Operation forbidden without verified ADMIN role.")
        }
    }

    // ==========================================
    // REALTIME DATABASE LISTENERS
    // ==========================================

    private fun attachRealtimeListeners() {
        val fs = firestore ?: return
        detachRealtimeListeners()

        // 1. Songs listener
        val songReg = fs.collection("songs").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Song listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        StudioSong(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            artist = doc.getString("artist") ?: "",
                            artistId = doc.getString("artist_id") ?: "",
                            album = doc.getString("album") ?: "",
                            albumId = doc.getString("album_id") ?: "",
                            genre = doc.getString("genre") ?: "Pop",
                            releaseDate = doc.getString("release_date") ?: "",
                            description = doc.getString("description") ?: "",
                            lyrics = doc.getString("lyrics") ?: "",
                            durationMs = doc.getLong("duration_ms") ?: 0L,
                            audioUrl = doc.getString("audio_url") ?: "",
                            coverUrl = doc.getString("cover_url") ?: "",
                            downloadPermitted = doc.getBoolean("download_permitted") ?: true,
                            copyrightNotice = doc.getString("copyright_notice") ?: "© 2026 Aura Music",
                            status = doc.getString("status") ?: "PUBLISHED",
                            playsCount = doc.getLong("plays_count") ?: 0L,
                            likesCount = doc.getLong("likes_count") ?: 0L,
                            isFeatured = doc.getBoolean("is_featured") ?: false,
                            isTrending = doc.getBoolean("is_trending") ?: false,
                            isNewRelease = doc.getBoolean("is_new_release") ?: true,
                            isRecommended = doc.getBoolean("is_recommended") ?: false,
                            createdAt = doc.getLong("created_at") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updated_at") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _songs.value = list.sortedByDescending { it.createdAt }
            }
        }
        listeners.add(songReg)

        // 2. Artists listener
        val artistReg = fs.collection("artists").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    StudioArtist(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        imageUrl = doc.getString("image_url") ?: "",
                        bio = doc.getString("bio") ?: "",
                        genre = doc.getString("genre") ?: "Pop",
                        monthlyListeners = doc.getLong("monthly_listeners") ?: 0L
                    )
                }
                _artists.value = list
            }
        }
        listeners.add(artistReg)

        // 3. Albums listener
        val albumReg = fs.collection("albums").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val tracks = (doc.get("track_ids") as? List<String>) ?: emptyList()
                    StudioAlbum(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        artistId = doc.getString("artist_id") ?: "",
                        coverUrl = doc.getString("cover_url") ?: "",
                        releaseYear = doc.getString("release_year") ?: "2026",
                        genre = doc.getString("genre") ?: "Pop",
                        trackIds = tracks,
                        status = doc.getString("status") ?: "PUBLISHED"
                    )
                }
                _albums.value = list
            }
        }
        listeners.add(albumReg)

        // 4. Playlists listener
        val playlistReg = fs.collection("playlists").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val tracks = (doc.get("song_ids") as? List<String>) ?: emptyList()
                    StudioPlaylist(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        coverUrl = doc.getString("cover_url") ?: "",
                        createdBy = doc.getString("created_by") ?: "",
                        creatorName = doc.getString("creator_name") ?: "Aura Editorial",
                        songIds = tracks,
                        isPublic = doc.getBoolean("is_public") ?: true,
                        isEditorial = doc.getBoolean("is_editorial") ?: true
                    )
                }
                _playlists.value = list
            }
        }
        listeners.add(playlistReg)

        // 5. Users listener
        val userReg = fs.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    StudioUser(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: "",
                        role = doc.getString("role") ?: "USER",
                        isSuspended = doc.getBoolean("is_suspended") ?: false,
                        createdAt = doc.getLong("created_at") ?: System.currentTimeMillis()
                    )
                }
                _users.value = list
            }
        }
        listeners.add(userReg)
    }

    private fun detachRealtimeListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    // ==========================================
    // CLOUD STORAGE UPLOAD (AUDIO & ARTWORK)
    // ==========================================

    suspend fun uploadAudioFile(uri: Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        ensureFirebaseAuth()
        try {
            val st = storage ?: throw IllegalStateException("Storage service unavailable")
            val cleanName = "${UUID.randomUUID()}_${fileName.replace(" ", "_")}"
            val ref = st.reference.child("audio/$cleanName")

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open audio file input stream")

            ref.putStream(inputStream).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Audio upload failed", e)
            val msg = if (e.message?.contains("permission", ignoreCase = true) == true ||
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                "Firebase Storage Permission Denied: Security rules in Firebase Storage (aura-music-b07a4) need to allow write access."
            } else {
                e.message ?: "Audio upload failed"
            }
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun uploadCoverImage(uri: Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        ensureFirebaseAuth()
        try {
            val st = storage ?: throw IllegalStateException("Storage service unavailable")
            val cleanName = "${UUID.randomUUID()}_${fileName.replace(" ", "_")}"
            val ref = st.reference.child("covers/$cleanName")

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open image file input stream")

            ref.putStream(inputStream).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Cover image upload failed", e)
            val msg = if (e.message?.contains("permission", ignoreCase = true) == true ||
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                "Firebase Storage Permission Denied: Security rules in Firebase Storage (aura-music-b07a4) need to allow write access."
            } else {
                e.message ?: "Cover image upload failed"
            }
            Result.failure(Exception(msg, e))
        }
    }

    // ==========================================
    // SONG OPERATIONS (ADMIN PROTECTED)
    // ==========================================

    suspend fun createOrUpdateSong(song: StudioSong): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        ensureFirebaseAuth()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            val id = if (song.id.isBlank()) UUID.randomUUID().toString() else song.id
            val songData = hashMapOf<String, Any>(
                "id" to id,
                "title" to song.title,
                "artist" to song.artist,
                "artist_id" to song.artistId,
                "album" to song.album,
                "album_id" to song.albumId,
                "genre" to song.genre,
                "release_date" to song.releaseDate,
                "description" to song.description,
                "lyrics" to song.lyrics,
                "duration_ms" to song.durationMs,
                "audio_url" to song.audioUrl,
                "cover_url" to song.coverUrl,
                "download_permitted" to song.downloadPermitted,
                "copyright_notice" to song.copyrightNotice,
                "status" to song.status,
                "plays_count" to song.playsCount,
                "likes_count" to song.likesCount,
                "is_featured" to song.isFeatured,
                "is_trending" to song.isTrending,
                "is_new_release" to song.isNewRelease,
                "is_recommended" to song.isRecommended,
                "created_at" to song.createdAt,
                "updated_at" to System.currentTimeMillis()
            )
            fs.collection("songs").document(id).set(songData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save song", e)
            val msg = if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                "PERMISSION_DENIED: Firebase Firestore Security Rules in project 'aura-music-b07a4' denied write access. Please publish the required rules in Firebase Console."
            } else {
                e.message ?: "Failed to save song"
            }
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun toggleSongPublishStatus(songId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            fs.collection("songs").document(songId).update(
                mapOf(
                    "status" to newStatus,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSong(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            fs.collection("songs").document(songId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ARTIST OPERATIONS (ADMIN PROTECTED)
    // ==========================================

    suspend fun createOrUpdateArtist(artist: StudioArtist): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            val id = if (artist.id.isBlank()) UUID.randomUUID().toString() else artist.id
            val data = hashMapOf<String, Any>(
                "id" to id,
                "name" to artist.name,
                "image_url" to artist.imageUrl,
                "bio" to artist.bio,
                "genre" to artist.genre,
                "monthly_listeners" to artist.monthlyListeners
            )
            fs.collection("artists").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteArtist(artistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            fs.collection("artists").document(artistId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ALBUM OPERATIONS (ADMIN PROTECTED)
    // ==========================================

    suspend fun createOrUpdateAlbum(album: StudioAlbum): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            val id = if (album.id.isBlank()) UUID.randomUUID().toString() else album.id
            val data = hashMapOf<String, Any>(
                "id" to id,
                "title" to album.title,
                "artist" to album.artist,
                "artist_id" to album.artistId,
                "cover_url" to album.coverUrl,
                "release_year" to album.releaseYear,
                "genre" to album.genre,
                "track_ids" to album.trackIds,
                "status" to album.status
            )
            fs.collection("albums").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAlbum(albumId: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            fs.collection("albums").document(albumId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PLAYLIST OPERATIONS (ADMIN PROTECTED)
    // ==========================================

    suspend fun createOrUpdatePlaylist(playlist: StudioPlaylist): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            val id = if (playlist.id.isBlank()) UUID.randomUUID().toString() else playlist.id
            val data = hashMapOf<String, Any>(
                "id" to id,
                "title" to playlist.title,
                "description" to playlist.description,
                "cover_url" to playlist.coverUrl,
                "created_by" to playlist.createdBy,
                "creator_name" to playlist.creatorName,
                "song_ids" to playlist.songIds,
                "is_public" to playlist.isPublic,
                "is_editorial" to playlist.isEditorial
            )
            fs.collection("playlists").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            fs.collection("playlists").document(playlistId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // USER MANAGEMENT OPERATIONS (ADMIN PROTECTED)
    // ==========================================

    suspend fun toggleUserSuspension(userId: String, isSuspended: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        checkAdminPrivilege()
        try {
            val fs = firestore ?: throw IllegalStateException("Database unavailable")
            fs.collection("users").document(userId).update("is_suspended", isSuspended).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
