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
package com.stratio.crossdata.server.actors

import java.util.UUID

import akka.actor.SupervisorStrategy.{Stop, Restart}
import akka.actor._
import akka.cluster.Cluster
import com.stratio.crossdata.common.{CancelQueryExecution, SQLCommand}
import com.stratio.crossdata.server.actors.JobActor.Commands.CancelJob
import com.stratio.crossdata.server.actors.JobActor.Events.{JobCompleted, JobFailed}
import com.stratio.crossdata.server.actors.ServerActor.JobId
import com.stratio.crossdata.server.config.ServerConfig
import org.apache.log4j.Logger
import org.apache.spark.sql.crossdata.XDContext

object ServerActor {
  def props(cluster: Cluster, xdContext: XDContext): Props = Props(new ServerActor(cluster, xdContext))

  case class JobId(requester: ActorRef, queryId: UUID)

}

class ServerActor(cluster: Cluster, xdContext: XDContext) extends Actor with ServerConfig {

  override lazy val logger = Logger.getLogger(classOf[ServerActor])

  override def receive: Actor.Receive = receive(Map.empty)

  private def receive(jobsById: Map[JobId, ActorRef]): Receive = {
    // Commands
    case sqlCommand @ SQLCommand(query, _, withColnames, timeout) =>
      logger.debug(s"Query received ${sqlCommand.queryId}: ${sqlCommand.query}. Actor ${self.path.toStringWithoutAddress}")
      val jobActor = context.actorOf(JobActor.props(xdContext, sqlCommand, sender(), timeout))
      context.become(receive(jobsById + (JobId(/*sender()*/ jobActor, sqlCommand.queryId) -> jobActor)))
      //jobActor ! StartJob
    case cqCommand @ CancelQueryExecution(queryId) =>
      val jid = JobId(sender(), queryId)
      jobsById.get(jid) foreach { job =>
        job ! CancelJob
        //context.become(receive(jobsById - jid))
      }
    // Events
    case JobFailed(e) =>
      logger.error(e.getMessage)
      //context.stop(sender())
    case JobCompleted =>
      //TODO: This could be changed so done works could be inquired about their state
      //context.stop(sender())
    case any =>
      logger.error(s"Something is going wrong! Unknown message: $any")
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(retryNoAttempts, retryCountWindow) {
    case _ => Restart //Crashed job gets restarted
  }
}
