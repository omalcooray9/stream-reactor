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
package com.landoop.streamreactor.connect.hive.sink.mapper

import com.landoop.streamreactor.connect.hive.StructMapper
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.errors.ConnectException

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

/**
  * An compile of [[StructMapper]] that will align an input
  * record so that it's structure matches that defined in the metastore.
  *
  * Sometimes an input record will not contain a value for a field defined
  * by the metastore schema. If the metastore field has a default value
  * or allows nulls, then output record could be padded to include that field.
  *
  * Secondly, the input record may specify extra fields which are not required
  * by the hive table. Therefore these records can be safely dropped.
  *
  * Lastly, for file formats that do not include field information, such as
  * CSV without headers, the field orderings must match the hive metastore.
  * An input record may include all the required fields but in a different
  * order, and so this transformer can reorder the fields as required.
  */
class MetastoreSchemaAlignMapper(schema: Schema) extends StructMapper {

  override def map(input: Struct): Struct = {
    //hive converts everything to lowercase
    val inputFieldsMapping = input.schema().fields().asScala.map(f => f.name().toLowerCase() -> f.name()).toMap
    val struct = schema.fields.asScala.foldLeft(new Struct(schema)) { (struct, field) =>
      Try(input.get(inputFieldsMapping(field.name))).toOption match {
        case Some(value)                     => struct.put(field.name, value)
        case None if field.schema.isOptional => struct.put(field.name, null)
        case None =>
          throw new ConnectException(
            s"Cannot map struct to required schema; ${field.name} is missing, no default value has been supplied and null is not permitted",
          )
      }
    }
    struct
  }
}
