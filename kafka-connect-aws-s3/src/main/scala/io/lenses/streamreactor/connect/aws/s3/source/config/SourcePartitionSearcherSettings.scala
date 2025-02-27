/*
 * Copyright 2017-2023 Lenses.io Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lenses.streamreactor.connect.aws.s3.source.config

import com.datamountaineer.streamreactor.common.config.base.traits.BaseSettings
import io.lenses.streamreactor.connect.aws.s3.config.S3Config
import io.lenses.streamreactor.connect.aws.s3.config.S3ConfigSettings._

import scala.concurrent.duration.DurationLong

trait SourcePartitionSearcherSettings extends BaseSettings {

  def getPartitionSearcherOptions(props: Map[String, _]): PartitionSearcherOptions =
    PartitionSearcherOptions(
      recurseLevels = getInt(SOURCE_PARTITION_SEARCH_RECURSE_LEVELS),
      continuous    = getBoolean(SOURCE_PARTITION_SEARCH_MODE),
      interval = S3Config.getLong(props, SOURCE_PARTITION_SEARCH_INTERVAL_MILLIS).getOrElse(
        SOURCE_PARTITION_SEARCH_INTERVAL_MILLIS_DEFAULT,
      ).millis,
    )
}
