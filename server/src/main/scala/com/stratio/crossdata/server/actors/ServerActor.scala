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

import akka.cluster.ClusterEvent.InitialStateAsSnapshot
import akka.contrib.pattern.DistributedPubSubExtension
import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.cluster.Cluster

import akka.contrib.pattern.DistributedPubSubMediator.{Publish, SubscribeAck, Subscribe}
import akka.remote.DisassociatedEvent
import com.stratio.crossdata.common.security.Session
import com.stratio.crossdata.common._
import com.stratio.crossdata.server.actors.JobActor.Commands.{StartJob, CancelJob}
import com.stratio.crossdata.common.{SecureCommand, SQLCommand}
import com.stratio.crossdata.server.actors.JobActor.Events.{JobCompleted, JobFailed}
import com.stratio.crossdata.server.config.ServerConfig
import org.apache.log4j.Logger
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.crossdata.XDContext

import scala.concurrent.duration.FiniteDuration


object ServerActor {

  val managementTopic: String = "jobsManagement"

  def props(cluster: Cluster, xdContext: XDContext): Props = Props(new ServerActor(cluster, xdContext))

  protected case class JobId(requester: ActorRef, queryId: UUID)

  protected case class ManagementEnvelope(command: ControlCommand, source: ActorRef)

  private object ManagementMessages {

    trait BroadcastMessage { val broadcaster: ActorRef }

    object BroadcastMessage {
      def unapply(broadcaster: ActorRef): BroadcastMessage = new BroadcastMessage {
        override val broadcaster: ActorRef = broadcaster
      }
    }

    case class DelegateCommand(scommand: SecureCommand, broadcaster: ActorRef) extends BroadcastMessage

    case class FinishJob(jobActor: ActorRef)

    trait CatalogCacheUpdate { val tableName: String }
    case class CacheTableAddition(tableName: String, logicalPlan: LogicalPlan,
                                  broadcaster: ActorRef) extends CatalogCacheUpdate with BroadcastMessage
    case class CacheTableDeletion(tableName: String,
                                  broadcaster: ActorRef) extends CatalogCacheUpdate with BroadcastMessage

  }

  case class State(jobsById: Map[JobId, ActorRef])

}

class ServerActor(cluster: Cluster, xdContext: XDContext) extends Actor with ServerConfig {

  import ServerActor._
  import ServerActor.ManagementMessages._

  override lazy val logger = Logger.getLogger(classOf[ServerActor])

  lazy val mediator = DistributedPubSubExtension(context.system).mediator

  override def preStart(): Unit = {
    super.preStart()

    // Subscribe to the management distributed topic
    mediator ! Subscribe(managementTopic, self)

    // Subscribe to disassociation events in the clustergetDbTableName(processTableIdentifier(tableIdentifier))
    cluster.subscribe(self, InitialStateAsSnapshot, classOf[DisassociatedEvent])

    // Subscribe to table cache changes

    xdContext.catalog.subscribeToChanges(addedTableEntry)
    xdContext.catalog.subscribeToChanges(removedTableEntry)

  }

  override def postStop(): Unit = {
    super.postStop()
    cluster.unsubscribe(self)

    xdContext.catalog.cancelSubscription(addedTableEntry)
    xdContext.catalog.cancelSubscription(removedTableEntry)
  }

  private val addedTableEntry: ((String, LogicalPlan)) => Unit = {
    (name: String, plan: LogicalPlan) => mediator ! Publish(managementTopic, CacheTableAddition(name, plan, self))
  }

  private val removedTableEntry: String => Unit = {
    name => mediator ! Publish(managementTopic, CacheTableDeletion(name, self))
  }

  // Actor behaviours

  override def receive: Actor.Receive = initial

  private val initial: Receive = {
    case SubscribeAck(Subscribe(managementTopic, None, self)) =>
      context.become(ready(State(Map.empty)))
  }

