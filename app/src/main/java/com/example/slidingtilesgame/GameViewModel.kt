package com.example.slidingtilesgame

import android.graphics.Bitmap
import android.os.Build
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import java.time.LocalDateTime

class GameViewModel : ViewModel() {
    var size: Int = 3
    var tileSize: Int = 0

    @RequiresApi(Build.VERSION_CODES.O)
    var startTime = LocalDateTime.now()
    var tileNumbers = mutableListOf<Int?>()
    var tiles = mutableListOf<Button?>()
    var isImage = false

    data class TileData(val number: Int, val bitmap: Bitmap?)
    var imageTileData = mutableListOf<TileData>()

}
