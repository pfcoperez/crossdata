/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.stratio.crossdata.server.actors

import akka.actor._
import com.stratio.crossdata.common.connector.ConnectorClusterConfig
import com.stratio.crossdata.common.data
import com.stratio.crossdata.common.data.ConnectorName
import com.stratio.crossdata.common.exceptions.ExecutionException
import com.stratio.crossdata.common.executionplan._
import com.stratio.crossdata.common.result._
import com.stratio.crossdata.common.statements.structures.selectors.SelectorHelper
import com.stratio.crossdata.common.utils.StringUtils
import com.stratio.crossdata.communication.{Connect, ConnectToConnector, DisconnectFromConnector, _}
import com.stratio.crossdata.core.coordinator.Coordinator
import com.stratio.crossdata.core.execution.{ExecutionInfo, ExecutionManager, ExecutionManagerException}
import com.stratio.crossdata.core.metadata.MetadataManager
import com.stratio.crossdata.core.query.PlannedQuery
import com.stratio.crossdata.common.logicalplan.PartialResults

object CoordinatorActor {

  /**
   * Token attached to query identifiers when the query is part of a trigger execution workflow.
   */
  val TriggerToken = "_T"

  def props(connectorMgr: ActorRef, coordinator: Coordinator): Props = Props(new CoordinatorActor
  (connectorMgr, coordinator))
}

class CoordinatorActor(connectorMgr: ActorRef, coordinator: Coordinator) extends Actor with ActorLogging {

  log.info("Lifting coordinator actor")

