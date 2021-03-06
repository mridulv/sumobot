/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.sumobot.plugins.pagerduty

import net.liftweb.json._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import scala.collection.mutable.ArrayBuffer

class PagerDutySchedulesManager(settings: PagerDutySettings) {
  private[this] val perPage = 100

  def getAllOnCalls: Option[Seq[PagerDutyOnCall]] = {
    val client = new DefaultHttpClient()
    try {
      var total = Integer.MAX_VALUE
      var page = 0
      var offset = 0
      val onCallsList = ArrayBuffer[PagerDutyOnCall]()
      while (page * perPage < total) {
        val url = s"${settings.url}/oncalls?offset=$offset&limit=$perPage&total=true"
        val getReq = new HttpGet(url)

        getReq.addHeader("Accept", "application/vnd.pagerduty+json;version=2")
        getReq.addHeader("Authorization", s"Token token=${settings.token}")

        val response = client.execute(getReq)
        val entity = response.getEntity


        if (entity != null) {
          val inputStream = entity.getContent
          val str = Some(scala.io.Source.fromInputStream(inputStream).mkString).getOrElse {
            return None
          }

          val json = JsonParser.parse(str)

          val jsonOnCalls = json \\ "oncalls"
          val JInt(jsonTotal) = json \\ "total"
          total = jsonTotal.intValue()
          page += 1
          offset = page * perPage

          implicit val formats = DefaultFormats.withHints(ShortTypeHints(List(classOf[PagerDutyOnCallUser], classOf[PagerDutyEscalationPolicy])))
          val oncalls: List[PagerDutyOnCall] = jsonOnCalls.children.map(_.extract[PagerDutyOnCall])

          onCallsList ++= oncalls
        }

      }
      Some(onCallsList.toList)
    } catch {
      case e: Exception =>
        println(e)
        None
    } finally {
      client.getConnectionManager.shutdown()
    }
  }
}
