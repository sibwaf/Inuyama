package ru.dyatel.inuyama

const val ITEM_TYPE_NETWORK = 1
const val ITEM_TYPE_DIRECTORY = 2
const val ITEM_TYPE_RUTRACKER_WATCH = 3
const val ITEM_TYPE_SERVICE_STATE = 4
const val ITEM_TYPE_NYAA_WATCH = 5
const val ITEM_TYPE_HOME_UPDATE = 6
const val ITEM_TYPE_RURANOBE_PROJECT = 7
const val ITEM_TYPE_RURANOBE_VOLUME = 8
const val ITEM_TYPE_PROXY = 9
const val ITEM_TYPE_FINANCE_ACCOUNT = 10
const val ITEM_TYPE_FINANCE_CATEGORY = 11
const val ITEM_TYPE_FINANCE_RECEIPT = 12
const val ITEM_TYPE_PAIRING_SERVER = 13
const val ITEM_TYPE_FINANCE_TRANSFER = 14

@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
const val SERVICE_TRANSMISSION = 1L
const val SERVICE_RUTRACKER = 2L
const val SERVICE_NYAA = 3L
const val SERVICE_RURANOBE = 4L
@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
const val SERVICE_PAIRING = 5L

const val NOTIFICATION_CHANNEL_UPDATES = "updates"

const val NOTIFICATION_ID_UPDATE = 1

const val WORK_NAME_OVERSEER = "${BuildConfig.APPLICATION_ID}:overseer"
const val WORK_NAME_BACKUP = "${BuildConfig.APPLICATION_ID}:backup"
const val WORK_NAME_ERROR_SENDER = "${BuildConfig.APPLICATION_ID}:errors"

const val DASHBOARD_UPDATE_COUNT = 8

const val RURANOBE_COVER_ASPECT_RATIO = 240.0 / 343
const val RURANOBE_COVER_SIZE = 6