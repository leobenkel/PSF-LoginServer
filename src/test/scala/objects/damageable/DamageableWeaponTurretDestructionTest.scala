package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.serverobject.turret._
import net.psforever.objects.vital.Vitality
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.services.support.SupportActor
import net.psforever.services.vehicle.VehicleServiceMessage
import net.psforever.services.vehicle.support.TurretUpgrader
import net.psforever.types._

import scala.concurrent.duration._

class DamageableWeaponTurretDestructionTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(10))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val building      = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
private val activityProbe = TestProbe()
private val avatarProbe   = TestProbe()
private val vehicleProbe  = TestProbe()
private val buildingProbe = TestProbe()
  zone.Activity = activityProbe.ref
  zone.AvatarEvents = avatarProbe.ref
  zone.VehicleEvents = vehicleProbe.ref
  building.Actor = buildingProbe.ref

private val turret = new FacilityTurret(GlobalDefinitions.manned_turret) //2, 5, 6
  turret.Actor = system.actorOf(Props(classOf[FacilityTurretControl], turret), "turret-control")
  turret.Zone = zone
  turret.Position = Vector3(1, 0, 0)
private val turretWeapon = turret.Weapons.values.head.Equipment.get.asInstanceOf[Tool]

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Spawn()
  player1.Position = Vector3(2, 2, 2)
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref
private val player2 =
    Player(Avatar(1, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=4
  player2.Spawn()
private val player2Probe = TestProbe()
  player2.Actor = player2Probe.ref

  guid.register(building, 1)
  guid.register(turret, 2)
  guid.register(player1, 3)
  guid.register(player2, 4)
  guid.register(turretWeapon, 5)
  guid.register(turretWeapon.AmmoSlot.Box, 6)
  turret.Seats(0).mount(player2)
  player2.VehicleSeated = turret.GUID
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = turret

private val turretSource = SourceEntry(turret)
private val weaponA      = Tool(GlobalDefinitions.jammer_grenade)
private val projectileA  = weaponA.Projectile
private val resolvedA = DamageInteraction(
    turretSource,
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectileA,
        weaponA.Definition,
        weaponA.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      turret.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageToA = resolvedA.calculate()

private val weaponB     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectileB = weaponB.Projectile
private val resolvedB = DamageInteraction(
    turretSource,
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectileB,
        weaponB.Definition,
        weaponB.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      turret.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageToB = resolvedB.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "DamageableWeaponTurret" should {
    "handle being destroyed gracefully" in {
      turret.Health = turret.Definition.DamageDestroysAt + 1 //initial state manip
      turret.Upgrade = TurretUpgrade.AVCombo                 //initial state manip; act like having being upgraded properly
      assert(turret.Health > turret.Definition.DamageDestroysAt)
      assert(!turret.Jammed)
      assert(!turret.Destroyed)

      turret.Actor ! Vitality.Damage(applyDamageToA) //also test destruction while jammered
      vehicleProbe.receiveN(2, 1000 milliseconds)    //flush jammered messages (see above)
      assert(turret.Health > turret.Definition.DamageDestroysAt)
      assert(turret.Jammed)
      assert(!turret.Destroyed)

      turret.Actor ! Vitality.Damage(applyDamageToB) //destroy
      val msg12_4 = avatarProbe.receiveN(3, 500 milliseconds)
      player1Probe.expectNoMessage(500 milliseconds)
      val msg3  = player2Probe.receiveOne(200 milliseconds)
      val msg56 = vehicleProbe.receiveN(2, 200 milliseconds)
      msg12_4.head match {
        case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => ;
        case _ =>
          assert(false, s"DamageableWeaponTurretDestructionTest-1: ${msg12_4.head}")
      }
      msg12_4(1) match {
        case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(2), _, _, Vector3(1, 0, 0))) => ;
        case _ =>
          assert(false, s"DamageableWeaponTurretDestructionTest-2: ${msg12_4(1)}")
      }
      msg3 match {
        case Player.Die(_) => true
        case _ =>
          assert(false, s"DamageableWeaponTurretDestructionTest-3: player not dead - $msg3")
      }
      msg12_4(2) match {
        case AvatarServiceMessage("test", AvatarAction.ObjectDelete(PlanetSideGUID(0), PlanetSideGUID(5), _)) => ;
        case _ =>
          assert(false, s"DamageableWeaponTurretDestructionTest-4: ${msg12_4(2)}")
      }
      msg56.head match {
        case VehicleServiceMessage.TurretUpgrade(SupportActor.ClearSpecific(List(t), _)) if turret eq t => ;
        case _ =>
          assert(false, s"DamageableWeaponTurretDestructionTest-5: ${msg56.head}")
      }
      msg56(1) match {
        case VehicleServiceMessage.TurretUpgrade(TurretUpgrader.AddTask(t, _, TurretUpgrade.None, _))
            if t eq turret => ;
          true
        case _ =>
          assert(false, s"DamageableWeaponTurretDestructionTest-6: ${msg56(1)}")
      }
      assert(turret.Health <= turret.Definition.DamageDestroysAt)
      assert(!turret.Jammed)
      assert(turret.Destroyed)
    }
  }
}
