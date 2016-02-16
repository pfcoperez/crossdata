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

package com.stratio.crossdata.common

import java.util.UUID

import org.apache.spark.sql.Row

import scala.concurrent.duration.Duration

//TODO: Remove `retrieveColumnNames` when a better alternative to PR#257 has been found
case class SQLCommand(query: String, queryId: UUID = UUID.randomUUID(),
                      flattenResult: Boolean = false,
                      timeout: Duration = Duration.Inf)

trait ControlCommand
case class CancelQueryExecution(queryId: UUID) extends ControlCommand


trait SQLResult extends Serializable {
  val queryId: UUID
  def resultSet: Array[Row]
  def hasError: Boolean
}

