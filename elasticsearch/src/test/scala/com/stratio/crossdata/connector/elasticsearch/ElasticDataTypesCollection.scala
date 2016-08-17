package com.stratio.crossdata.connector.elasticsearch

import java.util.GregorianCalendar

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.{MappingDefinition, TypedFieldDefinition}
import org.apache.spark.sql.crossdata.test.SharedXDContextTypesTest
import org.apache.spark.sql.crossdata.test.SharedXDContextTypesTest.SparkSQLColDef
import org.joda.time.DateTime

trait ElasticDataTypesCollection extends ElasticWithSharedContext with SharedXDContextTypesTest {

  override val emptyTypesSetError: String = "Type test entries should have been already inserted"

  override def dataTypesSparkOptions: Map[String, String] = Map(
    "resource" -> s"$Index/$Type",
    "es.nodes" -> s"$ElasticHost",
    "es.port" -> s"$ElasticRestPort",
    "es.nativePort" -> s"$ElasticNativePort",
    "es.cluster" -> s"$ElasticClusterName"
  )

  case class ESColumnMetadata(sparkCol: SparkSQLColDef, elasticType: TypedFieldDefinition, data: Any)

  lazy val dataTest = Seq(
    ESColumnMetadata(SparkSQLColDef("id", "INT", _ shouldBe a[java.lang.Integer]), "id" typed IntegerType, 1),
    ESColumnMetadata(SparkSQLColDef("age", "LONG", _ shouldBe a[java.lang.Long]), "age" typed LongType, 1L),
    ESColumnMetadata(SparkSQLColDef("description", "STRING", _ shouldBe a[java.lang.String]), "description" typed StringType, "1"),
    ESColumnMetadata(SparkSQLColDef("name", "STRING", _ shouldBe a[java.lang.String]), "name" typed StringType index NotAnalyzed, "1"),
    ESColumnMetadata(SparkSQLColDef("enrolled", "BOOLEAN", _ shouldBe a[java.lang.Boolean]), "enrolled" typed BooleanType, false),
    //ESColumnMetadata(SparkSQLColDef("birthday", "DATE", _ shouldBe a [java.sql.Date]), "birthday" typed DateType,DateTime.parse(1980 + "-01-01T10:00:00-00:00").toDate),// TODO date should be timestamp (if not supported by elastic natively)
    ESColumnMetadata(SparkSQLColDef("salary", "DOUBLE", _ shouldBe a[java.lang.Double]), "salary" typed DoubleType, 0.15),
    ESColumnMetadata(SparkSQLColDef("timecol", "TIMESTAMP", _ shouldBe a[java.sql.Timestamp]), "timecol" typed DateType,
      new java.sql.Timestamp(new GregorianCalendar(1970, 0, 1, 0, 0, 0).getTimeInMillis)),

    //ESColumnMetadata("float", "FLOAT", "float" typed FloatType, () => 0.15, _ shouldBe a[java.lang.Float]), // TODO float not supported natively
    //ESColumnMetadata("binary", "BINARY", "binary" typed BinaryType, () => Array(Byte.MaxValue, Byte.MinValue), x => x.isInstanceOf[Array[Byte]] shouldBe true) // TODO native and spark ko
    //ESColumnMetadata("tinyint", "TINYINT", "tinyint" typed ByteType, () => Byte.MinValue, _ shouldBe a[java.lang.Byte]), // TODO native ko
    //ESColumnMetadata("smallint", "SMALLINT", "smallint" typed ShortType, () => Short.MaxValue, _ shouldBe a[java.lang.Short]) // TODO native ko
    // TODO study access to multi-field (example name with multiple fields: raw (not analyzed) and name spanish (analyzed with specific analyzer) ?
    //ESColumnMetadata("subdocument", "STRUCT<field1: INT>", "subdocument"  inner ("field1" typed IntegerType), () =>  Map( "field1" -> 15), _ shouldBe a [Row]) // TODO native ko
    // TODO study nested type => to enable flattening
    // TODO complex structures => [[SharedXDContextTypesTest]]

  )


  override protected def typesSet: Seq[SparkSQLColDef] = dataTest map (_.sparkCol)


  abstract override def typeMapping(): MappingDefinition = super.typeMapping() ++

  override def saveTypesData: Int = client.map { dbCLi =>
    dbCLi.execute {
      index into Index / Type fields((dataTest map(entry => (entry.sparkCol.colname, entry.data))): _*)
    }.await
    dbCLi.execute {
      flush index Index
    }.await
  }.map(_ => 1).getOrElse(0)

}
