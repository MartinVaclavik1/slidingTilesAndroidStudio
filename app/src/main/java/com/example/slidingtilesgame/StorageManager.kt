package com.example.slidingtilesgame

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class StorageManager(val context: Context) {
    private val fileName = "game_history_list.json"
    private val gson = Gson()

    fun addStats(size: Int, time: String, minutes: Long, seconds: Long) {
        val existingStats = loadAllStats().toMutableList()
        var statsSize = existingStats.find {
                stats -> stats.gridSize == size
        }

        val newEntry = GameStats(size,time, minutes, seconds)
        if (statsSize != null) {
            if (statsSize.minutes > minutes) {
                existingStats.remove(statsSize)
                existingStats.add(newEntry)
            } else if (statsSize.minutes == minutes && statsSize.seconds > seconds) {
                existingStats.remove(statsSize)
                existingStats.add(newEntry)
            }
        }


        // 3. Převedeme celý seznam na JSON
        val jsonString = gson.toJson(existingStats)

        // 4. Zápis do souboru
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(jsonString.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAllStats(): List<GameStats> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            // Správný TypeToken pro List<GameStats>
            val listType = object : TypeToken<List<GameStats>>() {}.type
            gson.fromJson(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}