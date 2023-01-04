package net.psforever.login

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.io._

class TcpListener[T <: Actor](actorClass: Class[T], nextActorName: String, listenAddress: InetAddress, port: Int)
    extends Actor {
  private val log = org.log4s.getLogger(self.path.name)

  override def supervisorStrategy =
    OneForOneStrategy() {
      case _ => Stop
    }

  import context.system

  IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(listenAddress, port))

private var sessionId           = 0L
private var bytesRecevied       = 0L
private var bytesSent           = 0L
private var nextActor: ActorRef = ActorRef.noSender

  def receive = {
    case Tcp.Bound(local) =>
      log.debug(s"Now listening on TCP:$local")

      context.become(ready(sender()))
    case Tcp.CommandFailed(Tcp.Bind(_, address, _, _, _)) =>
      log.error("Failed to bind to the network interface: " + address)
      context.system.terminate()
    case default =>
      log.error(s"Unexpected message $default")
  }

  def ready(socket: ActorRef): Receive = {
    case Tcp.Connected(remote, local) =>
      val connection = sender()
      val session    = sessionId
      val handler    = context.actorOf(Props(actorClass, remote, connection), nextActorName + session)
      connection ! Tcp.Register(handler)
      sessionId += 1
    case Tcp.Unbind  => socket ! Tcp.Unbind
    case Tcp.Unbound => context.stop(self)
    case default     => log.error(s"Unhandled message: $default")
  }
}
