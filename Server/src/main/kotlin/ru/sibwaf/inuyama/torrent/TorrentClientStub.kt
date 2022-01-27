package ru.sibwaf.inuyama.torrent

class TorrentClientStub : TorrentClient {
    override suspend fun download(magnet: String, directory: String) = throw TorrentClientException("no torrent client available")
}
