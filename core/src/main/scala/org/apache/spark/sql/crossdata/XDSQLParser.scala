package org.apache.spark.sql.crossdata

import org.apache.spark.sql.SparkSQLParser
import org.apache.spark.sql.catalyst.AbstractSparkSQLParser
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan

class XDSQLParser(val sparkSQLParser: SparkSQLParser) extends AbstractSparkSQLParser {

  override protected def start: Parser[LogicalPlan] = ???

}
