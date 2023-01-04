package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.ActorTest
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
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.types._

import scala.concurrent.duration._

class DamageableVehicleDestroyTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(10))
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
  atv.Actor = system.actorOf(Props(classOf[VehicleControl], atv), "vehicle-control")
  atv.Position = Vector3(1, 0, 0)
private val atvWeapon = atv.Weapons(1).Equipment.get.asInstanceOf[Tool] //guid=4 & 5

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

  guid.register(atv, 1)
  guid.register(player1, 2)
  guid.register(player2, 3)
  guid.register(atvWeapon, 4)
  guid.register(atvWeapon.AmmoSlot.Box, 5)
  atv.Zone = zone
  atv.Seats(0).mount(player2)
  player2.VehicleSeated = atv.GUID

private val weapon        = Tool(GlobalDefinitions.suppressor)
private val projectile    = weapon.Projectile
private val vehicleSource = SourceEntry(atv)
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
      atv.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "DamageableVehicle" should {
    "handle destruction" in {
      atv.Health = atv.Definition.DamageDestroysAt + 1 //initial state manip
      atv.Shields = 1
      assert(atv.Health > atv.Definition.DamageDestroysAt)
      assert(atv.Shields == 1)
      assert(!atv.Destroyed)

      atv.Actor ! Vitality.Damage(applyDamageTo)
      val msg124 = avatarProbe.receiveN(3, 500 milliseconds)
      val msg3   = player2Probe.receiveOne(200 milliseconds)
      assert(
        msg124.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg124(1) match {
          case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(1), _, _, Vector3(1, 0, 0))) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg3 match {
          case Player.Die(_) => true
          case _             => false
        }
      )
      assert(
        msg124(2) match {
          case AvatarServiceMessage("test", AvatarAction.ObjectDelete(PlanetSideGUID(0), PlanetSideGUID(4), _)) => true
          case _                                                                                                => false
        }
      )
      assert(atv.Health <= atv.Definition.DamageDestroysAt)
      assert(atv.Destroyed)
      //
    }
  }
}
