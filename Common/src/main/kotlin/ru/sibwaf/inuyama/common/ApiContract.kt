package ru.sibwaf.inuyama.common

const val STATUS_OK = 0
const val STATUS_SERVER_ERROR = 1
const val STATUS_SESSION_ERROR = 2

open class ApiResponse(val status: Int)

open class StatefulApiRequest {
    lateinit var session: String
}

class BindSessionApiRequest

class BindSessionApiResponse(val session: String) : ApiResponse(STATUS_OK)

class TorrentDownloadApiRequest(val magnet: String, val path: String) : StatefulApiRequest()
