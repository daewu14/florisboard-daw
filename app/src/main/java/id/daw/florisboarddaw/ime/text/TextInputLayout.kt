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

package id.daw.florisboarddaw.ime.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import id.daw.florisboarddaw.R
import id.daw.florisboarddaw.ime.text.keyboard.TextKeyboardLayout
import id.daw.florisboarddaw.ime.smartbar.Smartbar
import id.daw.florisboarddaw.ime.smartbar.quickaction.QuickActionsOverflowPanel
import id.daw.florisboarddaw.ime.theme.FlorisImeTheme
import id.daw.florisboarddaw.ime.theme.FlorisImeUi
import id.daw.florisboarddaw.keyboardManager
import id.daw.florisboarddaw.lib.snygg.ui.solidColor

@Composable
fun TextInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val state by keyboardManager.activeState.collectAsState()
    val evaluator by keyboardManager.activeEvaluator.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Smartbar()
            if (state.isActionsOverflowVisible) {
                QuickActionsOverflowPanel()
            } else {
                Box {
                    if (evaluator.state.isIncognitoMode) {
                        val indicatorStyle = FlorisImeTheme.style.get(FlorisImeUi.IncognitoModeIndicator)
                        Icon(
                            modifier = Modifier
                                .requiredSize(192.dp)
                                .align(Alignment.Center),
                            painter = painterResource(R.drawable.ic_incognito),
                            contentDescription = null,
                            tint = indicatorStyle.foreground.solidColor(
                                default = FlorisImeTheme.fallbackContentColor().copy(alpha = 0.067f),
                            ),
                        )
                    }
                    TextKeyboardLayout(evaluator = evaluator)
                }
            }
        }
    }
}
