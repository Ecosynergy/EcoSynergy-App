package br.ecosynergy_app.home.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.ecosynergy_app.R
import br.ecosynergy_app.teams.CreateTeamActivity
import br.ecosynergy_app.teams.TeamAdapter
import br.ecosynergy_app.teams.viewmodel.TeamsViewModel
import br.ecosynergy_app.user.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class TeamsFragment : Fragment() {

    private val teamsViewModel: TeamsViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var teamsAdapter: TeamAdapter
    private lateinit var btnAddTeam: ImageButton
    private lateinit var linearAlert: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var userId: Int = 0
    private var userIdentifier: String = ""
    private var accessToken: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teams, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        teamsAdapter = TeamAdapter(emptyList())

        btnAddTeam = view.findViewById(R.id.btnAddTeam)
        linearAlert = view.findViewById(R.id.linearAlert)

        progressBar = view.findViewById(R.id.loadingProgressBar)

        linearAlert.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d("TeamsFragment", "onResume OK")
        teamsViewModel.getAllTeamsFromDB()
        observeTeamsData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTeamsData()
        observeUserData()
        btnAddTeam.setOnClickListener {
            val i = Intent(requireContext(), CreateTeamActivity::class.java)
            i.apply {
                putExtra("USER_ID", userId)
                putExtra("USER_IDENTIFIER", userIdentifier)
                putExtra("ACCESS_TOKEN", accessToken)
            }
            startActivity(i)
        }
        setupSwipeRefresh()
    }

    private fun observeUserData() {
        userViewModel.userInfo.observe(viewLifecycleOwner){user ->
            accessToken = user.accessToken
            userIdentifier = user.username
            userId = user.id
        }
    }

    private fun observeTeamsData() {
        // Start collecting the Flow of team data
        viewLifecycleOwner.lifecycleScope.launch {
            teamsViewModel.getAllTeamsFromDB().collect { teamData ->
                // Initialize the adapter with the collected team data
                teamsAdapter = TeamAdapter(teamData)
                recyclerView.adapter = teamsAdapter

                // Check if the team data is empty
                if (teamData.isEmpty()) {
                    linearAlert.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    linearAlert.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
                // Hide the progress bar after data is loaded
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            teamsViewModel.getAllTeamsFromDB()
            observeTeamsData()
            swipeRefresh.isRefreshing = false
        }
    }

}
