package com.example.brainbrawl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brainbrawl.routes.UteisNavegacao.abrirMainActivity
import com.example.brainbrawl.config.FirebasePaths
import com.example.brainbrawl.config.GameConstants
import com.example.brainbrawl.utils.CodigoSalaUtils.gerarCodigoSala
import com.example.brainbrawl.config.IntentExtras
import com.example.brainbrawl.databinding.ActivityPontuacao1x1Binding
import com.example.brainbrawl.models.HistoricoJogo
import com.example.brainbrawl.repositories.HistoricoRepository
import com.example.brainbrawl.repositories.PontuacaoRepository
import com.example.brainbrawl.services.AuthService
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
    private val historicoRepository = HistoricoRepository()
    private val authService = AuthService()
    private lateinit var codigoSala: String
    private var uid: String = ""
    private lateinit var nomeUtilizador: String
    private var nomeJogador: String = ""
    private var totalPontos: Double = 0.0
    private var totalRespostasCertas: Int = 0
    private var totalPerguntas: Int = 8
    private var nomeCategoria: String = ""
    private var historicoGuardado = false

    private var desforraListener: ValueEventListener? = null
    private var pontuacaoListener: PontuacaoRepository.ListenerHandle? = null
    private var novaSalaListener: ValueEventListener? = null

    private var jogadorAtualResultado: ResultadoJogador? = null
    private var adversario: ResultadoJogador? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Guardar os dados passados pelo Intent
        codigoSala = intent.getStringExtra(IntentExtras.CODIGO_SALA) ?: ""
        uid = intent.getStringExtra(IntentExtras.UID) ?: authService.utilizadorAtual()?.uid ?: ""
        nomeUtilizador = intent.getStringExtra(IntentExtras.NOME_UTILIZADOR) ?: ""
        nomeJogador = intent.getStringExtra(IntentExtras.NOME_JOGADOR) ?: nomeUtilizador
        totalPontos = intent.getDoubleExtra(IntentExtras.TOTAL_PONTOS, 0.0)
        totalRespostasCertas = intent.getIntExtra(IntentExtras.TOTAL_RESPOSTAS_CERTAS, 0)
        totalPerguntas = intent.getIntExtra(IntentExtras.TOTAL_PERGUNTAS, 8)
        nomeCategoria = intent.getStringExtra(IntentExtras.NOME_CATEGORIA) ?: ""

        // Chama a função para atualizar a pontuação do jogador
        carregarPontuacao1x1Realtime()
        // Chama a funçao para os jogadores jogarem novamente
        escutarNovaSalaDesforra()

        // Configura o botoa de voltar e desforra
        binding.btnVoltar.setOnClickListener {
            database.child(FirebasePaths.SALA_1X1).child(codigoSala).removeValue()
            abrirMainActivity(this, nomeUtilizador.ifBlank { null }, nomeJogador.ifBlank { null }, uid.ifBlank { null })
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
                    uid.takeIf { it.isNotBlank() }?.let { intent.putExtra(IntentExtras.UID, it) }
                    intent.putExtra(IntentExtras.NOME_UTILIZADOR, nomeUtilizador)
                    intent.putExtra(IntentExtras.NOME_JOGADOR, nomeJogador)
                    intent.putExtra(IntentExtras.NOME_CATEGORIA, nomeCategoria)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child(FirebasePaths.SALA_1X1).child(codigoSala).child("novaSalaDesforra")
            .addValueEventListener(novaSalaListener!!)
    }

    // Chama a fubção para pedir desforra
    private fun pedirDesforra() {
        val chaveAtual = chaveSalaAtual()
        if (chaveAtual.isBlank()) return

        database.child(FirebasePaths.SALA_1X1).child(codigoSala)
            .child(FirebasePaths.JOGADORES).child(chaveAtual).child("desforra").setValue(true)

        if (desforraListener == null) {
            val ref = database.child(FirebasePaths.SALA_1X1).child(codigoSala).child(FirebasePaths.JOGADORES)
            desforraListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var desforraAceita = false
                    var outroJogador: ResultadoJogador? = null
                    // Verifica se o outro jogador aceitou a desforra
                    for (child in snapshot.children) {
                        val chave = child.key.orEmpty()
                        if (chave != chaveAtual &&
                            child.child("desforra").getValue(Boolean::class.java) == true) {
                            desforraAceita = true
                            outroJogador = child.toResultadoDesforra(chave)
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
    private fun criarSalaDesforra(adversario: ResultadoJogador) {
        val novoCodigoSala = gerarCodigoSala()
        val salaRef = database.child(FirebasePaths.SALA_1X1).child(novoCodigoSala)
        val chaveAtualNova = chavePrimariaAtual()
        val chaveAdversarioNova = adversario.identificadorEstatisticas
        if (chaveAtualNova.isBlank() || chaveAdversarioNova.isBlank()) return

        salaRef.updateChildren(
            mapOf(
                "${FirebasePaths.JOGADORES}/$chaveAtualNova" to dadosJogadorAtualDesforra(),
                "${FirebasePaths.JOGADORES}/$chaveAdversarioNova" to dadosJogadorDesforra(adversario),
                FirebasePaths.ADMIN to nomeDisplayAtual(),
                FirebasePaths.ADMIN_ID to chaveAtualNova,
                FirebasePaths.ADMIN_UID to uid,
                FirebasePaths.ESTADO to GameConstants.ESTADO_EM_ESPERA,
                FirebasePaths.NOME_CATEGORIA to nomeCategoria,
                "${FirebasePaths.PRONTOS}/$chaveAtualNova" to true,
                "${FirebasePaths.PRONTOS}/$chaveAdversarioNova" to false
            )
        ).addOnSuccessListener {
            database.child(FirebasePaths.SALA_1X1).child(codigoSala).child(FirebasePaths.JOGADORES)
                .child(chaveSalaAtual()).child("desforra").removeValue()
            database.child(FirebasePaths.SALA_1X1).child(codigoSala).child(FirebasePaths.JOGADORES)
                .child(adversario.chave.ifBlank { adversario.identificadorEstatisticas }).child("desforra").removeValue()

            database.child(FirebasePaths.SALA_1X1).child(codigoSala).child("novaSalaDesforra").setValue(novoCodigoSala)
        }
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
        jogadorAtualResultado = jogadores.firstOrNull { jogadorAtualCorresponde(it) } ?: jogadorAtualResultado
        adversario = jogadores.firstOrNull { !jogadorAtualCorresponde(it) } ?: adversario

        if (jogadores.isNotEmpty()) {
            binding.txtNomeJogador1.text = jogadores[0].nome
            binding.txtPontos1.text = jogadores[0].pontos.toInt().toString()
        }
        if (jogadores.size > 1) {
            binding.txtNomeJogador2.text = jogadores[1].nome
            binding.txtPontos2.text = jogadores[1].pontos.toInt().toString()
        }
        if (jogadores.size <= 1) {
            binding.txtNomeJogador2.text = "Aguardando adversário..."
            binding.txtPontos2.text = ""
        }
    }

    private fun atualizarEstatisticasJogadorAtual(jogadores: List<ResultadoJogador>) {
        guardarHistoricoSeNecessario(jogadores)
        val resultadosComRespostas = jogadores.map { jogador ->
            if (jogadorAtualCorresponde(jogador)) {
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
            totalPerguntas = totalPerguntas,
            jogadoresParaAtualizar = identificadoresJogadorAtual().toSet()
        )
    }

    private fun guardarHistoricoSeNecessario(jogadores: List<ResultadoJogador>) {
        if (historicoGuardado || uid.isBlank() || jogadores.size < 2) return
        val atual = jogadores.firstOrNull { jogadorAtualCorresponde(it) } ?: return
        val outro = jogadores.firstOrNull { !jogadorAtualCorresponde(it) } ?: return

        historicoGuardado = true
        historicoRepository.guardarHistoricoUmaVez(
            uid = uid,
            historico = HistoricoJogo(
                historicoId = "${GameConstants.MODO_1X1}_$codigoSala",
                modo = GameConstants.MODO_1X1,
                codigoSala = codigoSala,
                nomeCategoria = nomeCategoria,
                pontuacao = atual.pontos,
                respostasCertas = totalRespostasCertas,
                totalPerguntas = totalPerguntas,
                venceu = atual.pontos > outro.pontos,
                empate = atual.pontos == outro.pontos,
                dataHora = System.currentTimeMillis(),
                jogadoresDaPartida = jogadores.map { it.nome }
            )
        ).addOnFailureListener {
            historicoGuardado = false
        }
    }

    private fun removerListener(listener: ValueEventListener?, campo: String) {
        listener?.let {
            database.child(FirebasePaths.SALA_1X1).child(codigoSala).child(campo).removeEventListener(it)
        }
    }

    private fun chaveSalaAtual(): String {
        return jogadorAtualResultado?.chave?.takeIf { it.isNotBlank() } ?: chavePrimariaAtual()
    }

    private fun chavePrimariaAtual(): String {
        return uid.ifBlank { nomeJogador.ifBlank { nomeUtilizador } }
    }

    private fun nomeDisplayAtual(): String {
        return nomeUtilizador.ifBlank { nomeJogador.ifBlank { uid } }
    }

    private fun jogadorAtualCorresponde(jogador: ResultadoJogador): Boolean {
        return identificadoresJogadorAtual().any { jogador.corresponde(it) }
    }

    private fun identificadoresJogadorAtual(): List<String> {
        return listOf(
            uid,
            jogadorAtualResultado?.chave.orEmpty(),
            nomeUtilizador,
            nomeJogador,
            nomeDisplayAtual()
        ).filter { it.isNotBlank() }.distinct()
    }

    private fun dadosJogadorAtualDesforra(): Map<String, Any> {
        val dados = dadosJogadorBase(nomeDisplayAtual())
        if (uid.isNotBlank()) dados[FirebasePaths.UID] = uid
        if (nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = nomeUtilizador
        if (nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = nomeJogador
        return dados
    }

    private fun dadosJogadorDesforra(resultado: ResultadoJogador): Map<String, Any> {
        val dados = dadosJogadorBase(resultado.nome)
        if (resultado.uid.isNotBlank()) dados[FirebasePaths.UID] = resultado.uid
        if (resultado.nomeUtilizador.isNotBlank()) dados[FirebasePaths.NOME_UTILIZADOR] = resultado.nomeUtilizador
        if (resultado.nomeJogador.isNotBlank()) dados[FirebasePaths.NOME_JOGADOR] = resultado.nomeJogador
        return dados
    }

    private fun dadosJogadorBase(nomeDisplay: String): LinkedHashMap<String, Any> {
        return linkedMapOf(
            FirebasePaths.NOME to nomeDisplay,
            FirebasePaths.NOME_DISPLAY to nomeDisplay,
            FirebasePaths.PONTUACAO to 0.0,
            FirebasePaths.TOTAL_RESPOSTAS_CERTAS to 0,
            FirebasePaths.ESTADO to GameConstants.ESTADO_ON,
            FirebasePaths.IS_HOST_ONLY to false
        )
    }

    private fun DataSnapshot.toResultadoDesforra(chave: String): ResultadoJogador {
        return ResultadoJogador(
            nome = nomeDisplay().ifBlank { chave },
            pontos = 0.0,
            uid = child(FirebasePaths.UID).texto(),
            chave = chave,
            nomeUtilizador = child(FirebasePaths.NOME_UTILIZADOR).texto(),
            nomeJogador = child(FirebasePaths.NOME_JOGADOR).texto()
        )
    }

    private fun DataSnapshot.nomeDisplay(): String {
        return child(FirebasePaths.NOME_DISPLAY).texto()
            .ifBlank { child(FirebasePaths.NOME_UTILIZADOR).texto() }
            .ifBlank { child(FirebasePaths.NOME_JOGADOR).texto() }
            .ifBlank { child(FirebasePaths.NOME).texto() }
    }

    private fun DataSnapshot.texto(): String {
        return getValue(String::class.java).orEmpty()
    }
}
