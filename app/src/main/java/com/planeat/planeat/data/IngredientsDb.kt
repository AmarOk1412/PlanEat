package com.planeat.planeat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@Database(entities = [Ingredient::class], version = 1,
          exportSchema = true // Ensure that the schema is exported
)
abstract class IngredientsDb : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao

    companion object {
        @Volatile
        private var INSTANCE: IngredientsDb? = null

        /**
         * Exports the current database to the specified location on external storage.
         * @param context The context to get the current database path.
         * @param backupFileName The name of the file to export the database to.
         * @return Boolean indicating success or failure.
         */
        fun exportDatabase(context: Context, backupFileName: String): Boolean {
            val currentDBPath = context.getDatabasePath("ingredients_database").absolutePath
            val backupDir = File(context.getExternalFilesDir(null), "db_backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs() // Create the directory if it doesn't exist
            }

            val backupDB = File(backupDir, backupFileName)

            return try {
                FileInputStream(currentDBPath).use { inputStream ->
                    FileOutputStream(backupDB).use { outputStream ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (inputStream.read(buffer).also { length = it } > 0) {
                            outputStream.write(buffer, 0, length)
                        }
                        outputStream.flush()
                    }
                }
                true // Success
            } catch (e: IOException) {
                e.printStackTrace()
                false // Failure
            }
        }

        fun getDatabase(context: Context): IngredientsDb {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IngredientsDb::class.java,
                    "ingredients_database"
                )
                .createFromAsset("prepopulated_database.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                instance
            }
        }
    }
}