// Copyright (c) 2019 PSForever
package net.psforever.objects.teamwork

import net.psforever.objects.avatar.Certification
import net.psforever.objects.entity.IdentifiableEntity
import net.psforever.types.{PlanetSideEmpire, PlanetSideGUID}

class Squad(squadId: PlanetSideGUID, alignment: PlanetSideEmpire.Value) extends IdentifiableEntity {
  super.GUID_=(squadId)
  private val faction: PlanetSideEmpire.Value = alignment //does not change
  private var zoneId: Option[Int]             = None
  private var task: String                    = ""
  private val membership: Array[Member]       = Array.fill[Member](10)(new Member)
  private val availability: Array[Boolean]    = Array.fill[Boolean](10)(elem = true)

  override def GUID_=(d: PlanetSideGUID): PlanetSideGUID = GUID

private def Faction: PlanetSideEmpire.Value = faction

private def CustomZoneId: Boolean = zoneId.isDefined

private def ZoneId: Int = zoneId.getOrElse(membership(0).ZoneId)

private def ZoneId_=(id: Int): Int = {
    ZoneId_=(Some(id))
  }

private def ZoneId_=(id: Option[Int]): Int = {
    zoneId = id
    ZoneId
  }

private def Task: String = task

private def Task_=(assignment: String): String = {
    task = assignment
    Task
  }

private def Membership: Array[Member] = membership

private def Availability: Array[Boolean] = availability

private def Leader: Member = {
    membership(0) match {
      case member if !member.Name.equals("") =>
        member
      case _ =>
        throw new Exception("can not find squad leader!")
    }
  }

private def Size: Int = membership.count(member => member.CharId != 0)

private def Capacity: Int = availability.count(open => open)

private def isAvailable(role: Int): Boolean = {
    availability.lift(role) match {
      case Some(true) =>
        membership(role).isAvailable
      case _ =>
        false
    }
  }

private def isAvailable(role: Int, certs: Set[Certification]): Boolean = {
    availability.lift(role) match {
      case Some(true) =>
        membership(role).isAvailable(certs)
      case _ =>
        false
    }
  }
}

object Squad {
  final val Blank = new Squad(PlanetSideGUID(0), PlanetSideEmpire.NEUTRAL) {
    override def ZoneId: Int                        = 0
    override def ZoneId_=(id: Int): Int             = 0
    override def ZoneId_=(id: Option[Int]): Int     = 0
    override def Task_=(assignment: String): String = ""
    override def Membership: Array[Member]          = Array.empty[Member]
    override def Availability: Array[Boolean]       = Array.fill[Boolean](10)(false)
  }
}
