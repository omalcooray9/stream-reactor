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
package com.landoop.streamreactor.connect.hive.sink

import com.landoop.streamreactor.connect.hive.formats.OrcHiveFormat
import com.landoop.streamreactor.connect.hive.formats.ParquetHiveFormat
import com.landoop.streamreactor.connect.hive.sink.config.HiveSinkConfig
import com.landoop.streamreactor.connect.hive.sink.evolution.AddEvolutionPolicy
import com.landoop.streamreactor.connect.hive.sink.partitioning.DynamicPartitionHandler
import com.landoop.streamreactor.connect.hive.sink.staging.DefaultCommitPolicy
import com.landoop.streamreactor.connect.hive.PartitionField
import com.landoop.streamreactor.connect.hive.TableName
import com.landoop.streamreactor.connect.hive.Topic
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HiveSinkConfigTest extends AnyWordSpec with Matchers {

  "HiveSink" should {
    "populate required table properties from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a,b,c from mytopic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.topic shouldBe Topic("mytopic")
      tableConfig.tableName shouldBe TableName("mytable")
      tableConfig.projection.get.map(_.getName).toList shouldBe Seq("a", "b", "c")
    }
    "populate aliases from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.projection.get.map(_.getName).toList shouldBe Seq("a", "b", "c")
      tableConfig.projection.get.map(_.getAlias).toList shouldBe Seq("x", "b", "z")
    }
    "set projection to None for *" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select * from mytopic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.projection shouldBe None
    }
    "populate format from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic storeas orc",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.format shouldBe OrcHiveFormat
    }
    "default to parquet when format is not specified" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.format shouldBe ParquetHiveFormat
    }
    "populate schema evolution policy from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic with_schema_evolution=add",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.evolutionPolicy shouldBe AddEvolutionPolicy
    }
    "populate commitPolicy from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic WITH_FLUSH_SIZE=912 WITH_FLUSH_INTERVAL=1 WITH_FLUSH_COUNT=333",
        ),
      )
      val tableConfig = config.tableOptions.head
      import scala.concurrent.duration._
      tableConfig.commitPolicy shouldBe DefaultCommitPolicy(Option(912), Option(1.second), Option(333))
    }
    "populate partitions from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic PARTITIONBY g,h,t",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.partitions shouldBe Seq(PartitionField("g"), PartitionField("h"), PartitionField("t"))
    }
    "populate partitioningPolicy from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic WITH_PARTITIONING=dynamic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.partitioner.getClass shouldBe classOf[DynamicPartitionHandler]
    }
    "populate create from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic AUTOCREATE",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.createTable shouldBe true
    }
    "default to createTable=false" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.createTable shouldBe false
    }
    "populate overwrite from KCQL" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic WITH_OVERWRITE",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.overwriteTable shouldBe true
    }
    "default to overwriteTable=false" in {
      val config = HiveSinkConfig.fromProps(
        Map(
          "connect.hive.database.name"  -> "mydatabase",
          "connect.hive.metastore"      -> "thrift",
          "connect.hive.metastore.uris" -> "thrift://localhost:9083",
          "connect.hive.fs.defaultFS"   -> "hdfs://localhost:8020",
          "connect.hive.kcql"           -> "insert into mytable select a as x,b,c as z from mytopic",
        ),
      )
      val tableConfig = config.tableOptions.head
      tableConfig.overwriteTable shouldBe false
    }
  }
}
