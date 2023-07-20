package id.aaaabima.squatsanalysisapp.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import id.aaaabima.squatsanalysisapp.MainViewModel
import id.aaaabima.squatsanalysisapp.PoseLandmarkerHelper
import id.aaaabima.squatsanalysisapp.R
import id.aaaabima.squatsanalysisapp.databinding.FragmentExerciseBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ExerciseFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {
    private var _binding: FragmentExerciseBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing: Int = 0

    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        requireActivity().apply {
            findViewById<TextView>(R.id.tv_feedback).isVisible = true
            findViewById<TextView>(R.id.tv_inference_time).isVisible = true
            findViewById<TextView>(R.id.tv_correct).isVisible = true
            findViewById<TextView>(R.id.tv_incorrect).isVisible = true
            findViewById<TextView>(R.id.tv_state).isVisible = true
        }
        // Check if all permissions are still present
        if (!PermissionFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.nav_host_fragment
            ).navigate(R.id.action_fragment_exercise_to_permissionFragment)
        }

        // Start the PoseLandmarkerHelper when user return to foreground
        backgroundExecutor.execute {
            if (poseLandmarkerHelper.isClose())
                poseLandmarkerHelper.setupPoseLandmarker()
        }
        updateControlsUi()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().apply {
            findViewById<TextView>(R.id.tv_feedback).isVisible = false
            findViewById<TextView>(R.id.tv_inference_time).isVisible = false
            findViewById<TextView>(R.id.tv_correct).isVisible = false
            findViewById<TextView>(R.id.tv_incorrect).isVisible = false
            findViewById<TextView>(R.id.tv_state).isVisible = false
        }
        if (this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            // Close the PoseLandmarkerHelper and release resources
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
        updateControlsUi()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()

        // Shutdown background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraFacing = viewModel.currentCameraFacing

        // Initialize background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // wait for view to be properly laid out
        binding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create PoseLandmarkerHelper for inference
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                poseLandmarkerHelperListener = this
            )
        }
    }

    private fun updateControlsUi() {
        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        backgroundExecutor.execute {
            poseLandmarkerHelper.clearPoseLandmarker()
            poseLandmarkerHelper.setupPoseLandmarker()
        }
        binding.overlay.clear()
    }

    // Initialize CameraX and bind camera use case
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // Camera provider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun bindCameraUseCases() {
        // Camera Provider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using 4:3 ratio as it is the closest to MediaPipe Pose Landmarker Model
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis using RGBA 8888 to match MediaPipe model
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectPose(image)
                    }
                }

        // Unbid use-case before rebinding it
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        poseLandmarkerHelper.detecLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                val view: Spinner? = activity?.findViewById(R.id.spinner_delegate)
                view?.setSelection(
                    PoseLandmarkerHelper.DELEGATE_CPU, false
                )
            }
        }
    }

    // Update UI after pose detected
    // Extract original image height/width to scale
    // Place landmarks properly through OverlayView
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_binding != null) {
                val inferenceTimeView: TextView =
                    requireActivity().findViewById(R.id.tv_inference_time)
                inferenceTimeView.text =
                    String.format(getString(R.string.inference_time), resultBundle.inferenceTime)

                binding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    requireActivity()
                )

                // Force redraw
                binding.overlay.invalidate()
            }
        }
    }

    companion object {
        private const val TAG = "Pose Landmarker"
    }
}