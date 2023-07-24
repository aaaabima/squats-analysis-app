package id.aaaabima.squatsanalysisapp.utils

enum class Threshold(val value: ArrayList<Number>) {
  NormalAngle(value = arrayListOf(0, 32)),
  TransAngle(value = arrayListOf(35, 65)),
  PassAngle(value = arrayListOf(70, 95)),
  HipThresh(value = arrayListOf(10, 50)),
  AnkleThresh(value = arrayListOf(45)),
  KneeThresh(value = arrayListOf(50, 70, 95)),
  OffsetThresh(value = arrayListOf(35.0f))
}