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
package com.datamountaineer.streamreactor.common.config.base.traits

import com.datamountaineer.streamreactor.common.config.base.const.TraitConfigConst.THREAD_POLL_PROP_SUFFIX

trait ThreadPoolSettings extends BaseSettings {
  def threadPoolConstant: String = s"$connectorPrefix.$THREAD_POLL_PROP_SUFFIX"

  def getThreadPoolSize: Int = {
    val threads = getInt(threadPoolConstant)
    if (threads <= 0) 4 * Runtime.getRuntime.availableProcessors()
    else threads
  }

}
