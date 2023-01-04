package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
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

class DamageableWeaponTurretDamageTest extends ActorTest {
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
private val turret = new TurretDeployable(GlobalDefinitions.portable_manned_turret_tr) //2
  turret.Actor = system.actorOf(Props(classOf[TurretControl], turret), "turret-control")
  turret.Zone = zone
  turret.Position = Vector3(1, 0, 0)
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Spawn()
  player1.Position = Vector3(2, 2, 2)
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref
private val player2 =
    Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=4
  player2.Spawn()
private val player2Probe = TestProbe()
  player2.Actor = player2Probe.ref
  guid.register(turret, 2)
  guid.register(player1, 3)
  guid.register(player2, 4)
  turret.Seats(0).mount(player2)
  player2.VehicleSeated = turret.GUID

private val weapon     = Tool(GlobalDefinitions.suppressor)
private val projectile = weapon.Projectile
private val pSource    = PlayerSource(player1)
private val resolved = DamageInteraction(
    SourceEntry(turret),
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
      turret.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "DamageableWeaponTurret" should {
    "handle damage" in {
      assert(turret.Health == turret.Definition.DefaultHealth)

      turret.Actor ! Vitality.Damage(applyDamageTo)
      val msg12 = vehicleProbe.receiveOne(500 milliseconds)
      val msg3  = activityProbe.receiveOne(500 milliseconds)
      val msg4  = avatarProbe.receiveOne(500 milliseconds)
      assert(
        msg12 match {
          case VehicleServiceMessage(
                "test",
                VehicleAction.PlanetsideAttribute(PlanetSideGUID(0), PlanetSideGUID(2), 0, _)
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg3 match {
          case activity: Zone.HotSpot.Activity =>
            activity.attacker == pSource &&
              activity.defender == SourceEntry(turret) &&
              activity.location == Vector3(1, 0, 0)
          case _ => false
        }
      )
      assert(
        msg4 match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.SendResponse(Service.defaultPlayerGUID, DamageWithPositionMessage(_, Vector3(2, 2, 2)))
              ) =>
            true
          case _ => false
        }
      )
      assert(turret.Health < turret.Definition.DefaultHealth)
    }
  }
}
