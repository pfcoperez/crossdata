/*
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
package org.apache.spark.sql.crossdata.serializers

import org.apache.spark.sql.crossdata.serializers.XDSerializationTest.TestCase
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types._
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class RowSerializerSpec extends XDSerializationTest[Row] with CrossdataCommonSerializer with Inside {

  lazy val schema = StructType(List(
    StructField("int",IntegerType,true),
    StructField("bigint",LongType,true),
    StructField("long",LongType,true),
    StructField("string",StringType,true),
    StructField("boolean",BooleanType,true),
    StructField("double",DoubleType,true),
    StructField("float",FloatType,true),
    StructField("decimalint",DecimalType(10,0),true),
    StructField("decimallong",DecimalType(10,0),true),
    StructField("decimaldouble",DecimalType(10,0),true),
    StructField("decimalfloat",DecimalType(10,0),true),
    StructField("date", DateType,true),
    StructField("timestamp",TimestampType,true),
    StructField("smallint", ShortType, true),
    StructField("binary",BinaryType,true),
    StructField("arrayint",ArrayType(IntegerType,true),true)/*,
    StructField("arraystring",ArrayType(StringType,true),true) ,
    StructField("mapstringint",MapType(StringType,IntegerType,true),true),
    StructField("mapstringstring",MapType(StringType,StringType,true),true),
    StructField("maptimestampinteger",MapType(TimestampType,IntegerType,true),true),
    StructField("struct",StructType(StructField("field1",IntegerType,true)::StructField("field2",IntegerType,true) ::Nil), true),
    StructField("arraystruct",ArrayType(StructType(StructField("field1",IntegerType,true)::StructField("field2", IntegerType,true)::Nil),true),true),
    StructField("structofstruct",StructType(StructField("field1",TimestampType,true)::StructField("field2", IntegerType, true)::StructField("struct1",StructType(StructField("structField1",StringType,true)::StructField("structField2",IntegerType,true)::Nil),true)::Nil),true)*/
  ))

  lazy val values: Array[Any] =  Array(
    2147483647,
    9223372036854775807L,
    9223372036854775807L,
    "string",
    true,
    3.0,
    3.0F,
    Decimal(12),
    Decimal(22),
    Decimal(32.0),
    Decimal(42.0),
    java.sql.Date.valueOf("2015-11-30"),
    java.sql.Timestamp.valueOf("2015-11-30 10:00:00.0"),
    12.toShort,
    "abcde".getBytes,
    Array[Int](4, 42)/*,
    Array("hello", "world"),
    Map("b" -> 2),
    Map("a" -> "A", "b" -> "B"),
    Map(java.sql.Timestamp.valueOf("2015-11-30 10:00:00.0") -> 25, java.sql.Timestamp.valueOf("2015-11-30 10:00:00.0") -> 12),
    new GenericRowWithSchema(Array(99,98), StructType(StructField("field1", IntegerType)
      ::StructField("field2", IntegerType)::Nil)),
    Array(
      new GenericRowWithSchema(Array(1,2), StructType(StructField("field1", IntegerType)::StructField("field2", IntegerType)::Nil)),
      new GenericRowWithSchema(Array(3,4), StructType(StructField("field1", IntegerType)::StructField("field2", IntegerType)::Nil))
    ),
    new GenericRowWithSchema(
      Array(
        java.sql.Timestamp.valueOf("2015-11-30 10:00:00.0"),
        42,
        new GenericRowWithSchema(
          Array("a glass of wine a day keeps the doctor away", 1138),
          StructType(StructField("structField1",StringType,true)::StructField("structField2",IntegerType,true)::Nil)
        )
      ),
      StructType(
        List(
          StructField("field1",TimestampType,true),
          StructField("field2", IntegerType, true),
          StructField("struct1",StructType(StructField("structField1",StringType,true)::StructField("structField2",IntegerType,true)::Nil),true)
        )
      )
    )*/
  )

  lazy val rowWithNoSchema = Row.fromSeq(values)
  lazy val rowWithSchema = new GenericRowWithSchema(values, schema)


  implicit val formats = json4sJacksonFormats + new RowSerializer(schema)


  override def testCases: Seq[TestCase] = Seq(
    TestCase("marshall & unmarshall a row with no schema", rowWithNoSchema),
    TestCase("marshall & unmarshall a row with schema", rowWithSchema)
  )

  it should " be able to recover Double values when their schema type is misleading" in {

    val schema = StructType(List(StructField("decimaldouble", DecimalType(10,1),true)))
    val row = Row.fromSeq(Array(32.1))

    val formats = json4sJacksonFormats + new RowSerializer(schema)

    val serialized = compact(render(Extraction.decompose(row)(formats)))
    val extracted = parse(serialized, false).extract[Row](formats, implicitly[Manifest[Row]])

    inside(extracted) {
      case r: Row => r.get(0) shouldBe Decimal(32.1)
    }

  }

}