package br.ecosynergy_app.teams

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.ecosynergy_app.R
import br.ecosynergy_app.RetrofitClient
import br.ecosynergy_app.user.MembersAdapter
import br.ecosynergy_app.user.UserViewModel
import br.ecosynergy_app.user.UserViewModelFactory
import br.ecosynergy_app.user.UsersAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class AddMembersBottomSheet : BottomSheetDialogFragment() {

    private lateinit var btnClose: ImageButton
    private lateinit var txtMember: TextInputEditText
    private lateinit var btnSearch: ImageButton

    private lateinit var teamsViewModel: TeamsViewModel
    private lateinit var userViewModel: UserViewModel

    private lateinit var recycleUsers: RecyclerView
    lateinit var usersAdapter: UsersAdapter
    private lateinit var shimmerUsers: ShimmerFrameLayout

    private var token: String? = ""
    private var teamId: String? = ""
    private var teamHandle: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_addmembers_bottom_sheet, container, false)

        teamsViewModel = ViewModelProvider(requireActivity(), TeamsViewModelFactory(RetrofitClient.teamsService))[TeamsViewModel::class.java]
        userViewModel = ViewModelProvider(requireActivity(), UserViewModelFactory(RetrofitClient.userService))[UserViewModel::class.java]

        btnClose = view.findViewById(R.id.btnClose)

        txtMember = view.findViewById(R.id.txtMember)
        btnSearch = view.findViewById(R.id.btnSearch)

        shimmerUsers = view.findViewById(R.id.shimmerUsers)
        recycleUsers = view.findViewById(R.id.recycleUsers)

        recycleUsers.layoutManager = LinearLayoutManager(requireContext())
        usersAdapter = UsersAdapter(mutableListOf(), teamId, teamHandle, teamsViewModel, requireActivity(), this)
        recycleUsers.adapter = usersAdapter

        val sp: SharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        token = sp.getString("accessToken", null)

        teamHandle = arguments?.getString("TEAM_HANDLE")
        teamId = arguments?.getString("TEAM_ID")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = 2000
        }

        btnClose.setOnClickListener{ dismiss() }

        btnSearch.setOnClickListener{
            searchUser(txtMember.text.toString())
        }
    }

    private fun searchUser(username : String){
        userViewModel.searchUser(token, username)
        userViewModel.users.observe(viewLifecycleOwner){ result ->
            result.onSuccess { response ->
                shimmerUsers.visibility = View.VISIBLE
                recycleUsers.visibility = View.GONE

                usersAdapter = UsersAdapter(response, teamId, teamHandle, teamsViewModel, requireActivity(), this)
                recycleUsers.adapter = usersAdapter

                shimmerUsers.animate().alpha(0f).setDuration(300).withEndAction {
                    shimmerUsers.stopShimmer()
                    shimmerUsers.animate().alpha(1f).setDuration(300)
                    shimmerUsers.visibility = View.GONE
                    recycleUsers.visibility = View.VISIBLE
                }
            }.onFailure { error->
                    error.printStackTrace()
                    Log.d("TeamOverviewFragment", "User Result Failed: ${error.message}")
                    shimmerUsers.visibility = View.VISIBLE
                    recycleUsers.visibility = View.GONE
                    showSnackBar("ERRO: Carregar usuários", "FECHAR", R.color.red)
            }

        }
    }

    private fun showSnackBar(message: String, action: String, bgTint: Int) {
        val rootView = requireView()
        val snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
            .setAction(action) {}
        snackBar.setBackgroundTint(ContextCompat.getColor(requireContext(), bgTint))
        snackBar.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        snackBar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        snackBar.show()
    }

}