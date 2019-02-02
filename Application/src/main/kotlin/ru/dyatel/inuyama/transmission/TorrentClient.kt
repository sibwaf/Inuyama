package ru.dyatel.inuyama.transmission

import ru.dyatel.inuyama.RemoteService

interface TorrentClient : RemoteService {

    fun download(magnet: String, directory: String?)

}
