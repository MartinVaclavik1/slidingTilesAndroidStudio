package com.example.slidingtilesgame

import android.graphics.Bitmap
import android.widget.Button
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    var size: Int = 3
    var tileSize: Int = 0
    var tileNumbers = mutableListOf<Int?>()
    var tiles = mutableListOf<Button?>()
    var isImage = false

    data class TileData(val number: Int, val bitmap: Bitmap?)
    var imageTileData = mutableListOf<TileData>()

}
