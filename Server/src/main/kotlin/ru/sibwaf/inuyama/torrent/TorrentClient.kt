package ru.sibwaf.inuyama.torrent

interface TorrentClient {

    fun download(magnet: String, directory: String?)

}
