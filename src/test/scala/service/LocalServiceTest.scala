// Copyright (c) 2017 PSForever
package service

import akka.actor.Props
import akka.testkit.TestProbe
import base.{ActorTest, FreedContextActorTest}
import net.psforever.objects.{GlobalDefinitions, SensorDeployable, Vehicle}
import net.psforever.objects.serverobject.PlanetSideServerObject
import net.psforever.objects.serverobject.terminals.{ProximityTerminal, Terminal}
import net.psforever.objects.vehicles.control.VehicleControl
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.packet.game._
import net.psforever.types.{PlanetSideEmpire, PlanetSideGUID, Vector3}
import net.psforever.services.{Service, ServiceManager}
import net.psforever.services.local._

import scala.concurrent.duration._

class LocalService1Test extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "construct" in {
      system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      assert(true)
    }
  }
}

class LocalService2Test extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "subscribe" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      assert(true)
    }
  }
}

class LocalService3Test extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "subscribe to a specific channel" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! Service.Leave()
      assert(true)
    }
  }
}

class LocalService4Test extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "subscribe" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! Service.LeaveAll()
      assert(true)
    }
  }
}

class LocalService5Test extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass an unhandled message" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! "hello"
      expectNoMessage()
    }
  }
}

class DeployableMapIconTest extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass DeployableMapIcon" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage(
        "test",
        LocalAction.DeployableMapIcon(
          PlanetSideGUID(10),
          DeploymentAction.Build,
          DeployableInfo(PlanetSideGUID(40), DeployableIcon.Boomer, Vector3(1, 2, 3), PlanetSideGUID(11))
        )
      )
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(10),
          LocalResponse.DeployableMapIcon(
            DeploymentAction.Build,
            DeployableInfo(PlanetSideGUID(40), DeployableIcon.Boomer, Vector3(1, 2, 3), PlanetSideGUID(11))
          )
        )
      )
    }
  }
}

class DoorClosesTest extends FreedContextActorTest {
private val probe = new TestProbe(system)
private val zone = new Zone("test", new ZoneMap("test-map"), 0) {
    override def SetupNumberPools() : Unit = { }
  }
  zone.init(context)
  expectNoMessage(500 milliseconds)

  "LocalService" should {
    "pass DoorCloses" in {
      zone.LocalEvents.tell(Service.Join("test"), probe.ref)
      zone.LocalEvents ! LocalServiceMessage("test", LocalAction.DoorCloses(PlanetSideGUID(10), PlanetSideGUID(40)))
      probe.expectMsg(LocalServiceResponse("/test/Local", PlanetSideGUID(10), LocalResponse.DoorCloses(PlanetSideGUID(40))))
    }
  }
}

class HackClearTest extends ActorTest {
  ServiceManager.boot(system)
private val obj = new PlanetSideServerObject() {
    def Faction    = PlanetSideEmpire.NEUTRAL
    def Definition = null
    GUID = PlanetSideGUID(40)
  }

  "LocalService" should {
    "pass HackClear" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage("test", LocalAction.HackClear(PlanetSideGUID(10), obj, 0L, 1000L))
      expectMsg(
        LocalServiceResponse("/test/Local", PlanetSideGUID(10), LocalResponse.SendHackMessageHackCleared(PlanetSideGUID(40), 0L, 1000L))
      )
    }
  }
}

class ProximityTerminalEffectOnTest extends ActorTest {
  ServiceManager.boot(system)
private val service  = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
private val terminal = new ProximityTerminal(GlobalDefinitions.medical_terminal)
  terminal.GUID = PlanetSideGUID(1)

  "LocalService" should {
    "pass ProximityTerminalEffect (true)" in {
      service ! Service.Join("nowhere")
      service ! Terminal.StartProximityEffect(terminal)
      expectMsg(
        LocalServiceResponse(
          "/nowhere/Local",
          PlanetSideGUID(0),
          LocalResponse.ProximityTerminalEffect(PlanetSideGUID(1), true)
        )
      )
    }
  }
}

class ProximityTerminalEffectOffTest extends ActorTest {
  ServiceManager.boot(system)
private val service  = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
private val terminal = new ProximityTerminal(GlobalDefinitions.medical_terminal)
  terminal.GUID = PlanetSideGUID(1)

