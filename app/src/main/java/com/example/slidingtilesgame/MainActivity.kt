package com.example.slidingtilesgame

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import android.graphics.Color
import android.net.Uri
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.abs
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var gridLayout: GridLayout
    private lateinit var novaHraButton: Button
    //private var gameViewModel.isImage = false
    private var bitmap : Bitmap? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { loadImage(it) }
        }

    private lateinit var root : LinearLayout
    private var activeDialog: AlertDialog? = null
    private var isFrozenGrid = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        gridLayout = GridLayout(this).apply {
            setPadding(0,0,0,10)
        }

        novaHraButton = Button(this).apply {
            text = "New Game"
            setOnClickListener { showOptionsPopup() }
        }

        root.addView(gridLayout)
        root.addView(novaHraButton)
        setContentView(root)


        if (gameViewModel.tileNumbers.isNotEmpty()) {
            if (gameViewModel.isImage) {
                setupImageGame()
            } else {
                reconstructGridFromData()
            }

        } else {
            showOptionsPopup()
        }

        onConfigurationChanged(resources.configuration)
    }
    private fun reconstructGridFromData() {
        gridLayout.removeAllViews()
        gridLayout.columnCount = gameViewModel.size
        gridLayout.rowCount = gameViewModel.size
        gameViewModel.tiles.clear()

        val tileSize = calculateTileSize()

        gameViewModel.tileNumbers.forEachIndexed { index, number ->
            val tile = if (number == null) null else Button(this).apply {
                text = number.toString()
                textSize = 20f
                setOnClickListener { moveTile(this) }
            }
            gameViewModel.tiles.add(tile)

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(index / gameViewModel.size)
                columnSpec = GridLayout.spec(index % gameViewModel.size)
                setMargins(5, 5, 5, 5)
            }

            gridLayout.addView(tile ?: Button(this).apply {
                visibility = Button.INVISIBLE
            }, params)
        }
    }
    private fun syncViewModel() {
        gameViewModel.tileNumbers = gameViewModel.tiles.map {
            it?.text?.toString()?.toIntOrNull()
        }.toMutableList()
    }

    private fun calculateTileSize(): Int {
        val displayMetrics = resources.displayMetrics
        return if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            displayMetrics.widthPixels / gameViewModel.size - 25
        } else {
            val availableHeight = displayMetrics.heightPixels - getStatusBarHeight() - 100
            availableHeight / gameViewModel.size - 10
        }
    }

    private fun showOptionsPopup() {
        bitmap = null

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        layout.addView(topBar)

        val sizeLabel = TextView(this).apply {
            text = "Select Grid Size"
            textSize = 16f
        }
        layout.addView(sizeLabel)

        val sizeSpinner = Spinner(this)
        var sizes = mutableListOf<Int>()
        (3..8).forEach { sizes.add(it) }
        sizeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        layout.addView(sizeSpinner)

        val typeLabel = TextView(this).apply {
            text = "Puzzle Type"
            textSize = 16f
            setPadding(0, 30, 0, 0)
        }
        layout.addView(typeLabel)

        val imageButton = Button(this).apply {
            text = "Pick Image"
            setOnClickListener { imagePicker.launch("image/*") }
        }

        layout.addView(imageButton)

        val deleteImageButton = Button(this).apply {
            text = "Delete Image"
            setOnClickListener { bitmap = null }
        }

        layout.addView(deleteImageButton)

        val startBtn = Button(this).apply {
            text = "Start"
        }
        layout.addView(startBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(false)
            .create()

        val cancelBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener{
                dialog.dismiss()
                activeDialog = null
            }
        }

        topBar.addView(cancelBtn)

        startBtn.setOnClickListener {
            gameViewModel.size = sizeSpinner.selectedItem as Int
            setupGame()
            if (bitmap!=null){
                gameViewModel.isImage = true;
                createImageTiles(bitmap!!)


            }else{
                gameViewModel.isImage = false
            }

            shuffleTiles()
            dialog.dismiss()
            activeDialog = null
            unfreezeGrid()
            gameViewModel.startTime = LocalDateTime.now()
        }

        activeDialog = dialog
        dialog.show()

    }

    private fun setupGame() {
        gridLayout.removeAllViews()
        gridLayout.columnCount = gameViewModel.size
        gridLayout.rowCount = gameViewModel.size
        gameViewModel.tiles.clear()
        gameViewModel.isImage = false

        createTiles()
        shuffleTiles()
        syncViewModel()
    }

    private fun createTiles() {
        val tileSize = calculateTileSize()

        for (i in 0 until gameViewModel.size * gameViewModel.size) {
            val tile = if (i == gameViewModel.size * gameViewModel.size - 1) null else Button(this).apply {
                text = (i + 1).toString()
                textSize = 20f
                setOnClickListener { moveTile(this) }
            }

            gameViewModel.tiles.add(tile)

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(i / gameViewModel.size)
                columnSpec = GridLayout.spec(i % gameViewModel.size)
                setMargins(5, 5, 5, 5)
            }

            gridLayout.addView(tile ?: Button(this).apply {
                visibility = Button.INVISIBLE
            }, params)
        }
    }

    private fun moveTile(tile: Button) {
        if (isFrozenGrid)
            return

        val tileIndex = gameViewModel.tiles.indexOf(tile)
        val emptyIndex = gameViewModel.tiles.indexOf(null)

        if (gameViewModel.isImage) {

            if (isAdjacent(tileIndex, emptyIndex)) {

                gameViewModel.tiles[emptyIndex] = tile
                gameViewModel.tiles[tileIndex] = null

                val temp = gameViewModel.imageTileData[tileIndex]
                gameViewModel.imageTileData[tileIndex] = gameViewModel.imageTileData[emptyIndex]
                gameViewModel.imageTileData[emptyIndex] = temp

                setupImageGame()

                if (isImageSolved()) {
                    showWinPopup()
                }
            }
        } else {

            if (isAdjacent(tileIndex, emptyIndex)) {
                gameViewModel.tiles[emptyIndex] = tile
                gameViewModel.tiles[tileIndex] = null
                syncViewModel()
                updateGrid()

                if (isSolved()) showWinPopup()
            }
        }
    }
        private fun isAdjacent(i: Int, j: Int): Boolean {
        val r1 = i / gameViewModel.size
        val c1 = i % gameViewModel.size
        val r2 = j / gameViewModel.size
        val c2 = j % gameViewModel.size
        return abs(r1 - r2) + abs(c1 - c2) == 1
    }

    private fun updateGrid() {
        gridLayout.removeAllViews()

        val tileSize = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            resources.displayMetrics.widthPixels / gameViewModel.size - 5
        else
            (resources.displayMetrics.heightPixels - getStatusBarHeight()) / gameViewModel.size - 5

        for (i in gameViewModel.tiles.indices) {
            val view = gameViewModel.tiles[i] ?: Button(this).apply {
                visibility = Button.INVISIBLE
            }

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(i / gameViewModel.size)
                columnSpec = GridLayout.spec(i % gameViewModel.size)
                setMargins(5, 5, 0, 0)
            }

            gridLayout.addView(view, params)
        }
    }

    private fun shuffleTiles() {
        do {
            if(gameViewModel.isImage) {
                gameViewModel.imageTileData.shuffle()
                setupImageGame()
            }
            else
                gameViewModel.tiles.shuffle()
        } while (!isSolvable() || isSolved())
        updateGrid()
    }

    private fun isSolved(): Boolean {
        for (i in 0 until gameViewModel.tiles.size - 1) {
            if (gameViewModel.tiles[i]?.text?.toString() != (i + 1).toString()) return false
        }
        return true
    }

    private fun isSolvable(): Boolean {
        var inversions = 0
        if (gameViewModel.isImage) {
            val indices = gameViewModel.imageTileData.mapIndexedNotNull { index, bmp ->
                if (bmp == null) null else index
            }

            for (i in indices.indices) {
                for (j in i + 1 until indices.size) {
                    if (indices[i] > indices[j]) inversions++
                }
            }

            return inversions % 2 == 0
        }
        val numbers = gameViewModel.tiles.filterNotNull().map { it.text.toString().toInt() }

        for (i in numbers.indices) {
            for (j in i + 1 until numbers.size) {
                if (numbers[i] > numbers[j]) inversions++
            }
        }

        if(gameViewModel.size % 2 == 0){    //sudé
            val index = gameViewModel.size - (gameViewModel.tiles.indexOf(null) / gameViewModel.size).toInt()
            //index prázdného pole od spoda je sudý a lichý počet
            // nebo lichý a sudý
            return (index % 2 == 0 && inversions % 2 == 1)
                    || (index % 2 == 1 && inversions % 2 == 0)
        }else{ //liché
            return inversions % 2 == 0
        }


    }
    private fun loadImage(uri: Uri) {
        gameViewModel.isImage = true
        bitmap = null
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val sizePx = minOf(original.width, original.height)
        val x = (original.width - sizePx) / 2
        val y = (original.height - sizePx) / 2

        val square = Bitmap.createBitmap(original, x, y, sizePx, sizePx)

        bitmap = square

    }

    private fun createImageTiles(bitmap: Bitmap) {
        gameViewModel.imageTileData.clear()
        val tileSize = bitmap.width / gameViewModel.size
        gameViewModel.tileSize = tileSize

        for (row in 0 until gameViewModel.size) {
            for (col in 0 until gameViewModel.size) {
                val number = row * gameViewModel.size + col + 1
                val image = if (number == gameViewModel.size * gameViewModel.size) null
                else Bitmap.createBitmap(bitmap, col * tileSize, row * tileSize, tileSize, tileSize)
                gameViewModel.imageTileData.add(GameViewModel.TileData(number, image))

            }
        }
    }
    private fun setupImageGame() {
        gridLayout.removeAllViews()
        gameViewModel.tiles.clear()

        val tileSize = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            resources.displayMetrics.widthPixels / gameViewModel.size - 5
        else
            (resources.displayMetrics.heightPixels - getStatusBarHeight()) / gameViewModel.size - 5

        for (tile in gameViewModel.imageTileData) {
            val button = if (tile.bitmap == null) null else Button(this).apply {
                background = tile.bitmap.toDrawable(resources)
                setOnClickListener { moveTile(this) }
            }
            gameViewModel.tiles.add(button)

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(gameViewModel.imageTileData.indexOf(tile) / gameViewModel.size)
                columnSpec = GridLayout.spec(gameViewModel.imageTileData.indexOf(tile) % gameViewModel.size)
                setMargins(5, 5, 0, 0)
            }

            gridLayout.addView(button ?: Button(this).apply { visibility = Button.INVISIBLE }, params)
        }
    }

    private fun isImageSolved(): Boolean {
        for (i in 0 until gameViewModel.imageTileData.size - 1) {
            if (gameViewModel.imageTileData[i].number != i + 1) return false
        }
        return true
    }


    private fun showWinPopup() {
        val endTime = java.time.LocalDateTime.now()
        val storage = StorageManager(this)
        var prevScore = "No scores for this grid size"

        val duration = java.time.Duration.between(gameViewModel.startTime, endTime)

        val minutes = duration.toMinutes()
        val seconds = duration.seconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        val previousHighscore = storage.loadAllStats().find { stats -> stats.gridSize == gameViewModel.size}
        if (previousHighscore != null) {
            prevScore = "Previous shortest time: " + previousHighscore.totalTime
        }
        storage.addStats(gameViewModel.size, timeString, minutes, seconds)

        freezeGrid()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Congratulations!")
            .setMessage(
                "You solved the puzzle!\n" +
                        "\nTotal time: $timeString\n" +
                        prevScore,
            )
            .setCancelable(false)
            .setPositiveButton("Play Again") { dialog, _ ->
                dialog.dismiss()
                showOptionsPopup()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }

        activeDialog = dialog.show()
        for (i in storage.loadAllStats()) {
            println(i.gridSize.toString() + i.totalTime)
        }

    }

    fun getStatusBarHeight(): Int {
        return ViewCompat.getRootWindowInsets(window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top ?: 0
    }

    fun isLandscape(): Boolean{
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun freezeGrid(){
        isFrozenGrid = true
    }

    fun unfreezeGrid(){
        isFrozenGrid = false
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            root.orientation = LinearLayout.HORIZONTAL
            root.gravity = Gravity.CENTER

            val gridParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 50
            }
            gridLayout.layoutParams = gridParams

            val buttonParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            novaHraButton.layoutParams = buttonParams

        } else {
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER

            gridLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }

        updateGrid()
    }
}


