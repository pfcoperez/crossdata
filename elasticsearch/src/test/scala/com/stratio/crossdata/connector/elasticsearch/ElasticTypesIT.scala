/**
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.crossdata.connector.elasticsearch

import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import org.apache.spark.sql.crossdata.test.SharedXDContextTypesTest
import org.apache.spark.sql.crossdata.test.SharedXDContextTypesTest.SparkSQLColdDef

import com.sksamuel.elastic4s.ElasticDsl._

import java.sql.{Timestamp, Date}

import scala.util.Try

class ElasticTypesIT extends ElasticWithSharedContext with SharedXDContextTypesTest {

  override val emptyTypesSetError: String = "Type test entries should have been already inserted"

  override def saveTypesData: Int = {
    val res = client.get.execute {
      index into Index / TypesType fields (
        "int" -> 1,
        "bigint" -> BigInt(2147483647),
        "long" -> 2147483647,
        //"string" -> "string",
        "boolean" -> true,
        "double" -> 3.3,
        "float" -> 3.3,
        "decimalint" -> BigDecimal(42.0),
        "decimallong" -> BigDecimal(42.0),
        "decimaldouble" -> BigDecimal(42.42),
        "decimalfloat" -> BigDecimal(42.42),
        //"date" -> new Date(1000000000),
        "timestamp" -> new Timestamp(1000000000),
        "tinyint" -> 42.toByte,
        "smallint" -> 42.toShort,
        "binary" -> Array(0xBA,0xDD,0xFF,0x00,0xDD)//,
        /*"arrayint" -> Array(1,1,2,3,5,8,13),
        "arraystring" -> Array("hello","world"),
        "mapintint" -> Map(1 -> 2, 2 -> 1),
        "mapstringint" -> Map("one" -> 1, "two" -> 2),
        "mapstringstring" -> Map("os" -> "Linux", "api" -> "posix"),
        "struct" -> Map("field1" -> 1, "field2" -> 2),
        "arraystruct" -> Array(
          Map("field1" -> 1, "field2" -> 2),
          Map("field1" -> 3, "field2" -> 4)
        ),
        "arraystructwithdate" -> Array(
          Map("field1" -> new Date(900000000), "field2" -> 1),
          Map("field1" -> new Date(1000000000), "field2" -> 2)
        ),
        "structofstruct" -> Map(
          "field1" -> new Date(900000000),
          "field2" -> 1,
          "field3" -> Map(
            "structField1" -> "hello world",
            "structField1" -> 42
          )
        ),
        "mapstruct" -> Map(
          "0001" -> Map(
            "structField1" -> new Date(900000000),
            "structField1" -> 42
          )
        )*/
      )
    }
    Try(res.await).map(_ => 1).getOrElse(0)
  }


  override def typeMapping(): MappingDefinition = {
    Type as (
      "int" typed IntegerType,
      "long" typed LongType,
      //"string" typed StringType,
      "boolean" typed BooleanType,
      "double" typed DoubleType,
      "float" typed FloatType,
      //"date" typed DateType,
      "timestamp" typed DateType,
      "tinyint" typed ByteType,
      //"smallint" typed ShortType,
      "binary" typed BinaryType//,
      //"arrayint" typed Array(1,1,2,3,5,8,13),
      //"arraystring" typed Array("hello","world"),
      //"mapintint" typed Map(1 -> 2, 2 -> 1),
      //"mapstringint" typed Map("one" -> 1, "two" -> 2),
      //"mapstringstring" typed Map("os" -> "Linux", "api" -> "posix"),
      )
  }

  override def dataTypesSparkOptions: Map[String, String] = defaultOptions + ("resource" -> s"$Index/$TypesType")

  override def sparkAdditionalKeyColumns: Seq[SparkSQLColdDef] = Seq()

  override protected def typesSet: Seq[SparkSQLColdDef] = super.typesSet filterNot {
    case SparkSQLColdDef(colname, _, _) =>
      Set(
        "struct", "arraystruct", "arraystructwithdate", "structofstruct", "mapstruct",
        "decimalint", "decimallong", "decimaldouble", "decimalfloat",
        "arrayint", "arraystring", "mapintint", "mapstringint", "mapstringstring",
        "string", "bigint", "float", "timestamp", "int", "byte", "binary", "tinyint", "smallint", "date"
      ) contains colname
  }

  doTypesTest("The elasticsearch connector")

}
