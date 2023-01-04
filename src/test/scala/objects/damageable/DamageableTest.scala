// Copyright (c) 2020 PSForever
package objects.damageable

import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.definition.ProjectileDefinition
import net.psforever.objects.equipment.JammableUnit
import net.psforever.objects.serverobject.damage.Damageable
import net.psforever.objects.serverobject.generator.Generator
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.serverobject.terminals.{Terminal, TerminalDefinition}
import net.psforever.objects.serverobject.tube.SpawnTube
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.zones.Zone
import net.psforever.types._
import org.specs2.mutable.Specification

class DamageableTest extends Specification {
  private val player1: Player = Player(
    Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)
  )
  private val pSource: PlayerSource             = PlayerSource(player1)
  private val weaponA: Tool                     = Tool(GlobalDefinitions.phoenix) //decimator
  private val projectileA: ProjectileDefinition = weaponA.Projectile

  "Damageable" should {
    "permit damage" in {
      val target = new SensorDeployable(GlobalDefinitions.motionalarmsensor)
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      Damageable.CanDamage(target, projectileA.Damage0, resolved) mustEqual true
    }

    "ignore attempts at non-zero damage" in {
      val target = new SensorDeployable(GlobalDefinitions.motionalarmsensor)
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      Damageable.CanDamage(target, 0, resolved) mustEqual false
    }

    "ignore attempts at damaging friendly targets not designated for friendly fire" in {
      val target = new Generator(GlobalDefinitions.generator)
      target.Owner =
        new Building("test-building", 0, 0, Zone.Nowhere, StructureType.Building, GlobalDefinitions.building) {
          Faction = player1.Faction
        }

      val resolvedFF = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )
      target.Definition.DamageableByFriendlyFire mustEqual false
      target.Faction == player1.Faction mustEqual true
      Damageable.CanDamage(target, projectileA.Damage0, resolvedFF) mustEqual false

      target.Owner.Faction = PlanetSideEmpire.NC
      val resolvedNonFF = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )
      target.Faction != player1.Faction mustEqual true
      Damageable.CanDamage(target, projectileA.Damage0, resolvedNonFF) mustEqual true
    }

    "ignore attempts at damaging a target that is not damageable" in {
      val target = new SpawnTube(GlobalDefinitions.respawn_tube_sanctuary)
      target.Owner =
        new Building("test-building", 0, 0, Zone.Nowhere, StructureType.Building, GlobalDefinitions.building) {
          Faction = PlanetSideEmpire.NC
        }
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      target.Definition.Damageable mustEqual false
      target.Faction != player1.Faction mustEqual true
      Damageable.CanDamage(target, projectileA.Damage0, resolved) mustEqual false
    }

    "permit damaging friendly targets, even those not designated for friendly fire, if the target is hacked" in {
      val player2 =
        Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute))
      player2.GUID = PlanetSideGUID(1)
      val target = new Terminal(new TerminalDefinition(0) {
        Damageable = true
        DamageableByFriendlyFire = false

        override def Request(player: Player, msg: Any): Terminal.Exchange = null
      })
      target.Owner =
        new Building("test-building", 0, 0, Zone.Nowhere, StructureType.Building, GlobalDefinitions.building) {
          Faction = player1.Faction
        }
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      target.Definition.DamageableByFriendlyFire mustEqual false
      target.Faction == player1.Faction mustEqual true
      target.HackedBy.isEmpty mustEqual true
      Damageable.CanDamage(target, projectileA.Damage0, resolved) mustEqual false

      target.HackedBy = player2
      target.Faction == player1.Faction mustEqual true
      target.HackedBy.nonEmpty mustEqual true
      Damageable.CanDamage(target, projectileA.Damage0, resolved) mustEqual true
    }

    val weaponB     = Tool(GlobalDefinitions.jammer_grenade)
    val projectileB = weaponB.Projectile

    "permit jamming" in {
      val target = new SensorDeployable(GlobalDefinitions.motionalarmsensor)
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileB, weaponB.Definition, weaponB.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      resolved.cause.source.HasJammedEffectDuration mustEqual true
      Damageable.CanJammer(target, resolved) mustEqual true
    }

    "ignore attempts at jamming if the projectile is does not cause the effect" in {
      val target = new SensorDeployable(GlobalDefinitions.motionalarmsensor)
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileA, weaponA.Definition, weaponA.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      ) //decimator

      resolved.cause.source.HasJammedEffectDuration mustEqual false
      Damageable.CanJammer(target, resolved) mustEqual false
    }

    "ignore attempts at jamming friendly targets" in {
      val target = new SensorDeployable(GlobalDefinitions.motionalarmsensor)
      target.Faction = player1.Faction
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileB, weaponB.Definition, weaponB.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      resolved.cause.source.HasJammedEffectDuration mustEqual true
      resolved.adversarial match {
        case Some(adversarial) => adversarial.attacker.Faction mustEqual adversarial.defender.Faction
        case None              => ko
      }
      Damageable.CanJammer(target, resolved) mustEqual false
    }

    "ignore attempts at jamming targets that are not jammable" in {
      val target = new TrapDeployable(GlobalDefinitions.tank_traps)
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileB, weaponB.Definition, weaponB.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      resolved.cause.source.HasJammedEffectDuration mustEqual true
      resolved.adversarial match {
        case Some(adversarial) => adversarial.attacker.Faction mustNotEqual adversarial.defender.Faction
        case None              => ko
      }
      target.isInstanceOf[JammableUnit] mustEqual false
      Damageable.CanJammer(target, resolved) mustEqual false
    }

    "permit jamming friendly targets if the target is hacked" in {
      val player2 =
        Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute))
      player2.GUID = PlanetSideGUID(1)
      val target = new SensorDeployable(GlobalDefinitions.motionalarmsensor)
      target.Faction = player1.Faction
      val resolved = DamageInteraction(
        SourceEntry(target),
        ProjectileReason(
          DamageResolution.Hit,
          Projectile(projectileB, weaponB.Definition, weaponB.FireMode, pSource, 0, Vector3.Zero, Vector3.Zero),
          target.DamageModel
        ),
        Vector3.Zero
      )

      resolved.cause.source.HasJammedEffectDuration mustEqual true
      resolved.adversarial match {
        case Some(adversarial) => adversarial.attacker.Faction mustEqual adversarial.defender.Faction
        case None              => ko
      }
      target.isInstanceOf[JammableUnit] mustEqual true
      target.HackedBy.nonEmpty mustEqual false
      Damageable.CanJammer(target, resolved) mustEqual false

      target.HackedBy = player2
      target.HackedBy.nonEmpty mustEqual true
      Damageable.CanJammer(target, resolved) mustEqual true
    }
  }
}
