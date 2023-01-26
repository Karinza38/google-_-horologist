/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.horologist.auth.data.tokenshare

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.horologist.auth.data.ExperimentalHorologistAuthDataApi
import com.google.android.horologist.auth.data.common.logging.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Base service class for applications wishing to receive auth token via data layer events.
 *
 * Must include the appropriate registration in the manifest. Such as
 *
 * ```
 * <service
 *   android:name=".SampleTokenListenerService"
 *   android:exported="true">
 *   <intent-filter>
 *     <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
 *     <data android:scheme="wear" android:host="*" android:pathPrefix="/horologist_auth" />
 *   </intent-filter>
 * </service>
 * ```
 */
@ExperimentalHorologistAuthDataApi
public abstract class TokenListenerService : WearableListenerService() {

    public abstract fun getCoroutineScope(): CoroutineScope

    public abstract suspend fun onTokenReceived(token: String): Unit

    @SuppressLint("VisibleForTests") // https://issuetracker.google.com/issues/239451111
    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        dataEventBuffer.forEach { event ->
            val uri = event.dataItem.uri
            if (event.type == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: $uri")
            } else if (event.type == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem changed: $uri")

                DataMapItem.fromDataItem(event.dataItem)
                    .dataMap.getString(KEY_TOKEN)
                    ?.let { token ->
                        getCoroutineScope().launch {
                            onTokenReceived(token)
                        }
                    }
                    ?: run {
                        Log.d(TAG, "Token not found in $uri.")
                    }
            }
        }
    }

    private companion object {
        private const val KEY_TOKEN = "token"
    }
}