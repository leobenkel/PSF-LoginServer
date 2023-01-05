package net.psforever.objects.serverobject.structures


trait SphereOfInfluence {
  private var soiRadius: Int = 0

private def SOIRadius: Int = soiRadius

private def SOIRadius_=(radius: Int): Int = {
    soiRadius = radius
    SOIRadius
  }
}
