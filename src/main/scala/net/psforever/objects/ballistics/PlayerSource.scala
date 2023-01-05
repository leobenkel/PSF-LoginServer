// Copyright (c) 2017 PSForever
package net.psforever.objects.ballistics

import net.psforever.objects.Player
import net.psforever.objects.definition.{AvatarDefinition, ExoSuitDefinition}
import net.psforever.objects.vital.resistance.ResistanceProfile
import net.psforever.types.{ExoSuitType, PlanetSideEmpire, Vector3}

final case class PlayerSource(
    name: String,
    char_id: Long,
    obj_def: AvatarDefinition,
    faction: PlanetSideEmpire.Value,
    exosuit: ExoSuitType.Value,
    seated: Boolean,
    health: Int,
    armor: Int,
    position: Vector3,
    orientation: Vector3,
    velocity: Option[Vector3],
    crouching: Boolean,
    jumping: Boolean,
    modifiers: ResistanceProfile
) extends SourceEntry {
  override def Name       = name
  override def Faction    = faction
  override def CharId     = char_id
  private def Definition  = obj_def
  def ExoSuit             = exosuit
  private def Seated      = seated
  private def Health      = health
  private def Armor       = armor
  private def Position    = position
  private def Orientation = orientation
  private def Velocity    = velocity
  private def Modifiers   = modifiers
}

object PlayerSource {
  def apply(tplayer: Player): PlayerSource = {
    PlayerSource(
      tplayer.Name,
      tplayer.CharId,
      tplayer.Definition,
      tplayer.Faction,
      tplayer.ExoSuit,
      tplayer.VehicleSeated.nonEmpty,
      tplayer.Health,
      tplayer.Armor,
      tplayer.Position,
      tplayer.Orientation,
      tplayer.Velocity,
      tplayer.Crouching,
      tplayer.Jumping,
      ExoSuitDefinition.Select(tplayer.ExoSuit, tplayer.Faction)
    )
  }
}
