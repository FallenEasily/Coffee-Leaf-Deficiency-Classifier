package com.aiclassifier.ui.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aiclassifier.R
import com.aiclassifier.databinding.FragmentDescriptionBinding
import com.aiclassifier.ui.ClassifierViewModel
import com.aiclassifier.ui.UiState

class DescriptionFragment : Fragment() {

    private val viewModel: ClassifierViewModel by activityViewModels()
    private var _binding: FragmentDescriptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.ClassifySuccess -> {
                    val top = state.result.topLabels.firstOrNull()
                    val label = top?.label ?: "Unknown"
                    val confidence = top?.score ?: 0f

                    binding.tvLabel.text = label
                    binding.tvDesc.text = getDescriptionForLabel(label)

                    binding.tvDesc.text = getDescriptionForLabel(label)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        binding.tvDesc.justificationMode = android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                    }
                }

                is UiState.DetectSuccess -> {
                    val top = state.result.detections.firstOrNull()
                    binding.tvLabel.text = top?.label ?: "No detection"
                    binding.tvDesc.text = "Detection result"
                }

                is UiState.Loading -> {
                    binding.tvLabel.text = "Loading..."
                    binding.tvDesc.text = "Please wait..."
                }

                is UiState.Error -> {
                    binding.tvLabel.text = "Error"
                    binding.tvDesc.text = "Failed to classify."
                }

                else -> {
                    binding.tvLabel.text = "--"
                    binding.tvDesc.text = ""
                }
            }
        }
    }

    private fun getDescriptionForLabel(label: String): String {
        // Convert to lowercase for case-insensitive matching, but keep exact keys for safety
        return when (label.lowercase()) {
            "nitrogen-n" -> "Nitrogen (N) deficiency can make older leaves appear to have uniform chlorosis or yellowing. In some cases, the plant growth gets stunted and the whole leaf may become chlorotic."

            "phosphorus-p" -> "Phosphorus (P) deficiency causes the leaves to be irregularly shaped with yellow tan spots, reddish or purplish hues on the underside of older leaf also with lobural interveinal chlorosis or in other words yellowing on the leaf vein, and may delay the development of the plant."

            "potasium-k" -> "Potassium (K) deficiency produces leaf margins that appear to be yellow and brown spots, which will lead to necrotic dark brown color, yellow halo around the necrotic areas, and the leaf edges curl upward."

            "calcium-ca" -> "Calcium (Ca) deficiency creates new leaves marginal chlorosis, leaf deformation making convex shape, and forming cork like shape in the veins underside of leaves. The leaves change their usual position which hangs downward and shows necrotic on the tips and margin."

            "magnesium-mg" -> "Magnesium (Mg) deficiency causes interveinal chlorosis to older leaves followed by severe defoliation, creating green stripes along the midrib."

            "boron-b" -> "Boron (B) deficiency makes young leaves appear to be small, elongated, twisted, wrinkled, with irregular edges, deformed, and leathery texture. Leaves that have these deficiency have shown a dull olive green chlorosis from the apex to base."

            "iron-fe" -> "Iron (Fe) deficiency causes interveinal chlorosis in young leaves where they are colored from greenish-yellow to very light green (nearly white). On the other hand, the veins remain green forming a net like pattern."

            "manganese-mn" -> "Manganese (Mn) deficiency also infects young leaves with interveinal chlorosis causing them to be pale green in color while the main veins and bands remain deep green. As the infection progresses, the leaves gradually turn more yellow and can develop small necrotic spots on the leaf blade."

            "healthy" -> "A healthy coffee leaf is vibrant green, smooth, and flat with a glossy texture. It shows no signs of chlorosis (yellowing), necrosis (brown or dark spots), or deformation. The leaf margins are smooth and intact, and the veins are distinctly lighter green than the blade, indicating optimal nutrient levels."

            else -> "Description not available for this label."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}