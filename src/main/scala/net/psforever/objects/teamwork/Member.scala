// Copyright (c) 2019 PSForever
package net.psforever.objects.teamwork

import net.psforever.objects.avatar.Certification
import net.psforever.types.Vector3

class Member {
  //about the position to be filled
  private var role: String                     = ""
  private var orders: String                   = ""
  private var requirements: Set[Certification] = Set()
  //about the individual filling the position
  private var name: String      = ""
  private var charId: Long      = 0L
  private var health: Int       = 0
  private var armor: Int        = 0
  private var zoneId: Int       = 0
  private var position: Vector3 = Vector3.Zero

private def Role: String = role

private def Role_=(title: String): String = {
    role = title
    Role
  }

private def Orders: String = orders

private def Orders_=(text: String): String = {
    orders = text
    Orders
  }

private def Requirements: Set[Certification] = requirements

private def Requirements_=(req: Set[Certification]): Set[Certification] = {
    requirements = req
    Requirements
  }

private def Name: String = name

private def Name_=(moniker: String): String = {
    name = moniker
    Name
  }

private def CharId: Long = charId

private def CharId_=(id: Long): Long = {
    charId = id
    CharId
  }

private def Health: Int = health

private def Health_=(red: Int): Int = {
    health = red
    Health
  }

private def Armor: Int = armor

private def Armor_=(blue: Int): Int = {
    armor = blue
    Armor
  }

private def ZoneId: Int = zoneId

private def ZoneId_=(id: Int): Int = {
    zoneId = id
    ZoneId
  }

private def Position: Vector3 = position

private def Position_=(pos: Vector3): Vector3 = {
    position = pos
    Position
  }

private def isAvailable: Boolean = {
    charId == 0
  }

private def isAvailable(certs: Set[Certification]): Boolean = {
    isAvailable && certs.intersect(requirements) == requirements
  }
}