  /**
    * If a `cmd` is passed to this method is because it has already been checked that this server can run it.
    *
    * @param cmd
    * @param st
    */
  private def executeAccepted(cmd: SecureCommand)(st: State): Unit = cmd match {
    case SecureCommand(sqlCommand @ SQLCommand(query, requestId, queryId, withColnames, timeout), session @ Session(id, requester)) =>
      logger.debug(s"Query received $queryId: $query. Actor ${self.path.toStringWithoutAddress}")
      logger.debug(s"Session identifier $session")
      val jobActor = context.actorOf(JobActor.props(xdContext, sqlCommand, sender(), timeout))
      jobActor ! StartJob
      context.become(ready(st.copy(jobsById = st.jobsById + (JobId(requester, sqlCommand.queryId) -> jobActor))))

    case SecureCommand(cc @ CancelQueryExecution(queryId), session @ Session(id, requester)) =>
      st.jobsById.get(JobId(requester, queryId)).get ! CancelJob
  }

  // Receive functions:

  // Broadcast messages treatment
  def broadcastRequestsRec(st: State): Receive = {
    case BroadcastMessage(broadcaster) if broadcaster == self => //Discards from this server broadcast messafes
    case DelegateCommand(cmd, broadcaster) if broadcaster != self =>
      cmd match { // Inner pattern matching for future delegated command validations
        case sc @ SecureCommand(CancelQueryExecution(queryId), Session(_, requester)) =>
          st.jobsById.get(JobId(requester, queryId)) foreach(_ => executeAccepted(sc)(st))
        /* If it doesn't validate it won't be re-broadcast since the source server already distributed it to all
            servers through the topic. */
      }
  }

  // Commands reception: Checks whether the command can be run at this Server passing it to the execution method if so
  def commandMessagesRec(st: State): Receive = {

    case sc @ SecureCommand(_: SQLCommand, _) => executeAccepted(sc)(st)

    case sc @ SecureCommand(cc: ControlCommand, session @ Session(id, requester)) =>
      st.jobsById.get(JobId(requester, cc.requestId)) map { _ =>
        executeAccepted(sc)(st) // Command validated to be executed by this server.
      } getOrElse {
        // If it can't run here it should be executed somewhere else
        mediator ! Publish(managementTopic, DelegateCommand(sc, self))
      }

  }

  // Manages events from the `JobActor` which runs the task
  def eventsRec(st: State): Receive = {

    case JobFailed(e) =>
      logger.error(e.getMessage)
      sentenceToDeath(sender())

    case JobCompleted =>
      sentenceToDeath(sender())

    case FinishJob(who) =>
      context.become(ready(st.copy(jobsById = st.jobsById.filterNot(_._2 == who))))
      context.children.find(_ == who).foreach(gracefullyKill)
  }

  // Function composition to build the finally applied receive-function
  private def ready(st: State): Receive =
    broadcastRequestsRec(st) orElse
      commandMessagesRec(st) orElse
      eventsRec(st) orElse {

    case DisassociatedEvent(_, remoteAddress, _) =>
      /*
        Uses `DisassociatedEvent` to get notified of an association loss.
        An akka association consist on the connection of a JVM to another to build remote connections upon.
        Thus, the reception of this event message means all remote clients within the addressed jvm are down.

         More info at: http://doc.akka.io/docs/akka/snapshot/scala/remoting.html
       */
      val newjobsmap = st.jobsById filter {
        case (JobId(requester, _), job) if requester.path.address == remoteAddress =>
          gracefullyKill(job) // WARNING! Side-effect
          false
        case _ => true
      }
      context.become(ready(st.copy(jobsById = newjobsmap)))

    case any =>
      logger.error(s"Something is going wrong! Unknown message: $any")
  }

  private def sentenceToDeath(victim: ActorRef): Unit = completedJobTTL match {
    case finite: FiniteDuration =>
      context.system.scheduler.scheduleOnce(finite, self, FinishJob(victim))(context.dispatcher)
    case _ => // Reprieve by infinite limit
  }

  private def gracefullyKill(victim: ActorRef): Unit = {
    victim ! CancelJob
    victim ! PoisonPill
  }

  //TODO: Use number of tries and timeout configuration parameters
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(retryNoAttempts, retryCountWindow) {
    case _ => Restart //Crashed job gets restarted (or not, depending on `retryNoAttempts` and `retryCountWindow`)
  }

}
