package ru.sibwaf.inuyama.torrent

interface TorrentClient {
    suspend fun download(magnet: String, directory: String)
}
