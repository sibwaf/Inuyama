package ru.sibwaf.inuyama.common

data class BindSessionApiRequest(val key: String)
data class BindSessionApiResponse(val token: String, val key: String)

// TODO: replace directories with categories, map to directories on server
data class TorrentDownloadApiRequest(val magnet: String, val path: String)
