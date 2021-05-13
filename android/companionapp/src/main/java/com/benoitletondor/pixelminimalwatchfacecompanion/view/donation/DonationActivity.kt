/*
 *   Copyright 2021 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.pixelminimalwatchfacecompanion.view.donation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.benoitletondor.pixelminimalwatchfacecompanion.R
import com.benoitletondor.pixelminimalwatchfacecompanion.helper.startSupportEmailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_donation.*

@AndroidEntryPoint
class DonationActivity : AppCompatActivity() {
    private val viewModel: DonationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donation)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        donation_activity_error_retry_button.setOnClickListener {
            viewModel.onRetryLoadSKUsButtonClicked()
        }

        donation_loaded_recycler_view.layoutManager = LinearLayoutManager(this)
        donation_loaded_recycler_view.setHasFixedSize(true)

        viewModel.donationSuccessEvent.observe(this, { sku ->
            AlertDialog.Builder(this)
                .setTitle(R.string.donation_success_title)
                .setMessage(getString(R.string.donation_success_message, sku.price))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        })

        viewModel.errorPayingEvent.observe(this, { error ->
            AlertDialog.Builder(this)
                .setTitle(R.string.donation_error_title)
                .setMessage(getString(R.string.donation_error_message, error.message))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        })

        viewModel.stateEventStream.observe(this, { state ->
            when(state) {
                DonationViewModel.State.Loading -> {
                    donation_activity_error_view.visibility = View.GONE
                    donation_activity_loading_view.visibility = View.VISIBLE
                    donation_activity_loaded_view.visibility = View.GONE
                }
                is DonationViewModel.State.ErrorLoading -> {
                    donation_activity_error_view.visibility = View.VISIBLE
                    donation_activity_loading_view.visibility = View.GONE
                    donation_activity_loaded_view.visibility = View.GONE

                    donation_activity_error_view_text.text = getString(R.string.donation_loading_error, state.error.message)
                }
                is DonationViewModel.State.Load -> {
                    donation_activity_error_view.visibility = View.GONE
                    donation_activity_loading_view.visibility = View.GONE
                    donation_activity_loaded_view.visibility = View.VISIBLE

                    donation_loaded_recycler_view.adapter = DonationsAdapter(state.SKUs) { clickedSku ->
                        viewModel.onDonateButtonClicked(clickedSku, this)
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.donation_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.send_feedback_button -> startSupportEmailActivity()
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}