  def receive = {

    case plannedQuery: PlannedQuery => {
      val workflow = plannedQuery.getExecutionWorkflow()
      log.debug("Workflow for " + workflow.getActorRef)

      workflow match {
        case workflow1: MetadataWorkflow => {
          val executionInfo = new ExecutionInfo
          executionInfo.setSender(StringUtils.getAkkaActorRefUri(sender))
          val queryId = plannedQuery.getQueryId
          executionInfo.setWorkflow(workflow1)
          if (workflow1.getActorRef() != null && workflow1.getActorRef().length() > 0) {
            val actorRef = context.actorSelection(workflow1.getActorRef())
            executionInfo.setQueryStatus(QueryStatus.IN_PROGRESS)
            executionInfo.setPersistOnSuccess(true)
            ExecutionManager.MANAGER.createEntry(queryId, executionInfo, true)
            log.info("ActorRef: " + actorRef.toString())

            actorRef.asInstanceOf[ActorSelection] ! workflow1.createMetadataOperationMessage()

          } else if (workflow1.getExecutionType == ExecutionType.CREATE_CATALOG || workflow1
            .getExecutionType == ExecutionType.CREATE_TABLE_AND_CATALOG) {
            coordinator.persistCreateCatalog(workflow1.getCatalogMetadata)

            executionInfo.setQueryStatus(QueryStatus.PLANNED)
            ExecutionManager.MANAGER.createEntry(workflow1.getCatalogMetadata.getName.toString, queryId, true)
            ExecutionManager.MANAGER.createEntry(queryId, executionInfo, true)
            val result = MetadataResult.createSuccessMetadataResult(MetadataResult.OPERATION_CREATE_CATALOG)
            result.setQueryId(queryId)
            sender ! result
          }
        }

        case workflow1: StorageWorkflow => {
          log.debug("CoordinatorActor: StorageWorkflow received")
          val queryId = plannedQuery.getQueryId
          val executionInfo = new ExecutionInfo
          executionInfo.setSender(StringUtils.getAkkaActorRefUri(sender))
          executionInfo.setWorkflow(workflow1)
          executionInfo.setQueryStatus(QueryStatus.IN_PROGRESS)
          ExecutionManager.MANAGER.createEntry(queryId, executionInfo)

          val actorRef=context.actorSelection(workflow1.getActorRef())
          actorRef ! workflow1.getStorageOperation()
        }

        case workflow1: ManagementWorkflow => {

          log.info("ManagementWorkflow received")

          val queryId = plannedQuery.getQueryId
          if (workflow1.getExecutionType == ExecutionType.ATTACH_CONNECTOR) {

            val credentials = null
            val managementOperation = workflow1.createManagementOperationMessage()
            val attachConnectorOperation = managementOperation.asInstanceOf[AttachConnector]
            val connectorClusterConfig = new ConnectorClusterConfig(
              attachConnectorOperation.targetCluster, SelectorHelper.convertSelectorMapToStringMap
                (MetadataManager.MANAGER.getCluster(attachConnectorOperation.targetCluster).getOptions))
            val connectorSelection = context.actorSelection(StringUtils.getAkkaActorRefUri(workflow1.getActorRef()))
            connectorSelection ! new Connect(credentials, connectorClusterConfig)

            val executionInfo = new ExecutionInfo()
            executionInfo.setQueryStatus(QueryStatus.IN_PROGRESS)
            executionInfo.setSender(StringUtils.getAkkaActorRefUri(sender))
            executionInfo.setWorkflow(workflow1)
            ExecutionManager.MANAGER.createEntry(queryId, executionInfo, true)

          }
          sender ! coordinator.executeManagementOperation(workflow1.createManagementOperationMessage())
        }

        case workflow1: QueryWorkflow => {
          log.info("\n\nCoordinatorActor: QueryWorkflow received")
          val queryId = plannedQuery.getQueryId
          val executionInfo = new ExecutionInfo
          executionInfo.setSender(StringUtils.getAkkaActorRefUri(sender))
          executionInfo.setWorkflow(workflow1)

          log.info("\n\nCoordinate workflow: " + workflow1.toString)
          executionInfo.setQueryStatus(QueryStatus.IN_PROGRESS)
          if(ResultType.RESULTS.equals(workflow1.getResultType)) {
            ExecutionManager.MANAGER.createEntry(queryId, executionInfo)
            val actorRef = StringUtils.getAkkaActorRefUri(workflow1.getActorRef())
            val actorSelection=context.actorSelection(actorRef)
            actorSelection.asInstanceOf[ActorSelection] ! workflow1.getExecuteOperation(queryId)
            log.info("\nmessage sent to" + actorRef.toString())
          }else if(ResultType.TRIGGER_EXECUTION.equals(workflow1.getResultType)){
            //Register the top level workflow
            ExecutionManager.MANAGER.createEntry(queryId+CoordinatorActor.TriggerToken, executionInfo)

            //Register the result workflow
            val nextExecutionInfo = new ExecutionInfo
            nextExecutionInfo.setSender(StringUtils.getAkkaActorRefUri(sender))
            nextExecutionInfo.setWorkflow(workflow1.getNextExecutionWorkflow)

            ExecutionManager.MANAGER.createEntry(queryId, nextExecutionInfo)

            val actorRef = StringUtils.getAkkaActorRefUri(workflow1.getActorRef())
            val actorSelection=context.actorSelection(actorRef)
            actorSelection.asInstanceOf[ActorSelection] ! workflow1.getExecuteOperation(queryId+CoordinatorActor
              .TriggerToken)
            log.info("\nmessage sent to" + actorRef.toString())

          }
        }
        case _ => {
          log.error("non recognized workflow")
        }
      }
    }

    case result: ConnectResult => {
      log.info("Connect result received from " + sender + " with SessionId = " + result.getSessionId);
    }

    case result: Result => {
      val queryId = result.getQueryId
      log.info("Receiving result from " + sender + " with queryId = " + queryId + " result: " + result)
      try {
        val executionInfo = ExecutionManager.MANAGER.getValue(queryId)
        //TODO Add two methods to StringUtils to retrieve AkkaActorRefUri tokening with # for connectors,
        // and $ for clients
        val target = executionInfo.asInstanceOf[ExecutionInfo].getSender.toString
          .replace("Actor[", "").replace("]", "").split("#")(0)
        //val clientActor = context.actorSelection(StringUtils.getAkkaActorRefUri(executionInfo
        //  .asInstanceOf[ExecutionInfo].getSender))
        val clientActor = context.actorSelection(target)
        log.info("Send result to: " + clientActor.toString())

        if (executionInfo.asInstanceOf[ExecutionInfo].isPersistOnSuccess) {
          coordinator.persist(executionInfo.asInstanceOf[ExecutionInfo].getWorkflow.asInstanceOf[MetadataWorkflow])
        }
        if(executionInfo.asInstanceOf[ExecutionInfo].isRemoveOnSuccess) {
          ExecutionManager.MANAGER.deleteEntry(queryId)
        }

        if(queryId.endsWith(CoordinatorActor.TriggerToken)){
          val triggerQueryId = queryId.substring(0, queryId.length-CoordinatorActor.TriggerToken.length)
          log.info("Retrieving Triggering queryId: " + triggerQueryId);
          val executionInfo = ExecutionManager.MANAGER.getValue(triggerQueryId).asInstanceOf[ExecutionInfo]
          val partialResults = result.asInstanceOf[QueryResult].getResultSet
          executionInfo.getWorkflow.getTriggerStep.asInstanceOf[PartialResults].setResults(partialResults)
          val actorRef = StringUtils.getAkkaActorRefUri(executionInfo.getWorkflow.getActorRef())
          val actorSelection=context.actorSelection(actorRef)
          actorSelection.asInstanceOf[ActorSelection] ! executionInfo.getWorkflow.asInstanceOf[QueryWorkflow]
            .getExecuteOperation(queryId+CoordinatorActor.TriggerToken)
        }else {
          clientActor ! result
        }
      } catch {
        case ex: ExecutionManagerException => {
          log.error(ex.getStackTraceString + "cannot access queryId actorRef associated value")
        }
      }
    }

    case ctc: ConnectToConnector =>
      MetadataManager.MANAGER.setConnectorStatus(new ConnectorName(ctc.msg), data.Status.ONLINE)
      log.info("Connected to connector")

    case dfc: DisconnectFromConnector =>
      MetadataManager.MANAGER.setConnectorStatus(new ConnectorName(dfc.msg), data.Status.OFFLINE)
      log.info("Disconnected from connector")

    case _ => {
      sender ! new ExecutionException("Non recognized workflow")
    }

  }

}