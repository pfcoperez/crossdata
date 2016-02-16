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

import akka.actor.{Props, ActorRef, Actor}
import com.stratio.crossdata.common.{CancelQueryExecution, SQLCommand}
import com.stratio.crossdata.common.result.{ErrorResult, SuccessfulQueryResult}
import com.stratio.crossdata.server.actors.JobActor.Commands.GetJobStatus
import com.stratio.crossdata.server.actors.JobActor.Events.{JobFailed, JobCompleted}
import com.stratio.crossdata.server.actors.JobActor.Task
import org.apache.spark.sql.crossdata.{XDContext, XDDataFrame}

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.duration.Duration
import scala.util.{Success, Failure}

object JobActor {

  object JobStatus extends Enumeration {
    type JobStatus = Value
    val Starting, Running, Failed, Expired, Completed = Value
  }

  trait JobEvent


  private object InternalEvents {
    case object JobStarted extends JobEvent
    case class JobExpired extends JobEvent
  }

  object Events {
    case object JobCompleted extends JobEvent
    case class JobFailed(err: Throwable) extends JobEvent
  }

  object Commands {
    trait JobCommand
    case object GetJobStatus extends JobCommand
    case object StartJob extends JobCommand
  }

  private[JobActor] case class Task(xdContext: XDContext, command: SQLCommand, timeout: Duration)

  def props(xDContext: XDContext, command: SQLCommand, requester: ActorRef, timeout: Duration): Props =
    Props(new JobActor(requester, Task(xDContext, command, timeout)))

}

class JobActor private(val requester: ActorRef, val task: Task) extends Actor {

  import JobActor.JobStatus._
  import JobActor.InternalEvents._

  override def preStart(): Unit = {
    super.preStart()

    import scala.concurrent.ExecutionContext.Implicits.global
    //import context.dispatcher

    taskExecution(task) onComplete {
      case Failure(e) =>
        self ! JobFailed(e)
      case Success(_) =>
        self ! JobCompleted
    }
  }

  override def receive: Receive = receive(Starting)

  private def receive(status: JobStatus): Receive = {
    // External commands
    case GetJobStatus =>
      sender ! status
    // State transitions
    case JobStarted if status == Starting =>
      context.become(receive(Running))
    case event @ JobFailed(e) if sender == self && (Seq(Starting, Running) contains status) =>
      context.become(receive(Failed))
      context.parent ! event
      requester ! ErrorResult(task.command.queryId, e.getMessage, Some(new Exception(e.getMessage)))
      throw e //Let It Crash: It'll be managed by its supervisor
    case JobCompleted if sender == self && status == Running =>
      context.become(receive(Completed))
      context.parent ! JobCompleted
  }

  def taskExecution(task: Task)(implicit executionContext: ExecutionContext): Future[Unit] = Future[Unit] {
    for(i <- 1 to 1800) {
      if(i == 5) context.parent ! CancelQueryExecution(task.command.queryId)
      println(s"---> Task execution in progress It $i")
      Thread.sleep(2000)
    }
  }

  /*def taskExecution(task: Task): Future[Unit] = Future {
    import task._

    val df = xdContext.sql(command.query)
    self ! JobStarted
    val rows = if(command.flattenResult)
    //TODO: Replace this cast by an implicit conversion
      df.asInstanceOf[XDDataFrame].flattenedCollect()
    else df.collect()

    // Sends the result to the requester
    requester ! SuccessfulQueryResult(command.queryId, rows, df.schema)
  }*/

}
