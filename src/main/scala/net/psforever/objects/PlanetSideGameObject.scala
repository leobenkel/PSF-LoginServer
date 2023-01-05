// Copyright (c) 2017 PSForever
package net.psforever.objects

import net.psforever.objects.definition.ObjectDefinition
import net.psforever.objects.entity.{IdentifiableEntity, SimpleWorldEntity, WorldEntity}
import net.psforever.types.Vector3

/**
  * A basic class that indicates an entity that exists somewhere in the world and has a globally unique identifier.
  */
abstract class PlanetSideGameObject extends IdentifiableEntity with WorldEntity {
  private var entity: WorldEntity = new SimpleWorldEntity()
  private var destroyed: Boolean  = false

  private def Entity: WorldEntity = entity

  private def Entity_=(newEntity: WorldEntity): Unit = {
    entity = newEntity
  }

  override def Position: Vector3 = Entity.Position

  override def Position_=(vec: Vector3): Vector3 = {
    Entity.Position = vec
  }

  override def Orientation: Vector3 = Entity.Orientation

  override def Orientation_=(vec: Vector3): Vector3 = {
    Entity.Orientation = vec
  }

  override def Velocity: Option[Vector3] = Entity.Velocity

  override def Velocity_=(vec: Option[Vector3]): Option[Vector3] = {
    Entity.Velocity = vec
  }

  def Destroyed: Boolean = destroyed

  def Destroyed_=(state: Boolean): Boolean = {
    destroyed = state
    Destroyed
  }

  def Definition: ObjectDefinition
}

object PlanetSideGameObject {
  private def toString(obj: PlanetSideGameObject): String = {
    val guid: String = if (obj.HasGUID) { obj.GUID.toString }
    else { "NOGUID" }
    val P = obj.Position
    s"[$guid](x,y,z=${P.x % .3f},${P.y % .3f},${P.z % .3f})"
  }
}
