package br.ecosynergy_app.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.ecosynergy_app.room.TeamsRepository
import br.ecosynergy_app.room.UserRepository

class TeamsViewModelFactory(private val service: TeamsService,
                            private val teamsRepository: TeamsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamsViewModel::class.java)) {
            return TeamsViewModel(service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}