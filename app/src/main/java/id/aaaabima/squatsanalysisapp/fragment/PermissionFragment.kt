package id.aaaabima.squatsanalysisapp.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import id.aaaabima.squatsanalysisapp.R
import kotlinx.coroutines.launch

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PermissionFragment : Fragment() {

  private val requestPermissionLauncher =
    registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
      if (isGranted) {
        Toast.makeText(
          context,
          "Permission request granted",
          Toast.LENGTH_LONG
        ).show()
        navigateToExercise()
      } else {
        Toast.makeText(
          context,
          "Permission request denied",
          Toast.LENGTH_LONG
        ).show()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    when (PackageManager.PERMISSION_GRANTED) {
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
      ) -> {
        navigateToExercise()
      }

      else -> {
        requestPermissionLauncher.launch(
          Manifest.permission.CAMERA
        )
      }
    }
  }

  private fun navigateToExercise() {
    lifecycleScope.launch {
      Navigation.findNavController(
        requireActivity(),
        R.id.nav_host_fragment
      ).navigate(R.id.action_permissionFragment_to_fragment_exercise)
    }
  }

  companion object {
    // Method to check if all permission required by the app are granted
    fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
      ContextCompat.checkSelfPermission(
        context,
        it
      ) == PackageManager.PERMISSION_GRANTED
    }
  }
}