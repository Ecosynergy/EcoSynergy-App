package br.ecosynergy_app.home.homefragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.ecosynergy_app.R
import br.ecosynergy_app.teams.CreateTeamBottomSheet
import br.ecosynergy_app.teams.TeamAdapter
import br.ecosynergy_app.teams.TeamsViewModel

class Teams : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var teamsAdapter: TeamAdapter
    private val teamsViewModel: TeamsViewModel by activityViewModels()

    private lateinit var btnAddTeam: ImageButton
    private lateinit var linearAlert: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_teams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        teamsAdapter = TeamAdapter(emptyList())

        btnAddTeam = view.findViewById(R.id.btnAddTeam)
        linearAlert = view.findViewById(R.id.linearAlert)

        linearAlert.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        observeTeamsData()
        btnAddTeam.setOnClickListener{
            val createTeamBottomSheet = CreateTeamBottomSheet()
            createTeamBottomSheet.show(parentFragmentManager, "CreateTeamBottomSheet")
        }
    }

    private fun observeTeamsData() {
        teamsViewModel.teamsResult.observe(viewLifecycleOwner) { result ->
            Log.d("TeamsFragment", "TeamsResult: $result")
            result.onSuccess { teamData ->
                teamsAdapter = TeamAdapter(teamData)
                recyclerView.adapter = teamsAdapter
                if (teamData.isNotEmpty()) {
                    linearAlert.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }.onFailure { e ->
                Log.e("TeamsFragment", "Error", e)
            }
        }
    }

}
