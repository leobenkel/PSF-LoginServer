// Copyright (c) 2019 PSForever
package net.psforever.objects.serverobject.structures

import net.psforever.objects.serverobject.PlanetSideServerObject

/**
  * Amenities are elements of the game that belong to other elements of the game.
  * Their owners are also elements of the game, ones that understand that they belong to a specific `Zone` object.
  * @see `PlanetSideServerObject`
  */
abstract class AmenityOwner extends PlanetSideServerObject {
  private var amenities: List[Amenity] = List.empty

  def Amenities: List[Amenity] = amenities

  private def Amenities_=(obj: Amenity): List[Amenity] = {
    amenities = amenities :+ obj
    obj.Owner = this
    amenities
  }

  private def RemoveAmenity(obj: Amenity): List[Amenity] = {
    amenities = amenities.filterNot(x => x == obj)
    amenities
  }
}
