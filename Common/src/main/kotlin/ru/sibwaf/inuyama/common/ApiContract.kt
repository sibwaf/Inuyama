package ru.sibwaf.inuyama.common

data class PingApiResponse(val key: String)

data class BindSessionApiRequest(val key: String, val challenge: String, val deviceId: String)
data class BindSessionApiResponse(val challenge: String, val token: String, val key: String)

// TODO: replace directories with categories, map to directories on server
data class TorrentDownloadApiRequest(val magnet: String, val path: String)
