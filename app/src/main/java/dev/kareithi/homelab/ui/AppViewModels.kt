package dev.kareithi.homelab.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.kareithi.homelab.HomelabApp
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel
import dev.kareithi.homelab.ui.settings.SettingsViewModel

/** ViewModel factory wired to the manual [dev.kareithi.homelab.di.AppContainer]. */
object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(homelabApp().container) }
        initializer { SettingsViewModel(homelabApp().container, homelabApp()) }
    }
}

private fun CreationExtras.homelabApp(): HomelabApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HomelabApp
