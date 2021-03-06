/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.discovery.dns

import akka.actor.ActorSystem
import akka.event.Logging
import akka.io.{ Dns, IO }
import akka.pattern.ask

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import akka.discovery._

/**
 * Looks for A records for a given service.
 */
class DnsSimpleServiceDiscovery(system: ActorSystem) extends SimpleServiceDiscovery {
  import SimpleServiceDiscovery._

  private val log = Logging(system, getClass)
  private val dns = IO(Dns)(system)
  import system.dispatcher

  override def lookup(name: String, resolveTimeout: FiniteDuration): Future[Resolved] = {
    def cleanIpString(ipString: String): String =
      if (ipString.startsWith("/")) ipString.tail else ipString

    dns.ask(Dns.Resolve(name))(resolveTimeout) map {
      case resolved: Dns.Resolved =>
        log.info("Resolved Dns.Resolved: {}", resolved)
        val addresses = resolved.ipv4.map { entry ⇒
          val address = cleanIpString(entry.getHostAddress)
          ResolvedTarget(host = address, port = None)
        }
        Resolved(name, addresses)

      case resolved ⇒
        log.warning("Resolved UNEXPECTED (resolving to Nil): {}", resolved.getClass)
        Resolved(name, Nil)
    }
  }
}
