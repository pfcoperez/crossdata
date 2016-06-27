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
package org.apache.spark.sql.crossdata.session

import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.plans.logical.LocalRelation
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.crossdata.catalog.CatalogChain
import org.apache.spark.sql.crossdata.catalog.XDCatalog.CrossdataTable
import org.apache.spark.sql.crossdata.catalog.interfaces.{XDPersistentCatalog, XDTemporaryCatalog}
import org.apache.spark.sql.crossdata.catalog.temporary.{HashmapCatalog, HazelcastCatalog}
import org.apache.spark.sql.crossdata.test.SharedXDContextTest
import org.junit.runner.RunWith
import org.scalatest.Entry
import org.scalatest.junit.JUnitRunner

import scala.util.{Success, Try}

@RunWith(classOf[JUnitRunner])
class HazelcastSessionProviderSpec extends SharedXDContextTest {

  val SparkSqlConfigString = "config.spark.sql.inMemoryColumnarStorage.batchSize=5000"


  "HazelcastSessionProvider" should "provides new sessions whose properties are initialized properly" in {

    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.parseString(SparkSqlConfigString))

    val session = createNewSession(hazelcastSessionProvider)

    session.conf.settings should contain(Entry("spark.sql.inMemoryColumnarStorage.batchSize", "5000"))

    val tempCatalogs = tempCatalogsFromSession(session)

    tempCatalogs should have length 2
    tempCatalogs.head shouldBe a[HashmapCatalog]
    tempCatalogs(1) shouldBe a[HazelcastCatalog]

    hazelcastSessionProvider.close()
  }


  it should "provides a common persistent catalog and isolated catalogs" ignore {
    // TODO we should share the persistentCatalog

    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.empty())

    val (sessionTempCatalogs, sessionPersCatalogs) = {
      val session = createNewSession(hazelcastSessionProvider)
      (tempCatalogsFromSession(session), persistentCatalogsFromSession(session))
    }
    val (session2TempCatalogs, session2PersCatalogs) = {
      val session = createNewSession(hazelcastSessionProvider)
      (tempCatalogsFromSession(session), persistentCatalogsFromSession(session))
    }

    Seq(sessionTempCatalogs, session2TempCatalogs) foreach (_ should have length 2)

    sessionTempCatalogs.head should not be theSameInstanceAs(session2TempCatalogs.head)
    sessionTempCatalogs(1) should not be theSameInstanceAs(session2TempCatalogs(1))

    Seq(sessionPersCatalogs, session2PersCatalogs) foreach (_ should have length 1)
    sessionPersCatalogs.head should be theSameInstanceAs session2PersCatalogs.head

    hazelcastSessionProvider.close()
  }

  it should "allow to lookup an existing session" in {

    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.empty())
    val sessionId = UUID.randomUUID()
    val tableIdent = TableIdentifier("tab")

    val session = createNewSession(hazelcastSessionProvider, sessionId)

    session.catalog.registerTable(tableIdent, LocalRelation(), Some(CrossdataTable("tab", None, None, "fakedatasource")))

    hazelcastSessionProvider.session(sessionId) should matchPattern {
      case Success(s: XDSession) if Try(s.catalog.lookupRelation(tableIdent)).isSuccess =>
    }

    hazelcastSessionProvider.close()
  }


  it should "fail when trying to lookup a non-existing session" in {

    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.empty())

    hazelcastSessionProvider.session(UUID.randomUUID()).isFailure shouldBe true

    hazelcastSessionProvider.close()
  }



  it should "remove the session metadata when closing an open session" in {
    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.empty())
    val sessionId = UUID.randomUUID()

    val session = hazelcastSessionProvider.newSession(sessionId)

    hazelcastSessionProvider.closeSession(sessionId)

    hazelcastSessionProvider.session(sessionId).isFailure shouldBe true

    hazelcastSessionProvider.close()
  }

  it should "fail when trying to close a non-existing session" in {

    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.empty())

    val session = hazelcastSessionProvider.newSession(UUID.randomUUID())

    hazelcastSessionProvider.closeSession(UUID.randomUUID()).isFailure shouldBe true
    
    hazelcastSessionProvider.close()
  }



  it should "close the hazelcast instance when closing" in {
    val hazelcastSessionProvider = new HazelcastSessionProvider(xdContext.sc, ConfigFactory.empty())
    val sessionID = UUID.randomUUID()

    hazelcastSessionProvider.newSession(sessionID)
    hazelcastSessionProvider.close()

    a [RuntimeException] shouldBe thrownBy (hazelcastSessionProvider.session(sessionID))
    
  }

  private def tempCatalogsFromSession(session: XDSession): Seq[XDTemporaryCatalog] = {
    session.catalog shouldBe a[CatalogChain]
    session.catalog.asInstanceOf[CatalogChain].temporaryCatalogs
  }

  private def persistentCatalogsFromSession(session: XDSession): Seq[XDPersistentCatalog] = {
    session.catalog shouldBe a[CatalogChain]
    session.catalog.asInstanceOf[CatalogChain].persistentCatalogs
  }

  private def createNewSession(hazelcastSessionProvider: HazelcastSessionProvider, uuid: UUID = UUID.randomUUID()): XDSession = {
    val optSession = hazelcastSessionProvider.newSession(uuid).toOption
    optSession shouldBe defined
    optSession.get
  }

}
