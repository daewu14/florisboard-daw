package id.daw.florisboarddaw.ime.nlp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import id.daw.florisboarddaw.appContext
import id.daw.florisboarddaw.assetManager
import id.daw.florisboarddaw.lib.FlorisLocale
import id.daw.florisboarddaw.lib.android.copy
import id.daw.florisboarddaw.lib.devtools.flogDebug
import id.daw.florisboarddaw.lib.devtools.flogError
import id.daw.florisboarddaw.lib.ext.Extension
import id.daw.florisboarddaw.lib.ext.ExtensionComponent
import id.daw.florisboarddaw.lib.ext.ExtensionComponentName
import id.daw.florisboarddaw.lib.ext.ExtensionEditor
import id.daw.florisboarddaw.lib.ext.ExtensionMeta
import id.daw.florisboarddaw.lib.io.FlorisRef
import id.daw.florisboarddaw.lib.io.FsDir
import id.daw.florisboarddaw.lib.io.parentDir
import id.daw.florisboarddaw.lib.io.subFile
import id.daw.florisboarddaw.lib.kotlin.tryOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class LanguagePackComponent(
    override val id: String,
    override val label: String,
    override val authors: List<String>,
    val locale: FlorisLocale = FlorisLocale.fromTag(id),
    val hanShapeBasedKeyCode: String = "abcdefghijklmnopqrstuvwxyz",
) : ExtensionComponent {
    @Transient var parent: LanguagePackExtension? = null

    @SerialName("hanShapeBasedTable")
    private val _hanShapeBasedTable: String? = null  // Allows overriding the sqlite3 table to query in the json
    val hanShapeBasedTable
        get() = _hanShapeBasedTable ?: locale.variant
}

@SerialName(LanguagePackExtension.SERIAL_TYPE)
@Serializable
class LanguagePackExtension( // FIXME: how to make this support multiple types of language packs, and selectively load?
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val items: List<LanguagePackComponent> = listOf(),
    val hanShapeBasedSQLite: String = "han.sqlite3",
) : Extension() {

    override fun components(): List<ExtensionComponent> = items

    override fun edit(): ExtensionEditor {
        TODO("LOL LMAO")
    }

    companion object {
        const val SERIAL_TYPE = "ime.extension.languagepack"
    }

    override fun serialType() = SERIAL_TYPE

    @Transient var hanShapeBasedSQLiteDatabase: SQLiteDatabase = SQLiteDatabase.create(null)

    override fun onAfterLoad(context: Context, cacheDir: FsDir) {
        // FIXME: this is loading language packs of all subtypes when they load.
        super.onAfterLoad(context, cacheDir)

        val databasePath = workingDir?.subFile(hanShapeBasedSQLite)?.path
        if (databasePath == null) {
            flogError { "Han shape-based language pack not found or loaded" }
        } else try {
            // TODO: use lock on database?
            hanShapeBasedSQLiteDatabase.takeIf { it.isOpen }?.close()
            hanShapeBasedSQLiteDatabase =
                SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (e: SQLiteException) {
            flogError { "SQLiteException in openDatabase: path=$databasePath, error='${e}'" }
        }
    }

    override fun onBeforeUnload(context: Context, cacheDir: FsDir) {
        super.onBeforeUnload(context, cacheDir)
        hanShapeBasedSQLiteDatabase.takeIf { it.isOpen }?.close()
    }
}
