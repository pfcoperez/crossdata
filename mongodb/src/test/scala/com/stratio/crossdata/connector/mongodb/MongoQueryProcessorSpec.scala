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
package com.stratio.crossdata.connector.mongodb

import java.util
import java.util.regex.Pattern

import com.mongodb.{DBObject, QueryOperators}
import com.stratio.crossdata.test.BaseXDTest
import com.stratio.datasource.mongodb.config.{MongodbConfig, MongodbConfigBuilder}
import org.apache.spark.sql.sources._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MongoQueryProcessorSpec extends BaseXDTest {

  val Keyspace = "keyspace"
  val Table = "table"
  val TableQN = s"$Keyspace.$Table"
  val ColumnAge = "age"
  val ColumnId = "id"
  val ObjectId = "_id"
  val Limit = 12000
  val ValueAge = 25
  val ValueAge2 = 30
  val ValueId = "00123"
  val config = MongodbConfigBuilder()
    .set(MongodbConfig.Host, List("host:port"))
    .set(MongodbConfig.Database, "db")
    .set(MongodbConfig.Collection, "collection")
    .build()

  "A MongoQueryProcessor" should "build a query requiring some columns" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId, ColumnAge), Array(), config)
    val columnsSet = requiredColumns.keySet

    filters.keySet should have size 0

    columnsSet should have size 3
    columnsSet should contain allOf (ColumnId, ColumnAge)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ColumnAge) should be (1)
    requiredColumns.get(ObjectId) should be (0)
  }

  it should "build a query with two equal filters" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(EqualTo(ColumnAge, ValueAge), EqualTo(ColumnId, ValueId)), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain(ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 2
    filters.get(ColumnId) should be (ValueId.toString)
    filters.get(ColumnAge) should be (ValueAge)

  }

  it should "build a query with an IN clause" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(In(ColumnAge, Array(ValueAge, ValueAge2))), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain (ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(ColumnAge) shouldBe a [DBObject]

    val inListValues = filters.get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.IN)
    inListValues shouldBe a [Seq[_]]
    inListValues.asInstanceOf[Array[Any]] should have size 2
    inListValues.asInstanceOf[Array[Any]] should contain allOf (ValueAge, ValueAge2)

  }

  it should "build a query with a LT clause" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(LessThan(ColumnAge, ValueAge)), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain(ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(ColumnAge) shouldBe a [DBObject]

    filters.get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.LT) shouldBe (ValueAge)

  }

  it should "build a query with a LTE clause" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(LessThanOrEqual(ColumnAge, ValueAge)), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain(ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(ColumnAge) shouldBe a [DBObject]
    filters.get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.LTE) shouldBe (ValueAge)

  }


  it should "build a query with a GTE clause" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(GreaterThanOrEqual(ColumnAge, ValueAge)), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain(ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(ColumnAge) shouldBe a [DBObject]

    filters.get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.GTE) shouldBe (ValueAge)

  }

  it should "build a query with an IS NOT NULL clause" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(IsNotNull(ColumnAge)), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain(ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(ColumnAge) shouldBe a [DBObject]

    filters.get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.NE) shouldBe (null)

  }

  it should "build a query with an AND(v1 > x <= v2)" in {
    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(And(GreaterThan(ColumnAge, ValueAge), LessThanOrEqual(ColumnAge, ValueAge2))), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain (ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(QueryOperators.AND) shouldBe a [util.ArrayList[_]]
    val subfilters = filters.get(QueryOperators.AND).asInstanceOf[util.ArrayList[DBObject]]

    //filter GT
    subfilters.get(0).get(ColumnAge) shouldBe a [DBObject]
    subfilters.get(0).get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.GT) shouldBe (ValueAge)

    //filter LTE
    subfilters.get(1).get(ColumnAge) shouldBe a [DBObject]
    subfilters.get(1).get(ColumnAge).asInstanceOf[DBObject].get(QueryOperators.LTE) shouldBe (ValueAge2)
  }

  it should "build a query with a REGEX clause " in {

    val (filters, requiredColumns) = MongoQueryProcessor.buildNativeQuery(Array(ColumnId), Array(StringContains(ColumnId, ValueId.toString)), config)
    val filterSet = filters.keySet

    requiredColumns.keySet should contain (ColumnId)
    requiredColumns.get(ColumnId) should be (1)
    requiredColumns.get(ObjectId) should be (0)

    filterSet should have size 1
    filters.get(ColumnId) shouldBe a [Pattern]

    filters.get(ColumnId).asInstanceOf[Pattern].pattern should be (Pattern.compile(s".*${ValueId.toString}.*").pattern)
  }

}
