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
            "nitrogen-n" -> "Kekurangan nitrogen (N) dapat menyebabkan daun-daun tua tampak mengalami klorosis atau menguning secara seragam. Dalam beberapa kasus, pertumbuhan tanaman terhambat dan seluruh daun dapat menjadi klorosis."

            "phosphorus-p" -> "Kekurangan fosfor (P) menyebabkan daun berbentuk tidak beraturan dengan bintik-bintik kuning kecoklatan, warna kemerahan atau keunguan di bagian bawah daun yang lebih tua, juga dengan klorosis interveinal lobural atau dengan kata lain menguningnya pembuluh daun, dan dapat menunda perkembangan tanaman."

            "potasium-k" -> "Kekurangan kalium (K) menyebabkan tepi daun tampak berwarna kuning dan berbintik cokelat, yang akan menyebabkan warna cokelat gelap nekrotik, lingkaran kuning di sekitar area nekrotik, dan tepi daun melengkung ke atas."

            "calcium-ca" -> "Kekurangan kalsium (Ca) menyebabkan daun baru klorosis marginal, deformasi daun menjadi cembung, dan terbentuknya bentuk seperti gabus pada pembuluh darah di bagian bawah daun. Daun mengubah posisi biasanya menjadi menggantung ke bawah dan menunjukkan nekrosis pada ujung dan tepinya."

            "magnesium-mg" -> "Kekurangan magnesium (Mg) menyebabkan klorosis antar vena pada daun yang lebih tua, diikuti oleh pengguguran daun yang parah, sehingga terbentuk garis-garis hijau di sepanjang tulang daun."

            "boron-b" -> "Kekurangan boron (B) menyebabkan daun muda tampak kecil, memanjang, terpelintir, keriput, dengan tepi tidak beraturan, cacat, dan bertekstur seperti kulit. Daun yang mengalami kekurangan ini menunjukkan klorosis hijau zaitun kusam dari ujung ke pangkal."

            "iron-fe" -> "Kekurangan zat besi (Fe) menyebabkan klorosis antar vena pada daun muda, di mana warnanya berubah dari hijau kekuningan menjadi hijau sangat muda (hampir putih). Di sisi lain, urat daun tetap hijau membentuk pola seperti jaring."

            "manganese-mn" -> "Kekurangan mangan (Mn) juga menginfeksi daun muda dengan klorosis antar vena yang menyebabkan warnanya menjadi hijau pucat sementara vena dan pita utama tetap hijau tua. Seiring perkembangan infeksi, daun secara bertahap menjadi lebih kuning dan dapat mengembangkan bintik-bintik nekrotik kecil pada permukaan daun."

            "healthy" -> "Daun kopi yang sehat berwarna hijau cerah, halus, dan rata dengan tekstur mengkilap. Daun tersebut tidak menunjukkan tanda-tanda klorosis (menguning), nekrosis (bintik-bintik cokelat atau gelap), atau deformasi. tepi daun halus dan utuh, dan urat daun berwarna hijau lebih terang daripada bagian tengah daun, yang menunjukkan kadar nutrisi yang optimal."

            else -> "Deskripsi untuk label ini tidak tersedia."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}