  "LocalService" should {
    "pass ProximityTerminalEffect (false)" in {
      service ! Service.Join("nowhere")
      service ! Terminal.StopProximityEffect(terminal)
      expectMsg(
        LocalServiceResponse(
          "/nowhere/Local",
          PlanetSideGUID(0),
          LocalResponse.ProximityTerminalEffect(PlanetSideGUID(1), false)
        )
      )
    }
  }
}

class RouterTelepadTransportTest extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass RouterTelepadTransport" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage(
        "test",
        LocalAction.RouterTelepadTransport(
          PlanetSideGUID(10),
          PlanetSideGUID(11),
          PlanetSideGUID(12),
          PlanetSideGUID(13)
        )
      )
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(10),
          LocalResponse.RouterTelepadTransport(PlanetSideGUID(11), PlanetSideGUID(12), PlanetSideGUID(13))
        )
      )
    }
  }
}

class SetEmpireTest extends ActorTest {
  ServiceManager.boot(system)
private val obj = new SensorDeployable(GlobalDefinitions.motionalarmsensor)

  "LocalService" should {
    "pass SetEmpire" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage("test", LocalAction.SetEmpire(PlanetSideGUID(10), PlanetSideEmpire.TR))
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(0),
          LocalResponse.SetEmpire(PlanetSideGUID(10), PlanetSideEmpire.TR)
        )
      )
    }
  }
}

class ToggleTeleportSystemTest extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass ToggleTeleportSystem" in {
      val router = Vehicle(GlobalDefinitions.router)
      router.Actor = system.actorOf(Props(classOf[VehicleControl], router), "test-router")
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage("test", LocalAction.ToggleTeleportSystem(PlanetSideGUID(10), router, None))
      expectMsg(
        LocalServiceResponse("/test/Local", PlanetSideGUID(10), LocalResponse.ToggleTeleportSystem(router, None))
      )
    }
  }
}

class TriggerEffectTest extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass TriggerEffect (1)" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage("test", LocalAction.TriggerEffect(PlanetSideGUID(10), "on", PlanetSideGUID(40)))
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(10),
          LocalResponse.TriggerEffect(PlanetSideGUID(40), "on", None, None)
        )
      )
    }
  }
}

class TriggerEffectInfoTest extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass TriggerEffect (2)" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage(
        "test",
        LocalAction.TriggerEffectInfo(PlanetSideGUID(10), "on", PlanetSideGUID(40), true, 1000)
      )
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(10),
          LocalResponse.TriggerEffect(PlanetSideGUID(40), "on", Some(TriggeredEffect(true, 1000)), None)
        )
      )
    }
  }
}

class TriggerEffectLocationTest extends ActorTest {
  ServiceManager.boot(system)

  "LocalService" should {
    "pass TriggerEffect (3)" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage(
        "test",
        LocalAction.TriggerEffectLocation(
          PlanetSideGUID(10),
          "spawn_object_failed_effect",
          Vector3(1.1f, 2.2f, 3.3f),
          Vector3(4.4f, 5.5f, 6.6f)
        )
      )
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(10),
          LocalResponse.TriggerEffect(
            PlanetSideGUID(0),
            "spawn_object_failed_effect",
            None,
            Some(TriggeredEffectLocation(Vector3(1.1f, 2.2f, 3.3f), Vector3(4.4f, 5.5f, 6.6f)))
          )
        )
      )
    }
  }
}

class TriggerSoundTest extends ActorTest {
  import net.psforever.packet.game.TriggeredSound
  ServiceManager.boot(system)

  "LocalService" should {
    "pass TriggerSound" in {
      val service = system.actorOf(Props(classOf[LocalService], Zone.Nowhere), "l_service")
      service ! Service.Join("test")
      service ! LocalServiceMessage(
        "test",
        LocalAction.TriggerSound(PlanetSideGUID(10), TriggeredSound.LockedOut, Vector3(1.1f, 2.2f, 3.3f), 0, 0.75f)
      )
      expectMsg(
        LocalServiceResponse(
          "/test/Local",
          PlanetSideGUID(10),
          LocalResponse.TriggerSound(TriggeredSound.LockedOut, Vector3(1.1f, 2.2f, 3.3f), 0, 0.75f)
        )
      )
    }
  }
}

object LocalServiceTest {
  //decoy
}
