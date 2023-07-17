package id.aaaabima.squats_analysis_app.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import id.aaaabima.squats_analysis_app.MainViewModel
import id.aaaabima.squats_analysis_app.databinding.FragmentSettingsBinding
import java.util.Locale

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSettingsUi()
        setButtonListener()
    }

    private fun setButtonListener() {
        binding.detectionThresholdPlus.setOnClickListener {
            viewModel.currentMinPoseDetectionConfidence.let {
                if (it <= 0.81) {
                    viewModel.setMinPoseDetectionConfidence(it + 0.1f)
                    updateSettingsUi()
                }
            }
        }

        binding.detectionThresholdMinus.setOnClickListener {
            viewModel.currentMinPoseDetectionConfidence.let {
                if (it >= 0.2) {
                    viewModel.setMinPoseDetectionConfidence(it - 0.1f)
                    updateSettingsUi()
                }
            }
        }
        binding.trackingThresholdPlus.setOnClickListener {
            viewModel.currentMinPoseTrackingConfidence.let {
                if (it <= 0.81) {
                    viewModel.setMinPoseTrackingConfidence(it + 0.1f)
                    updateSettingsUi()
                }
            }
        }

        binding.trackingThresholdMinus.setOnClickListener {
            viewModel.currentMinPoseTrackingConfidence.let {
                if (it >= 0.2) {
                    viewModel.setMinPoseTrackingConfidence(it - 0.1f)
                    updateSettingsUi()
                }
            }
        }
        binding.presenceThresholdPlus.setOnClickListener {
            viewModel.currentMinPosePresenceConfidence.let {
                if (it <= 0.81) {
                    viewModel.setMinPosePresenceConfidence(it + 0.1f)
                    updateSettingsUi()
                }
            }
        }

        binding.presenceThresholdMinus.setOnClickListener {
            viewModel.currentMinPosePresenceConfidence.let {
                if (it >= 0.2) {
                    viewModel.setMinPosePresenceConfidence(it - 0.1f)
                    updateSettingsUi()
                }
            }
        }

        binding.spinnerDelegate.setSelection(
            viewModel.currentDelegate, false
        )
        binding.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.setDelegate(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No implementation
                }
            }

        binding.spinnerCamera.setSelection(
            viewModel.currentCameraFacing, false
        )
        binding.spinnerCamera.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.setCameraFacing(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No implementation
                }

            }
    }

    private fun updateSettingsUi() {
        binding.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
            )
        binding.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
            )
        binding.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
            )
    }
}