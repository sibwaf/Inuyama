package ru.sibwaf.inuyama.common

const val STATUS_OK = 0
const val STATUS_SERVER_ERROR = 1

open class ApiResponse(val status: Int)
