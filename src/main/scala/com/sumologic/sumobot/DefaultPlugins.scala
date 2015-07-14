package com.sumologic.sumobot

import akka.actor.Props
import com.sumologic.sumobot.plugins.aws.AWSCredentialSource
import com.sumologic.sumobot.plugins.awssupport.AWSSupport
import com.sumologic.sumobot.plugins.beer.Beer
import com.sumologic.sumobot.plugins.conversations.Conversations
import com.sumologic.sumobot.plugins.help.Help
import com.sumologic.sumobot.plugins.jenkins.{Jenkins, JenkinsJobClient}
import com.sumologic.sumobot.plugins.jira.{Jira, JiraClient}
import com.sumologic.sumobot.plugins.pagerduty.{PagerDuty, PagerDutySchedulesManager}
import com.sumologic.sumobot.plugins.upgradetests.UpgradeTestRunner

object DefaultPlugins {
  implicit val system = Main.system

  def setup(): Unit = {

    system.actorOf(Props(classOf[Help]), "help")
    system.actorOf(Props(classOf[Conversations]), "conversations")
    system.actorOf(Props(classOf[Beer]), "beer")

    JenkinsJobClient.createClient("jenkins").foreach {
      jenkinsClient =>
        system.actorOf(props = Jenkins.props("jenkins", jenkinsClient))
    }

    JenkinsJobClient.createClient("hudson").foreach {
      hudsonClient =>
        system.actorOf(props = Jenkins.props("hudson", hudsonClient))
        system.actorOf(Props(classOf[UpgradeTestRunner], hudsonClient), "upgrade-test-runner")
    }

    PagerDutySchedulesManager.createClient().foreach {
      pagerDutySchedulesManager =>
        system.actorOf(Props(classOf[PagerDuty], pagerDutySchedulesManager, None), "pagerduty")
    }

    JiraClient.createClient.foreach {
      jiraClient =>
        system.actorOf(Props(classOf[Jira], jiraClient), "jira")
    }

    val awsCreds = AWSCredentialSource.credentials
    if (awsCreds.nonEmpty) {
      system.actorOf(Props(classOf[AWSSupport], awsCreds), "aws-support")
    }
  }
}
