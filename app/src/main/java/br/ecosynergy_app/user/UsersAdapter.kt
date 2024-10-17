package br.ecosynergy_app.user

import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import br.ecosynergy_app.R
import br.ecosynergy_app.home.HomeActivity
import br.ecosynergy_app.teams.viewmodel.RoleRequest
import br.ecosynergy_app.teams.viewmodel.TeamsViewModel

class UsersAdapter(
    private var usersList: MutableList<UserResponse>,
    private var teamId: Int,
    private var teamHandle: String,
    private val teamsViewModel: TeamsViewModel,
    private val memberIds: List<String>,
    private val accessToken: String,
    private val userId: Int,
    private val username: String,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    fun updateList(newList: MutableList<UserResponse>) {
        usersList = newList.sortedBy { it.fullName }.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.users_layout, parent, false)
        return ViewHolder(view, usersList, teamId, teamHandle, teamsViewModel, memberIds, accessToken, userId, username, activity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(usersList[position])
    }

    override fun getItemCount(): Int = usersList.size

    class ViewHolder(
        itemView: View,
        private var usersList: MutableList<UserResponse>,
        private var teamId: Int,
        private var teamHandle: String,
        private val teamsViewModel: TeamsViewModel,
        private val memberIds: List<String>,
        private val accessToken: String,
        private val userId: Int,
        private var username: String,
        private val activity: FragmentActivity
    ) : RecyclerView.ViewHolder(itemView) {

        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val txtFullname: TextView = itemView.findViewById(R.id.txtFullname)
        private val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
        private val imgUser: ImageView = itemView.findViewById(R.id.imgUser)
        private val btnInvite: ImageButton = itemView.findViewById(R.id.btnInvite)
        private var memberId: Int = 0

        fun bind(user: UserResponse) {
            username = user.username

            txtFullname.text = user.fullName
            txtUsername.text = "@$username"
            txtEmail.text = user.email

            memberId = user.id

            imgUser.setImageResource(HomeActivity().getDrawableForLetter(user.fullName.first()))

            imgUser.setOnClickListener{
                val i = Intent(activity, UserInfoActivity::class.java)
                i.apply {
                    putExtra("USERNAME", user.username)
                    putExtra("FULLNAME", user.fullName)
                    putExtra("EMAIL", user.email)
                    putExtra("GENDER", user.gender)
                    putExtra("NATIONALITY", user.nationality)
                    putExtra("FULLNAME", user.fullName)
                    putExtra("CREATED", user.createdAt)
                }
                //activity.startActivity(i)
            }

            Log.d("UsersAdapter", "MemberIDS: $memberIds")

            if (memberIds.contains(memberId.toString())) {
                btnInvite.visibility = View.GONE
            } else {
                btnInvite.visibility = View.VISIBLE
            }

            btnInvite.setOnClickListener {
                inviteUser()
            }
        }

        private fun inviteUser(){
            val builder = AlertDialog.Builder(itemView.context)
            builder.setTitle("Você deseja convidar este  usuário para a equipe atual?")
            builder.setMessage("O convite será enviado para este usuário.")

            builder.setPositiveButton("Sim") { dialog, _ ->
                teamsViewModel.addMember(accessToken, teamId, memberId, RoleRequest("COMMON_USER"))
                btnInvite.visibility = View.GONE

              //  LoginActivity().showSnackBar("Usuário convidado com sucesso!", "FECHAR", R.color.greenDark, activity)
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()


        }
    }
}
