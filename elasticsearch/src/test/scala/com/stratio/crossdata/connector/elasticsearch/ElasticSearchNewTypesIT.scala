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
package com.stratio.crossdata.connector.elasticsearch

import org.apache.spark.sql.crossdata.ExecutionType._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ElasticSearchNewTypesIT extends ElasticDataTypesWithSharedContext {

  // TODO replace old test when values are checked
  "A ElasticSearchQueryProcessor " should "Return types in correct format" in {
    assumeEnvironmentIsUpAndRunning

    val dataframe = sql(s"SELECT * FROM $Type where id = 1")

    Seq( Spark, Native).foreach{ executionType =>

      val rows = dataframe.collect(executionType)
      //Expectations
      rows should have length 1
      val row = rows(0)

      dataTest.zipWithIndex.foreach{ case (colMetadata, idx) =>
        val field = row.get(idx)
        colMetadata.typeValidation(field)
      }
    }

    // TODO test values

  }
}
