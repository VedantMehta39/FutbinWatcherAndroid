package com.vedant.futsnap.UI

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.vedant.futsnap.*
import com.vedant.futsnap.UI.Models.Platform
import com.vedant.futsnap.UI.Models.PlayerDialogFragModel
import com.vedant.futsnap.Network.ResponseModels.Player
import com.vedant.futsnap.Network.ResponseModels.PlayerTrackingRequest
import com.vedant.futsnap.UI.ErrorHandling.ErrorHandling
import com.vedant.futsnap.UI.Validators.TextContentValidator
import com.vedant.futsnap.UI.Validators.TextLengthValidator
import com.vedant.futsnap.UI.ViewModels.CustomViewModelFactory
import com.vedant.futsnap.UI.ViewModels.MainActivityViewModel
import com.vedant.futsnap.UI.ViewModels.SinglePlayerDialogFragmentViewModel
import com.vedant.futsnap.Utils.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import com.squareup.picasso.Picasso
import javax.inject.Inject


class SinglePlayerDialog(val data: PlayerDialogFragModel) : DialogFragment() {
    @Inject
    lateinit var customViewModelFactory: CustomViewModelFactory

    @Inject
    lateinit var clientUtility: ClientUtility

    lateinit var playerImageView: ImageView
    lateinit var currentPriceTextView: TextView
    lateinit var targetPrice: TextInputEditText
    lateinit var playerNameTextView: TextView
    lateinit var psPlatformButton: MaterialButton
    lateinit var xboxPlatformButton: MaterialButton
    lateinit var platform_toggle: MaterialButtonToggleGroup
    lateinit var gte_lt_toggle: MaterialButtonToggleGroup
    lateinit var progressBar: ProgressBar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.single_player_dialog, null)
        val dialog = getBaseDialog(view)
        dialog.show()
        FUTSnapApp.component.inject(this)
        val vm = ViewModelProvider(
            this,
            customViewModelFactory
        ).get(SinglePlayerDialogFragmentViewModel::class.java)
        vm.initSelectedPlayer(data)

        initViews(view)
        showLoadingSpinner(true)

        vm.selectedPlayer.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let {
                showLoadingSpinner(false)

                bindDataToView(it)

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _ ->
                    val mainActivityVm =
                        ViewModelProvider(requireActivity(), customViewModelFactory)
                            .get(MainActivityViewModel::class.java)

                    val contentValidator = TextContentValidator("-.")
                    val lengthValidator = TextLengthValidator(1, null)

                    if (lengthValidator.validate(targetPrice.text.toString()) and
                        contentValidator.validate(targetPrice.text.toString())
                    ) {
                        val notifiedPlayer =
                            PlayerTrackingRequest()
                        notifiedPlayer.PlayerId = it.id
                        notifiedPlayer.Player =
                            Player(
                                it.id,
                                it.cardName,
                                it.imageURL
                            )
                        val chosenPlatformId = platform_toggle.checkedButtonId
                        val chosenComparisonDirectionIds = gte_lt_toggle.checkedButtonIds
                        when (chosenPlatformId) {
                            R.id.ps_button -> notifiedPlayer.Platform = Platform.PS.ordinal
                            R.id.xbox_button -> notifiedPlayer.Platform = Platform.XB.ordinal
                        }
                        if (chosenComparisonDirectionIds.contains(R.id.gte_target)) {
                            notifiedPlayer.Gte = true
                        }
                        if (chosenComparisonDirectionIds.contains(R.id.lt_target)) {
                            notifiedPlayer.Lt = true
                        }
                        notifiedPlayer.TargetPrice = StringFormatter.getNumberFromLocaleFormattedString(targetPrice.text.toString())
                        notifiedPlayer.ClientId = mainActivityVm.clientId

                        if (it.isEdited) {
                            mainActivityVm.editPlayerTrackingRequest(
                                notifiedPlayer.PlayerId,
                                notifiedPlayer
                            ).observe(this, Observer { response ->
                                when (response) {
                                    is NetworkResponse.Success -> {
                                        DynamicToast.makeSuccess(
                                            requireContext(), "Player Tracking Request" +
                                                    " edited successfully!", Toast.LENGTH_SHORT
                                        ).show()
                                        dismiss()
                                    }
                                    is NetworkResponse.Failure -> {
                                        dismiss()
                                        val errorHandler = ErrorHandling(
                                            requireContext(),
                                            parentFragmentManager,
                                            clientUtility::addOrUpdateTokenOnServer
                                        )
                                        errorHandler.handle(response.error)
                                    }
                                }
                            })
                        } else {
                            mainActivityVm.addPlayerTrackingRequest(notifiedPlayer)
                                .observe(this, Observer { response ->
                                    when (response) {
                                        is NetworkResponse.Success -> {
                                            DynamicToast.makeSuccess(
                                                requireContext(), "Player Tracking Request" +
                                                        " added successfully!", Toast.LENGTH_SHORT
                                            ).show()
                                            dismiss()
                                        }
                                        is NetworkResponse.Failure -> {
                                            dismiss()
                                            val errorHandler = ErrorHandling(
                                                requireContext(),
                                                parentFragmentManager,
                                                clientUtility::addOrUpdateTokenOnServer
                                            )
                                            errorHandler.handle(response.error)
                                        }
                                    }
                                })
                        }
                    }
                }
            }
        })
        vm.error.observe(this, Observer { errorEvent ->
            errorEvent.getContentIfNotHandled()?.let {
                    error ->
                val errorHandler = ErrorHandling(
                    requireContext(),
                    parentFragmentManager, clientUtility::addOrUpdateTokenOnServer
                )
                errorHandler.handle(error)
            }
        })
        return dialog
    }

    private fun initViews(view: View) {
        playerImageView = view.findViewById(R.id.img_player)
        currentPriceTextView = view.findViewById(R.id.tv_current_price)

        targetPrice = (view.findViewById(R.id.et_target_price))
        targetPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                text?.let {
                    val validator = TextContentValidator("-.")
                    if (!validator.validate(text.toString())) {
                        targetPrice.error = validator.errorMessage
                    }
                }
            }

        })

        playerNameTextView = view.findViewById(R.id.tv_player_name)
        psPlatformButton = view.findViewById(R.id.ps_button)
        xboxPlatformButton = view.findViewById(R.id.xbox_button)
        platform_toggle = view.findViewById(R.id.platform_toggle)
        gte_lt_toggle = view.findViewById(R.id.gte_lt_toggle)
        progressBar = view.findViewById(R.id.dialog_load_spinner)
    }

    private fun bindDataToView(data: PlayerDialogFragModel) {
        playerNameTextView.text = (data.cardName)

        targetPrice.setText(StringFormatter.getLocaleFormattedStringFromNumber(data.targetPrice))

        if (data.gte) {
            gte_lt_toggle.check(R.id.gte_target)
        } else {
            gte_lt_toggle.check(R.id.lt_target)
        }
        when (data.platform) {
            Platform.PS -> {
                platform_toggle.check(R.id.ps_button)
                currentPriceTextView.text = StringFormatter.getLocaleFormattedStringFromNumber(
                    data.currentPrice[Platform.PS]!!
                )
            }
            Platform.XB -> {
                platform_toggle.check(R.id.xbox_button)
                currentPriceTextView.text = StringFormatter.getLocaleFormattedStringFromNumber(
                    data.currentPrice[Platform.XB]!!
                )
            }
        }
        Picasso.get().load(data.imageURL).into(playerImageView)


        psPlatformButton.setOnClickListener {
            currentPriceTextView.text = StringFormatter.getLocaleFormattedStringFromNumber(
                data.currentPrice[Platform.PS]!!
            )
        }
        xboxPlatformButton.setOnClickListener {
            currentPriceTextView.text = StringFormatter.getLocaleFormattedStringFromNumber(
                data.currentPrice[Platform.XB]!!
            )
        }

    }

    private fun showLoadingSpinner(show: Boolean) {
        val progressBarVisibility: Int
        val otherVisibility: Int
        when (show) {
            true -> {
                progressBarVisibility = View.VISIBLE
                otherVisibility = View.GONE
            }
            false -> {
                progressBarVisibility = View.GONE
                otherVisibility = View.VISIBLE
            }
        }
        progressBar.visibility = progressBarVisibility
        currentPriceTextView.visibility = otherVisibility
        targetPrice.visibility = otherVisibility
        playerNameTextView.visibility = otherVisibility
        psPlatformButton.visibility = otherVisibility
        xboxPlatformButton.visibility = otherVisibility
        platform_toggle.visibility = otherVisibility
        gte_lt_toggle.visibility = otherVisibility
        playerImageView.visibility = otherVisibility

    }

    private fun getBaseDialog(view: View): AlertDialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(view).setNegativeButton(R.string.cancel) { _, _ ->
            dismiss()
        }.setPositiveButton(R.string.ok, null)
        return builder.create()
    }

}