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
package akka.cluster.crossdata.builders

import akka.cluster.{Member, MemberStatus, UniqueAddress}

object MemberBuilder {

  def extractUpNumber(member: Member): Int = member.upNumber

  def apply(uniqueAddress: UniqueAddress, upNumber: Int, status: MemberStatus, roles: Set[String]): Member =
    new Member(uniqueAddress, upNumber, status, roles)

}
