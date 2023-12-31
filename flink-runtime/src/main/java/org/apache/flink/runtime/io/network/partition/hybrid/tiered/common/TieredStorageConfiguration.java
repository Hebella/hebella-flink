/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.io.network.partition.hybrid.tiered.common;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.io.network.partition.hybrid.tiered.tier.TierFactory;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/** Configurations for the Tiered Storage. */
public class TieredStorageConfiguration {

    // TODO, after implementing the tier factory, add appreciate implementations to the array.
    private static final TierFactory[] DEFAULT_MEMORY_DISK_TIER_FACTORIES = new TierFactory[0];

    /** If the remote storage tier is not used, this field may be null. */
    @Nullable private final String remoteStorageBasePath;

    public TieredStorageConfiguration(@Nullable String remoteStorageBasePath) {
        this.remoteStorageBasePath = remoteStorageBasePath;
    }

    public List<TierFactory> getTierFactories() {
        return Arrays.asList(DEFAULT_MEMORY_DISK_TIER_FACTORIES);
    }

    @Nullable
    public String getRemoteStorageBasePath() {
        return remoteStorageBasePath;
    }

    public static TieredStorageConfiguration.Builder builder() {
        return new TieredStorageConfiguration.Builder();
    }

    /** Builder for {@link TieredStorageConfiguration}. */
    public static class Builder {

        @Nullable private String remoteStorageBasePath;

        TieredStorageConfiguration.Builder setRemoteStorageBasePath(String remoteStorageBasePath) {
            this.remoteStorageBasePath = remoteStorageBasePath;
            return this;
        }

        public TieredStorageConfiguration build() {
            return new TieredStorageConfiguration(remoteStorageBasePath);
        }
    }

    public static TieredStorageConfiguration fromConfiguration(Configuration conf) {
        // TODO, from the configuration, get the configured options(i.e., remote storage path, the
        // reserved storage size, etc.), then set them to the builder.
        return new TieredStorageConfiguration.Builder().build();
    }
}
