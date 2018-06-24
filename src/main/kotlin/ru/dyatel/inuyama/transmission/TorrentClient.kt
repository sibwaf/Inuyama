package ru.dyatel.inuyama.transmission

interface TorrentClient {

    fun download(magnet: String, directory: String?)

}
