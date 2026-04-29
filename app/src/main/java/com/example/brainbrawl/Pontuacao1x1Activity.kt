package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacao1x1Binding
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.EstatisticasService
import com.example.brainbrawl.services.EstatisticasService.ResultadoJogador
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Pontuacao1x1Activity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPontuacao1x1Binding.inflate(layoutInflater)
    }
    private val database = FirebaseDatabase.getInstance().reference
    private val pontuacaoRepository = PontuacaoRepository()
    private lateinit var codigoSala: String
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var totalPontos: Double = 0.0
    private var totalRespostasCertas: Int = 0
    private var nomeCategoria: String = ""

    private var desforraListener: ValueEventListener? = null
    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null
    private var novaSalaListener: ValueEventListener? = null

    private var adversario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""

        // Chama a função para atualizar a pontuação do jogador
        carregarPontuacao1x1Realtime()
        // Chama a funçao para os jogadores jogarem novamente
        escutarNovaSalaDesforra()

        // Configura o botoa de voltar e desforra
        binding.btnVoltar.setOnClickListener {
            database.child("sala_1x1").child(codigoSala).removeValue()
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null })
            finish()
        }

        binding.btnDesforra.setOnClickListener {
            pedirDesforra()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removerListener(desforraListener, "jogadores")
        pontuacaoRepository.removerListener(pontuacaoListener)
        removerListener(novaSalaListener, "novaSalaDesforra")
    }

    // Chama a função para escutar a nova sala de desforra
    private fun escutarNovaSalaDesforra() {
        novaSalaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Guarda o novo código da sala
                val novaSala = snapshot.getValue(String::class.java)
                if (!novaSala.isNullOrEmpty()) {
                    // Envia de volta para a SalaDeEspera1x1Activity com o novo código da sala
                    val intent = Intent(this@Pontuacao1x1Activity, SalaDeEspera1x1Activity::class.java)
                    intent.putExtra(IntentExtras.CODIGO_SALA, novaSala)
                    intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                    intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("sala_1x1").child(codigoSala).child("novaSalaDesforra")
            .addValueEventListener(novaSalaListener!!)
    }

    // Chama a fubção para pedir desforra
    private fun pedirDesforra() {
        database.child("sala_1x1").child(codigoSala)
            .child("jogadores").child(nomeUtilizador).child("desforra").setValue(true)

        if (desforraListener == null) {
            val ref = database.child("sala_1x1").child(codigoSala).child("jogadores")
            desforraListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var desforraAceita = false
                    var outroJogador: String? = null
                    // Verifica se o outro jogador aceitou a desforra
                    for (child in snapshot.children) {
                        if (child.key != nomeUtilizador &&
                            child.child("desforra").getValue(Boolean::class.java) == true) {
                            desforraAceita = true
                            outroJogador = child.key
                            break
                        }
                    }
                    if (desforraAceita && outroJogador != null) {
                        removerListener(desforraListener, "jogadores")
                        criarSalaDesforra(outroJogador)
                    } else {
                        Toast.makeText(this@Pontuacao1x1Activity, "Aguardando o outro jogador aceitar desforra", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(desforraListener!!)
        }
    }

    // Função para criar uma nova sala de desforra
    private fun criarSalaDesforra(adversario: String) {
        val novoCodigoSala = gerarCodigoSala()
        val salaRef = database.child("sala_1x1").child(novoCodigoSala)

        salaRef.child("jogadores").child(nomeUtilizador).setValue(true)
        salaRef.child("jogadores").child(adversario).setValue(true)
        salaRef.child("admin").setValue(nomeUtilizador)
        salaRef.child("estado").setValue("em_espera")
        salaRef.child("nomeCategoria").setValue(nomeCategoria)
        salaRef.child("prontos").child(nomeUtilizador).setValue(true)
        salaRef.child("prontos").child(adversario).setValue(false)

        database.child("sala_1x1").child(codigoSala).child("jogadores").child(nomeUtilizador).child("desforra").removeValue()
        database.child("sala_1x1").child(codigoSala).child("jogadores").child(adversario).child("desforra").removeValue()

        database.child("sala_1x1").child(codigoSala).child("novaSalaDesforra").setValue(novoCodigoSala)
    }

    // Função para carregar a pontuação dos jogadores
    private fun carregarPontuacao1x1Realtime() {
        pontuacaoListener = pontuacaoRepository.escutarPontuacoes1x1(
            codigoSala = codigoSala,
            onPontuacoes = { jogadores ->
                atualizarUiPontuacoes(jogadores)
                atualizarEstatisticasJogadorAtual(jogadores)
            },
            onErro = {
                Toast.makeText(this@Pontuacao1x1Activity, "Erro ao carregar pontuação", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun atualizarUiPontuacoes(jogadores: List<ResultadoJogador>) {
        if (jogadores.isNotEmpty()) {
            binding.txtNomeJogador1.text = jogadores[0].nome
            binding.txtPontos1.text = jogadores[0].pontos.toInt().toString()
            if (jogadores[0].nome != nomeUtilizador) adversario = jogadores[0].nome
        }
        if (jogadores.size > 1) {
            binding.txtNomeJogador2.text = jogadores[1].nome
            binding.txtPontos2.text = jogadores[1].pontos.toInt().toString()
            if (jogadores[1].nome != nomeUtilizador) adversario = jogadores[1].nome
        }
        if (jogadores.size <= 1) {
            binding.txtNomeJogador2.text = "Aguardando adversário..."
            binding.txtPontos2.text = ""
        }
    }

    private fun atualizarEstatisticasJogadorAtual(jogadores: List<ResultadoJogador>) {
        val resultadosComRespostas = jogadores.map { jogador ->
            if (jogador.nome == nomeUtilizador) {
                jogador.copy(respostasCertas = totalRespostasCertas)
            } else {
                jogador
            }
        }

        pontuacaoRepository.atualizarEstatisticasSalaUmaVez(
            tipoSala = PontuacaoRepository.TipoSala.UM_CONTRA_UM,
            codigoSala = codigoSala,
            resultados = resultadosComRespostas,
            modo = EstatisticasService.Modo.UM_CONTRA_UM,
            totalPerguntas = 8,
            jogadoresParaAtualizar = setOf(nomeUtilizador)
        )
    }

    private fun removerListener(listener: ValueEventListener?, campo: String) {
        listener?.let {
            database.child("sala_1x1").child(codigoSala).child(campo).removeEventListener(it)
        }
    }
}
