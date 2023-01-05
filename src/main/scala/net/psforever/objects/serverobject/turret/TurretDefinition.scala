// Copyright (c) 2017 PSForever
package net.psforever.objects.serverobject.turret

import net.psforever.objects.definition.{ObjectDefinition, ToolDefinition}
import net.psforever.objects.vehicles.{MountableWeaponsDefinition, Turrets}
import net.psforever.objects.vital.resistance.ResistanceProfileMutators
import net.psforever.objects.vital.resolution.DamageResistanceModel

import scala.collection.mutable

/**
  * The definition for any `MannedTurret`.
  */
trait TurretDefinition
  extends MountableWeaponsDefinition
  with ResistanceProfileMutators
  with DamageResistanceModel {
  odef: ObjectDefinition =>
  Turrets(odef.ObjectId) //let throw NoSuchElementException
  /* key - upgrade, value - weapon definition */
  private val weaponPaths: mutable.HashMap[Int, mutable.HashMap[TurretUpgrade.Value, ToolDefinition]] =
    mutable.HashMap[Int, mutable.HashMap[TurretUpgrade.Value, ToolDefinition]]()

  /** can only be mounted by owning faction when `true` */
  private var factionLocked: Boolean = true

  /** creates internal ammunition reserves that can not become depleted
    * see `MannedTurret.TurretAmmoBox` for details
    */
  private var hasReserveAmmunition: Boolean = false

private def WeaponPaths: mutable.HashMap[Int, mutable.HashMap[TurretUpgrade.Value, ToolDefinition]] = weaponPaths

private def FactionLocked: Boolean = factionLocked

private def FactionLocked_=(ownable: Boolean): Boolean = {
    factionLocked = ownable
    FactionLocked
  }

private def ReserveAmmunition: Boolean = hasReserveAmmunition

private def ReserveAmmunition_=(reserved: Boolean): Boolean = {
    hasReserveAmmunition = reserved
    ReserveAmmunition
  }
}
