package br.ecosynergy_app.teams

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.ecosynergy_app.R
import br.ecosynergy_app.RetrofitClient
import br.ecosynergy_app.home.HomeActivity
import br.ecosynergy_app.room.AppDatabase
import br.ecosynergy_app.room.teams.Members
import br.ecosynergy_app.room.teams.MembersRepository
import br.ecosynergy_app.room.teams.TeamsRepository
import br.ecosynergy_app.room.user.UserRepository
import br.ecosynergy_app.user.UserViewModel
import br.ecosynergy_app.user.UserViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class TeamOverviewFragment : Fragment(R.layout.fragment_team_overview) {

    private lateinit var userViewModel: UserViewModel
    private lateinit var teamsViewModel: TeamsViewModel

    private lateinit var teamPicture: CircleImageView

    private lateinit var txtTeamName: TextView
    private lateinit var txtHandle: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtTimezone: TextView
    private lateinit var txtActivities: TextView
    private lateinit var txtDailyGoal: TextView
    private lateinit var txtWeeklyGoal: TextView
    private lateinit var txtMonthlyGoal: TextView
    private lateinit var txtAnnualGoal: TextView

    private lateinit var btnEdit: LinearLayout

    private lateinit var btnDelete: MaterialButton

    private lateinit var shimmerImg: ShimmerFrameLayout

    private var utcToTextMap: Map<String?, String> = mapOf()

    private var userId: Int = 0
    private var userRole : String = ""
    private var accessToken: String = ""

    private var teamName: String = ""
    private var teamId: Int = 0
    private var teamHandle: String = ""
    private var members: List<Members> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_team_overview, container, false)

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

        teamPicture = view.findViewById(R.id.teamPicture)
        txtTeamName = view.findViewById(R.id.txtTeamName)
        txtHandle = view.findViewById(R.id.txtHandle)
        txtTimezone = view.findViewById(R.id.txtTimezone)
        txtDescription = view.findViewById(R.id.txtDescription)
        txtActivities = view.findViewById(R.id.txtActivities)

        txtDailyGoal = view.findViewById(R.id.txtDailyGoal)
        txtWeeklyGoal = view.findViewById(R.id.txtWeeklyGoal)
        txtMonthlyGoal = view.findViewById(R.id.txtMonthlyGoal)
        txtAnnualGoal = view.findViewById(R.id.txtAnnualGoal)

        btnEdit = view.findViewById(R.id.btnEdit)

        btnDelete = view.findViewById(R.id.btnDelete)

        shimmerImg = view.findViewById(R.id.shimmerImg)

        txtTimezone.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtHandle.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtTeamName.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtDailyGoal.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtMonthlyGoal.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtWeeklyGoal.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))
        txtAnnualGoal.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled))


        val timezones = loadTimezones()
        utcToTextMap = timezones.associate { it.utc.firstOrNull() to it.text }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeTeamInfo()

        btnDelete.text = "Excluir $teamHandle"
        btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Você deseja excluir $teamHandle?")
            builder.setMessage("Se excluir esta equipe, perderá todos os dados armazenados nela.")

            builder.setPositiveButton("Sim") { dialog, _ ->
                deleteTeam()
                dialog.dismiss()
                requireActivity().supportFragmentManager.popBackStack()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        btnEdit.setOnClickListener {
            val i = Intent(requireContext(), EditTeamActivity::class.java)
            i.apply {
                putExtra("accessToken", accessToken)
                putExtra("teamid", teamId)
                putExtra("teamName", txtTeamName.text.toString())
                putExtra("teamHandle", txtHandle.text.toString())
                putExtra("teamDescription", txtDescription.text.toString())
                putExtra("teamTimezone", txtTimezone.text.toString())
                putExtra("teamSector", txtActivities.text.toString())
                putExtra("dailyGoal", txtDailyGoal.text.toString())
                putExtra("weeklyGoal", txtWeeklyGoal.text.toString())
                putExtra("monthlyGoal", txtMonthlyGoal.text.toString())
                putExtra("annualGoal", txtAnnualGoal.text.toString())
            }
            startActivity(i)
        }
    }

    private fun deleteTeam() {
        teamsViewModel.deleteTeam(accessToken, teamId)
    }

    private fun observeTeamInfo() {
        teamsViewModel.getTeamById(teamId)
        teamsViewModel.teamDB.observe(viewLifecycleOwner) { teamInfo ->
            teamsViewModel.getMembersByTeamId(teamId)
            teamsViewModel.allMembersDB.observe(viewLifecycleOwner) { membersInfo ->

                teamId = teamInfo.id
                teamName = teamInfo.name
                members = membersInfo

                val userMember = members.find { it.userId == userId }
                userRole = userMember?.role.toString()

                btnEdit.visibility = if (userRole == "ADMINISTRATOR") View.VISIBLE else View.GONE
                btnDelete.visibility = if (userRole == "ADMINISTRATOR") View.VISIBLE else View.GONE

                shimmerImg.visibility = View.VISIBLE
                teamPicture.visibility = View.GONE

                val drawableId = HomeActivity().getDrawableForLetter(teamName.first())
                teamPicture.setImageResource(drawableId)
                txtTeamName.text = teamName
                txtHandle.text = teamInfo.handle
                txtDescription.text = teamInfo.description

                txtDailyGoal.text = teamInfo.dailyGoal.toString()
                txtWeeklyGoal.text = teamInfo.weeklyGoal.toString()
                txtMonthlyGoal.text = teamInfo.monthlyGoal.toString()
                txtAnnualGoal.text = teamInfo.annualGoal.toString()

                txtTimezone.text = utcToTextMap[teamInfo.timeZone]

                val sectors = loadSectorsAndActivities()

                val activityId = teamInfo.activityId

                val activity = sectors.flatMap { it.activities }.find { it.activities_id == activityId }
                val sector = sectors.find { it.sector == teamInfo.activitySector }

                val activityNameBr = activity?.activities_br ?: "Atividade Desconhecida"
                val sectorNameBr = sector?.sector_br ?: "Setor Desconhecido"

                Log.d("TeamOverviewFragment", "Setor: $sectorNameBr Atividade: $activityNameBr")

                txtActivities.text = "$sectorNameBr/$activityNameBr"

                shimmerImg.animate().alpha(0f).setDuration(300).withEndAction {
                    shimmerImg.stopShimmer()
                    shimmerImg.animate().alpha(1f).setDuration(300)
                    shimmerImg.visibility = View.GONE
                    teamPicture.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadTimezones(): List<Timezone> {
        val jsonString = getJson("timezones.json")
        val gson = Gson()
        val listType = object : TypeToken<List<Timezone>>() {}.type
        return gson.fromJson(jsonString, listType)
    }

    private fun loadSectorsAndActivities(): List<Sector> {
        val jsonFileString = getJson("sectors.json")
        val gson = Gson()
        val listSectorType = object : TypeToken<List<Sector>>() {}.type
        return gson.fromJson(jsonFileString, listSectorType)
    }

    private fun getJson(fileName: String): String? {
        return try {
            val inputStream = requireContext().assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }
}