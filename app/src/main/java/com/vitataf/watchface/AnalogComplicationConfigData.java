/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vitataf.watchface;

import android.content.Context;
import android.graphics.Color;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;

/**
 * Data represents different views for configuring the
 * {@link MyWatchFace} watch face's appearance and complications
 * via {@link AnalogComplicationConfigActivity}.
 */
public class AnalogComplicationConfigData {


    /**
     * Interface all ConfigItems must implement so the {@link RecyclerView}'s Adapter associated
     * with the configuration activity knows what type of ViewHolder to inflate.
     */
    public interface ConfigItemType {
        int getConfigType();
    }

    /**
     * Returns Watch Face Service class associated with configuration Activity.
     */
    public static Class getWatchFaceServiceClass() {
        return MyWatchFace.class;
    }

    /**
     * Returns Material Design color options.
     */
    public static ArrayList<Integer> getColorOptionsDataSet() {
        ArrayList<Integer> colorOptionsDataSet = new ArrayList<>();
        colorOptionsDataSet.add(Color.parseColor("#FFFFFF")); // White

        colorOptionsDataSet.add(Color.parseColor("#FFEB3B")); // Yellow
        colorOptionsDataSet.add(Color.parseColor("#FFC107")); // Amber
        colorOptionsDataSet.add(Color.parseColor("#FF9800")); // Orange
        colorOptionsDataSet.add(Color.parseColor("#FF5722")); // Deep Orange

        colorOptionsDataSet.add(Color.parseColor("#F44336")); // Red
        colorOptionsDataSet.add(Color.parseColor("#E91E63")); // Pink

        colorOptionsDataSet.add(Color.parseColor("#9C27B0")); // Purple
        colorOptionsDataSet.add(Color.parseColor("#673AB7")); // Deep Purple
        colorOptionsDataSet.add(Color.parseColor("#3F51B5")); // Indigo
        colorOptionsDataSet.add(Color.parseColor("#2196F3")); // Blue
        colorOptionsDataSet.add(Color.parseColor("#03A9F4")); // Light Blue

        colorOptionsDataSet.add(Color.parseColor("#00BCD4")); // Cyan
        colorOptionsDataSet.add(Color.parseColor("#009688")); // Teal
        colorOptionsDataSet.add(Color.parseColor("#4CAF50")); // Green
        colorOptionsDataSet.add(Color.parseColor("#8BC34A")); // Lime Green
        colorOptionsDataSet.add(Color.parseColor("#CDDC39")); // Lime

        colorOptionsDataSet.add(Color.parseColor("#607D8B")); // Blue Grey
        colorOptionsDataSet.add(Color.parseColor("#9E9E9E")); // Grey
        colorOptionsDataSet.add(Color.parseColor("#795548")); // Brown
        colorOptionsDataSet.add(Color.parseColor("#000000")); // Black

        return colorOptionsDataSet;
    }

    /**
     * Includes all data to populate each of the 5 different custom
     * {@link ViewHolder} types in {@link AnalogComplicationConfigRecyclerViewAdapter}.
     */
    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face preview and complications UX in settings Activity.
        ConfigItemType complicationConfigItem =
                new PreviewAndComplicationsConfigItem(R.drawable.add_complication);
        settingsConfigData.add(complicationConfigItem);

        return settingsConfigData;
    }

    /**
     * Data for Watch Face Preview with Complications Preview item in RecyclerView.
     */
    public static class PreviewAndComplicationsConfigItem implements ConfigItemType {

        private int defaultComplicationResourceId;

        PreviewAndComplicationsConfigItem(int defaultComplicationResourceId) {
            this.defaultComplicationResourceId = defaultComplicationResourceId;
        }

        public int getDefaultComplicationResourceId() {
            return defaultComplicationResourceId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG;
        }
    }

}