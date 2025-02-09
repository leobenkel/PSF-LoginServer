package net.psforever.actors.net

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{Actor, ActorRef, Cancellable, MDCContextAware, typed}
import com.github.t3hnar.bcrypt._
import net.psforever.objects.{Account, Default}
import net.psforever.packet.PlanetSideGamePacket
import net.psforever.packet.game.LoginRespMessage.{LoginError, StationError, StationSubscriptionStatus}
import net.psforever.packet.game._
import net.psforever.persistence
import net.psforever.services.ServiceManager
import net.psforever.services.ServiceManager.Lookup
import net.psforever.services.account.{ReceiveIPAddress, StoreAccountData}
import net.psforever.types.PlanetSideEmpire
import net.psforever.util.Config
import net.psforever.util.Database._
import org.log4s.Logger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex
import scala.util.{Failure, Success}
/*
object LoginActor {
  def apply(
      middlewareActor: typed.ActorRef[MiddlewareActor.Command],
      uuid: String
  ): Behavior[Command] =
    Behaviors.setup(context => new LoginActor(context, middlewareActor, uuid).start())

  sealed trait Command

}

class LoginActor(
    middlewareActor: typed.ActorRef[MiddlewareActor.Command],
    uuid: String
) {

  def start(): Unit = {
    Behaviors.receiveMessagePartial {}
  }
}

 */

