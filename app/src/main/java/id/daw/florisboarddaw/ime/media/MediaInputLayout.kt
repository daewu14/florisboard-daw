/*
 * Copyright (C) 2022 Patrick Goldinger
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

package id.daw.florisboarddaw.ime.media

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import id.daw.florisboarddaw.R
import id.daw.florisboarddaw.ime.input.InputEventDispatcher
import id.daw.florisboarddaw.ime.input.LocalInputFeedbackController
import id.daw.florisboarddaw.ime.keyboard.FlorisImeSizing
import id.daw.florisboarddaw.ime.keyboard.KeyData
import id.daw.florisboarddaw.ime.media.emoji.EmojiPaletteView
import id.daw.florisboarddaw.ime.media.emoji.PlaceholderLayoutDataMap
import id.daw.florisboarddaw.ime.media.emoji.parseRawEmojiSpecsFile
import id.daw.florisboarddaw.ime.text.keyboard.TextKeyData
import id.daw.florisboarddaw.ime.theme.FlorisImeTheme
import id.daw.florisboarddaw.ime.theme.FlorisImeUi
import id.daw.florisboarddaw.keyboardManager
import id.daw.florisboarddaw.lib.snygg.ui.SnyggSurface
import kotlinx.coroutines.coroutineScope

@SuppressLint("MutableCollectionMutableState")
@Composable
fun MediaInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    var emojiLayoutDataMap by remember { mutableStateOf(PlaceholderLayoutDataMap) }
    LaunchedEffect(Unit) {
        emojiLayoutDataMap = parseRawEmojiSpecsFile(context, "ime/media/emoji/root.txt")
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.imeUiHeight()),
        ) {
            EmojiPaletteView(
                modifier = Modifier.weight(1f),
                fullEmojiMappings = emojiLayoutDataMap,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FlorisImeSizing.keyboardRowBaseHeight * 0.8f),
            ) {
                KeyboardLikeButton(
                    inputEventDispatcher = keyboardManager.inputEventDispatcher,
                    keyData = TextKeyData.IME_UI_MODE_TEXT,
                ) {
                    Text(
                        text = "ABC",
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                KeyboardLikeButton(
                    inputEventDispatcher = keyboardManager.inputEventDispatcher,
                    keyData = TextKeyData.DELETE,
                ) {
                    Icon(painter = painterResource(R.drawable.ic_backspace), contentDescription = null)
                }
            }
        }
    }
}

@Composable
internal fun KeyboardLikeButton(
    modifier: Modifier = Modifier,
    inputEventDispatcher: InputEventDispatcher,
    keyData: KeyData,
    content: @Composable RowScope.() -> Unit,
) {
    val inputFeedbackController = LocalInputFeedbackController.current
    var isPressed by remember { mutableStateOf(false) }
    val keyStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.EmojiKey,
        code = keyData.code,
        isPressed = isPressed,
    )
    SnyggSurface(
        modifier = modifier.pointerInput(Unit) {
            forEachGesture {
                coroutineScope {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false).also {
                            if (it.pressed != it.previousPressed) it.consume()
                        }
                        isPressed = true
                        inputEventDispatcher.sendDown(keyData)
                        inputFeedbackController.keyPress(keyData)
                        val up = waitForUpOrCancellation()
                        isPressed = false
                        if (up != null) {
                            inputEventDispatcher.sendUp(keyData)
                        } else {
                            inputEventDispatcher.sendCancel(keyData)
                        }
                    }
                }
            }
        },
        style = keyStyle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
