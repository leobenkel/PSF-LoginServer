package objects.damageable

import akka.actor.Props
import akka.actor.typed.scaladsl.adapter._
import akka.testkit.TestProbe
import base.FreedContextActorTest
import net.psforever.actors.zone.ZoneActor
import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.vehicles.control.VehicleControl
import net.psforever.objects.vital.Vitality
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.packet.game.DamageWithPositionMessage
import net.psforever.services.Service
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.services.vehicle.{VehicleAction, VehicleServiceMessage}
import net.psforever.types._

import scala.concurrent.duration._

class DamageableVehicleDamageMountedTest extends FreedContextActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val activityProbe = TestProbe()
private val avatarProbe   = TestProbe()
private val vehicleProbe  = TestProbe()
  zone.Activity = activityProbe.ref
  zone.AvatarEvents = avatarProbe.ref
  zone.VehicleEvents = vehicleProbe.ref
  zone.actor = new TestProbe(system).ref.toTyped[ZoneActor.Command]

private val lodestar = Vehicle(GlobalDefinitions.lodestar) //guid=1 & 4,5,6,7,8,9
  lodestar.Position = Vector3(1, 0, 0)
private val atv = Vehicle(GlobalDefinitions.quadstealth) //guid=11
  atv.Position = Vector3(1, 0, 0)
  atv.Actor = system.actorOf(Props(classOf[VehicleControl], atv), "atv-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=2
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref
private val player2 =
    Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player2.Spawn()
private val player2Probe = TestProbe()
  player2.Actor = player2Probe.ref
private val player3 =
    Player(Avatar(0, "TestCharacter3", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=10
  player3.Spawn()
private val player3Probe = TestProbe()
  player3.Actor = player3Probe.ref

  guid.register(lodestar, 1)
  guid.register(player1, 2)
  guid.register(player2, 3)
  guid.register(lodestar.Utilities(2)(), 4)
  guid.register(lodestar.Utilities(3)(), 5)
  guid.register(lodestar.Utilities(4)(), 6)
  guid.register(lodestar.Utilities(5)(), 7)
  guid.register(lodestar.Utilities(6)(), 8)
  guid.register(lodestar.Utilities(7)(), 9)
  guid.register(player3, 10)
  guid.register(atv, 11)

  //the lodestar control actor needs to load after the utilities have guid's assigned
  lodestar.Definition.Initialize(lodestar, context)
  lodestar.Zone = zone
  lodestar.Seats(0).mount(player2)
  player2.VehicleSeated = lodestar.GUID
  atv.Zone = zone
  atv.Seats(0).mount(player3)
  player3.VehicleSeated = atv.GUID
  lodestar.CargoHolds(1).mount(atv)
  atv.MountedIn = lodestar.GUID

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val pSource    = PlayerSource(player1)
private val resolved = DamageInteraction(
    SourceEntry(lodestar),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        pSource,
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      lodestar.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "handle damage with mounted vehicles" in {
    lodestar.Shields = 1 //initial state manip
    atv.Shields = 1      //initial state manip
    assert(lodestar.Health == lodestar.Definition.DefaultHealth)
    assert(lodestar.Shields == 1)
    assert(atv.Health == atv.Definition.DefaultHealth)
    assert(atv.Shields == 1)

    lodestar.Actor ! Vitality.Damage(applyDamageTo)
    val msg12 = vehicleProbe.receiveN(2, 500 milliseconds)
    val msg3  = activityProbe.receiveOne(500 milliseconds)
    val msg45 = avatarProbe.receiveN(2, 500 milliseconds)
    msg12.head match {
      case VehicleServiceMessage(_, VehicleAction.PlanetsideAttribute(PlanetSideGUID(0), PlanetSideGUID(1), 68, _)) => ;
      case _                                                                                                        => assert(false)
    }
    msg12(1) match {
      case VehicleServiceMessage(
            "test",
            VehicleAction.PlanetsideAttribute(PlanetSideGUID(0), PlanetSideGUID(1), 0, _)
          ) =>
        ;
      case _ => assert(false)
    }
    msg3 match {
      case activity: Zone.HotSpot.Activity =>
        assert(
          activity.attacker == pSource &&
            activity.defender == SourceEntry(lodestar) &&
            activity.location == Vector3(1, 0, 0)
        )
      case _ => assert(false)
    }
    msg45.head match {
      case AvatarServiceMessage(
            "TestCharacter2",
            AvatarAction.SendResponse(Service.defaultPlayerGUID, DamageWithPositionMessage(400, Vector3(2, 0, 0)))
          ) =>
        ;
      case _ => assert(false)
    }
    msg45(1) match {
      case AvatarServiceMessage(
            "TestCharacter3",
            AvatarAction.SendResponse(Service.defaultPlayerGUID, DamageWithPositionMessage(0, Vector3(2, 0, 0)))
          ) =>
        ;
      case _ => assert(false)
    }
    assert(lodestar.Health < lodestar.Definition.DefaultHealth)
    assert(lodestar.Shields == 0)
    assert(atv.Health == atv.Definition.DefaultHealth)
    assert(atv.Shields == 1)
  }
}
