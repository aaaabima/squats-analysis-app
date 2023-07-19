package id.aaaabima.squatsanalysisapp.utils

enum class Threshold(
    val normalAngle: ArrayList<Int> = arrayListOf(0, 32),
    val transAngle: ArrayList<Int> = arrayListOf(35, 65),
    val passAngle: ArrayList<Int> = arrayListOf(70, 95),
    val hipThresh: ArrayList<Int> = arrayListOf(10, 50),
    val ankleThresh: ArrayList<Int> = arrayListOf(45),
    val kneeThresh: ArrayList<Int> = arrayListOf(50, 70, 95),
    val offsetThresh: ArrayList<Float> = arrayListOf(35.0f)
)