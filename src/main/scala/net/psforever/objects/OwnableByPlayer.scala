// Copyright (c) 2019 PSForever
package net.psforever.objects

import net.psforever.types.PlanetSideGUID

trait OwnableByPlayer {
  private var owner: Option[PlanetSideGUID] = None
  private var ownerName: Option[String]     = None

  private def Owner: Option[PlanetSideGUID] = owner

  private def Owner_=(owner: PlanetSideGUID): Option[PlanetSideGUID] = Owner_=(Some(owner))

  private def Owner_=(owner: Player): Option[PlanetSideGUID] = Owner_=(Some(owner.GUID))

  private def Owner_=(owner: Option[PlanetSideGUID]): Option[PlanetSideGUID] = {
    owner match {
      case Some(_) =>
        this.owner = owner
      case None =>
        this.owner = None
    }
    Owner
  }

  def OwnerName: Option[String] = ownerName

  private def OwnerName_=(owner: String): Option[String] = OwnerName_=(Some(owner))

  private def OwnerName_=(owner: Player): Option[String] = OwnerName_=(Some(owner.Name))

  private def OwnerName_=(owner: Option[String]): Option[String] = {
    owner match {
      case Some(_) =>
        ownerName = owner
      case None =>
        ownerName = None
    }
    OwnerName
  }

  /**
    * na
    * @param player na
    * @return na
    */
  private def AssignOwnership(player: Player): OwnableByPlayer = AssignOwnership(Some(player))

  /**
    * na
    * @param playerOpt na
    * @return na
    */
  private def AssignOwnership(playerOpt: Option[Player]): OwnableByPlayer = {
    playerOpt match {
      case Some(player) =>
        Owner = player
        OwnerName = player
      case None =>
        Owner = None
        OwnerName = None
    }
    this
  }
}
