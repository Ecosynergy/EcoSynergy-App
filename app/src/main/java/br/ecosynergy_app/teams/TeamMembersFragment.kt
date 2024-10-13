package br.ecosynergy_app.teams

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.ecosynergy_app.R
import br.ecosynergy_app.RetrofitClient
import br.ecosynergy_app.room.AppDatabase
import br.ecosynergy_app.room.teams.Members
import br.ecosynergy_app.room.teams.MembersRepository
import br.ecosynergy_app.room.teams.TeamsRepository
import br.ecosynergy_app.room.user.UserRepository
import br.ecosynergy_app.user.MembersAdapter
import br.ecosynergy_app.user.UserViewModel
import br.ecosynergy_app.user.UserViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class TeamMembersFragment : Fragment(R.layout.fragment_team_members) {
    private lateinit var recycleMembers: RecyclerView
    lateinit var membersAdapter: MembersAdapter
    private lateinit var shimmerMembers: ShimmerFrameLayout

    private lateinit var userViewModel: UserViewModel
    private lateinit var teamsViewModel: TeamsViewModel

    private lateinit var swipeRefresh: SwipeRefreshLayout

    private lateinit var txtSearch: TextInputEditText
    private lateinit var btnAddMember: ImageButton

    private var membersList: List<Members> = listOf()
    private var memberIds: MutableList<Int> = mutableListOf()
    private var memberRoles: List<String> = listOf()
    private var currentUserRole: String? = ""

    private var userId: Int = 0
    private var teamId: Int = 0
    private var accessToken: String = ""
    private var teamHandle: String = ""
    private var userRole: String = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_team_members, container, false)

        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val userRepository = UserRepository(userDao)

        val teamsDao = AppDatabase.getDatabase(requireContext()).teamsDao()
        val teamsRepository = TeamsRepository(teamsDao)

        val membersDao = AppDatabase.getDatabase(requireContext()).membersDao()
        val membersRepository = MembersRepository(membersDao)

        teamsViewModel = ViewModelProvider(
            this,
            TeamsViewModelFactory(RetrofitClient.teamsService, teamsRepository, membersRepository)
        )[TeamsViewModel::class.java]
        userViewModel = ViewModelProvider(
            this,
            UserViewModelFactory(RetrofitClient.userService, userRepository)
        )[UserViewModel::class.java]

        teamId = requireArguments().getInt("TEAM_ID")
        teamHandle = requireArguments().getString("TEAM_HANDLE").toString()
        userId = requireArguments().getInt("USER_ID")
        accessToken = requireArguments().getString("ACCESS_TOKEN").toString()

        shimmerMembers = view.findViewById(R.id.shimmerMembers)
        recycleMembers = view.findViewById(R.id.recycleMembers)

        txtSearch = view.findViewById(R.id.txtSearch)
        btnAddMember = view.findViewById(R.id.btnAddMember)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        recycleMembers.layoutManager = LinearLayoutManager(requireContext())
        membersAdapter = MembersAdapter(
            emptyList(),
            emptyList(),
            currentUserRole,
            teamId,
            userId,
            accessToken,
            teamsViewModel,
            requireActivity(),
            this,
            memberIds
        )
        recycleMembers.adapter = membersAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeMembersInfo()
        setupSwipeRefresh()

        parentFragmentManager.setFragmentResultListener("requestKey", this) { key, _ ->
            if (key == "requestKey") {
                shimmerMembers.startShimmer()
                shimmerMembers.visibility = View.VISIBLE
                recycleMembers.visibility = View.GONE
                observeMembersInfo()
            }
        }

        txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterMembers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnAddMember.setOnClickListener {
            val memberIdsString = memberIds.joinToString(",")
            val addMembersBottomSheet = AddMembersBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("TEAM_HANDLE", teamHandle)
                    putInt("TEAM_ID", teamId)
                    putString("MEMBER_IDS", memberIdsString)
                    putInt("USER_ID", userId)
                    putString("ACCESS_TOKEN", accessToken)
                }
            }
            addMembersBottomSheet.show(parentFragmentManager, "AddMembersBottomSheet")
            Log.d("btnAddMember", "MemberIDS: $memberIdsString, $teamId")
        }
    }

    private fun observeMembersInfo() {
        teamsViewModel.allMembersDB.observe(viewLifecycleOwner) { membersInfo ->
            val userMember = membersInfo.find { it.userId == userId }
            userRole = userMember?.role.toString()
            if (userRole == "ADMINISTRATOR") {
                btnAddMember.visibility = View.VISIBLE
            } else {
                btnAddMember.visibility = View.GONE
            }
        }

        teamsViewModel.getMembersByTeamId(teamId)
        teamsViewModel.allMembersDB.observe(viewLifecycleOwner) { membersResponse ->
            memberIds = membersResponse.map { it.userId }.toMutableList()
            memberRoles = membersResponse.map { it.role }
            membersList = membersResponse

            val userMember = membersResponse.find { it.userId == userId }
            currentUserRole = userMember?.role

            shimmerMembers.visibility = View.VISIBLE
            recycleMembers.visibility = View.GONE

            val pairedMembers = membersResponse.map { member ->
                val role = membersResponse.find { it.userId.toString() == member.userId.toString() }?.role
                    ?: "Unknown"
                Pair(member, role)
            }.sortedBy { it.first.fullName }

            val sortedUsers = pairedMembers.map { it.first }
            val sortedRoles = pairedMembers.map { it.second }

            membersAdapter = MembersAdapter(
                sortedUsers,
                sortedRoles,
                currentUserRole,
                teamId,
                userId,
                accessToken,
                teamsViewModel,
                requireActivity(),
                this,
                memberIds
            )
            recycleMembers.adapter = membersAdapter

            shimmerMembers.animate().alpha(0f).setDuration(300).withEndAction {
                shimmerMembers.stopShimmer()
                shimmerMembers.animate().alpha(1f).setDuration(300)
                shimmerMembers.visibility = View.GONE
                recycleMembers.visibility = View.VISIBLE
            }

        }
    }

    private fun filterMembers(query: String) {
        val filteredList = membersList.filter {
            it.fullName.contains(query, ignoreCase = true) || it.username.contains(
                query,
                ignoreCase = true
            )
        }
        membersAdapter.updateList(filteredList, memberRoles)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            observeMembersInfo()
            swipeRefresh.isRefreshing = false
        }
    }
}