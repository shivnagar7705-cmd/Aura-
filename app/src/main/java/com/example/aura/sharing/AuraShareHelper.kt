package com.example.aura.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.aura.model.Album
import com.example.aura.model.Artist
import com.example.aura.model.Playlist
import com.example.aura.model.Song

object AuraShareHelper {

    private const val AURA_DOMAIN = "https://aura.audio"

    fun getSongShareUrl(songId: String): String = "$AURA_DOMAIN/song/$songId"
    fun getAlbumShareUrl(albumId: String): String = "$AURA_DOMAIN/album/$albumId"
    fun getArtistShareUrl(artistId: String): String = "$AURA_DOMAIN/artist/$artistId"
    fun getPlaylistShareUrl(playlistId: String): String = "$AURA_DOMAIN/playlist/$playlistId"

    fun shareSong(context: Context, song: Song) {
        val link = getSongShareUrl(song.id)
        val shareText = "Listen to \"${song.title}\" by ${song.artist} on AURA — Feel Every Beat.\n$link"
        launchSystemShareSheet(context, shareText, "Share Song")
    }

    fun shareAlbum(context: Context, album: Album) {
        val link = getAlbumShareUrl(album.id)
        val shareText = "Listen to the album \"${album.title}\" by ${album.artist} on AURA.\n$link"
        launchSystemShareSheet(context, shareText, "Share Album")
    }

    fun shareArtist(context: Context, artist: Artist) {
        val link = getArtistShareUrl(artist.id)
        val shareText = "Discover ${artist.name} on AURA.\n$link"
        launchSystemShareSheet(context, shareText, "Share Artist")
    }

    fun sharePlaylist(context: Context, playlist: Playlist) {
        val link = getPlaylistShareUrl(playlist.id)
        val shareText = "Check out the playlist \"${playlist.title}\" on AURA.\n$link"
        launchSystemShareSheet(context, shareText, "Share Playlist")
    }

    fun copyToClipboard(context: Context, text: String, toastMessage: String = "Link copied to clipboard") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AURA Link", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun launchSystemShareSheet(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AURA Music")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
