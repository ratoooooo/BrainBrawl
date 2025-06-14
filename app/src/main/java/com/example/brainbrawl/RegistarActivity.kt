package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.Uteis.hashPassword
import com.example.brainbrawl.databinding.ActivityRegistarBinding
import com.google.firebase.database.FirebaseDatabase

class RegistarActivity : AppCompatActivity() {
    // Acessar os elementos do layout
    private val binding by lazy {
        ActivityRegistarBinding.inflate(layoutInflater)
    }
    // Acessar a base de dados
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        var avatarSelecionadoIndex = 0

        // Varivel para armazenar os avatares
        val avatarResources = arrayOf(
            R.drawable.avatar_1_playstore,
            R.drawable.avatar_2_playstore,
            R.drawable.avatar_3_playstore,
            R.drawable.avatar_4_playstore,
            R.drawable.avatar_5_playstore,
            R.drawable.avatar_6_playstore,
            //R.drawable.avatar_7_playstore,
            R.drawable.avatar_9_playstore,
            R.drawable.avatar_10_playstore,
            R.drawable.avatar_11_playstore,
            R.drawable.avatar_12_playstore,
            R.drawable.avatar_13_playstore
        )

        // Adapter para o GridView
        val gridAdapter = AvatarGridAdapter(this, avatarResources)
        binding.gridAvatars.adapter = gridAdapter

        // Inicializa o avatar selecionado
        binding.imgAvatarSelecionado.setImageResource(avatarResources[avatarSelecionadoIndex])

        // Seleção do avatar na grelha
        binding.gridAvatars.setOnItemClickListener { _, _, position, _ ->
            avatarSelecionadoIndex = position
            binding.imgAvatarSelecionado.setImageResource(avatarResources[position])
        }

        // Configurar botão de registo
        binding.btnRegistar.setOnClickListener {
            // GGuardar os dados inseridos nos campos de texto
            val nomeUtilizador = binding.edtNomeJogador.text.toString().trim()
            val password = binding.edtPasswordJogador.text.toString().trim()

            // Validar os campos
            val erro = Uteis.validarCampos(nomeUtilizador, password)
            // Verificar se há erros
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Acessar a base de dados e ao nó "jogadores"
            database.child("jogadores").child(nomeUtilizador).get()
                .addOnSuccessListener { snapshot ->
                    //Verificar se jogador já existe
                    if (snapshot.exists()) {
                        Toast.makeText(this, "Jogador já existe", Toast.LENGTH_SHORT).show()
                    } else {
                        //Chamar a função para adicionar o jogador
                        adicionarJogador(nomeUtilizador, password, avatarSelecionadoIndex)
                        // Abrir LoginActivity e passar o nome do utilizador
                        var intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("nomeUtilizador", nomeUtilizador)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    // Exibir mensagem de erro
                    Toast.makeText(this, "Erro ao verificar jogador: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Configurar botão de voltar
        binding.btnVoltar.setOnClickListener {
            // Abrir LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    // Função para adicionar o jogador ao Firebase com senha encriptada
    private fun adicionarJogador(nomeUtilizador: String, password: String, avatarSelecionadoIndex: Int) {
        val hashedPassword = hashPassword(password)
        val nomeAvatar = "avatar_${avatarSelecionadoIndex + 1}_playstore"
        val jogadorData = mapOf(
            "password" to hashedPassword,
            "avatar" to nomeAvatar,
            "pontuacao" to 0.0,
            "totalJogos" to 0,
            "totalVitorias" to 0,
            "totalRespostasCertas" to 0
        )
        database.child("jogadores").child(nomeUtilizador).setValue(jogadorData)
    }


}