class LoginActor(middlewareActor: typed.ActorRef[MiddlewareActor.Command], connectionId: String, sessionId: Long)
    extends Actor
    with MDCContextAware {
  private[this] val log: Logger = org.log4s.getLogger

  import scala.concurrent.ExecutionContext.Implicits.global

  private case class UpdateServerList()

  private val usernameRegex: Regex = """[A-Za-z0-9]{3,}""".r

  private var accountIntermediary: ActorRef = ActorRef.noSender

  private var updateServerListTask: Cancellable = Default.Cancellable

  private var ipAddress: String         = ""
  private var hostName: String          = ""
  private var canonicalHostName: String = ""
  private var port: Int                 = 0

  private val serverName: String = Config.app.world.serverName
  private val publicAddress: InetSocketAddress =
    new InetSocketAddress(InetAddress.getByName(Config.app.public), Config.app.world.port)

  // Reference: https://stackoverflow.com/a/50470009
  private val numBcryptPasses: Int = 10

  ServiceManager.serviceManager ! Lookup("accountIntermediary")

  override def postStop(): Unit = {
    if (updateServerListTask != null)
      updateServerListTask.cancel()
  }

  def receive: Receive = {
    case ServiceManager.LookupResult("accountIntermediary", endpoint) =>
      accountIntermediary = endpoint
    case ReceiveIPAddress(address) =>
      ipAddress = address.Address
      hostName = address.HostName
      canonicalHostName = address.CanonicalHostName
      port = address.Port
    case UpdateServerList() =>
      updateServerList()
    case packet: PlanetSideGamePacket =>
      handleGamePkt(packet)
    case default => failWithError(s"Invalid packet class received: $default")
  }

  private def handleGamePkt: PlanetSideGamePacket => Unit = {
    case LoginMessage(majorVersion, minorVersion, buildDate, username, password, token, revision) =>
      // TODO: prevent multiple LoginMessages from being processed in a row!! We need a state machine

      val clientVersion = s"Client Version: $majorVersion.$minorVersion.$revision, $buildDate"

      if (token.isDefined)
        log.trace(s"New login UN:$username Token:${token.get}. $clientVersion")
      else {
        log.trace(s"New login UN:$username. $clientVersion")
      }

      accountLogin(username, password.getOrElse(""))

    case ConnectToWorldRequestMessage(name, _, _, _, _, _, _) =>
      log.info(s"Connect to world request for '$name'")
      val response = ConnectToWorldMessage(serverName, publicAddress.getAddress.getHostAddress, publicAddress.getPort)
      middlewareActor ! MiddlewareActor.Send(response)
      middlewareActor ! MiddlewareActor.Close()

    case pkt =>
      log.warn(s"Unhandled GamePacket $pkt")
  }

  private def accountLogin(username: String, password: String): Unit = {
    import ctx._
    val newToken = this.generateToken()
    val result = for {
      // backwards compatibility: prefer exact match first, then try lowercase
      accountsExact <- ctx.run(query[persistence.Account].filter(_.username == lift(username)))
      accountsLower <- accountsExact.headOption match {
        case None =>
          ctx.run(query[persistence.Account].filter(_.username.toLowerCase == lift(username).toLowerCase))
        case Some(_) =>
          Future.successful(Seq())
      }
      accountOption <- accountsExact.headOption orElse accountsLower.headOption match {
        case Some(account) => Future.successful(Some(account))
        case None =>
          if (Config.app.login.createMissingAccounts) {
            val passhash: String = password.bcrypt(numBcryptPasses)
            ctx.run(
              query[persistence.Account]
                .insert(_.passhash -> lift(passhash), _.username -> lift(username))
                .returningGenerated(_.id)
            ) flatMap { id => ctx.run(query[persistence.Account].filter(_.id == lift(id))) } map { accounts =>
              Some(accounts.head)
            }
          } else {
            loginFailureResponse(username, newToken)
            Future.successful(None)
          }
      }
      login <- accountOption match {
        case Some(account) =>
          (account.inactive, password.isBcrypted(account.passhash)) match {
            case (false, true) =>
              accountIntermediary ! StoreAccountData(newToken, Account(account.id, account.username, account.gm))
              val future = ctx.run(
                query[persistence.Login].insert(
                  _.accountId         -> lift(account.id),
                  _.ipAddress         -> lift(ipAddress),
                  _.canonicalHostname -> lift(canonicalHostName),
                  _.hostname          -> lift(hostName),
                  _.port              -> lift(port)
                )
              )
              loginSuccessfulResponse(username, newToken)
              updateServerListTask =
                context.system.scheduler.scheduleWithFixedDelay(0 seconds, 5 seconds, self, UpdateServerList())
              future
            case (_, false) =>
              loginPwdFailureResponse(username, newToken)
              Future.successful(None)
            case (true, _) =>
              loginAccountFailureResponse(username, newToken)
              Future.successful(None)
          }
        case None => Future.successful(None)
      }
    } yield login

    result.onComplete {
      case Success(_) =>
      case Failure(e) => log.error(e.getMessage)
    }
  }

  private def loginSuccessfulResponse(username: String, newToken: String): Unit = {
    middlewareActor ! MiddlewareActor.Send(
      LoginRespMessage(
        newToken,
        LoginError.Success,
        StationError.AccountActive,
        StationSubscriptionStatus.Active,
        0,
        username,
        10001
      )
    )
  }

  private def loginPwdFailureResponse(username: String, newToken: String): Unit = {
    log.warn(s"Failed login to account $username")
    middlewareActor ! MiddlewareActor.Send(
      LoginRespMessage(
        newToken,
        LoginError.BadUsernameOrPassword,
        StationError.AccountActive,
        StationSubscriptionStatus.Active,
        685276011,
        username,
        10001
      )
    )
  }

  private def loginFailureResponse(username: String, newToken: String): Unit = {
    log.warn("DB problem")
    middlewareActor ! MiddlewareActor.Send(
      LoginRespMessage(
        newToken,
        LoginError.unk1,
        StationError.AccountActive,
        StationSubscriptionStatus.Active,
        685276011,
        username,
        10001
      )
    )
  }

  private def loginAccountFailureResponse(username: String, newToken: String): Unit = {
    log.warn(s"Account $username inactive")
    middlewareActor ! MiddlewareActor.Send(
      LoginRespMessage(
        newToken,
        LoginError.BadUsernameOrPassword,
        StationError.AccountClosed,
        StationSubscriptionStatus.Active,
        685276011,
        username,
        10001
      )
    )
  }

  private def generateToken(): String = {
    val r  = new scala.util.Random
    val sb = new StringBuilder
    for (_ <- 1 to 31) {
      sb.append(r.nextPrintableChar())
    }
    sb.toString
  }

  private def updateServerList(): Unit = {
    middlewareActor ! MiddlewareActor.Send(
      VNLWorldStatusMessage(
        "Welcome to PlanetSide! ",
        Vector(
          WorldInformation(
            serverName,
            WorldStatus.Up,
            Config.app.world.serverType,
            Vector(WorldConnectionInfo(publicAddress)),
            PlanetSideEmpire.VS
          )
        )
      )
    )
  }

  private def failWithError(error: String): Unit = {
    log.error(error)
    middlewareActor ! MiddlewareActor.Close()
  }

}
