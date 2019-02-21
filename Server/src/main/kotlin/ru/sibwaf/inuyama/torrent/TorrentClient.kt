package ru.sibwaf.inuyama.torrent

interface TorrentClient {

    // TODO: categories
    fun download(magnet: String, directory: String?)

}
