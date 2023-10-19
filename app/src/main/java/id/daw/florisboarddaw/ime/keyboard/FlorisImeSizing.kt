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

package id.daw.florisboarddaw.ime.keyboard

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.daw.florisboarddaw.R
import id.daw.florisboarddaw.app.florisPreferenceModel
import id.daw.florisboarddaw.ime.onehanded.OneHandedMode
import id.daw.florisboarddaw.ime.smartbar.ExtendedActionsPlacement
import id.daw.florisboarddaw.ime.smartbar.SmartbarLayout
import id.daw.florisboarddaw.ime.text.keyboard.TextKeyboard
import id.daw.florisboarddaw.keyboardManager
import id.daw.florisboarddaw.lib.android.isOrientationLandscape
import id.daw.florisboarddaw.lib.observeAsTransformingState
import id.daw.florisboarddaw.lib.util.ViewUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState

private val LocalKeyboardRowBaseHeight = staticCompositionLocalOf { 65.dp }
private val LocalSmartbarHeight = staticCompositionLocalOf { 40.dp }

object FlorisImeSizing {
    val keyboardRowBaseHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyboardRowBaseHeight.current

    val smartbarHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalSmartbarHeight.current

    @Composable
    fun keyboardUiHeight(): Dp {
        val context = LocalContext.current
        val keyboardManager by context.keyboardManager()
        val evaluator by keyboardManager.activeEvaluator.collectAsState()
        val lastCharactersEvaluator by keyboardManager.lastCharactersEvaluator.collectAsState()
        val rowCount = when (evaluator.keyboard.mode) {
            KeyboardMode.CHARACTERS,
            KeyboardMode.NUMERIC_ADVANCED,
            KeyboardMode.SYMBOLS,
            KeyboardMode.SYMBOLS2 -> lastCharactersEvaluator.keyboard as TextKeyboard
            else -> evaluator.keyboard as TextKeyboard
        }.rowCount.coerceAtLeast(4)
        return (keyboardRowBaseHeight * rowCount)
    }

    @Composable
    fun smartbarUiHeight(): Dp {
        val prefs by florisPreferenceModel()
        val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
        val smartbarLayout by prefs.smartbar.layout.observeAsState()
        val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.observeAsState()
        val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()
        val height =
            if (smartbarEnabled) {
                if (smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED && extendedActionsExpanded &&
                    extendedActionsPlacement != ExtendedActionsPlacement.OVERLAY_APP_UI) {
                    smartbarHeight * 2
                } else {
                    smartbarHeight
                }
            } else {
                0.dp
            }
        return height
    }

    @Composable
    fun imeUiHeight(): Dp {
        return keyboardUiHeight() + smartbarUiHeight()
    }

    object Static {
        var keyboardRowBaseHeightPx: Int = 0

        var smartbarHeightPx: Int = 0
    }
}

@Composable
fun ProvideKeyboardRowBaseHeight(content: @Composable () -> Unit) {
    val prefs by florisPreferenceModel()
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current

    val heightFactorPortrait by prefs.keyboard.heightFactorPortrait.observeAsTransformingState { it.toFloat() / 100f }
    val heightFactorLandscape by prefs.keyboard.heightFactorLandscape.observeAsTransformingState { it.toFloat() / 100f }
    val oneHandedMode by prefs.keyboard.oneHandedMode.observeAsState()
    val oneHandedModeScaleFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsTransformingState { it.toFloat() / 100f }

    val baseRowHeight = remember(
        configuration, resources, heightFactorPortrait, heightFactorLandscape,
        oneHandedMode, oneHandedModeScaleFactor,
    ) {
        calcInputViewHeight(resources) * when {
            configuration.isOrientationLandscape() -> heightFactorLandscape
            else -> heightFactorPortrait * (if (oneHandedMode != OneHandedMode.OFF) oneHandedModeScaleFactor else 1f)
        }
    }
    val smartbarHeight = baseRowHeight * 0.753f

    SideEffect {
        FlorisImeSizing.Static.keyboardRowBaseHeightPx = baseRowHeight.toInt()
        FlorisImeSizing.Static.smartbarHeightPx = smartbarHeight.toInt()
    }

    CompositionLocalProvider(
        LocalKeyboardRowBaseHeight provides ViewUtils.px2dp(baseRowHeight).dp,
        LocalSmartbarHeight provides ViewUtils.px2dp(smartbarHeight).dp,
    ) {
        content()
    }
}

/**
 * Calculates the input view height based on the current screen dimensions and the auto
 * selected dimension values.
 *
 * This method and the fraction values have been inspired by [OpenBoard](https://github.com/dslul/openboard)
 * but are not 1:1 the same. This implementation differs from the
 * [original](https://github.com/dslul/openboard/blob/90ae4c8aec034a8935e1fd02b441be25c7dba6ce/app/src/main/java/org/dslul/openboard/inputmethod/latin/utils/ResourceUtils.java)
 * by calculating the average of the min and max height values, then taking at least the input
 * view base height and return this resulting value.
 */
private fun calcInputViewHeight(resources: Resources): Float {
    val dm = resources.displayMetrics
    val minBaseSize = when {
        resources.configuration.isOrientationLandscape() -> resources.getFraction(
            R.fraction.inputView_minHeightFraction, dm.heightPixels, dm.heightPixels
        )
        else -> resources.getFraction(
            R.fraction.inputView_minHeightFraction, dm.widthPixels, dm.widthPixels
        )
    }
    val maxBaseSize = resources.getFraction(
        R.fraction.inputView_maxHeightFraction, dm.heightPixels, dm.heightPixels
    )
    return ((minBaseSize + maxBaseSize) / 2.0f).coerceAtLeast(
        resources.getDimension(R.dimen.inputView_baseHeight)
    ) * 0.21f
}
