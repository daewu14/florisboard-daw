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

package id.daw.florisboarddaw.lib.kotlin

@Throws(NoSuchElementException::class)
fun <K, V> Map<K, V>.getKeyByValue(value: V): K {
    for ((k, v) in this.entries) {
        if (value == v) return k
    }
    throw NoSuchElementException("Value $value is missing in the map.")
}

inline fun <T, reified R> Array<T>.map(transform: (T) -> R): Array<R> {
    return Array(this.size) { n -> transform(this[n]) }
}
