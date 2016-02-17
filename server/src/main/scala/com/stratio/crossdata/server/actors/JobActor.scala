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

import java.util.concurrent.Callable

import akka.actor.{Props, ActorRef, Actor}
import com.stratio.crossdata.common.{CancelQueryExecution, SQLCommand}
import com.stratio.crossdata.common.result.{ErrorResult, SuccessfulQueryResult}
import com.stratio.crossdata.server.actors.JobActor.Commands.{CancelJob, GetJobStatus}
import com.stratio.crossdata.server.actors.JobActor.Events.{JobFailed, JobCompleted}
import com.stratio.crossdata.server.actors.JobActor.{Task, JobStatus}
import org.apache.spark.sql.crossdata.{XDContext, XDDataFrame}

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

object AbruptlyCancellableFuture {
  def apply[T](job: => T)(implicit ec: ExecutionContext): AbruptlyCancellableFuture[T] = new
      AbruptlyCancellableFuture[T](job)
}

class AbruptlyCancellableFuture[T] private (job: => T)(implicit ec: ExecutionContext) extends Future[T] {

  private val p = Promise[T]()
  private val f = p.future

  def kill: Unit = synchronized {
    jobThread foreach (_.interrupt)
  }

  override def onComplete[U](func: (Try[T]) => U)(implicit executor: ExecutionContext): Unit = f.onComplete(func)(executor)
  override def isCompleted: Boolean = f.isCompleted
  override def value: Option[Try[T]] = f.value
  override def result(atMost: Duration)(implicit permit: CanAwait): T = f.result(atMost)
  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = f.ready(atMost).asInstanceOf[this.type]
}

object JobActor {

  type RunningJobType = AbruptlyCancellableFuture[Unit]

  trait JobStatus
  object JobStatus {
    case object Starting extends JobStatus
    case class Running(job: RunningJobType) extends JobStatus
    case object Failed extends JobStatus
    case object Cancelled extends JobStatus
    case object Expired extends JobStatus
    case object Completed extends JobStatus
  }

  trait JobEvent

  private object InternalEvents {
    case class JobStarted(job: RunningJobType) extends JobEvent
    case object JobExpired extends JobEvent
  }

  object Events {
    case object JobCompleted extends JobEvent
    case class JobFailed(err: Throwable) extends JobEvent
  }

  object Commands {
    trait JobCommand
    case object GetJobStatus extends JobCommand
    //case object StartJob extends JobCommand
    case object CancelJob extends JobCommand
  }

  case class Task(xdContext: XDContext, command: SQLCommand, timeout: Duration)

  def props(xDContext: XDContext, command: SQLCommand, requester: ActorRef, timeout: Duration): Props =
    Props(new JobActor(requester, Task(xDContext, command, timeout)))

}

class JobActor private(val requester: ActorRef, val task: Task) extends Actor {

  import JobActor.JobStatus._
  import JobActor.InternalEvents._

  override def preStart(): Unit = {
    super.preStart()

    import scala.concurrent.ExecutionContext.Implicits.global

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
    case CancelJob =>
      Some(status) collect  {
        case Running(job) => job.kill
        case Starting => ()
      } foreach { _ => context.become(receive(Cancelled)) }

    // State transitions
    case JobStarted(job) if status == Starting =>
      context.become(receive(Running(job)))
    case event @ JobFailed(e) if sender == self =>
      Some(status) collect {
        case Starting =>
        case Running(_) =>
      } foreach { _ =>
        context.become(receive(Failed))
        context.parent ! event
        requester ! ErrorResult(task.command.queryId, e.getMessage, Some(new Exception(e.getMessage)))
        throw e //Let It Crash: It'll be managed by its supervisor
      }
    case JobCompleted if sender == self =>
      Some(status) collect {
        case Running(_) =>
      } foreach { _ =>
        context.become(receive(Completed))
        context.parent ! JobCompleted
      }
  }

  def taskExecution(task: Task)(implicit executionContext: ExecutionContext): AbruptlyCancellableFuture[Unit] = {
    val jb = AbruptlyCancellableFuture[Unit] {
      for(i <- 1 to 1800) {
        if(i == 5) context.parent ! CancelQueryExecution(task.command.queryId)
        println(s"---> Task execution in progress It $i")
        Thread.sleep(2000)
      }
    }
    self ! JobStarted(jb)
    jb
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
