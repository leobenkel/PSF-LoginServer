package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.FreedContextActorTest
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
import net.psforever.services.Service
import net.psforever.services.vehicle.{VehicleAction, VehicleServiceMessage}
import net.psforever.types._

import scala.concurrent.duration._

class DamageableVehicleJammeringMountedTest extends FreedContextActorTest {
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

private val atv = Vehicle(GlobalDefinitions.quadassault) //guid=1
  atv.Actor = system.actorOf(Props(classOf[VehicleControl], atv), "atv-control")
  atv.Position = Vector3(1, 0, 0)
private val atvWeapon = atv.Weapons(1).Equipment.get.asInstanceOf[Tool] //guid=4 & 5

private val lodestar = Vehicle(GlobalDefinitions.lodestar) //guid=6
  lodestar.Position = Vector3(1, 0, 0)

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=7
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref
private val player2 =
    Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=8
  player2.Spawn()
private val player2Probe = TestProbe()
  player2.Actor = player2Probe.ref
private val player3 =
    Player(Avatar(0, "TestCharacter3", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=9
  player3.Spawn()
private val player3Probe = TestProbe()
  player3.Actor = player3Probe.ref

  guid.register(atv, 1)
  guid.register(atvWeapon, 2)
  guid.register(atvWeapon.AmmoSlot.Box, 3)
  guid.register(lodestar, 4)
  guid.register(lodestar.Utilities(2)(), 5)
  guid.register(lodestar.Utilities(3)(), 6)
  guid.register(lodestar.Utilities(4)(), 7)
  guid.register(lodestar.Utilities(5)(), 8)
  guid.register(lodestar.Utilities(6)(), 9)
  guid.register(lodestar.Utilities(7)(), 10)
  guid.register(player1, 11)
  guid.register(player2, 12)
  guid.register(player3, 13)

  lodestar.Definition.Initialize(lodestar, context)
  atv.Zone = zone
  lodestar.Zone = zone
  atv.Seats(0).mount(player2)
  player2.VehicleSeated = atv.GUID
  lodestar.Seats(0).mount(player3)
  player3.VehicleSeated = lodestar.GUID
  lodestar.CargoHolds(1).mount(atv)
  atv.MountedIn = lodestar.GUID

private val vehicleSource = SourceEntry(lodestar)
private val weapon        = Tool(GlobalDefinitions.jammer_grenade)
private val projectile    = weapon.Projectile
private val resolved = DamageInteraction(
    vehicleSource,
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
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

  "handle jammering with mounted vehicles" in {
    assert(lodestar.Health == lodestar.Definition.DefaultHealth)
    assert(!lodestar.Jammed)
    assert(atv.Health == atv.Definition.DefaultHealth)
    assert(!atv.Jammed)

    lodestar.Actor ! Vitality.Damage(applyDamageTo)
    val msg12 = vehicleProbe.receiveOne(500 milliseconds)
    avatarProbe.expectNoMessage(500 milliseconds)
    player1Probe.expectNoMessage(200 milliseconds)
    player2Probe.expectNoMessage(200 milliseconds)
    player3Probe.expectNoMessage(200 milliseconds)
    assert(
      msg12 match {
        case VehicleServiceMessage(
              "test",
              VehicleAction.PlanetsideAttribute(Service.defaultPlayerGUID, PlanetSideGUID(4), 27, 1)
            ) =>
          true
        case _ => false
      }
    )
    assert(lodestar.Health == lodestar.Definition.DefaultHealth)
    assert(lodestar.Jammed)
    assert(atv.Health == atv.Definition.DefaultHealth)
    assert(!atv.Jammed)
  }
}
