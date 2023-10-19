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

package id.daw.florisboarddaw.app.settings.localization

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.daw.florisboarddaw.R
import id.daw.florisboarddaw.app.LocalNavController
import id.daw.florisboarddaw.app.Routes
import id.daw.florisboarddaw.app.settings.advanced.Restore
import id.daw.florisboarddaw.app.settings.theme.ThemeManagerScreenAction
import id.daw.florisboarddaw.cacheManager
import id.daw.florisboarddaw.ime.core.DisplayLanguageNamesIn
import id.daw.florisboarddaw.ime.keyboard.LayoutType
import id.daw.florisboarddaw.ime.nlp.LanguagePackExtension
import id.daw.florisboarddaw.ime.nlp.han.HanShapeBasedLanguageProvider
import id.daw.florisboarddaw.keyboardManager
import id.daw.florisboarddaw.lib.android.readToFile
import id.daw.florisboarddaw.lib.android.showLongToast
import id.daw.florisboarddaw.lib.compose.FlorisScreen
import id.daw.florisboarddaw.lib.compose.FlorisWarningCard
import id.daw.florisboarddaw.lib.compose.stringRes
import id.daw.florisboarddaw.lib.io.ZipUtils
import id.daw.florisboarddaw.lib.io.parentDir
import id.daw.florisboarddaw.lib.io.subFile
import id.daw.florisboarddaw.lib.observeAsNonNullState
import id.daw.florisboarddaw.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup

@Composable
fun LocalizationScreen() = FlorisScreen {
    title = stringRes(R.string.settings__localization__title)
    previewFieldVisible = true
    iconSpaceReserved = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val cacheManager by context.cacheManager()

    floatingActionButton {
        ExtendedFloatingActionButton(
            icon = { Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringRes(R.string.settings__localization__subtype_add_title),
            ) },
            text = { Text(
                text = stringRes(R.string.settings__localization__subtype_add_title),
            ) },
            onClick = { navController.navigate(Routes.Settings.SubtypeAdd) },
        )
    }


    content {
        ListPreference(
            prefs.localization.displayLanguageNamesIn,
            title = stringRes(R.string.settings__localization__display_language_names_in__label),
            entries = DisplayLanguageNamesIn.listEntries(),
        )
        Preference(
//            iconId = R.drawable.ic_edit,
            title = stringRes(R.string.settings__localization__language_pack_title),
            summary = stringRes(R.string.settings__localization__language_pack_summary),
            onClick = {
                navController.navigate(Routes.Settings.LanguagePackManager(LanguagePackManagerScreenAction.MANAGE))
            },
        )
        PreferenceGroup(title = stringRes(R.string.settings__localization__group_subtypes__label)) {
            val subtypes by subtypeManager.subtypesFlow.collectAsState()
            if (subtypes.isEmpty()) {
                FlorisWarningCard(
                    modifier = Modifier.padding(all = 8.dp),
                    text = stringRes(R.string.settings__localization__subtype_no_subtypes_configured_warning),
                )
            } else {
                val currencySets by keyboardManager.resources.currencySets.observeAsNonNullState()
                val layouts by keyboardManager.resources.layouts.observeAsNonNullState()
                val displayLanguageNamesIn by prefs.localization.displayLanguageNamesIn.observeAsState()
                for (subtype in subtypes) {
                    val cMeta = layouts[LayoutType.CHARACTERS]?.get(subtype.layoutMap.characters)
                    val sMeta = layouts[LayoutType.SYMBOLS]?.get(subtype.layoutMap.symbols)
                    val currMeta = currencySets[subtype.currencySet]
                    val summary = stringRes(
                        id = R.string.settings__localization__subtype_summary,
                        "characters_name" to (cMeta?.label ?: "null"),
                        "symbols_name" to (sMeta?.label ?: "null"),
                        "currency_set_name" to (currMeta?.label ?: "null"),
                    )
                    Preference(
                        title = when (displayLanguageNamesIn) {
                            DisplayLanguageNamesIn.SYSTEM_LOCALE -> subtype.primaryLocale.displayName()
                            DisplayLanguageNamesIn.NATIVE_LOCALE -> subtype.primaryLocale.displayName(subtype.primaryLocale)
                        },
                        summary = summary,
                        onClick = { navController.navigate(
                            Routes.Settings.SubtypeEdit(subtype.id)
                        ) },
                    )
                }
            }
        }

        //PreferenceGroup(title = stringRes(R.string.settings__localization__group_layouts__label)) {
        //}
    }
}