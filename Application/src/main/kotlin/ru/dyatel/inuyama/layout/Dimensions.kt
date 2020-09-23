package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import org.jetbrains.anko.dip

val Context.DIM_SMALL
    get() = dip(2)

val View.DIM_SMALL
    get() = context.DIM_SMALL

val Context.DIM_MEDIUM
    get() = dip(4)

val View.DIM_MEDIUM
    get() = context.DIM_MEDIUM

val Context.DIM_LARGE
    get() = dip(8)

val View.DIM_LARGE
    get() = context.DIM_LARGE

val Context.DIM_EXTRA_LARGE
    get() = dip(16)

val View.DIM_EXTRA_LARGE
    get() = context.DIM_EXTRA_LARGE

const val SP_SMALL = 12f

const val SP_MEDIUM = 16f

const val SP_LARGE = 20f

const val SP_EXTRA_LARGE = 24f