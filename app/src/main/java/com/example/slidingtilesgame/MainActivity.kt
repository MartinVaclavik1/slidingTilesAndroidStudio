package com.example.slidingtilesgame

import android.R.style.Theme
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : ComponentActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var novaHraButton: Button
    private var isImage = false
    private var bitmap : Bitmap? = null
    private var imageTiles: MutableList<Tile> = mutableListOf()
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { loadImage(it) }
        }
    private var size = 3
    private val tiles = mutableListOf<Button?>()
    private lateinit var root : LinearLayout

    private var isFrozenGrid = false
//TODO opravit refresh aplikace/neaktualizování theme po změně - v manifestu android:configChanges="uiMode|
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        gridLayout = GridLayout(this).apply {
        }
        novaHraButton = Button(this).apply {
            text = "New Game"
            setOnClickListener { showOptionsPopup() }
        }

        root.addView(gridLayout)
        root.addView(novaHraButton)

        setContentView(root)

        setupGame()
        showOptionsPopup()
        onConfigurationChanged(resources.configuration)
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
            setOnClickListener{dialog.dismiss()}
        }

        topBar.addView(cancelBtn)

        startBtn.setOnClickListener {
            size = sizeSpinner.selectedItem as Int
            setupGame()
            if (bitmap!=null){
                isImage = true;
                createImageTiles(bitmap!!)

            }else{
                isImage = false
            }

            shuffleTiles()
            dialog.dismiss()
            unfreezeGrid()
        }

