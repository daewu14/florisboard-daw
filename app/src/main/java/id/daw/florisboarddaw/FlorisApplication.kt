/*
 * Copyright (C) 2021 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package id.daw.florisboarddaw

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.core.os.UserManagerCompat
import dev.patrickgold.jetpref.datastore.BuildConfig
import id.daw.florisboarddaw.app.florisPreferenceModel
import id.daw.florisboarddaw.ime.clipboard.ClipboardManager
import id.daw.florisboarddaw.ime.core.SubtypeManager
import id.daw.florisboarddaw.ime.dictionary.DictionaryManager
import id.daw.florisboarddaw.ime.editor.EditorInstance
import id.daw.florisboarddaw.ime.keyboard.KeyboardManager
import id.daw.florisboarddaw.ime.media.emoji.FlorisEmojiCompat
import id.daw.florisboarddaw.ime.nlp.NlpManager
import id.daw.florisboarddaw.ime.text.gestures.GlideTypingManager
import id.daw.florisboarddaw.ime.theme.ThemeManager
import id.daw.florisboarddaw.lib.NativeStr
import id.daw.florisboarddaw.lib.cache.CacheManager
import id.daw.florisboarddaw.lib.crashutility.CrashUtility
import id.daw.florisboarddaw.lib.devtools.Flog
import id.daw.florisboarddaw.lib.devtools.LogTopic
import id.daw.florisboarddaw.lib.devtools.flogError
import id.daw.florisboarddaw.lib.devtools.flogInfo
import id.daw.florisboarddaw.lib.ext.ExtensionManager
import id.daw.florisboarddaw.lib.io.AssetManager
import id.daw.florisboarddaw.lib.io.deleteContentsRecursively
import id.daw.florisboarddaw.lib.io.subFile
import id.daw.florisboarddaw.lib.kotlin.tryOrNull
import id.daw.florisboarddaw.lib.toNativeStr
import dev.patrickgold.jetpref.datastore.JetPref
import java.lang.ref.WeakReference

/**
 * Global weak reference for the [FlorisApplication] class. This is needed as in certain scenarios an application
 * reference is needed, but the Android framework hasn't finished setting up
 */
private var FlorisApplicationReference = WeakReference<FlorisApplication?>(null)

@Suppress("unused")
class FlorisApplication : Application() {
    companion object {
        private const val ICU_DATA_ASSET_PATH = "icu4c/icudt.dat"

        private external fun nativeInitICUData(path: NativeStr): Int

        init {
            try {
                // System.loadLibrary("florisboard-native")
            } catch (_: Exception) {
            }
        }
    }

    private val prefs by florisPreferenceModel()
    private val mainHandler by lazy { Handler(mainLooper) }

    val assetManager = lazy { AssetManager(this) }
    val cacheManager = lazy { CacheManager(this) }
    val clipboardManager = lazy { ClipboardManager(this) }
    val editorInstance = lazy { EditorInstance(this) }
    val extensionManager = lazy { ExtensionManager(this) }
    val glideTypingManager = lazy { GlideTypingManager(this) }
    val keyboardManager = lazy { KeyboardManager(this) }
    val nlpManager = lazy { NlpManager(this) }
    val subtypeManager = lazy { SubtypeManager(this) }
    val themeManager = lazy { ThemeManager(this) }

    override fun onCreate() {
        super.onCreate()
        FlorisApplicationReference = WeakReference(this)
        try {
            JetPref.configure(saveIntervalMs = 500)
            Flog.install(
                context = this,
                isFloggingEnabled = BuildConfig.DEBUG,
                flogTopics = LogTopic.ALL,
                flogLevels = Flog.LEVEL_ALL,
                flogOutputs = Flog.OUTPUT_CONSOLE,
            )
            CrashUtility.install(this)
            FlorisEmojiCompat.init(this)

            if (!UserManagerCompat.isUserUnlocked(this)) {
                cacheDir?.deleteContentsRecursively()
                extensionManager.value.init()
                registerReceiver(BootComplete(), IntentFilter(Intent.ACTION_USER_UNLOCKED))
                return
            }

            init()
        } catch (e: Exception) {
            CrashUtility.stageException(e)
            return
        }
    }

    fun init() {
        initICU(this)
        cacheDir?.deleteContentsRecursively()
        prefs.initializeBlocking(this)
        extensionManager.value.init()
        clipboardManager.value.initializeForContext(this)
        DictionaryManager.init(this)
    }

    fun initICU(context: Context): Boolean {
        try {
            val androidAssetManager = context.assets ?: return false
            val icuTmpDataFile = context.cacheDir.subFile("icudt.dat")
            icuTmpDataFile.outputStream().use { os ->
                androidAssetManager.open(ICU_DATA_ASSET_PATH).use { it.copyTo(os) }
            }
            val status = nativeInitICUData(icuTmpDataFile.absolutePath.toNativeStr())
            icuTmpDataFile.delete()
            return if (status != 0) {
                flogError { "Native ICU data initializing failed with error code $status!" }
                false
            } else {
                flogInfo { "Successfully loaded ICU data!" }
                true
            }
        } catch (e: Exception) {
            flogError { e.toString() }
            return false
        }
    }

    private inner class BootComplete : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                try {
                    unregisterReceiver(this)
                } catch (e: Exception) {
                    flogError { e.toString() }
                }
                mainHandler.post { init() }
            }
        }
    }
}

private tailrec fun Context.florisApplication(): FlorisApplication {
    return when (this) {
        is FlorisApplication -> this
        is ContextWrapper -> when {
            this.baseContext != null -> this.baseContext.florisApplication()
            else -> FlorisApplicationReference.get()!!
        }
        else -> tryOrNull { this.applicationContext as FlorisApplication } ?: FlorisApplicationReference.get()!!
    }
}

fun Context.appContext() = lazyOf(this.florisApplication())

fun Context.assetManager() = this.florisApplication().assetManager

fun Context.cacheManager() = this.florisApplication().cacheManager

fun Context.clipboardManager() = this.florisApplication().clipboardManager

fun Context.editorInstance() = this.florisApplication().editorInstance

fun Context.extensionManager() = this.florisApplication().extensionManager

fun Context.glideTypingManager() = this.florisApplication().glideTypingManager

fun Context.keyboardManager() = this.florisApplication().keyboardManager

fun Context.nlpManager() = this.florisApplication().nlpManager

fun Context.subtypeManager() = this.florisApplication().subtypeManager

fun Context.themeManager() = this.florisApplication().themeManager