//        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
//            Configuration.UI_MODE_NIGHT_YES -> {
//                layout.setBackgroundColor(Color.BLACK)
//                sizeLabel.setTextColor(Color.WHITE)
//                typeLabel.setTextColor(Color.WHITE)
//                sizeSpinner.setBackgroundColor(Color.WHITE)
//            }
//            Configuration.UI_MODE_NIGHT_NO -> {
//                layout.setBackgroundColor(Color.WHITE)
//                sizeLabel.setTextColor(Color.BLACK)
//                typeLabel.setTextColor(Color.BLACK)
//                sizeSpinner.setBackgroundColor(Color.BLACK)
//            }
//        }
        dialog.show()

    }

    private fun setupGame() {
        gridLayout.removeAllViews()
        gridLayout.columnCount = size
        gridLayout.rowCount = size
        tiles.clear()
        isImage = false

        createTiles()
        shuffleTiles()
    }

    private fun createTiles() {
        val tileSize = resources.displayMetrics.widthPixels / size - 20

        for (i in 0 until size * size) {
            val tile = if (i == size * size - 1) null else Button(this).apply {
                text = (i + 1).toString()
                textSize = 20f
                //setBackgroundColor(Color.LTGRAY)
                setOnClickListener { moveTile(this) }
            }

            tiles.add(tile)

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(i / size)
                columnSpec = GridLayout.spec(i % size)
                setMargins(5, 5, 5, 5)
            }

            gridLayout.addView(tile ?: Button(this).apply {
                visibility = Button.INVISIBLE
            }, params)
        }
    }

    private fun moveTile(tile: Button) {
        if(isFrozenGrid)
            return

        val tileIndex = tiles.indexOf(tile)
        val emptyIndex = tiles.indexOf(null)

        if(isImage){


            if (isAdjacent(tileIndex, emptyIndex)) {

                tiles[emptyIndex] = tile
                tiles[tileIndex] = null

                val temp = imageTiles[tileIndex]
                imageTiles[tileIndex] = imageTiles[emptyIndex]
                imageTiles[emptyIndex] = temp

                setupImageGame()

                if (isImageSolved()) {
                    showWinPopup()
                }
            }
        }else{

        if (isAdjacent(tileIndex, emptyIndex)) {
            tiles[emptyIndex] = tile
            tiles[tileIndex] = null
            updateGrid()
        }

        if (isSolved()) {
            showWinPopup()
        }
    }

    }

    private fun isAdjacent(i: Int, j: Int): Boolean {
        val r1 = i / size
        val c1 = i % size
        val r2 = j / size
        val c2 = j % size
        return abs(r1 - r2) + abs(c1 - c2) == 1
    }

    private fun updateGrid() {
        gridLayout.removeAllViews()

        val tileSize = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            resources.displayMetrics.widthPixels / size - 5
        else
            (resources.displayMetrics.heightPixels - getStatusBarHeight()) / size - 5

        for (i in tiles.indices) {
            val view = tiles[i] ?: Button(this).apply {
                visibility = Button.INVISIBLE
            }

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(i / size)
                columnSpec = GridLayout.spec(i % size)
                setMargins(5, 5, 0, 0)
            }

            gridLayout.addView(view, params)
        }
    }

    private fun shuffleTiles() {
        do {
            if(isImage) {
                imageTiles.shuffle()
                setupImageGame()
            }
            else
                tiles.shuffle()
        } while (!isSolvable() || isSolved())
        updateGrid()
    }

    private fun isSolved(): Boolean {
        for (i in 0 until tiles.size - 1) {
            if (tiles[i]?.text?.toString() != (i + 1).toString()) return false
        }
        return true
    }

    private fun isSolvable(): Boolean {
        var inversions = 0
        if (isImage) {
            val indices = imageTiles.mapIndexedNotNull { index, bmp ->
                if (bmp == null) null else index
            }

            for (i in indices.indices) {
                for (j in i + 1 until indices.size) {
                    if (indices[i] > indices[j]) inversions++
                }
            }

            return inversions % 2 == 0
        }
        val numbers = tiles.filterNotNull().map { it.text.toString().toInt() }

        for (i in numbers.indices) {
            for (j in i + 1 until numbers.size) {
                if (numbers[i] > numbers[j]) inversions++
            }
        }

        if(size % 2 == 0){    //sudé
            val index = tiles.indexOf(null) +1
            //index prázdného pole od spoda je sudý a lichý počet
            // nebo lichý a sudý
            return (index % 2 == 0 && inversions % 2 == 1)
                    || (index % 2 == 1 && inversions % 2 == 0)
        }else{ //liché
            return inversions % 2 == 0
        }


    }
    private fun loadImage(uri: Uri) {
        isImage = true
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
        imageTiles.clear()
        val tileSize = bitmap.width / size

        for (row in 0 until size) {
            for (col in 0 until size) {
                val number = row * size + col + 1
                val image = if (number == size * size) null
                else Bitmap.createBitmap(bitmap, col * tileSize, row * tileSize, tileSize, tileSize)
                imageTiles.add(Tile(number, image))
            }
        }
    }
    private fun setupImageGame() {
        gridLayout.removeAllViews()
        tiles.clear()

        val tileSize = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            resources.displayMetrics.widthPixels / size - 5
        else
            (resources.displayMetrics.heightPixels - getStatusBarHeight()) / size - 5

        for (tile in imageTiles) {
            val button = if (tile.image == null) null else Button(this).apply {
                background = tile.image.toDrawable(resources)
                setOnClickListener { moveTile(this) }
            }
            tiles.add(button)

            val params = GridLayout.LayoutParams().apply {
                width = tileSize
                height = tileSize
                rowSpec = GridLayout.spec(imageTiles.indexOf(tile) / size)
                columnSpec = GridLayout.spec(imageTiles.indexOf(tile) % size)
                setMargins(5, 5, 0, 0)
            }

            gridLayout.addView(button ?: Button(this).apply { visibility = Button.INVISIBLE }, params)
        }
    }

    private fun isImageSolved(): Boolean {
        for (i in 0 until imageTiles.size - 1) {
            if (imageTiles[i].number != i + 1) return false
        }
        return true
    }

    data class Tile(
        val number: Int,
        val image: Bitmap?,
    )

    private fun showWinPopup() {

        freezeGrid()    //aby uživatel nemohl hrát již dohranou hru
        AlertDialog.Builder(this)
            .setTitle("🎉 Congratulations!")
            .setMessage("You solved the puzzle!")
            .setCancelable(false)
            .setPositiveButton("Play Again") { dialog, _ ->
                dialog.dismiss()
                showOptionsPopup()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
        updateGrid()

        if(isLandscape()){
            root.orientation = LinearLayout.HORIZONTAL
        }else{
            root.orientation = LinearLayout.VERTICAL
        }
//        when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
//            Configuration.UI_MODE_NIGHT_YES -> {
//                root.setBackgroundColor(Color.BLACK)
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//            }
//            Configuration.UI_MODE_NIGHT_NO -> {
//                root.setBackgroundColor(Color.WHITE)
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//            }
//        }



    }

}


