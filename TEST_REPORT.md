# Pergunta o Luso - TEST_REPORT

Data: 2026-05-08

## Firebase Rules hardening - 2026-05-08

### Resumo da revisao

- `firebase-rules.json` foi revisto contra os paths usados por repositories de jogadores, amigos, categorias, salas, jogo competitivo, pontuacao, historico, ranking e matchmaking.
- A ronda focou em reduzir abuso sem alterar Kotlin/UI nem quebrar convidados, legado, convites, salas ou matchmaking.
- As rules continuam a validar formato/ownership/limites basicos; a justica de pontuacao, XP e ranking continua pendente de backend autoritativo.

### Paths endurecidos

- `jogadores/{jogadorId}`: bloqueia chaves `guest_*`, mantendo UID-first para Auth e fallback legado por password.
- `categoriasPublicas/{categoriaId}`: exige Auth para writes, bloqueia convidados de publicar/avaliar/incrementar usos e impede nao-criadores de alterar perguntas/metadados estruturais.
- `jogadores/{uid}/categoriasPersonalizadas`: exige Auth no proprio UID ou fallback legado explicito por `nomeUtilizador/password`.
- `sala_2x2/{codigo}/equipaA` e `equipaB`: campos arbitrarios removidos; apenas identidade temporaria conhecida e aceite.
- `matchmaking/{modo}/resultados` e `matchmaking/{modo}/matches`: `jogadores` e `matches` passaram a validar campos esperados e bloquear `$other`.

### Paths ainda permissivos por compatibilidade

- `jogadores` continua `.read=true` para ranking, pesquisa social, login legado e fallback por `nomeUtilizador`.
- `salas`, `sala_1x1`, `sala_2x2` e `matchmaking` mantem leitura aberta porque convidados, sala por codigo, podios e ecrã de matchmaking dependem disso.
- Salas ainda tem fallbacks `auth == null` para convidados e fluxos antigos de jogo.
- `matchmaking/{modo}` mantem write no nivel do modo porque o cliente atual faz transacao no node inteiro para criar matches.
- Perfis legados com `password` mantem alguma escrita sem Auth para nao quebrar compatibilidade antiga.

### Convidados e estatisticas

- Convidado pode continuar a jogar em estruturas temporarias de sala/matchmaking.
- Convidado nao pode criar `jogadores/guest_*`.
- Convidado nao escreve `historicoJogos`.
- Convidado nao publica/avalia categorias publicas.
- A garantia de nao gravar estatisticas/XP/ranking permanece dupla: Kotlin exige `uid` valido e nao guest, e rules bloqueiam perfil `guest_*`.

### Validacoes executadas

- `python3 -m json.tool firebase-rules.json`
  - OK; JSON valido.
- Pesquisa de permissividades restantes com `rg` para `.read=true`, `auth == null`, `.indexOn`, `guest_` e `$other`.
  - OK; permissoes restantes foram documentadas.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Testes manuais recomendados

- Auth/registo: criar conta nova, confirmar `jogadores/{uid}`, perfil/avatar, logout/login.
- Legado: testar login antigo por `nomeUtilizador/password`, se ainda estiver ativo no ambiente.
- Convidado: jogar sala/matchmaking e confirmar ausencia de `jogadores/guest_*`, historico e XP.
- Grupo/1x1/2x2: criar/entrar/jogar ate podio, com Auth e convidados.
- Amigos/convites: pedido, aceitar, remover, convite 1x1, convite 2x2.
- Categorias: Auth cria/edita/publica/avalia/guarda; guest deve ser bloqueado em publicar/avaliar/guardar.
- Ataques basicos: tentar escrever perfil alheio, pontuacao negativa, campo inesperado, historico de outro uid, avaliacao com uid diferente e categoria publica de outro criador.

### Riscos pendentes

- Results/XP/ranking continuam client-side; rules nao conseguem provar legitimidade de jogo.
- Leitura global de `jogadores` permanece por ranking/pesquisa/login legado.
- Matchmaking ainda e cliente-side e precisa de Cloud Functions para selecao autoritativa.
- Fallbacks `auth == null` em salas continuam necessarios enquanto convidados sem Firebase Anonymous Auth forem suportados.

## Fecho competitivo 1x1/2x2, desforra e podio - 2026-05-08

### Problemas encontrados

- `Pontuacao1x1Activity` e `Pontuacao2x2Activity` ainda apagavam diretamente `sala_1x1/{codigoSala}` e `sala_2x2/{codigoSala}` no botao Voltar.
- `Pontuacao1x1Activity` concentrava logica Firebase de desforra, criacao de sala nova, listeners e limpeza de flags.
- `Pontuacao2x2Activity` mostrava Toasts de vencedor/espera dentro do listener realtime, repetindo mensagens sempre que a sala mudava.
- As Activities ja tinham protecoes por `uid`, mas foram reforcadas com `tipoJogador/isGuest` para impedir convidados de gravar estatisticas/historico.

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/Pontuacao1x1ViewModel.kt`

### Ficheiros alterados nesta fase

- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/res/layout/activity_pontuacao1x1.xml`
- `app/src/main/res/layout/activity_pontuacao_multi.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Fluxo 1x1

- O botao Voltar deixou de chamar `removeValue()` na sala; agora apenas regressa a Main/Login com `uid`/nomes preservados.
- A sala antiga permanece disponivel no Firebase depois de um jogador sair do podio.
- A desforra escreve apenas `jogadores/{playerKey}/desforra=true`, observa aceitacao do adversario e cria/navega para uma nova `sala_1x1`.
- A nova sala de desforra preserva `uid`, `playerKey`, `tipoJogador`, `isGuest`, `nomeDisplay`, `nomeUtilizador`, `nomeJogador` e `avatar` quando existem.
- O estado visual mostra "A aguardar adversario..." ou "Desforra aceite. A criar nova sala..." sem depender de Toast repetido.

### Fluxo 2x2

- O botao Voltar deixou de chamar `removeValue()` em `sala_2x2/{codigoSala}`.
- O resultado final passou a ser mostrado em `txtResultado2x2`: "Vitoria da Equipa A!", "Vitoria da Equipa B!" ou "Empate!".
- Enquanto o podio esta incompleto, `txtEstadoPontuacao` mostra "A aguardar todos os jogadores terminarem... X/4".
- Os Toasts repetidos de vencedor/espera foram removidos; manteve-se apenas o Toast pontual de novo recorde para autenticados.

### Protecao de convidados e anti-duplicacao

- `Pontuacao1x1Activity` e `Pontuacao2x2Activity` so gravam estatisticas/historico quando `uid` esta preenchido, `isGuest=false` e `tipoJogador != guest`.
- Convidados continuam a aparecer no podio, mas nao consultam recorde, nao gravam historico e nao atualizam `jogadores`.
- `PontuacaoRepository.atualizarProximoJogador` ignora resultados sem `uid`, evitando resolver `guestKey` ou nome de convidado como perfil persistente.
- Flags locais `estatisticasAtualizadas`, `historicoGuardado`, `recordeConsultado` e `navegouParaDesforra` reduzem chamadas repetidas quando listeners disparam novamente ou a Activity recria.
- A criacao da desforra usa transacao em `novaSalaDesforra`; se outra instancia ja criou a sala, o fluxo reutiliza o codigo existente.
- A sala antiga nao e apagada ao pedir desforra, aceitar desforra ou voltar do podio.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK em verificacao intermedia apos as alteracoes de codigo.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Testes manuais recomendados

- 1x1 autenticado: confirmar podio, Voltar sem apagar `sala_1x1/{codigoSala}`, estatisticas uma vez por jogador.
- 1x1 desforra: jogador A pede, jogador B aceita, uma unica nova sala e ambos entram na mesma `SalaDeEspera1x1Activity`.
- 1x1 com convidado: convidado aparece no podio/desforra, mas nao cria `jogadores/{guestKey}` nem grava XP/estatisticas/historico.
- 2x2 autenticado: podio completo mostra resultado fixo, sem spam de Toast, e Voltar nao apaga `sala_2x2/{codigoSala}`.
- 2x2 misto: convidados aparecem no podio, mas apenas autenticados com UID gravam dados persistentes.
- Recriacao/rotacao: confirmar que estatisticas, historico e navegacao de desforra nao duplicam.

### Riscos pendentes

- A limpeza definitiva/TTL de salas antigas continua pendente e deve ser feita por mecanismo dedicado, idealmente Cloud Functions ou rotina administrativa.
- A desforra continua cliente-side; a transacao evita duplicacao normal, mas validacao autoritativa contra cliente malicioso exigiria backend/rules mais fortes.
- Firebase Rules nao foram alteradas nesta fase.
- Matchmaking, convites, formula de pontuacao, XP, rankings, historico funcional e categorias nao foram reestruturados nesta fase.

## Matchmaking real, convidados e avatares - 2026-05-08

### Estrutura Firebase final

- `matchmaking/1x1/fila/{playerKey}` e `matchmaking/2x2/fila/{playerKey}` guardam `playerKey`, `uid`, `tipoJogador`, `nomeUtilizador`, `nomeJogador`, `nomeDisplay`, `avatar`, `timestampEntrada`, `estado` e `isGuest`.
- `matchmaking/1x1/resultados/{playerKey}` e `matchmaking/2x2/resultados/{playerKey}` guardam `codigoSala`, `modo`, `nomeCategoria`, `criadorId`, `criadorUid`, `estado=encontrado`, `timestampEntrada` e `jogadores/{playerKey}`.
- `matchmaking/{modo}/matches/{matchId}` guarda o claim transacional usado para evitar matches duplicados.
- 1x1 cria sala real em `sala_1x1/{codigoSala}` com 2 jogadores, admin/adminId/adminUid, `estado=em_espera`, `nomeCategoria` e `prontos`.
- 2x2 cria sala real em `sala_2x2/{codigoSala}` com 4 jogadores, admin/adminId/adminUid, `estado=em_espera` e `nomeCategoria`; equipas continuam a ser definidas pelo fluxo existente ao iniciar.

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/models/MatchmakingModels.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/utils/AvatarUtils.kt`

### Ficheiros alterados nesta fase

- `MatchmakingActivity.kt` e `activity_matchmaking.xml`
- `SalaDeEspera1x1Activity.kt`, `SalaDeEspera2x2Activity.kt`, `Sala1x1ViewModel.kt`, `Sala2x2ViewModel.kt`
- `Jogo1x1Activity.kt`, `Jogo2x2Activity.kt`, `Jogo1x1ViewModel.kt`, `Jogo2x2ViewModel.kt`
- `Pontuacao1x1Activity.kt`, `Pontuacao2x2Activity.kt`, `PontuacoesActivity.kt`, `PontuacaoRepository.kt`
- `JogadorSalaIdentidade.kt`, `JogoCompetitivoRepository.kt`, `UteisNavegacao.kt`
- `FirebasePaths.kt`, `GameConstants.kt`, `IntentExtras.kt`
- `MainActivity.kt`, `MeuPerfilActivity.kt`, `PerfilAmigoActivity.kt`, `AmigoAdapter.kt`, `JogadoresSalaAdapter.kt`, `RegistarActivity.kt`, `RegistarViewModel.kt`
- `firebase-rules.json`, `ARCHITECTURE_PLAN.md`, `FIREBASE_RULES_NOTES.md`, `TEST_REPORT.md`

### Autenticados vs convidados

- Autenticado: `playerKey = uid`, `uid` preenchido, `tipoJogador=auth`, `nomeDisplay` vem de `nomeUtilizador`/`nomeJogador`/`uid`, avatar vem do perfil.
- Convidado: `uid` vazio, `tipoJogador=guest`, `playerKey = guest_{nome}_{timestamp}`, `nomeDisplay = nomeJogador`, avatar fallback.
- O `playerKey` e passado para sala, jogo e pontuacao para nao depender apenas do nome visual.

### Garantia de convidados sem estatisticas

- Convidados aparecem em sala, jogo e podio, mas nao criam perfil em `jogadores`.
- `Pontuacao1x1Activity`, `Pontuacao2x2Activity` e `PontuacoesActivity` so chamam atualizacao de estatisticas quando `uid` nao esta vazio.
- `PontuacaoRepository` tambem ignora resultados sem `uid`, evitando fallback por nome que poderia atingir um perfil real indevido.
- `HistoricoRepository` ja rejeita `uid` vazio, e os ecras de pontuacao mantem esse guard.

### Race conditions mitigadas

- Entrada usa `playerKey` como chave unica; mesmo UID substitui a propria entrada.
- Ao entrar num modo, remove a entrada/resultado do mesmo `playerKey` no outro modo.
- `onDisconnect` remove a fila se a ligacao cair.
- Entradas stale com mais de 2 minutos sao limpas ao entrar e dentro da transacao de claim.
- O claim de match corre em transacao em `matchmaking/{modo}` e so prossegue se o jogador que tentou criar esta entre os selecionados mais antigos.
- `matches/{matchId}` evita claim duplicado do mesmo conjunto.
- Navegacao e cancelamento usam flags para evitar eventos duplicados.
- Cancelar remove fila se nao houver resultado; se ja houver resultado, mostra que a partida ja foi encontrada e nao apaga a sala.

### Avatares

- `AvatarUtils` centraliza a resolucao: normaliza strings, aceita `avatar_1_playstore`, `avatar_1`, extensoes e prefixos `@drawable/`/`@mipmap/`.
- Procura primeiro em `drawable`, depois `mipmap`, e faz fallback seguro para `R.drawable.avatar_1_playstore`.
- Registo continua alinhado: index 0 grava `avatar_1_playstore`, index 7 grava `avatar_8_playstore`, index 11 grava `avatar_12_playstore`.

### Comandos executados

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK.
- `python3 -m json.tool firebase-rules.json`
  - OK.

Avisos mantidos: `SalaRepository.kt:84 Parameter 'adminHint' is never used` e deprecated Gradle features.

### Testes manuais recomendados

- 1x1 com duas contas Auth: fila, sala `sala_1x1`, resultados, entrada na mesma sala e inicio.
- 1x1 convidado + Auth: convidado joga, aparece no podio e nao cria/atualiza `jogadores`.
- 2x2 com quatro contas: nenhuma sala com 3 jogadores; sala criada ao quarto jogador; admin inicia.
- 2x2 misto com convidados: sala com 4 jogadores, convidados visiveis e sem estatisticas.
- Cancelar antes/depois do match.
- Mesmo UID em dois dispositivos e alternancia rapida entre 1x1/2x2.
- Avatar `avatar_1_playstore`, `avatar_8_playstore`, vazio e invalido em Main/perfis/amigos.

### Riscos pendentes

- Matchmaking ainda e cliente-side. Rules reduzem writes malformados, mas Cloud Functions seriam necessarias para anti-abuso forte e escolha autoritativa dos jogadores.
- Se o cliente que ganhou o claim fechar exatamente antes de criar a sala/publicar resultados, o claim pode ficar em `criando` ate uma limpeza futura/manual.
- Os fluxos antigos de convites continuam preservados, mas tambem continuam com as limitacoes de seguranca client-side ja documentadas.

## Estabilizacao tecnica e UX pequena - 2026-05-08

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/drawable/ic_history.xml`
- `app/src/main/res/drawable/ic_logout.xml`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `TEST_REPORT.md`

### Problemas corrigidos

- Recriado `ExampleUnitTest.kt` em `app/src/test`, porque a arvore de testes unitarios nao existia neste checkout apesar de estar referida em relatorios anteriores.
- O teste de estatisticas foi atualizado para construir `EstatisticasService.EstatisticasAtuais` com `recordePontuacao`.
- O empate 2x2 ficou coberto por teste com a regra atual: `textoVencedor2x2(150.0, 150.0)` devolve `Empate!` e `vencedores(..., DOIS_CONTRA_DOIS)` devolve `emptySet()`.
- O logout/sair na Main deixou de estar escondido como `1dp`/`gone`; o mesmo `btn_voltar` agora e uma acao visivel no cabecalho com descricao `Terminar sessao`.
- A logica existente de logout foi preservada: se existir `uid` ou `nomeUtilizador`, chama `jogadorRepository.marcarOffline`; depois chama `authService.terminarSessao()`, abre `LoginActivity` e faz `finish()`. Para convidado, volta ao login sem tentar marcar offline.
- O botao `btn_historico` passou a ter `contentDescription="Historico"` e icone proprio de historico, mantendo a abertura de `HistoricoActivity`.
- Em `JogoActivity`, as quatro opcoes passam a ser bloqueadas depois de resposta ou timeout, e desbloqueadas ao apresentar uma nova pergunta.
- O feedback visual de resposta foi mantido: resposta correta verde, resposta errada vermelha e resposta correta sempre visivel.

### Verificacoes executadas

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK. Testes unitarios compilados e executados.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK.

### Avisos e pendencias

- O wrapper Gradle falhou no sandbox ao tentar aceder a `~/.gradle`; os comandos foram executados com o JBR do Android Studio fora do sandbox.
- Mantem-se o warning conhecido: `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Mantem-se o aviso generico de deprecated Gradle features.
- Matchmaking real nao foi implementado nesta fase.
- Firebase Rules nao foram alteradas nesta fase.
- Pontuacao, ScoreService, XP, rankings, historico funcional, categorias e regras de estatisticas nao foram alterados nesta fase, exceto pela reposicao/atualizacao dos testes unitarios desatualizados.
- Segurança Firebase autoritativa continua pendente para uma fase propria.

## Auditoria completa desconfiada - UI, fluxo e dados - 2026-05-07

### Estado geral

- Projeto compila com Kotlin/XML/ViewBinding depois da auditoria.
- Login/registo, Main, perfil, ranking, historico, convites, salas e pontuacoes mantem os mesmos ViewModels/repositories/paths principais.
- Nao foram alteradas Firebase rules nesta ronda.
- Foram corrigidos apenas bugs pequenos e diretos em UI, navegacao, fallback e estatisticas.

### Problemas encontrados

- **Critico**: `MatchmakingActivity` existe e e aberta pela Main, mas neste checkout nao existe `MatchmakingRepository`, `MatchmakingViewModel` nem chamada para entrar/cancelar fila. Resultado: 1x1/2x2 aleatorio mostram ecrã de procura, mas nao fazem matchmaking real.
- **Medio**: Main mostrava avatar, nivel e XP default/hardcoded do XML, nao os dados reais do jogador.
- **Medio**: item `Perfil` no bottom nav da Main nao tinha ID/listener funcional.
- **Medio**: `Entrar numa Sala` tinha listener no Kotlin, mas o botao ficou invisivel no XML (`1dp`, `gone`) durante o redesign.
- **Medio**: empate 2x2 ainda dava vitoria estatistica a Equipa A em `EstatisticasService.vencedores()`.
- **Medio**: grelha de avatar do registo mostrava `avatar_8_playstore` no fim, mas o ViewModel grava por `avatar_${index + 1}_playstore`, podendo gravar avatar diferente do escolhido.
- **Baixo**: dialogs de dicas ainda usavam fundo programatico `#FFC400` em `UteisDicas.kt`.
- **Baixo**: `PontuacoesActivity` ainda usava `#FFC400` na medalha de primeiro lugar.
- **Baixo**: ranking carregava ate 100 jogadores por defeito, apesar da UI/objetivo ser top 10.
- **Baixo**: layouts `activity_convidar_amigo.xml` e `activity_convidar_amigo2x2.xml` ainda estavam crus/sem fundo premium.
- **Sugestao futura**: pontuacao/estatisticas continuam confiadas ao cliente. Um cliente adulterado ainda pode tentar escrever resultados validos pelas rules. Correcao robusta exige backend autoritativo/Cloud Functions.

### Correcoes aplicadas

- `MainActivity` agora carrega perfil por UID/nome com `JogadorRepository.obterPerfil()` e atualiza nome, avatar, nivel, XP atual e XP necessario.
- `activity_main.xml` passou a ter defaults seguros (`avatar_1`, nivel 1, 0/300 XP) enquanto o perfil real carrega.
- Bottom nav da Main ganhou IDs/listeners corretos para `Ranking` e `Perfil`.
- `Entrar numa Sala` voltou a ser um botao visivel, mantendo o ID `btn_entrar_sala` e o listener existente.
- `MeuPerfilActivity` ganhou fallback de avatar e botao voltar funcional mesmo quando o perfil nao carrega.
- `RegistarActivity` passou a mostrar avatares em ordem 1-12, alinhado com o nome gravado no perfil.
- `EstatisticasService.vencedores()` deixou de atribuir vencedores em empate 2x2.
- `UteisDicas.kt` passou a usar cores do design system em vez de amarelo agressivo.
- Medalha de primeiro lugar em `PontuacoesActivity` passou para dourado suave.
- `RankingRepository` passou a carregar top 10 por defeito.
- Layouts de convite 1x1/2x2 receberam fundo premium, titulo e surfaces/botao alinhados com o design system.

### Ficheiros alterados nesta auditoria

- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
- `app/src/main/java/com/example/brainbrawl/UteisDicas.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/RankingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_convidar_amigo.xml`
- `app/src/main/res/layout/activity_convidar_amigo2x2.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Ficheiros/areas analisados sem alteracao direta

- Auth/registo: `LoginActivity.kt`, `RegistarViewModel.kt`, `AuthService.kt`, `JogadorRepository.kt`.
- Perfil/ranking/historico: `MeuPerfilViewModel.kt`, `RankingActivity.kt`, `RankingViewModel.kt`, `HistoricoActivity.kt`, `HistoricoRepository.kt`.
- Amigos/convites: `AmigosActivity.kt`, `AmigosViewModel.kt`, `AmigosRepository.kt`, adapters sociais.
- Salas/jogo/pontuacao: `Sala*Activity.kt`, `Sala*ViewModel.kt`, `Jogo*Activity.kt`, `Jogo*ViewModel.kt`, `PontuacaoRepository.kt`.
- Firebase: `FirebasePaths.kt` e `firebase-rules.json`.
- UI: layouts principais em `res/layout`, drawables partilhados e styles/themes.

### Riscos pendentes

- Matchmaking aleatorio esta incompleto no codigo deste checkout; corrigir exige implementar repository/ViewModel/fila/sala/cancelamento.
- As rules permitem writes client-side de resultados/estatisticas dentro de contratos validos; isto nao protege contra cliente malicioso.
- Alguns nomes internos ainda usam `BrainBrawl` em package/theme/style (`Theme.BrainBrawl`, `BrainBrawlButton`), mas nao sao texto visivel de UI.
- Ecras com listas longas dependem de RecyclerView; ecras longos principais usam ScrollView, mas teste visual manual em 360dp deve ser repetido no emulador.

### Verificacoes executadas nesta auditoria

- Pesquisa de amarelo/laranja antigo em `res`/Kotlin.
- Pesquisa de texto visivel `BrainBrawl`.
- Verificacao de JSON das Firebase rules com `python3 -m json.tool`.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK durante a auditoria apos as correcoes de ViewBinding.
- Sequencia completa `clean`, `assembleDebug`, `testDebugUnitTest` e `build`
  - OK. `testDebugUnitTest` continua sem fontes de teste (`NO-SOURCE`).
  - Mantem o warning conhecido: `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
  - Mantem o aviso generico de deprecated Gradle features.

## Redesign Pergunta o Luso - registo em 2 passos e auditoria UI - 2026-05-07

### Ficheiros alterados nesta ronda

- `app/src/main/res/layout/activity_registar.xml`
- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/drawable/bg_dica_card.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Fluxo de registo atualizado

- `RegistarActivity` continua a ser a Activity unica de registo e continua a chamar `RegistarViewModel.registar(nomeUtilizador, email, password, avatarSelecionadoIndex)` no passo final.
- O ecrã foi dividido visualmente em `page_conta` e `page_perfil`.
- Passo 1: email, palavra-passe, confirmar palavra-passe, card de requisitos, botao `CONTINUAR` e link para login.
- Passo 2: nome de utilizador, avatar selecionado em destaque, grelha `grid_avatars`, botao `CRIAR CONTA` e botao `Voltar`.
- O botao de voltar superior regressa ao passo 1 quando o passo 2 esta visivel; no passo 1 regressa ao login.
- A validacao de criacao de conta, Firebase Auth, criacao de perfil no Realtime Database e navegacao para Main permanecem no ViewModel/fluxo existente.

### Imagem da app

- Confirmado que `avatar_14_foreground.webp` existe em `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi` e `mipmap-xxxhdpi`.
- `activity_login.xml` e `activity_registar.xml` usam `@mipmap/avatar_14_foreground` como imagem visual da app.
- Nao foram introduzidos logotipos inventados nem texto visivel "BrainBrawl".

### Auditoria visual

- Verificados layouts principais em `res/layout` e referencias de drawables/cores antigas.
- Encontrados ecras de dicas/instrucoes ainda dependentes do card antigo atraves de `bg_dica_card.xml`, especialmente `activity_escolha_categoria_modos.xml` e `activity_tipo_modo_classico.xml`.
- `bg_dica_card.xml` foi atualizado para surface cream, raio maior e stroke dourado subtil, alinhando esses ecras com o design system sem alterar o fluxo.
- As referencias restantes a `botao_branco_arredondado`, `botao_voltar` e `bg_app_gradient` sao aceitaveis porque esses drawables partilhados ja foram redesenhados para o visual premium clean.
- As referencias restantes a `BrainBrawl` aparecem em nomes internos de styles/themes (`Theme.BrainBrawl`, `BrainBrawlButton`) e pacote Android; nao sao texto visivel na UI.
- `activity_main.xml` recebeu `android:orientation` explicito num `LinearLayout` oculto para resolver o erro de lint sem alterar comportamento visual.

### Verificacao manual

- Login abre com o novo visual, nome "Pergunta o Luso" e imagem `avatar_14_foreground`.
- Navegacao Login -> Registo verificada no emulador.
- Passo 1 do Registo verificado com campos email/password/confirmacao, card de requisitos, botao `CONTINUAR` e link de login.
- Preenchimento de email e passwords iguais verificado; o botao `CONTINUAR` avanca para o passo 2.
- Passo 2 verificado com campo de nome, avatar selecionado e grelha de avatares.
- A criacao Firebase completa e a verificacao de perfil/avatar na Main nao foram concluidas manualmente nesta ronda por instabilidade do daemon ADB durante a navegacao; o caminho de criacao de conta foi preservado e compilado.
- Responsividade: o registo usa `ScrollView`/`fillViewport` e conteudo com alturas fixas confortaveis para evitar cortes; teste manual em ecrã pequeno especifico nao foi concluido por causa da mesma instabilidade ADB.

### Problemas encontrados

- O ADB apresentou instabilidade recorrente no sandbox, exigindo execucoes escaladas para instalar e inspecionar o emulador.
- Mantem-se o warning conhecido de `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Mantem-se o aviso generico de deprecated Gradle features.

### Verificacoes executadas nesta ronda

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK antes desta atualizacao documental, validando ViewBinding do registo em 2 passos.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK. Mantem o warning conhecido: `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK. Sem fontes de teste unitario debug neste checkout (`NO-SOURCE`).
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK depois de corrigir o erro de lint `Orientation` em `activity_main.xml`.

## Rollback visual da Fase 2 UI/UX - 2026-05-07

### Ficheiros revertidos nesta ronda

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/activity_registar.xml`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `TEST_REPORT.md`

### O que foi revertido

- `MainActivity`, `LoginActivity` e `RegistarActivity` voltaram visualmente ao estado da Fase 1, mantendo a base comum aprovada (`bg_app_gradient`, `bg_button_*`, `bg_input_surface`, cores e dimens).
- Removidos os IDs e ligacoes Kotlin adicionados apenas pela Fase 2 visual: card de perfil, card extra de ranking, avatar/nivel no hub e helpers visuais associados.
- Removido `windowSoftInputMode="adjustResize"` que tinha sido introduzido no Manifest apenas por causa da Fase 2.
- Mantidos os IDs funcionais antigos usados por ViewBinding: login/registo, criar sala, entrar sala, categorias, matchmaking 1x1/2x2, ranking, historico, amigos e logout.
- Nao foram alterados repositories, services, viewmodels, Firebase rules, ranking, matchmaking, XP, recordes ou pontuacao.
- `ARCHITECTURE_PLAN.md` nao foi alterado por este rollback, porque nao houve mudanca arquitetural.

### Verificacoes executadas apos este rollback

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK. Mantem o warning conhecido: `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK. Sem testes unitarios debug compilados neste checkout (`NO-SOURCE`).
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK. Mantem o warning conhecido de `adminHint` e o aviso generico de deprecated Gradle features.

## Auditoria completa pre-funcionalidades - 2026-05-03

### Estado geral

- Projeto em estado funcional para os fluxos principais reportados: Auth, sessao persistente, registo, salas, jogos, pontuacoes, XP/niveis, rankings e perfil.
- A migracao hibrida UID/Auth esta consistente no essencial: dados novos autenticados privilegiam `uid`; `nomeUtilizador` permanece como display/fallback para dados antigos e convidados.
- Repositories/ViewModels ja absorvem grande parte da logica que antes estaria nas Activities, embora ainda existam ficheiros grandes e Activities com responsabilidades de UI + navegacao + pequenos efeitos Firebase.

### Problemas encontrados

- **Medio**: em `Pontuacao2x2Activity.kt`, o toast `NOVO RECORD!` comparava a pontuacao do jogo com `pontuacao` acumulada, nao com `recordePontuacao`. Isto podia impedir feedback correto de novo recorde.
- **Medio**: em empate 2x2, `EstatisticasService.textoVencedor2x2()` mostra `Empate!`, mas `vencedores()` considera Equipa A vencedora para estatisticas (`totalA >= totalB`). Nao foi alterado porque mudaria regras de vitoria, XP e ranking.
- **Medio**: em `firebase-rules.json`, `categoriasPublicas` ainda tem write amplo quando `usos` aumenta ou quando uma avaliacao aparece em `newData`; as validacoes de tipo existem, mas a rule nao limita estritamente a mudanca ao campo pretendido.
- **Baixo**: `AmigosRepository.atualizarConviteEssencial()` deixa a atualizacao secundaria de convite como melhor esforco; se falhar, pode existir divergencia temporaria entre copia recebida e enviada.
- **Baixo**: `Pontuacao2x2Activity.kt` usava string hardcoded `"sala_2x2"` no botao voltar.
- **Baixo**: `SalaRepository.garantirJogadorNaSala()` mantem `adminHint` sem uso real. O comportamento e intencional para confiar nos dados da sala, mas o warning permanece.
- **Sugestao futura**: `CategoriaRepository.kt`, `AmigosRepository.kt`, `JogoCompetitivoRepository.kt` e `PontuacaoRepository.kt` continuam grandes; dividir apenas quando houver uma razao funcional clara.
- **Sugestao futura**: writes sensiveis de resultados/estatisticas continuam client-side; seguranca forte exigiria Cloud Functions/backend autoritativo.

### Correcoes aplicadas

- `PontuacaoRepository.kt`: adicionado metodo `obterRecordePontuacaoJogador()` para ler `recordePontuacao`.
- `Pontuacao2x2Activity.kt`: o toast de novo recorde passou a comparar contra `recordePontuacao`.
- `Pontuacao2x2Activity.kt`: substituido path hardcoded por `FirebasePaths.SALA_2X2`.

### Verificacoes executadas nesta auditoria

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK. Mantem o warning conhecido: `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK. Mantem o warning conhecido de `adminHint` e o aviso generico de deprecated Gradle features.

## Auditoria - pontuacao, estatisticas, ranking e XP

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `TEST_REPORT.md`

### Causas exatas encontradas

- A pontuacao global do perfil estava a ser atualizada por maximo (`maxOf`) em vez de acumulacao; isto impedia somar resultados de jogos sucessivos (ex.: jogo com 2100 nao era somado ao total ja existente).
- No fluxo de pontuacao de grupo (`PontuacoesActivity`), a app tentava atualizar estatisticas/XP de todos os jogadores a partir de um unico cliente.
- Com regras por `auth.uid` em `jogadores/{uid}`, esse padrao podia falhar para jogadores terceiros e interromper o fluxo, deixando pontuacao/XP por gravar.

### Correcao aplicada

- `EstatisticasService.calcularAtualizacao` passou a somar pontuacao: `pontuacaoAtual + pontosDoJogo`.
- `PontuacoesActivity` passou a atualizar apenas o jogador atual (`jogadoresParaAtualizar`), alinhado com o padrao ja usado em `Pontuacao1x1Activity` e `Pontuacao2x2Activity`.
- Manteve-se a anti-duplicacao existente por sala em `estatisticasAtualizadas/{identificador}`.
- Regras de pontuacao do jogo e regras de XP mantidas sem alteracoes.

### Verificacao de regras Firebase

- Confirmado que `firebase-rules.json` ja permite e valida os campos:
  - `pontuacao`
  - `totalJogos`
  - `totalVitorias`
  - `totalVitoriasModoSolo`
  - `totalVitoriasModo1x1`
  - `totalVitoriasModo2x2`
  - `totalRespostasCertas`
  - `taxaAcertos`
  - `xpTotal`
  - `nivel`
  - `xpNoNivelAtual`
  - `xpNecessarioProximoNivel`

## Sistema de XP + Niveis

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/models/Jogador.kt`
- `app/src/main/java/com/example/brainbrawl/models/RankingJogador.kt`
- `app/src/main/java/com/example/brainbrawl/services/ProgressaoService.kt`
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MeuPerfilViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RankingAdapter.kt`
- `app/src/main/res/layout/activity_meu_perfil.xml`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `firebase-rules.json`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### O que foi implementado

- Sistema progressivo de XP separado da pontuacao:
  - `+50 XP` por jogo terminado
  - `+100 XP` extra por vitoria
  - `+10 XP` por resposta certa
- Regra de nivel progressiva aplicada:
  - `xpNecessario = 300 + ((nivelAtual - 1) * 150)`
- `ProgressaoService` novo para:
  - calcular XP ganho
  - calcular `nivel`, `xpNoNivelAtual` e `xpNecessarioProximoNivel` a partir de `xpTotal`
  - suportar multiplos niveis ganhos de uma vez
- Integracao no fluxo existente de estatisticas em `EstatisticasService.calcularAtualizacao` para atualizar:
  - `xpTotal`
  - `nivel`
  - `xpNoNivelAtual`
  - `xpNecessarioProximoNivel`
- Anti-duplicacao preservada:
  - continua a usar os marcadores transacionais `estatisticasAtualizadas/{identificador}` por sala
- Convidados continuam sem criar perfil e sem ganhar XP, pois apenas perfis resolvidos em `jogadores/{uid|legado}` sao atualizados.
- Compatibilidade com perfis antigos:
  - fallback de leitura para `xpTotal=0`, `nivel=1`, `xpNoNivelAtual=0`, `xpNecessarioProximoNivel=300`
- UI do perfil atualizada para mostrar:
  - `Nível X`
  - `XP atual / XP necessário`
- Ranking mostra nivel de forma simples junto ao nome (`Nv X`).
- Firebase Rules atualizadas para validar campos XP/nivel como numeros nao negativos (nivel >= 1).

## Correcao 2x2 - vitorias por modo

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `TEST_REPORT.md`

### Causa exata

- Em `Pontuacao2x2Activity`, o fluxo 2x2 chamava `atualizarEstatisticasSalaUmaVez` sem `jogadoresParaAtualizar`, tentando atualizar estatisticas de todos os jogadores a partir de um unico cliente.
- Com `uid` como chave principal e rules atuais, cada cliente so deve atualizar o proprio perfil em `jogadores/{uid}`; no 1x1 esse filtro ja existia.
- Isso podia impedir o incremento correto de `totalVitoriasModo2x2` para os vencedores.

### O que foi corrigido

- `Pontuacao2x2Activity` passou a atualizar estatisticas apenas para o jogador atual (`jogadoresParaAtualizar = identificadoresJogadorAtual().toSet()`), alinhado com o padrao do 1x1.
- A atualizacao ficou condicionada a podio completo (`podio.size >= 4`) para evitar calcular vencedores com resultados parciais.
- Foi adicionado controlo local `estatisticasAtualizadas` para evitar chamadas repetidas durante o mesmo ciclo de vida da Activity; a protecao transacional em `estatisticasAtualizadas/{identificador}` continua ativa para reaberturas.
- Regra de empate 2x2 foi mantida sem alteracoes.

## Auditoria - pontuacao, estatisticas, ranking e XP

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `TEST_REPORT.md`

### Causas exatas encontradas

- A pontuacao global do perfil estava a ser atualizada por maximo (`maxOf`) em vez de acumulacao; isto impedia somar resultados de jogos sucessivos (ex.: jogo com 2100 nao era somado ao total ja existente).
- No fluxo de pontuacao de grupo (`PontuacoesActivity`), a app tentava atualizar estatisticas/XP de todos os jogadores a partir de um unico cliente.
- Com regras por `auth.uid` em `jogadores/{uid}`, esse padrao podia falhar para jogadores terceiros e interromper o fluxo, deixando pontuacao/XP por gravar.

### Correcao aplicada

- `EstatisticasService.calcularAtualizacao` passou a somar pontuacao: `pontuacaoAtual + pontosDoJogo`.
- `PontuacoesActivity` passou a atualizar apenas o jogador atual (`jogadoresParaAtualizar`), alinhado com o padrao ja usado em `Pontuacao1x1Activity` e `Pontuacao2x2Activity`.
- Manteve-se a anti-duplicacao existente por sala em `estatisticasAtualizadas/{identificador}`.
- Regras de pontuacao do jogo e regras de XP mantidas sem alteracoes.

### Verificacao de regras Firebase

- Confirmado que `firebase-rules.json` ja permite e valida os campos:
  - `pontuacao`
  - `totalJogos`
  - `totalVitorias`
  - `totalVitoriasModoSolo`
  - `totalVitoriasModo1x1`
  - `totalVitoriasModo2x2`
  - `totalRespostasCertas`
  - `taxaAcertos`
  - `xpTotal`
  - `nivel`
  - `xpNoNivelAtual`
  - `xpNecessarioProximoNivel`

## Sistema de XP + Niveis

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/models/Jogador.kt`
- `app/src/main/java/com/example/brainbrawl/models/RankingJogador.kt`
- `app/src/main/java/com/example/brainbrawl/services/ProgressaoService.kt`
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MeuPerfilViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RankingAdapter.kt`
- `app/src/main/res/layout/activity_meu_perfil.xml`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `firebase-rules.json`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### O que foi implementado

- Sistema progressivo de XP separado da pontuacao:
  - `+50 XP` por jogo terminado
  - `+100 XP` extra por vitoria
  - `+10 XP` por resposta certa
- Regra de nivel progressiva aplicada:
  - `xpNecessario = 300 + ((nivelAtual - 1) * 150)`
- `ProgressaoService` novo para:
  - calcular XP ganho
  - calcular `nivel`, `xpNoNivelAtual` e `xpNecessarioProximoNivel` a partir de `xpTotal`
  - suportar multiplos niveis ganhos de uma vez
- Integracao no fluxo existente de estatisticas em `EstatisticasService.calcularAtualizacao` para atualizar:
  - `xpTotal`
  - `nivel`
  - `xpNoNivelAtual`
  - `xpNecessarioProximoNivel`
- Anti-duplicacao preservada:
  - continua a usar os marcadores transacionais `estatisticasAtualizadas/{identificador}` por sala
- Convidados continuam sem criar perfil e sem ganhar XP, pois apenas perfis resolvidos em `jogadores/{uid|legado}` sao atualizados.
- Compatibilidade com perfis antigos:
  - fallback de leitura para `xpTotal=0`, `nivel=1`, `xpNoNivelAtual=0`, `xpNecessarioProximoNivel=300`
- UI do perfil atualizada para mostrar:
  - `Nível X`
  - `XP atual / XP necessário`
- Ranking mostra nivel de forma simples junto ao nome (`Nv X`).
- Firebase Rules atualizadas para validar campos XP/nivel como numeros nao negativos (nivel >= 1).

## Correcao 2x2 - vitorias por modo

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `TEST_REPORT.md`

### Causa exata

- Em `Pontuacao2x2Activity`, o fluxo 2x2 chamava `atualizarEstatisticasSalaUmaVez` sem `jogadoresParaAtualizar`, tentando atualizar estatisticas de todos os jogadores a partir de um unico cliente.
- Com `uid` como chave principal e rules atuais, cada cliente so deve atualizar o proprio perfil em `jogadores/{uid}`; no 1x1 esse filtro ja existia.
- Isso podia impedir o incremento correto de `totalVitoriasModo2x2` para os vencedores.

### O que foi corrigido

- `Pontuacao2x2Activity` passou a atualizar estatisticas apenas para o jogador atual (`jogadoresParaAtualizar = identificadoresJogadorAtual().toSet()`), alinhado com o padrao do 1x1.
- A atualizacao ficou condicionada a podio completo (`podio.size >= 4`) para evitar calcular vencedores com resultados parciais.
- Foi adicionado controlo local `estatisticasAtualizadas` para evitar chamadas repetidas durante o mesmo ciclo de vida da Activity; a protecao transacional em `estatisticasAtualizadas/{identificador}` continua ativa para reaberturas.
- Regra de empate 2x2 foi mantida sem alteracoes.

## Ranking por Modo

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/RankingActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RankingAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/models/RankingJogador.kt`
- `app/src/main/java/com/example/brainbrawl/models/RankingTipo.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/RankingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/RankingViewModel.kt`
- `app/src/main/res/layout/activity_ranking.xml`
- `app/src/main/res/layout/item_ranking_jogador.xml`
- `firebase-rules.json`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi implementado

- Reutilizacao do fluxo existente `RankingActivity` -> `RankingViewModel` -> `RankingRepository` -> `RankingAdapter`.
- Novo `RankingTipo` para centralizar tipo de ranking, campo Firebase (`orderByChild`), titulo e label do valor principal.
- Alternancia simples por botoes: `Global`, `Solo`, `1x1`, `2x2`.
- Rankings disponiveis:
  - `Global` por `pontuacao`
  - `Recorde` por `recordePontuacao`
  - `Solo` por `totalVitoriasModoSolo`
  - `1x1` por `totalVitoriasModo1x1`
  - `2x2` por `totalVitoriasModo2x2`
- Compatibilidade mantida com dados antigos: campos ausentes passam a `0`, sem quebrar perfis legados.
- Convidados/perfis invalidos continuam ignorados no ranking.
- `firebase-rules.json` atualizado com `.indexOn` para `pontuacao`, `recordePontuacao`, `totalVitoriasModoSolo`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2`.

## Recorde de Pontuacao

### O que ja estava implementado

- `FirebasePaths.RECORDE_PONTUACAO = "recordePontuacao"`.
- `EstatisticasService.EstatisticasAtuais` ja carregava `recordePontuacao`.
- `EstatisticasService.calcularAtualizacao` ja somava `pontuacao` acumulada e calculava `recordePontuacao = max(recordeAnterior, pontosDoJogo)`.
- `PontuacaoRepository.toEstatisticasAtuais` ja lia `recordePontuacao` com fallback `0.0`.
- `JogadorRepository` ja criava jogadores novos e perfis Auth com `recordePontuacao = 0.0` e lia perfis antigos com fallback `0.0`.
- `MeuPerfilActivity` ja mostrava `Recorde de Pontuação` em vez de `pontuacao` acumulada.
- `RankingTipo.RECORDE`, `RankingJogador.recordePontuacao`, `RankingActivity` e `activity_ranking.xml` ja tinham a aba `Recorde`.

### O que faltava

- `firebase-rules.json` ainda nao validava `recordePontuacao` e nao tinha `.indexOn` para a query do ranking de recordes.
- `RankingRepository` deduplicava perfis hibridos sempre pela maior `pontuacao`; no ranking `Recorde`, agora deduplica pelo valor do tipo selecionado.
- Testes unitarios agora cobrem os cenarios de manter recorde quando o jogo novo e menor e atualizar recorde quando o jogo novo e maior.

### Checklist manual

1. Criar jogador novo e confirmar `jogadores/{uid|nome}/recordePontuacao = 0`.
2. Fazer 1800 pontos e confirmar `pontuacao = 1800` e `recordePontuacao = 1800`.
3. Fazer 1500 pontos e confirmar `pontuacao = 3300` e `recordePontuacao = 1800`.
4. Fazer 2100 pontos e confirmar `pontuacao = 5400` e `recordePontuacao = 2100`.
5. Abrir `Meu Perfil` e confirmar `Recorde de Pontuação: 2100 pontos`.
6. Abrir Ranking > `Recorde` e confirmar titulo `Ranking de Recordes`, label `Melhor Jogo` e ordenacao por `recordePontuacao`.
7. Abrir Ranking > `Global` e confirmar que continua ordenado por `pontuacao` acumulada.

### Verificacoes executadas nesta revisao

- `jq empty firebase-rules.json`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificacoes executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK. Antes de passar, o teste `ExampleUnitTest.kt` estava desatualizado e nao passava `recordePontuacao` para `EstatisticasAtuais`; foi corrigida apenas a fixture do teste.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

Nota: permanece apenas o warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used`.

## Ranking Global

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RankingActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RankingAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/models/RankingJogador.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/RankingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/RankingViewModel.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_ranking.xml`
- `app/src/main/res/layout/item_ranking_jogador.xml`
- `app/src/main/AndroidManifest.xml`
- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi implementado

- Botao de ranking no canto superior direito da `MainActivity`, usando o icone de trofeu existente.
- `RankingActivity` com loading, estado vazio, erro, botao voltar e lista em `RecyclerView`.
- `RankingRepository` consulta `jogadores` com `orderByChild("pontuacao").limitToLast(100)`.
- O ranking e ordenado novamente no cliente por seguranca, em ordem decrescente de pontuacao.
- Perfis sem nome ou sem `pontuacao` numerica sao ignorados.
- Perfis marcados com `isHostOnly` sao ignorados.
- Entradas sem identidade de perfil persistente sao ignoradas para nao incluir convidados.
- Perfis novos por UID e perfis antigos por `nomeUtilizador` continuam aceites; duplicados por nome preferem o perfil com UID.
- `firebase-rules.json` recebeu `.indexOn` de `pontuacao` em `jogadores`.

### Verificacoes executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

Nota: permanece apenas o warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used`.

### Como testar manualmente

1. Fazer login com uma conta registada e abrir a `MainActivity`.
2. Tocar no icone de trofeu no canto superior direito.
3. Confirmar que aparece "Ranking Global" e, durante a leitura, o loading.
4. Confirmar que a lista mostra posicao, nome, pontuacao, jogos, vitorias e taxa de acertos.
5. Confirmar no Firebase que jogadores sem `pontuacao` numerica ou sem nome nao aparecem.
6. Confirmar que perfis antigos em `jogadores/{nomeUtilizador}` com `pontuacao` continuam a aparecer.
7. Desligar rede ou bloquear leitura das rules num projeto de teste e confirmar a mensagem de erro.
8. Voltar pelo botao superior esquerdo e confirmar retorno a `MainActivity`.

## Correcao 2x2 - inicio da sala, equipas e podio

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `TEST_REPORT.md`

### Causa exata

- A sala 2x2 pre-cria jogadores a partir dos convites e cada cliente volta a confirmar a sua presenca ao entrar na sala. Em contas hibridas/legadas, a mesma pessoa podia chegar com chave `uid` ou chave `nomeUtilizador`, deixando a contagem crua de `jogadores` diferente dos 4 jogadores reais.
- `Sala2x2ViewModel` usava `jogadoresNaSala.size == 4`; com duplicados por uid/nome o botao podia ficar desativado apesar de a UI mostrar quatro pessoas.
- A identificacao de admin tambem era fragil porque comparava apenas os campos `adminUid/adminId/admin` contra a identidade local, sem cruzar a entrada real do jogador em `jogadores`.
- `guardarEquipas2x2` escrevia os nos inteiros `equipaA` e `equipaB`. As Firebase Rules de `sala_2x2` autorizam escrita nos filhos `equipaA/{jogadorId}` e `equipaB/{jogadorId}`; por isso o clique do admin podia falhar antes de mudar `estado` para `em_jogo`.
- Quando um perfil social era resolvido por fallback legado, a criacao 2x2 podia perder o `uid` Auth do criador, gravando `adminUid` vazio ou incompatível com as rules.

### O que foi corrigido

- `Sala2x2ViewModel` deduplica jogadores por `uid`, `nomeUtilizador`, `nomeJogador`, `nomeDisplay` e chave Firebase antes de contar a sala e formar equipas.
- O botao iniciar fica ativo apenas quando o utilizador atual e admin real e existem 4 jogadores reais unicos.
- O inicio 2x2 grava `equipaA/{chaveJogador}` e `equipaB/{chaveJogador}` por jogador, respeitando as rules, e so depois muda `estado` para `em_jogo`.
- A sala deixa de tentar gravar equipas automaticamente durante a publicacao de estado da UI; as equipas sao gravadas no clique de iniciar.
- `JogoCompetitivoRepository.verificarAdmin` cruza `adminUid/adminId/admin` com a entrada real do jogador na sala, mantendo fallback por nome.
- `ConvidarAmigo2x2Activity` preserva o `uid` Auth local quando o perfil social resolvido vem sem uid.
- `AmigosRepository` tenta resolver por `nomeUtilizador` quando a procura direta por `uid` nao encontra perfil, mantendo compatibilidade com dados legados.
- O pódio 2x2 continua a esperar pelos jogadores reais de `equipaA/equipaB` e pelos nos de pontuacao e total de certas de cada chave.

### Verificacoes executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

Nota: permanece apenas o warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used`.

### Nos Firebase esperados

Antes de iniciar o jogo:

- `sala_2x2/{codigo}/admin`
- `sala_2x2/{codigo}/adminId`
- `sala_2x2/{codigo}/adminUid`
- `sala_2x2/{codigo}/estado = em_espera`
- `sala_2x2/{codigo}/nomeCategoria`
- `sala_2x2/{codigo}/jogadores/{chaveJogador}` para 4 jogadores reais, preferencialmente UIDs quando existem.
- Cada jogador em `jogadores/{chaveJogador}` deve ter `uid`, `nomeUtilizador`/`nomeDisplay` quando disponiveis.

Depois do admin clicar iniciar:

- `sala_2x2/{codigo}/equipaA/{chaveJogador}` com 2 jogadores.
- `sala_2x2/{codigo}/equipaB/{chaveJogador}` com 2 jogadores.
- `sala_2x2/{codigo}/estado = em_jogo`.
- `sala_2x2/{codigo}/perguntas`.

Durante/depois do jogo:

- `sala_2x2/{codigo}/respostas/{chaveJogador}/{indice}`.
- `sala_2x2/{codigo}/pontuacoes_A/{chaveJogador}` para os 2 jogadores da equipa A.
- `sala_2x2/{codigo}/pontuacoes_B/{chaveJogador}` para os 2 jogadores da equipa B.
- `sala_2x2/{codigo}/totalPerguntasCertas_A/{chaveJogador}` para os 2 jogadores da equipa A.
- `sala_2x2/{codigo}/totalPerguntasCertas_B/{chaveJogador}` para os 2 jogadores da equipa B.
- `sala_2x2/{codigo}/estatisticasAtualizadas/{identificador}` depois da pontuacao atualizar estatisticas.

### Como testar manualmente 2x2 com 4 contas

1. Entrar em quatro dispositivos/emuladores com quatro contas diferentes.
2. Na conta 1, escolher modo 2x2, categoria e convidar 3 amigos.
3. Confirmar no Firebase que a sala criada tem `adminUid` igual ao UID Auth da conta 1 quando a conta e autenticada.
4. Nas contas 2, 3 e 4, aceitar o convite e abrir a sala 2x2.
5. Confirmar que a UI mostra duas pessoas na equipa A e duas na equipa B.
6. Confirmar que o botao iniciar esta ativo apenas na conta 1/admin.
7. Clicar iniciar na conta 1.
8. Confirmar que `equipaA` e `equipaB` aparecem antes de `estado = em_jogo`.
9. Confirmar que todos os clientes abrem `Jogo2x2Activity`.
10. Responder ou deixar terminar todas as perguntas nas 4 contas.
11. Confirmar que cada conta escreve em `respostas/{chaveJogador}/{indice}`.
12. Confirmar que cada conta escreve a pontuacao e total de certas no ramo da sua equipa.
13. Confirmar que a mensagem "Aguarde que todos terminem!" so persiste ate ao quarto jogador real terminar.
14. Confirmar que todas as contas abrem `Pontuacao2x2Activity` e o podio mostra os quatro jogadores.

## Correcao 2x2 - build e espera pelo podio

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `TEST_REPORT.md`

### Causa exata

- `Jogo2x2ViewModel.kt` tinha sido substituido por codigo de sala de espera, criando uma segunda `Sala2x2ViewModel` e removendo `Jogo2x2ViewModel`, `Jogo2x2Event`, `JogoCompetitivoPerguntaUiState` e `JogoCompetitivoPontuacaoDados`.
- Por isso `Jogo2x2Activity`, `Jogo1x1Activity` e `Jogo1x1ViewModel` deixaram de resolver os tipos competitivos partilhados e o projeto nao compilava.
- No fluxo 2x2, o estado podia mudar para `em_jogo` sem garantir primeiro a escrita de `equipaA` e `equipaB`; quando isso acontecia, cada jogador podia entrar no jogo sem conseguir identificar a equipa e acabava por nao gravar em `pontuacoes_A`/`pontuacoes_B` nem em `totalPerguntasCertas_A`/`totalPerguntasCertas_B`.
- A espera pelo podio tambem dependia de contagens fixas em `pontuacoes_A` e `pontuacoes_B`; agora verifica as chaves reais em `equipaA`/`equipaB` e so avanca quando cada jogador real tem pontuacao e total de certas gravados.

### O que foi corrigido

- `Jogo2x2ViewModel.kt` voltou a conter apenas o ViewModel/tipos do jogo 2x2 e os estados competitivos partilhados.
- `Sala2x2ViewModel.kt` ficou como unico dono de `Sala2x2ViewModel`, `Sala2x2UiState` e `Sala2x2Event`.
- `SalaDeEspera2x2Activity.kt` voltou a importar `Sala2x2UiState` e `Sala2x2Event`.
- O botao iniciar 2x2 grava `equipaA` e `equipaB` e so depois muda `estado` para `em_jogo`.
- `Jogo2x2ViewModel` deixa de continuar jogo/finalizacao se nao identificar `A` ou `B`.
- `guardarResultado2x2` escreve explicitamente em `pontuacoes_A`/`pontuacoes_B` e `totalPerguntasCertas_A`/`totalPerguntasCertas_B`.
- `escutarPodio2x2` escuta a sala completa e confirma resultados por chave real de equipa.

### Verificacoes executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente 2x2 com 4 contas

1. Entrar em quatro dispositivos/emuladores com quatro contas Firebase Auth diferentes.
2. Na conta 1, criar sala 2x2 e confirmar no Firebase `sala_2x2/{codigo}/jogadores/{uidConta1}`.
3. Nas contas 2, 3 e 4, entrar na mesma sala e confirmar que existem quatro filhos reais em `jogadores`.
4. Confirmar antes de iniciar que a UI mostra dois jogadores na equipa A e dois na equipa B.
5. Carregar em iniciar apenas na conta admin.
6. Confirmar no Firebase, antes ou no momento da transicao para jogo, que existem `equipaA` com 2 UIDs e `equipaB` com 2 UIDs.
7. Jogar ate ao fim nas quatro contas, respondendo normalmente ou deixando o tempo acabar.
8. Confirmar que cada jogador grava respostas em `respostas/{uid}/{indice}`.
9. Confirmar que os dois jogadores da equipa A gravam em `pontuacoes_A/{uid}` e `totalPerguntasCertas_A/{uid}`.
10. Confirmar que os dois jogadores da equipa B gravam em `pontuacoes_B/{uid}` e `totalPerguntasCertas_B/{uid}`.
11. Confirmar que os jogadores ficam em "Aguarde que todos terminem!" ate ao quarto resultado real.
12. Confirmar que, apos o quarto resultado, todas as contas abrem `Pontuacao2x2Activity` e o podio mostra os quatro jogadores.

## Fase final - UID como chave principal

### Ficheiros principais alterados nesta ronda

- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `ARCHITECTURE_PLAN.md`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/routes/UteisNavegacao.kt`
- `app/src/main/java/com/example/brainbrawl/UteisSala.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/CategoriaRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- Activities de modo/categorias/perfil/pontuacoes que recuperam UID por Intent/Auth fallback.

### O que foi validado

- `uid` continua a ser a chave principal em novos perfis, salas, jogadores de sala, pontuacoes e categorias quando existe Firebase Auth.
- `nomeUtilizador` fica preservado como display e fallback para dados antigos.
- `adminUid` passa a ser gravado em salas novas autenticadas, mantendo `adminId` para compatibilidade.
- Categorias personalizadas/publicas ja nao expõem overloads publicos por apenas `nomeUtilizador`; o fallback por nome ficou interno.
- `firebase-rules.json` valida JSON e inclui regras baseadas em `auth.uid` para perfis, salas, jogadores de sala, categorias e criador de categorias publicas.
- O projeto nao compila com o `java` default da maquina (`25.0.2`) por limitacao do Kotlin/Gradle DSL: `JavaVersion.parse(25.0.2)`.
- Os comandos foram executados com o JBR do Android Studio (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`, Java 21).

### Verificacoes executadas

- `node -e "JSON.parse(require('fs').readFileSync('firebase-rules.json','utf8'))"`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Fallbacks que continuam intencionais

- `jogadores/{nomeUtilizador}` para perfis legados e login antigo por nome/password.
- Categorias antigas em `jogadores/{nomeUtilizador}/categoriasPersonalizadas`.
- Salas antigas cujos jogadores, prontidao, respostas ou pontuacoes ainda estejam por nome.
- `adminId`/`admin` em salas antigas, ao lado do novo `adminUid`.
- `nomeUtilizador`, `nomeJogador` e `nomeDisplay` em modelos de sala, pontuacao e UI.

### Segurança ainda pendente

- Estatisticas finais continuam calculadas no cliente; as rules impedem escrita no perfil de outro utilizador, mas nao provam que os totais do proprio utilizador sao justos.
- Incrementos de `usos` e transacoes de avaliacao de categorias publicas ainda precisam de tolerancia para compatibilidade.
- Cloud Functions continuam recomendadas para ranking, estatisticas, validacao de fim de jogo e protecao forte contra resultados fabricados.

## Firebase Rules - perfil Auth em jogadores/{uid}

### Ficheiros alterados nesta ronda

- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `TEST_REPORT.md`

### O que foi corrigido

- `jogadores/{uid}` agora permite escrita quando existe utilizador autenticado e `auth.uid == uid`.
- Escrita em perfis Auth fica limitada ao proprio utilizador.
- A validacao de `jogadores/{id}` passou a aceitar dois formatos: perfil Auth por `uid` e perfil legado com `password`.
- Perfis Auth validam `uid`, `nomeUtilizador`, `email`, `avatar`, `estado`, `pontuacao`, `taxaAcertos`, `totalJogos`, `totalRespostasCertas`, `totalVitorias`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2` e `totalVitoriasModoSolo`.
- Mantida leitura de `jogadores` para login/perfil e compatibilidade temporaria com queries por `nomeUtilizador`/`email`.
- Nao houve alteracao de estrutura Firebase nem de codigo Kotlin.

### Motivo

- A conta Firebase Auth era criada, mas o perfil em Realtime Database falhava porque as rules antigas exigiam `password` em todos os nodes de `jogadores`.
- Como perfis Auth novos vivem em `jogadores/{uid}` e nao guardam `password`, a validacao rejeitava a escrita com `Permission denied`.

### Testes feitos

- Validacao sintatica de `firebase-rules.json` com `JSON.parse`.
  - OK.

### Testes manuais recomendados apos publicar as rules

1. Criar nova conta com email/password e confirmar criacao de `jogadores/{uid}`.
2. Confirmar que `uid` no perfil e igual ao `uid` do Firebase Authentication.
3. Fazer login com a nova conta e confirmar que ja nao aparece `Conta autenticada sem perfil de jogador`.
4. Confirmar que perfis antigos por nome continuam legiveis e nao foram apagados.
5. Tentar atualizar o perfil autenticado normal pela app e confirmar sucesso.

## Firebase Rules - indices para Auth hibrido

### Ficheiros alterados nesta ronda

- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `TEST_REPORT.md`

### O que foi corrigido

- Adicionado `.indexOn` em `jogadores` para `nomeUtilizador`.
- Adicionado tambem indice para `email`, preparando consultas por email durante a migracao Firebase Auth.
- Nao houve alteracao de estrutura Firebase.
- Nao houve alteracao de codigo Kotlin.

### Motivo

- A fase Auth hibrida cria perfis novos em `jogadores/{uid}`.
- Para continuar compatível com fluxos que ainda passam `nomeUtilizador`, o app resolve perfis com query por `nomeUtilizador`.
- Sem indice, o Realtime Database devolve o erro: `Index not defined, add ".indexOn": "nomeUtilizador"`.

### Testes feitos

- Validacao sintatica de `firebase-rules.json` com `JSON.parse`.
  - OK.

## Migração Firebase Authentication - base

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/services/AuthService.kt`

### Ficheiros alterados nesta ronda

- `app/build.gradle.kts`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/LoginViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/RegistarViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/LoginActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/activity_registar.xml`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Adicionada dependencia `com.google.firebase:firebase-auth` usando o Firebase BoM existente.
- Criado `AuthService` para encapsular `FirebaseAuth.currentUser`, `createUserWithEmailAndPassword`, `signInWithEmailAndPassword` e `signOut`.
- `RegistarViewModel` passou a criar conta Firebase Auth por email/password e, apos obter `uid`, criar perfil em `jogadores/{uid}`.
- O perfil Auth guarda `uid`, `nomeUtilizador`, `email`, `avatar`, `estado`, `pontuacao`, `taxaAcertos`, `totalJogos`, `totalRespostasCertas`, `totalVitorias`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2` e `totalVitoriasModoSolo`.
- `RegistarActivity` ganhou campo de email e, apos registo, abre diretamente `MainActivity`.
- `LoginViewModel` passou a fazer login por email/password com Firebase Auth e a reutilizar `FirebaseAuth.currentUser` para sessao persistente.
- `LoginActivity` redireciona para `MainActivity` quando ja existe `currentUser` com perfil.
- `MainActivity` passou a chamar `FirebaseAuth.signOut()` no logout e a preservar `uid`/`email` nos extras de base.
- `JogadorRepository` passou a resolver perfil/avatar/estado por `uid` ou por `nomeUtilizador`, mantendo suporte aos perfis antigos.
- O login antigo por `jogadores/{nome}/password` continua disponivel quando o identificador inserido nao e email.

### Estrutura nova usada

- Perfil principal novo: `jogadores/{uid}`.
- Campos novos/preparados: `uid`, `email`, `nomeUtilizador`.
- Extras novos/preparados: `IntentExtras.UID` (`uid`) e `IntentExtras.EMAIL` (`email`).
- Compatibilidade: `nomeUtilizador` continua a ser transportado nos extras e usado pelos fluxos ainda nao migrados.

### Ainda usa `nomeUtilizador`

- Amigos e convites.
- Categorias personalizadas/publicas.
- Salas de espera e jogo.
- Pontuacoes/estatisticas finais.
- Navegacao existente entre Activities.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Criar nova conta com nome, email e password; confirmar que entra direto no `MainActivity`.
2. Confirmar no Firebase que foi criado `jogadores/{uid}` com `uid`, `nomeUtilizador`, `email`, avatar e estatisticas a zero.
3. Fechar e reabrir a app; confirmar que `LoginActivity` detecta `currentUser` e abre `MainActivity`.
4. Fazer logout no botão de voltar/sair da Main; confirmar `FirebaseAuth.signOut()` e regresso ao Login.
5. Fazer login com email/password; confirmar entrada no Main e `estado = on`.
6. Fazer login legado com nome/password de uma conta antiga; confirmar que ainda funciona.
7. Entrar sem conta; confirmar que o fluxo convidado continua igual.
8. Confirmar que dados antigos em `jogadores/{nome}` nao foram apagados.

### Proximos passos

- Migrar repositories sociais/categorias/salas/pontuacoes para receber e persistir `uid`.
- Criar uma estrategia de mapeamento para resultados que ainda chegam com `nomeUtilizador`.
- Atualizar Firebase Rules para `auth.uid` quando os writes principais ja estiverem em `jogadores/{uid}`.

## Migração MVVM leve - jogo

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/JogoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo2x2ViewModel.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo2x2Activity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Em grupo/classico/caotico/eliminatorias, carregar perguntas, observar `perguntaAtualIndex`, observar fim de eliminatorias, sincronizar `serverTimeOffset`/`perguntaHoraInicio`, enviar respostas, calcular pontuacao, obter jogadores restantes, eliminar jogador, avancar perguntas, guardar resultado final e detectar fim de jogo passaram para `JogoViewModel`.
- Em 1x1, leitura da categoria real, carregamento/criacao transacional das perguntas, sincronizacao do inicio de pergunta, offset do servidor, calculo de pontuacao, guardar pontuacao final, espera pelo podio e deteccao de fim passaram para `Jogo1x1ViewModel`.
- Em 2x2, leitura da categoria real, identificacao da equipa, carregamento/criacao transacional das perguntas, sincronizacao do inicio de pergunta, offset do servidor, envio de resposta, calculo de pontuacao, guardar resultado por equipa, espera pelo podio e deteccao de fim passaram para `Jogo2x2ViewModel`.
- Activities continuam responsaveis por UI/layout, opcoes visuais, timers visuais, progress bar, sons, toasts, feedback de botoes, animações existentes e navegacao.
- Repositories continuam responsaveis por Firebase; nao foram alterados nomes de nodes, estrutura Firebase, regras de pontuacao ou regras dos modos.
- Listeners de jogo passaram a ser guardados e removidos nos ViewModels por `removerListeners()` e `onCleared`.

### Testes feitos

- `./gradlew assembleDebug`
  - Falhou no ambiente por Java `25.0.2` (`JavaVersion.parse` no Kotlin/Gradle), antes de compilar codigo da app.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Grupo/classico: criar sala como admin, entrar com outro jogador, iniciar jogo, confirmar pergunta sincronizada, resposta, bonus, avanco de perguntas pelo admin e podio final.
2. Grupo/caotico: repetir fluxo de grupo e confirmar tempo de 10 segundos, pontuacao caotica e sincronizacao do `perguntaHoraInicio`.
3. Eliminatorias: jogar com pelo menos dois jogadores reais, errar uma resposta num cliente, confirmar marcacao como eliminado, abertura de espera e abertura do podio quando `estado = terminado`.
4. Eliminatorias/admin: confirmar que o admin apenas observa, avanca perguntas apos o timer e termina quando resta um jogador real.
5. 1x1: iniciar com dois jogadores, confirmar que ambos recebem as mesmas perguntas, timer de 15 segundos, pontuacao/bonus, espera pelo adversario e podio quando ambos gravam pontuacao.
6. 2x2: iniciar com quatro jogadores, confirmar equipa correta, envio de resposta em `respostas/{jogador}/{indice}`, resultado por `pontuacoes_A`/`pontuacoes_B`, espera por todos e podio final.
7. Em todos os modos, sair/rodar ecras durante jogo e confirmar que nao aparecem perguntas duplicadas, timers duplicados ou listeners a disparar depois de sair.
8. Confirmar no Firebase que `salas`, `sala_1x1` e `sala_2x2` mantem os mesmos paths/campos.

## Migracao UID - Bloco 3 Jogo

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/JogoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/UteisSala.kt`
- `app/src/main/java/com/example/brainbrawl/routes/UteisNavegacao.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherModoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `app/src/main/java/com/example/brainbrawl/EscolhaCategoriaModosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- As Activities de jogo leem `IntentExtras.UID` e usam `AuthService.currentUser` como fallback, mantendo `nomeUtilizador` e `nomeJogador` para display/compatibilidade.
- `JogoViewModel`, `Jogo1x1ViewModel` e `Jogo2x2ViewModel` passaram a trabalhar com `JogadorSalaIdentidade`, cuja chave principal e o `uid` quando existe.
- `JogoRepository` resolve a chave real do jogador em `salas/{codigo}/jogadores` antes de escrever respostas, eliminacao e resultado final.
- `JogoCompetitivoRepository` resolve jogadores 1x1/2x2 por `uid`, chave antiga, `nomeUtilizador`, `nomeJogador` ou `nomeDisplay`.
- Convites 1x1/2x2 criam salas competitivas com jogadores em formato hibrido e `adminId`, preservando `admin` como nome de display.
- Prontos, equipas, respostas 2x2 e pontuacoes competitivas usam a chave efetiva da sala.
- A passagem de `uid` foi preservada nos fluxos de modo/categoria/sala/jogo/pontuacao sem alterar UI, navegacao, regras, tempos ou nomes de paths Firebase.

### Compatibilidade mantida

- Salas antigas com jogadores guardados por nome continuam a ser encontradas pela lista de chaves de compatibilidade.
- Convidados continuam sem `uid` e usam `nomeJogador`/`nomeUtilizador` como fallback.
- `nomeUtilizador` e `nomeJogador` continuam a ser enviados para as Activities de pontuacao para manter os contratos atuais.
- Listeners existentes continuam guardados em ViewModels/repositories e removidos por `removerListeners()`/`onCleared`.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Grupo/classico: criar sala autenticado, entrar com outro jogador, confirmar no Firebase que `salas/{codigo}/jogadores/{uid}` e usado quando ha Auth e que o nome mostrado continua legivel.
2. Grupo/caotico: confirmar tempo de 10 segundos, respostas em `perguntaAtual/respostas/{chaveJogador}` e resultado final no mesmo jogador da sala.
3. Eliminatorias: errar uma resposta, confirmar `estado=eliminado`, `pontuacao` e `totalRespostasCertas` no jogador correto; confirmar fim quando resta um jogador real.
4. 1x1: enviar convite, aceitar, confirmar `sala_1x1/{codigo}/jogadores/{uid}`, `prontos/{uid}` e `pontuacoes/{uid}` quando autenticado.
5. 2x2: criar convite com quatro jogadores, confirmar `equipaA`/`equipaB`, `respostas/{uid}/{indice}`, `pontuacoes_A`/`pontuacoes_B` e espera pelo podio.
6. Compatibilidade: repetir com uma sala antiga por nome e confirmar que nao cria jogador duplicado ao responder/finalizar.

### Ainda depende de `nomeUtilizador`

- Activities de pontuacao e `PontuacaoRepository` ainda usam nomes para display, recordes, estatisticas e desforra.
- Categorias personalizadas/publicas continuam a usar `nomeUtilizador` como dono/criador.
- Alguns extras de navegacao continuam a transportar `nomeUtilizador` por compatibilidade e display.
- Estatisticas finais ainda precisam do Bloco Pontuacoes para escrever definitivamente em `jogadores/{uid}`.

### Proximo bloco sugerido

- Pontuacoes: migrar `PontuacaoRepository`, `PontuacoesActivity`, `Pontuacao1x1Activity` e `Pontuacao2x2Activity` para separar `uid` de nome de display e atualizar estatisticas por `uid`.

## Migracao UID - Bloco 4 Pontuacoes e Estatisticas

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- `ResultadoJogador` passou a transportar `uid`, chave real da sala, `nomeUtilizador` e `nomeJogador`, mantendo `nome` como texto de podio/display.
- Leitura de resultados finais de grupo, 1x1 e 2x2 passa a preservar metadados de identidade quando existem e a cair para a chave/nome antigo em salas legadas.
- Atualizacao de estatisticas globais resolve o perfil em `jogadores/{uid}` primeiro, com fallback por chave/nome legado, sem criar perfil para convidados.
- Vencedores e marcadores `estatisticasAtualizadas` usam `uid` quando existe; convidados/dados antigos continuam a usar chave ou nome.
- `Pontuacao1x1Activity` usa identidade hibrida para reconhecer jogador atual/adversario, atualizar apenas o jogador local e criar sala de desforra com chave principal por `uid` quando possivel.
- `Pontuacao2x2Activity` usa identidade hibrida para detetar o recorde do jogador local.

### Mantido sem alterações

- UI, textos, layouts, navegacao, regras de pontuacao e regras de vencedores.
- Paths principais de salas/resultados: `salas`, `sala_1x1`, `sala_2x2`, `pontuacoes`, `pontuacoes_A`, `pontuacoes_B` e `totalPerguntasCertas_*`.
- Admin host-only continua fora do podio/estatisticas.
- Convidados continuam sem perfil em `jogadores`.
- Contratos das Activities de pontuacao continuam a receber `nomeUtilizador`/`nomeJogador` para display e compatibilidade.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Grupo: criar sala autenticado, jogar ate ao podio e confirmar que `estatisticasAtualizadas/{uid}` e `jogadores/{uid}` sao usados quando ha Auth.
2. Caotico: repetir grupo em modo caotico e confirmar que tempos/pontuacao nao mudaram e as estatisticas atualizam uma vez.
3. Eliminatorias: eliminar jogadores, abrir podio final e confirmar que jogadores reais atualizam estatisticas e admin host-only fica fora.
4. 1x1: terminar partida com dois autenticados, confirmar podio, estatisticas por `uid` e desforra com jogadores na nova sala por `uid`.
5. 2x2: terminar com quatro jogadores, confirmar podio por equipa, vencedor igual ao anterior e estatisticas/vitorias 2x2 por `uid`.
6. Convidado: jogar como convidado e confirmar que aparece no podio, mas nao e criado perfil em `jogadores`.
7. Admin host-only: iniciar/observar grupo como admin host-only e confirmar que nao entra no podio nem nas estatisticas.

### Ainda depende de `nomeUtilizador`

- Display de podios e nomes nos resultados.
- Fallback para perfis legados e salas antigas.
- Categorias personalizadas/publicas, criador/dono e alguns paths `jogadores/{nomeUtilizador}/categoriasPersonalizadas`.
- Extras de navegacao mantidos por compatibilidade.

### Proximo bloco recomendado

- Categorias/ownership: migrar criador, dono, categorias personalizadas/publicas e publicacoes para `uid`, mantendo `nomeUtilizador` como display e fallback legado.

## Migracao UID - Bloco 5 Categorias

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/models/Categoria.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/CategoriaRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/CategoriasViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/EditarCategoriaViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/ExplorarCategoriasViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/UteisSala.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Categorias personalizadas passam a ser procuradas por `uid` primeiro e por `nomeUtilizador` como fallback legado.
- Criacao de categorias e novas perguntas passa a escrever em `jogadores/{uid}/categoriasPersonalizadas` quando existe Auth.
- Edicao, eliminacao e leitura de perguntas resolvem a categoria existente antes de escrever, para nao perder perguntas antigas por nome.
- Categorias publicas passam a guardar `criadorUid`, `criadorId`, `nomeUtilizador` e `nomeDisplay`.
- Publicacao verifica ids publicos antigos e novos antes de criar/atualizar, reduzindo risco de duplicar categorias publicas.
- Guardar copia de categoria publica usa `uid` como dono quando existe.
- Avaliacoes usam `uid` como chave principal e verificam chaves antigas para impedir avaliacao duplicada.
- Convidados continuam impedidos de criar, publicar, guardar copia e avaliar.
- Salas criadas a partir de categoria personalizada carregam perguntas com identidade hibrida e guardam `donoUid` nos metadados quando disponivel.

### Mantido sem alterações

- UI, textos, layouts e navegacao.
- Estrutura principal dos nodes `categorias`, `categoriasPublicas` e `categoriasPersonalizadas`.
- Perguntas existentes em categorias antigas por nome continuam legiveis.
- Contador de usos continua transacional em `categoriasPublicas/{id}/usos`.
- Jogar com categoria publica continua permitido para jogadores com nome/identidade, mantendo o fluxo atual.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Criar categoria: iniciar sessao, criar categoria nova e confirmar `jogadores/{uid}/categoriasPersonalizadas/{categoria}`.
2. Editar pergunta: abrir categoria criada e editar uma pergunta sem criar categoria duplicada.
3. Eliminar pergunta: eliminar pergunta e confirmar que apenas a pergunta escolhida saiu.
4. Publicar categoria: tornar publica e confirmar `criadorUid`, `nomeUtilizador`, `nomeDisplay`, perguntas e `categoriaPublicaId`.
5. Explorar categoria publica: abrir explorar, ver nome/criador/rating/usos e jogar sem mudar UI.
6. Guardar copia: guardar categoria publica e confirmar copia em `jogadores/{uid}/categoriasPersonalizadas`.
7. Jogar com categoria publica: iniciar sala, confirmar perguntas carregadas e incremento de `usos`.

### Ainda depende de `nomeUtilizador`

- Display do criador/dono.
- Fallback para categorias personalizadas antigas em `jogadores/{nomeUtilizador}`.
- Fallback de avaliacoes/publicacoes antigas que usavam nome como chave.
- Extras de navegacao preservados para compatibilidade.

### Proximo bloco recomendado

- Firebase Rules/Auth hardening: proteger writes por `auth.uid` em jogadores, categorias, amigos, salas e estatisticas, mantendo fallback apenas onde ainda houver dados antigos.

## Migração MVVM leve - salas

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/SalaGrupoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/EsperaEliminadoViewModel.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/EsperaEliminadoActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Entrada em sala por codigo, validacao de nome, verificacao de sala existente/nome repetido, leitura de avatar e adicao do jogador passaram para `SalaGrupoViewModel`.
- Observacao em tempo real da lista de jogadores, estado da sala e sala apagada em grupo passou para `SalaGrupoViewModel`.
- Verificacao das condicoes para iniciar jogo de grupo, mudanca para `em_jogo`, saida de jogador e apagamento de sala pelo admin passaram para `SalaGrupoViewModel`.
- Em 1x1, adicionar jogador, marcar pronto, obter admin, observar jogadores/estado/sala apagada, verificar prontos, iniciar jogo e sair/apagar sala passaram para `Sala1x1ViewModel`.
- Em 2x2, adicionar jogador, obter admin, observar jogadores/estado/sala apagada, calcular/guardar equipas, iniciar jogo e sair/apagar sala passaram para `Sala2x2ViewModel`.
- Em eliminatorias, a espera pelo estado `terminado` passou para `EsperaEliminadoViewModel`.
- Activities continuam responsaveis por UI, toasts e navegacao para `JogoActivity`, `Jogo1x1Activity`, `Jogo2x2Activity`, `PontuacoesActivity` e `MainActivity`.
- Os listeners continuam a usar os repositories existentes e sao removidos pelas Activities e por `onCleared`, reduzindo risco de fugas de memoria.
- Nao foram alterados layouts, nomes de nodes Firebase, regras de admin, fluxo de inicio de jogo nem Activities de jogo.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Criar sala de grupo/classico com utilizador registado; confirmar codigo, lista de jogadores e botao de iniciar apenas para admin.
2. Entrar na sala de grupo por codigo com outro jogador; confirmar validacoes de codigo vazio, codigo invalido e nome repetido.
3. Iniciar jogo de grupo quando ha pelo menos um jogador real alem do admin; confirmar navegacao para `JogoActivity` em todos os clientes.
4. Sair como jogador nao admin; confirmar remocao apenas desse jogador e regresso ao menu.
5. Sair como admin; confirmar que a sala e apagada e os restantes jogadores voltam ao menu.
6. Criar/aceitar convite 1x1; confirmar lista de dois jogadores, pronto automatico, botao ativo apenas para admin e navegacao sincronizada para `Jogo1x1Activity`.
7. Criar/aceitar convite 2x2 com quatro jogadores; confirmar distribuicao visual das equipas, escrita de `equipaA`/`equipaB` e navegacao sincronizada para `Jogo2x2Activity`.
8. Em eliminatorias, ser eliminado e aguardar o fim da sala; confirmar que a Activity abre o podio quando `estado = terminado`.
9. Rodar/fechar/reabrir ecras de sala durante a espera; confirmar que listas nao duplicam e que nao ficam listeners aparentes ativos.
10. Confirmar no Firebase que `salas`, `sala_1x1` e `sala_2x2` mantem os mesmos paths/campos.

## Migração MVVM leve - autenticação

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/LoginViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/RegistarViewModel.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/LoginActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Validacao de campos de login e registo passou para `LoginViewModel`/`RegistarViewModel`, usando `UteisValidacao`.
- Verificacao de jogador existente e leitura de perfil passaram a ser chamadas pelos ViewModels via `JogadorRepository`.
- Comparacao entre password inserida e hash SHA-256 guardado passou para `LoginViewModel`.
- Criacao de jogador registado passou para `RegistarViewModel` e `JogadorRepository`, mantendo os mesmos campos Firebase atuais.
- O nome do avatar guardado continua a seguir `avatar_{index + 1}_playstore`; a selecao visual do avatar continua na Activity.
- Entrada como convidado passou para `LoginViewModel`, mantendo a mesma validacao de nome vazio e a mesma navegacao.
- Marcacao de estado online no login registado passou para `LoginViewModel`, continuando a usar `JogadorRepository.marcarOnline`.
- Login manual foi mantido; nao foi introduzido Firebase Auth.
- Activities continuam responsaveis por UI, toasts e navegacao.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Tentar login com campos vazios, nome curto/invalido e password curta; confirmar as mesmas mensagens de validacao.
2. Tentar login com jogador inexistente; confirmar `Jogador não encontrado`.
3. Tentar login com password errada; confirmar `Senha incorreta` e limpeza do campo de password.
4. Fazer login com conta registada valida; confirmar toast de sucesso, entrada no `MainActivity` e `estado = on` no Firebase.
5. Entrar como convidado com nome vazio e depois com nome valido; confirmar bloqueio/toast e navegacao com `nomeJogador`.
6. Registar novo jogador com avatar diferente do primeiro; confirmar criacao em `jogadores/{nome}` com `password`, `avatar`, `pontuacao`, totais e vitorias por modo.
7. Tentar registar jogador existente; confirmar `Jogador já existe`.
8. Confirmar que nao foi criada nenhuma estrutura Firebase nova e que o login continua manual com hash SHA-256.

## Migração MVVM leve - categorias

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/CategoriasViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/ExplorarCategoriasViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/EditarCategoriaViewModel.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Listar categorias personalizadas e categorias publicas usadas no dialog de categorias passou para `CategoriasViewModel`.
- Publicar categoria, remover categoria publica e eliminar categoria personalizada passaram para `CategoriasViewModel`.
- Observar categorias publicas em tempo real passou para `ExplorarCategoriasViewModel`, com listener removido em `onDestroy`/`onCleared`.
- Guardar copia de categoria publica e avaliar categoria passaram para `ExplorarCategoriasViewModel`.
- Carregar perguntas editaveis, guardar pergunta, eliminar pergunta e validacao basica do formulario passaram para `EditarCategoriaViewModel`.
- A validacao preserva a ordem e mensagens atuais: categoria nao permitida, tamanhos maximos, opcoes diferentes e campos obrigatorios.
- Activities continuam responsaveis por UI, dialogs, adapters/listas visuais, toasts, limpar formulario, criacao de sala e navegacao.
- `CategoriaRepository` continua responsavel pelo Firebase; nao foram alterados paths nem estrutura Firebase.
- Compatibilidade com convidados foi mantida: convidados continuam bloqueados ao criar/guardar/avaliar onde a UI ja bloqueava.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Entrar com conta registada, abrir categorias personalizadas e confirmar lista, estado Publica/Privada e botao `Criar nova categoria`.
2. Criar uma categoria, adicionar pergunta com 4 opcoes e confirmar que aparece na lista de perguntas.
3. Testar validacoes: opcoes repetidas, campos vazios, categoria oficial e campos longos.
4. Editar uma pergunta existente e confirmar que atualiza sem duplicar.
5. Eliminar pergunta e categoria personalizada.
6. Tornar categoria publica, atualizar publica e remover publica; confirmar toasts e atualizacao do dialog.
7. Abrir `Explorar Categorias`, confirmar lista publica, guardar copia com conta registada e avaliar uma categoria.
8. Repetir guardar/avaliar/criar categoria como convidado e confirmar os bloqueios com toast.
9. Confirmar no Firebase que `categoriasPublicas` e `jogadores/{nome}/categoriasPersonalizadas` mantem os mesmos paths/campos.

## Migração MVVM leve - AmigosActivity

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/AmigosViewModel.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- A lista de amigos, incluindo avatar e estado de cada jogador, passou a ser carregada/exposta por `AmigosViewModel`.
- A observacao em tempo real de amigos, pedidos de amizade e convites recebidos passou para `AmigosViewModel`.
- Pesquisa de jogador, envio de pedido, aceitar pedido e metodos para recusar pedido passaram para `AmigosViewModel`.
- Aceitar convite passou a ser chamado por `AmigosViewModel`; a Activity continua a mostrar o toast e navegar para a sala de espera 1x1/2x2 como antes.
- Metodos para recusar/remover convite foram expostos no `AmigosViewModel`, usando os metodos ja existentes em `AmigosRepository`, sem alterar a UI atual.
- Os listeners sociais continuam removidos em `onStop`/`onDestroy` e tambem em `onCleared`, evitando fugas de memoria.
- `AmigosRepository` e `JogadorRepository` continuam responsaveis pelo Firebase; nao foram alterados paths nem estrutura Firebase.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Entrar com uma conta registada e abrir `Amigos`; confirmar que o proprio utilizador aparece primeiro com avatar/estado e que os amigos aparecem como antes.
2. Pesquisar um utilizador inexistente; confirmar toast `Utilizador não encontrado`.
3. Pesquisar um amigo existente; confirmar toast de que ja e amigo e que o layout de adicionar fica escondido.
4. Pesquisar um utilizador valido que ainda nao e amigo; confirmar botao `Adicionar {nome}` e enviar pedido.
5. Noutra conta, confirmar que o pedido aparece em `Pedidos de amizade recebidos` e aceitar; confirmar toast `Amizade aceite!` e atualizacao das listas.
6. Enviar convite 1x1/2x2 a partir dos fluxos existentes; confirmar que o convite recebido aparece em `Amigos` e que aceitar navega para a sala de espera correta.
7. Sair e voltar ao ecrã `Amigos` algumas vezes; confirmar que nao ha duplicacao de listas nem listeners aparentes.
8. Confirmar no Firebase que `jogadores/{nome}/amigos`, `pedidos_amizade`, `convites_recebidos` e `convites_enviados` mantem a mesma estrutura.

## Migração MVVM leve - perfil

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/viewmodels/MeuPerfilViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/PerfilAmigoViewModel.kt`

### Ficheiros alterados nesta ronda

- `app/build.gradle.kts`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- `MeuPerfilActivity` passou a observar `MeuPerfilViewModel` para receber nome, avatar, pontuacao, taxa de acertos, total de jogos, total de vitorias e total de respostas certas.
- `PerfilAmigoActivity` passou a observar `PerfilAmigoViewModel` para receber os mesmos dados do amigo, incluindo fallback para perfil inexistente.
- A remocao de amigo passou a ser chamada pelo `PerfilAmigoViewModel`; o toast de sucesso e a navegacao de volta para `AmigosActivity` continuam na Activity.
- `JogadorRepository` e `AmigosRepository` continuam responsaveis pelo Firebase; nao foram alterados paths nem estrutura Firebase.
- Layouts, textos principais, badges, avatar, botoes, toasts e navegacao continuam controlados pelas Activities.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Como testar manualmente

1. Entrar com uma conta registada e abrir `Meu Perfil`; confirmar nome, avatar, pontuacao, jogos, vitorias, taxa de acertos e badges.
2. Abrir a lista de amigos e entrar no perfil de um amigo; confirmar que os mesmos dados aparecem sem mudanca visual.
3. Remover um amigo pelo perfil; confirmar o toast `Amigo removido com sucesso!` e o regresso a `AmigosActivity`.
4. Abrir o perfil de um amigo/perfil inexistente, se o fluxo permitir, e confirmar fallback com avatar padrao e estatisticas a zero.
5. Confirmar no Firebase que a estrutura de `jogadores/{nome}` e `amigos` nao mudou.

## Correções de bugs e UX antes da próxima fase de arquitetura

### Bugs encontrados e causa provável

- Modo caótico/grupo: o pódio do admin podia abrir antes dos jogadores gravarem os resultados finais. Além disso, a leitura do pódio procurava `totalPerguntasCertas`, mas `JogoRepository` grava `totalRespostasCertas` nos jogadores da sala.
- Modo caótico/grupo: o admin host-only estava corretamente marcado com `isHostOnly=true`, mas a leitura one-shot de resultados tornava a experiência inconsistente quando o admin chegava ao pódio primeiro.
- Editar categoria: ao abrir uma categoria existente, o campo do nome continuava editável. Se o utilizador alterasse o nome durante a edição de uma pergunta, a pergunta podia ser gravada noutra categoria usando o mesmo conteúdo, parecendo duplicação.
- Meu perfil: ainda eram renderizados dados técnicos/sensíveis (`estado`, indicação de `password` e detalhes internos).
- Explorar Categorias: não havia entrada direta para criar categoria personalizada.
- Categorias: faltavam dicas/descrições equivalentes às dicas dos modos de jogo.
- Main: o botão de voltar/sair estava no fluxo principal de ações, aumentando o risco de toque acidental.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolhaCategoriaModosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_meu_perfil.xml`
- `app/src/main/res/layout/activity_escolher_categoria.xml`
- `app/src/main/res/layout/activity_escolha_categoria_modos.xml`
- `app/src/main/res/layout/activity_explorar_categorias.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en-rGB/strings.xml`
- `app/src/main/res/values-de-rDE/strings.xml`

### Bugs corrigidos

- Pódio de grupo/caótico passou a escutar resultados em tempo real e só atualiza estatísticas quando todos os jogadores reais têm resultado guardado.
- Leitura de respostas certas em grupo usa `totalRespostasCertas`, mantendo fallback para `totalPerguntasCertas` sem alterar a estrutura Firebase.
- Admin host-only continua excluído do pódio/estatísticas por `isHostOnly=true`.
- Edição de categoria existente bloqueia o nome da categoria e guarda perguntas pelo `perguntaId` original, evitando duplicação ao editar.
- Eliminar pergunta em edição limpa o formulário e recarrega a lista da categoria correta.
- Meu Perfil deixou de mostrar `estado`, `password`, toast de debug e bloco de detalhes técnicos.
- Categorias ganharam botão de dicas com textos curtos e consistentes com `UteisDicas`.
- Main moveu `Sair` para o canto superior esquerdo, mantendo a lógica de marcar offline e voltar ao login.
- Explorar Categorias ganhou botão `Criar Categoria`; convidados são bloqueados com aviso e utilizadores registados entram no fluxo de criação com `nomeUtilizador`/`nomeJogador` preservados.

### Testes feitos

- `./gradlew build`
  - Falhou no ambiente por Java `25.0.2`, incompatível com o Kotlin/Gradle do projeto.
- `JAVA_HOME=/tmp/codex-jdks/jdk17/Contents/Home ./gradlew build`
  - OK.
- `JAVA_HOME=/tmp/codex-jdks/jdk17/Contents/Home ./gradlew testDebugUnitTest`
  - OK.
- Verificação estática dos fluxos alterados:
  - sala caótica/grupo preserva `nomeUtilizador`/`nomeJogador` até `JogoActivity` e `PontuacoesActivity`;
  - pódio ignora admin host-only e aguarda jogadores reais;
  - edição usa categoria original e `perguntaId` original;
  - perfil já não referencia o bloco removido;
  - criar categoria em Explorar bloqueia convidados.

### Pendentes

- Teste manual com Firebase em dois dispositivos/emuladores:
  - modo caótico com admin + jogador real até ao pódio;
  - confirmar que o admin vê pontuação/respostas certas do jogador depois de gravadas;
  - editar pergunta existente, criar nova pergunta e eliminar pergunta;
  - confirmar visualmente dicas nas categorias em ecrã pequeno;
  - confirmar Meu Perfil sem campos técnicos;
  - confirmar botão `Sair` no novo local e fluxo de logout;
  - confirmar botão `Criar Categoria` em Explorar Categorias para registado e convidado.

### Notas

- A estrutura Firebase não foi alterada.
- Foi usado um JDK 17 temporário em `/tmp/codex-jdks` apenas para executar Gradle, porque o Java global da máquina é `25.0.2`.

## Migração de pontuações e estatísticas

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leitura de pontuações finais de grupo para `PontuacaoRepository`.
- Leitura de pódio 1x1 em tempo real para `PontuacaoRepository`.
- Leitura de equipas, pontuações e respostas certas 2x2 para `PontuacaoRepository`.
- Atualização de `pontuacao`, `totalJogos`, `totalVitorias`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2`, `totalVitoriasModoSolo`, `totalRespostasCertas` e `taxaAcertos` para `PontuacaoRepository`.
- Cálculo de taxa de acertos, decisão de vencedor por modo, pódio 2x2 e validação anti-duplicação para `EstatisticasService`.
- Proteção contra duplicação de estatísticas por transação em `estatisticasAtualizadas/{nomeJogador}` dentro da sala de resultados.
- Filtro para não criar perfis de convidados: só atualiza estatísticas se `jogadores/{nome}` existe e tem `password`.
- Filtro para manter admin host-only fora do pódio/estatísticas em grupos.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.
- Testes unitários adicionados para:
  - média de `taxaAcertos`;
  - vencedor 1x1 pelo pódio ordenado;
  - regra atual de empate 2x2 para estatísticas, mantendo vitória da Equipa A;
  - bloqueio lógico quando estatísticas já foram atualizadas.

### Verificação estática dos requisitos

- Fim de jogo grupo: `PontuacoesActivity` lê `salas/{codigo}/jogadores`, ignora `admin`/`isHostOnly` e atualiza estatísticas via repository.
- Fim de jogo 1x1: `Pontuacao1x1Activity` lê `sala_1x1/{codigo}/pontuacoes` via repository e atualiza só o perfil do cliente atual, usando as respostas certas do intent.
- Fim de jogo 2x2: `Pontuacao2x2Activity` lê `equipaA`, `equipaB`, `pontuacoes_A`, `pontuacoes_B`, `totalPerguntasCertas_A` e `totalPerguntasCertas_B` via repository.
- Pódio correto: as ordenações antigas foram mantidas; grupo/1x1 por pontuação descrescente e 2x2 com equipa vencedora primeiro, Equipa A em empate.
- Estatísticas atualizadas uma única vez: cada jogador da sala recebe marcador transacional próprio antes do update.
- Convidados não aparecem em `jogadores`: updates em `jogadores/{nome}` são ignorados quando não existe `password`.
- Admin host-only não aparece como jogador: grupo ignora `admin`, nome vazio e `isHostOnly=true`.
- Perfil continua a mostrar estatísticas via `JogadorRepository`; não foi necessário alterar `MeuPerfilActivity` nem `PerfilAmigoActivity`.

### Pendentes

- Teste manual em emuladores/dispositivos com Firebase para confirmar visualmente:
  - fim de jogo grupo e pódio;
  - fim de jogo 1x1 com duas contas registadas;
  - fim de jogo 2x2 com quatro contas registadas;
  - recriar/rodar a Activity de pontuação e confirmar que `totalJogos` só incrementa uma vez;
  - entrar como convidado e confirmar que não é criado perfil em `jogadores`;
  - criar sala de grupo como admin host-only e confirmar que não aparece no pódio;
  - abrir `MeuPerfilActivity`/`PerfilAmigoActivity` depois do jogo e confirmar os totais.
- No 1x1, a estrutura atual só guarda `pontuacoes/{nome}`; por isso cada cliente atualiza as suas próprias respostas certas a partir do intent. Uma melhoria futura seria persistir também respostas certas por jogador na sala 1x1 antes do ecrã de pontuação.

### Como testar manualmente

1. Criar duas contas registadas e jogar modo grupo até ao fim; confirmar pódio e incrementos de `totalJogos`, `totalVitoriasModoSolo`, `totalRespostasCertas` e `taxaAcertos`.
2. Voltar/rodar/reabrir o ecrã de pontuação da mesma sala; confirmar que `estatisticasAtualizadas/{nome}` existe e os totais não duplicam.
3. Jogar 1x1 com duas contas registadas; confirmar vencedor, pontuação máxima e estatísticas do próprio perfil em cada dispositivo.
4. Jogar 2x2 com quatro contas registadas; confirmar pódio por equipa, empate se aplicável e vitórias 2x2.
5. Repetir grupo com convidado/admin host-only; confirmar que convidado sem `password` não ganha perfil e admin host-only não entra no pódio.
6. Abrir `MeuPerfilActivity` e `PerfilAmigoActivity` para validar que os números apresentados batem certo com `jogadores/{nome}`.

## Correções críticas no fluxo de jogo competitivo/eliminatórias

### Causa dos problemas

- `SalaDeEspera1x1Activity` e `SalaDeEspera2x2Activity`: o botão `Iniciar` dependia de `admin`, mas `admin` era lido de forma assíncrona. Como os convites já criam a sala com os convidados em `jogadores`, o listener de jogadores podia correr antes de `admin=true` e não voltar a disparar quando o convite era aceite. Resultado: botão visível mas preso como disabled.
- `SalaDeEspera2x2Activity`: pelo mesmo race, `equipaA`/`equipaB` podiam não ser gravadas se a sala já tivesse 4 nomes antes de `admin` ser carregado.
- `JogoActivity` em eliminatórias: o jogador que falhava não tinha um fluxo seguro para sair do jogo e aguardar o final. Além disso, o fim antecipado das eliminatórias não sinalizava os restantes clientes/eliminados com um estado comum da sala.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/EsperaEliminadoActivity.kt`
- `app/src/main/res/layout/activity_espera_eliminado.xml`
- `app/src/main/AndroidManifest.xml`
- `TEST_REPORT.md`

### Bugs corrigidos

- 1x1 recalcula o estado do botão quando chegam jogadores e quando o admin é identificado.
- 1x1 mantém a validação de `prontos` antes de passar `sala_1x1/{codigo}/estado` para `em_jogo`.
- 2x2 recalcula o estado do botão quando chegam jogadores e quando o admin é identificado.
- 2x2 grava `equipaA`, `equipaB`, `pontuacaoA` e `pontuacaoB` quando a sala já está completa e o admin chega depois.
- Eliminatórias marcam o jogador como eliminado usando o campo existente `salas/{codigo}/jogadores/{nome}/estado = "eliminado"`, sem apagar o jogador e sem criar novos ramos Firebase.
- A pontuação e respostas certas do eliminado ficam preservadas no nó do jogador da sala.
- Foi criado `EsperaEliminadoActivity`, que escuta `salas/{codigo}/estado` e envia o eliminado para `PontuacoesActivity` quando o admin marca `estado = "terminado"`.
- `JogoActivity` passa a escutar `estado="terminado"` em eliminatórias para redirecionar também jogadores ativos quando o jogo termina antecipadamente.

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- Instalação do APK debug em `emulator-5554`, `emulator-5556` e `emulator-5558`
  - OK.
- Teste manual 1x1 com contas temporárias `CodexA1x1` e `CodexB1x1`
  - convite criado e aceite;
  - botão `Iniciar` ativou para o admin;
  - `sala_1x1/{codigo}/estado` passou para `em_jogo`;
  - admin e convidado abriram `Jogo1x1Activity`;
  - sala e contas temporárias removidas no fim.
- Teste manual 2x2 com contas temporárias `CodexA2x2`, `CodexB2x2`, `CodexC2x2` e `CodexD2x2`
  - admin convidou 3 contas;
  - 2 convidados aceitaram em emuladores reais; a 4.ª conta ficou só como convidado pré-criado porque havia 3 emuladores ativos;
  - botão `Iniciar` ativou para o admin;
  - `sala_2x2/{codigo}/estado` passou para `em_jogo`;
  - admin e os 2 convidados presentes abriram `Jogo2x2Activity`;
  - sala e contas temporárias removidas no fim.
- Teste manual eliminatórias com convidados `CodexHost` e `CodexGuest`
  - admin criou sala eliminatórias;
  - convidado entrou;
  - convidado falhou por tempo esgotado, que usa o mesmo caminho `!acertouUltimaPergunta` da resposta errada;
  - `salas/{codigo}/jogadores/CodexGuest/estado` passou para `eliminado`;
  - `salas/{codigo}/estado` passou para `terminado`;
  - admin e eliminado chegaram a `PontuacoesActivity`;
  - sala temporária removida no fim.

### Pendentes

- Repetir o 2x2 com 4 clientes/emuladores reais em simultâneo.
- Repetir eliminatórias com toque explícito numa opção errada; o teste manual desta ronda cobriu o mesmo ramo de eliminação através de tempo esgotado.
- Não avançar ainda para `PontuacaoRepository`.

## Bugs corrigidos

- Modos de grupo/todos em `SalaDeEsperaGrupoActivity` passam a arrancar com admin/host + pelo menos 1 jogador real.
- O admin continua marcado com `isHostOnly=true`; jogadores que entram sem ser admin ficam com `isHostOnly=false`.
- O botão `Iniciar Jogo` só ativa para o admin quando existe pelo menos 1 jogador real além do host.
- Antes de mudar `salas/{codigo}/estado` para `em_jogo`, a sala volta a validar os jogadores no Firebase.
- O perfil de amigo deixou de mostrar detalhes/debug em baixo, incluindo estado, password, respostas certas e vitórias por modo.
- O fluxo de categorias personalizadas passou a mostrar uma lista direta, com ações por categoria: `Jogar`, `Editar` e `Eliminar`.
- `Editar` abre diretamente a categoria escolhida em `AdicionarPerguntaActivity`.
- `Eliminar` remove a categoria em `jogadores/{nomeUtilizador}/categoriasPersonalizadas/{categoria}` com confirmação.
- O fluxo de criar/editar categoria ganhou botão `Voltar` e preserva `nomeUtilizador`, `nomeJogador`, `modoJogo` e `admin`.
- O seletor de categorias usado por 1x1/2x2 ganhou botão `Voltar`.

## Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolhaCategoriaModosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `app/src/main/res/layout/activity_perfil_amigo.xml`
- `app/src/main/res/layout/activity_adicionar_pergunta.xml`
- `app/src/main/res/layout/activity_escolha_categoria_modos.xml`
- `TEST_REPORT.md`

## Testes feitos

- `./gradlew assembleDebug`
  - Falhou no ambiente atual porque o Java por defeito é `25.0.2`, incompatível com o Kotlin/Gradle usado neste projeto.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - Compilou debug/release e unit tests, mas falhou em `:app:lintDebug` por erro pré-existente em `AdicionarPerguntaActivity.kt:239` (`MissingSuperCall` em `onBackPressed`).
- Verificação estática dos fluxos alterados:
  - botão `Iniciar Jogo` usa mínimo 1 jogador real além do admin;
  - perfil de amigo já não referencia nem renderiza `txtDetalhesPerfil`;
  - lista de categorias personalizadas mantém a associação ao jogador registado;
  - botões `Voltar` adicionados preservam extras de utilizador/jogador.

## O que ficou pendente

- Teste manual em emulador/dispositivos com Firebase para confirmar visualmente:
  - modo grupo com admin + 1 jogador real;
  - perfil de amigo sem detalhes pequenos;
  - editar uma categoria específica diretamente;
  - eliminar uma categoria;
  - botões `Voltar` nos ecrãs alterados.
- Não executei esses testes UI/Firebase nesta ronda porque não há duas sessões/dispositivos controlados ativos no ambiente atual.

---

## Categorias públicas/partilhadas

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/UteisSala.kt`
- `app/src/main/res/layout/activity_explorar_categorias.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`
- `TEST_REPORT.md`

### Nova estrutura Firebase usada

- `categoriasPublicas/{categoriaId}`
  - `id`
  - `nome`
  - `descricao`
  - `criador`
  - `criadorId`
  - `nomeUtilizador`
  - `perguntas`
  - `usos`
  - `ratingMedio`
  - `totalAvaliacoes`
  - `dataCriacao`
  - `dataPublicacao`
  - `avaliacoes/{nomeUtilizador}`
- `jogadores/{nomeUtilizador}/categoriasPersonalizadas/{nomeCategoria}/categoriaPublicaId`
- `jogadores/{nomeUtilizador}/categoriasPersonalizadas/{nomeCategoria}/estadoPublicacao`
- `jogadores/{nomeUtilizador}/categoriasPersonalizadas/{nomeCategoria}/origemCategoriaPublica` nas cópias guardadas a partir de categorias públicas.
- `salas/{codigoSala}/categoriaPublica`
- `salas/{codigoSala}/categoriaPublicaId`
- `salas/{codigoSala}/criadorCategoriaPublica`
- `salas/{codigoSala}/criadorCategoriaPublicaId`

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- Verificação estática dos fluxos adicionados:
  - jogador registado consegue publicar uma categoria pessoal sem apagar a original;
  - publicação valida perguntas com 4 opções antes de criar/atualizar `categoriasPublicas`;
  - publicação usa id determinístico por criador + nome de categoria para evitar duplicados;
  - ecrã `Explorar Categorias` lista categorias públicas e mostra nome, criador, descrição curta, perguntas, usos e rating;
  - `Jogar` cria sala normal com as perguntas da categoria pública e incrementa `usos` por transação;
  - `Guardar` copia a categoria pública para categorias pessoais com `origemCategoriaPublica`;
  - avaliação usa `avaliacoes/{nomeUtilizador}` e transação para permitir uma avaliação por jogador registado.

### Bugs encontrados/corrigidos

- O contador `usos` foi ajustado para transação Firebase, evitando perdas quando duas salas são criadas quase ao mesmo tempo.
- A cópia guardada de uma categoria pública gera um nome disponível se o jogador já tiver uma categoria com o mesmo nome.
- Convidados e jogadores sem conta registada ficam bloqueados de publicar, guardar e avaliar pelo fluxo da app.

### O que ficou pendente

- Teste manual com duas contas reais no Firebase:
  - conta A cria e publica;
  - conta B vê em `Explorar Categorias`, joga, guarda cópia e edita apenas a cópia;
  - conta A atualiza/remove a pública original.
- Regras reais do Firebase Realtime Database. Não existe ficheiro de regras neste repositório e a app usa login próprio em `jogadores`, não Firebase Auth; por isso as permissões foram protegidas no cliente, mas a proteção server-side ainda precisa de regras/claims ou migração para Firebase Auth.

---

## Amigos em tempo real e migração leve de jogo

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/services/GameService.kt`
- `app/src/main/java/com/example/brainbrawl/services/ScoreService.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### Bugs sociais corrigidos

- Remoção de amizade passou a ser bilateral:
  - `jogadores/{A}/amigos/{B}` é removido.
  - `jogadores/{B}/amigos/{A}` também é removido.
- `PerfilAmigoActivity` continua a chamar `AmigosRepository.removerAmigo`, agora com a correção bilateral.
- `AmigosActivity` passou a observar em tempo real:
  - `jogadores/{nome}/amigos`
  - `jogadores/{nome}/pedidos_amizade`
  - `jogadores/{nome}/convites_recebidos`
- Os listeners sociais são removidos em `onStop` e `onDestroy`.

### Partes de jogo migradas

- `JogoRepository` assumiu o acesso Firebase de grupo em `salas/{codigoSala}` para:
  - obter admin e modo de jogo;
  - carregar perguntas;
  - escutar `perguntaAtualIndex`;
  - obter/atualizar `perguntaHoraInicio`;
  - limpar e escrever respostas;
  - obter jogadores;
  - remover jogador eliminado;
  - guardar pontuação final e total de respostas certas;
  - obter estado da sala;
  - escutar `.info/serverTimeOffset`.
- `GameService` ficou com lógica pura pequena:
  - tempo total por modo;
  - filtragem de jogadores restantes em eliminatórias;
  - decisão de fim de eliminatórias.
- `ScoreService` ficou com a fórmula atual de pontuação e bónus, sem alterar valores ou regras.

### Testes executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.

### Verificações estáticas

- `AmigosActivity`, `PerfilAmigoActivity` e `JogoActivity` já não usam `FirebaseDatabase` diretamente.
- Não foram alterados layouts, adapters, `Jogo1x1Activity.kt`, `Jogo2x2Activity.kt`, pontuações nem estrutura Firebase.
- Os nomes de nodes sociais e de jogo foram mantidos.

### Pendentes de teste manual

- Adicionar amigo.
- Aceitar pedido.
- Remover amigo e confirmar remoção nos dois lados.
- Confirmar que pedido aparece sem sair do ecrã.
- Confirmar que convite aparece sem sair do ecrã.
- Criar sala grupo.
- Entrar com jogador.
- Iniciar jogo clássico.
- Responder perguntas.
- Terminar jogo.
- Verificar pontuação/pódio.
- Testar modo caótico.
- Testar eliminatórias, se possível.

### Notas

- Nesta ronda foi usado `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` para os comandos Gradle.
- O `build` completo com o JBR do Android Studio passou na migração competitiva; a falha de lint registada anteriormente não voltou a ocorrer nesta execução.

---

## Migração competitiva 1x1 e 2x2

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/services/ScoreCompetitivoService.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo2x2Activity.kt`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- `SalaDeEspera1x1Activity.kt` deixou de usar Firebase diretamente para `sala_1x1`:
  - adicionar jogador;
  - marcar `prontos`;
  - ler `admin`;
  - escutar `jogadores`, `estado` e sala apagada;
  - verificar prontos;
  - mudar `estado` para `em_jogo`;
  - apagar sala ou remover jogador ao sair.
- `Jogo1x1Activity.kt` deixou de usar Firebase diretamente para:
  - ler `nomeCategoria`;
  - carregar ou criar `perguntas` com transação;
  - escutar `.info/serverTimeOffset`;
  - sincronizar `perguntaInicios/{index}` e `perguntaHoraInicio`;
  - guardar `pontuacoes/{nomeUtilizador}`;
  - aguardar pódio completo.
- `SalaDeEspera2x2Activity.kt` deixou de usar Firebase diretamente para `sala_2x2`:
  - adicionar jogador;
  - ler `admin`;
  - escutar `jogadores`, `estado` e sala apagada;
  - escrever `equipaA`, `equipaB`, `pontuacaoA` e `pontuacaoB`;
  - mudar `estado` para `em_jogo`;
  - apagar sala ou remover jogador/equipas ao sair.
- `Jogo2x2Activity.kt` deixou de usar Firebase diretamente para:
  - ler `nomeCategoria`;
  - identificar equipa;
  - carregar ou criar `perguntas` com transação;
  - guardar `respostas/{nomeUtilizador}/{perguntaAtualIndex}`;
  - guardar `pontuacoes_A`, `pontuacoes_B`, `totalPerguntasCertas_A` e `totalPerguntasCertas_B`;
  - escutar `.info/serverTimeOffset`;
  - sincronizar `perguntaInicios/{index}` e `perguntaHoraInicio`;
  - aguardar pódio completo.
- A fórmula de pontuação competitiva usada por 1x1 e 2x2 passou para `ScoreCompetitivoService`, mantendo:
  - tempo base de 15 segundos;
  - bónus de sequência `+50`, `+75` e `+150`;
  - os mesmos toasts de bónus nas Activities.

### Mantido sem alterações

- Estrutura Firebase e nomes dos nós existentes.
- UI, layouts, textos principais e navegação.
- Fluxo de convites 1x1/2x2.
- `JogoActivity.kt` de grupo.
- `CategoriaRepository.kt` e ecrãs de categorias.
- Ecrãs de pontuação, exceto por continuarem a receber os mesmos dados vindos dos jogos.

### Testes executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Testes unitários adicionados

- `ScoreCompetitivoService`:
  - pontuação sem bónus;
  - bónus de 2 respostas certas;
  - bónus de 3 respostas certas;
  - bónus máximo para 4 ou mais respostas certas.

### Verificações estáticas

- `SalaDeEspera1x1Activity.kt`, `Jogo1x1Activity.kt`, `SalaDeEspera2x2Activity.kt` e `Jogo2x2Activity.kt` já não importam `FirebaseDatabase`, `ValueEventListener`, `DataSnapshot` nem `DatabaseError`.
- Os nomes `sala_1x1` e `sala_2x2` ficaram centralizados em `JogoCompetitivoRepository`.
- Não foram alterados layouts, adapters, convites, `JogoActivity.kt` de grupo ou categorias.

### Pendentes de teste manual

- Criar convite 1x1.
- Aceitar convite 1x1.
- Jogar 1x1 até ao fim.
- Verificar pódio 1x1.
- Criar convite 2x2.
- Aceitar convite 2x2.
- Jogar 2x2 até ao fim.
- Verificar pódio 2x2.
- Confirmar que estatísticas continuam a atualizar.

Não executei estes testes manuais nesta ronda porque o ambiente atual não tem duas sessões/dispositivos Firebase ativos controlados.

---

## Models e constantes/config

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/models/Pergunta.kt`
- `app/src/main/java/com/example/brainbrawl/models/Convite.kt`
- `app/src/main/java/com/example/brainbrawl/models/Jogador.kt`
- `app/src/main/java/com/example/brainbrawl/models/SalaGrupo.kt`
- `app/src/main/java/com/example/brainbrawl/models/Sala1x1.kt`
- `app/src/main/java/com/example/brainbrawl/models/Sala2x2.kt`
- `app/src/main/java/com/example/brainbrawl/models/Categoria.kt`
- `app/src/main/java/com/example/brainbrawl/models/Pontuacao.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/config/GameConstants.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/UteisJogo.kt`
- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ConviteAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/SalaRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/CategoriaRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/services/GameService.kt`
- `app/src/main/java/com/example/brainbrawl/services/ScoreService.kt`
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- `Pergunta` saiu do pacote default e passou para `models/Pergunta.kt`.
- O modelo de convite usado por amigos/convites passou para `models/Convite.kt`.
- Foram criados modelos graduais para jogador, salas grupo/1x1/2x2, categoria e pontuação, todos como `data class` com defaults para compatibilidade Firebase.
- Foram criadas constantes para paths Firebase, extras de intents e modos/estados.
- Repositories e services passaram a usar `FirebasePaths` e `GameConstants` para os nomes pedidos, mantendo os mesmos valores de Firebase.

### Mantido sem alterações

- Estrutura Firebase.
- Nomes reais dos nodes e campos Firebase.
- Valores dos extras de intents.
- UI, layouts e navegação.
- Regras de pontuação, estados e modos.

### Pendentes

- Substituir strings de extras nas Activities por `IntentExtras`, gradualmente.
- Decidir numa fase posterior se DTOs locais dos repositories devem passar para `models/`.
- Testes manuais completos em dispositivos/sessões reais continuam necessários.

### Como testar

- Correr `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`.
- Correr `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`.
- Correr `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`.
- Validar manualmente login, criação/entrada em sala, convites 1x1/2x2, jogo grupo, jogo 1x1, jogo 2x2 e pódios.
- Confirmar no Firebase que continuam a ser usados os mesmos paths: `jogadores`, `salas`, `sala_1x1`, `sala_2x2`, `categorias`, `categoriasPersonalizadas`, `categoriasPublicas`, `amigos`, `pedidos_amizade`, `convites_recebidos` e `convites_enviados`.

### Testes executados nesta ronda

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa nos repositories/services já não encontra hardcoded as strings pedidas para Firebase, extras, estados e modos.
- `Pergunta` já não é importado a partir do pacote default.
- `Convite1x1` deixou de existir como modelo; os fluxos de convite usam `models.Convite`.

---

## IntentExtras - Bloco 1

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/LoginActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/UteisNavegacao.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherModoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leituras e escritas de extras nestes ficheiros passaram a usar `IntentExtras`.
- `UteisNavegacao` recebeu constantes para extras ainda existentes no fluxo de pontuação: `totalPontos`, `numeroPerguntasCertas`, `totalPerguntascertas` e `equipa`.
- `EscolherModoActivity` passou a usar `GameConstants` para `classico` e `eliminatorias` nos pontos tocados.

### Mantido sem alterações

- Valores dos extras.
- Navegação entre Activities.
- UI e textos.
- Compatibilidade com extras antigos, porque as constantes mantêm exatamente os mesmos nomes.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa nos cinco ficheiros do bloco não encontrou literais dos extras migrados.

### Pendentes

- Migrar extras nas restantes Activities em blocos pequenos.

---

## IntentExtras - Bloco 2 Salas

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/EsperaEliminadoActivity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leituras/escritas de extras de sala passaram a usar `IntentExtras`: `codigoSala`, `nomeUtilizador`, `nomeJogador`, `nomeCategoria`, `modoJogo` e `admin`.
- `EsperaEliminadoActivity` passou a usar `IntentExtras` para transportar os resultados até ao pódio: `totalPontos`, `numeroPerguntasCertas`, `totalPerguntascertas`, `respostasCertas` e `totalPerguntas`.
- Os extras legados `categoria` e `respostasCertas` foram centralizados em `IntentExtras`, mantendo os valores antigos.
- Estados/modos comparados nos ficheiros tocados passaram a usar `GameConstants` quando estavam no mesmo bloco.

### Mantido sem alterações

- Navegação.
- UI e textos.
- Valores dos extras.
- Compatibilidade com convidados e utilizadores registados.
- Compatibilidade com extras legados como `categoria`, `totalPerguntascertas` e `respostasCertas`.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa de `getStringExtra`/`putExtra`/`get*Extra` nos cinco ficheiros do bloco não encontrou literais dos extras migrados.
- A única ocorrência literal remanescente de `"pontuacao"` em `SalaDeEsperaActivity.kt` pertence ao mapa Firebase do jogador da sala, não a um extra de Intent.

### Pendentes

- Migrar extras nas restantes Activities em blocos pequenos, sem mexer ainda em jogos, pontuações, amigos, convites ou categorias avançadas.

---

## IntentExtras - Bloco 3 Jogos

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo2x2Activity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leituras iniciais de `codigoSala`, `nomeUtilizador`, `nomeJogador` e `nomeCategoria` passaram a usar `IntentExtras`.
- Redirecionamentos de `JogoActivity` para `EsperaEliminadoActivity` e `PontuacoesActivity` passaram a usar `IntentExtras`.
- Os modos `1x1`, `2x2` e `eliminatorias`, estados `terminado`/`eliminado` e marcador `admin` tocados no bloco passaram a usar `GameConstants`.
- `Jogo1x1Activity` e `Jogo2x2Activity` continuam a chamar `UteisNavegacao.enviarPontuacaoActivity`, preservando a montagem centralizada dos extras de pontuação.

### Mantido sem alterações

- Navegação para pontuações.
- Valores dos extras.
- UI e textos.
- Fluxos de convidados e utilizadores registados.
- Compatibilidade com extras legados como `totalPerguntascertas` e `respostasCertas`.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa de `getStringExtra("...")` e `putExtra("...")` em `JogoActivity.kt`, `Jogo1x1Activity.kt` e `Jogo2x2Activity.kt` não encontrou literais de extras.
- A única ocorrência literal remanescente de `"A"`/`"B"` em `Jogo2x2Activity.kt` é comentário explicativo.

### Pendentes

- Migrar extras nas restantes Activities em blocos pequenos, sem mexer ainda em pontuações, amigos, convites ou categorias avançadas.

---

## IntentExtras - Bloco 4 Pontuações

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leituras de extras de resultados passaram a usar `IntentExtras`: `codigoSala`, `nomeUtilizador`, `nomeJogador`, `nomeCategoria`, `totalPontos`, `totalPerguntas`, `totalRespostasCertas` e `equipa`.
- Navegação de desforra em `Pontuacao1x1Activity` passou a escrever extras com `IntentExtras`.

### Mantido sem alterações

- Navegação de voltar para Main.
- Navegação de desforra/replay 1x1.
- Valores dos extras.
- Atualização de estatísticas via `PontuacaoRepository`.
- UI e textos.
- Acesso Firebase ainda existente nestas Activities.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa de `getStringExtra("...")`, `getDoubleExtra("...")`, `getIntExtra("...")` e `putExtra("...")` nas três Activities de pontuação não encontrou literais de extras.
- A ocorrência literal remanescente de `nomeCategoria` em `Pontuacao1x1Activity.kt` pertence ao campo Firebase da sala de desforra, não a um extra de Intent.

### Pendentes

- Migrar extras nas restantes Activities em blocos pequenos, sem mexer ainda em amigos, convites ou categorias avançadas.

---

## IntentExtras - Bloco 5 Social/Perfil

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leituras/escritas de extras sociais passaram a usar `IntentExtras`: `nomeUtilizador`, `nomeAmigo`, `codigoSala` e `nomeCategoria`.
- Aceitação de convite em `AmigosActivity` passou a comparar o modo 2x2 com `GameConstants.MODO_2X2`.
- Foi criada a constante `IntentExtras.NOME_AMIGO = "nomeAmigo"`.

### Mantido sem alterações

- Navegação entre amigos, perfil e salas de espera por convite.
- Valores dos extras.
- Fluxos de convite 1x1 e 2x2.
- UI e textos.
- Acesso social via repositories.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa de `getStringExtra("...")` e `putExtra("...")` nos cinco ficheiros do bloco não encontrou literais de extras.

### Pendentes

- Migrar extras nas restantes Activities em blocos pequenos, sem mexer ainda em categorias.

---

## IntentExtras - Bloco 6 Categorias

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolhaCategoriaModosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- Leituras/escritas de extras de categorias passaram a usar `IntentExtras`: `nomeUtilizador`, `nomeJogador`, `nomeCategoria`, `modoJogo` e `admin`.
- Os valores de modo tocados neste bloco passaram a usar `GameConstants`: `classico`, `1x1` e `2x2`.
- Fluxos de criacao/edicao de categorias, exploracao de categorias publicas e escolha de categoria para 1x1/2x2 mantiveram os mesmos dados transportados por Intent.

### Mantido sem alterações

- Navegacao.
- UI e textos.
- Valores dos extras.
- Criar, editar e eliminar categorias personalizadas.
- Categorias publicas, guardar copia e avaliacao.
- Escolha de categoria para grupo, 1x1 e 2x2.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa de `getStringExtra("...")`, `getBooleanExtra("...")` e `putExtra("...")` nos quatro ficheiros do bloco nao encontrou literais de extras.
- A pesquisa global ainda encontra extras hardcoded fora deste bloco em `RegistarActivity.kt` e `AmigoAdapter.kt`, mantidos para migracao posterior.

### Pendentes

- Migrar extras soltos nos restantes ficheiros em blocos pequenos, sem alterar navegacao nem valores dos extras.

---

## IntentExtras - Bloco 7 Revisão Final

### Ficheiros onde ainda havia extras hardcoded

- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
  - `putExtra("nomeUtilizador", ...)`
- `app/src/main/java/com/example/brainbrawl/AmigoAdapter.kt`
  - `putExtra("nomeUtilizador", ...)`
  - `putExtra("nomeAmigo", ...)`

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AmigoAdapter.kt`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi migrado

- `RegistarActivity` passou a enviar `nomeUtilizador` com `IntentExtras.NOME_UTILIZADOR`.
- `AmigoAdapter` passou a abrir `MeuPerfilActivity` e `PerfilAmigoActivity` usando `IntentExtras.NOME_UTILIZADOR` e `IntentExtras.NOME_AMIGO`.

### Mantido sem alterações

- Navegacao apos registo.
- Navegacao da lista de amigos para perfil proprio e perfil de amigo.
- Valores dos extras (`nomeUtilizador` e `nomeAmigo`).
- UI, textos e estrutura Firebase.
- Strings de Firebase e campos de dados, que nao foram substituidos por serem contratos da base de dados e nao extras de Intent.

### Verificações executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Verificações estáticas

- Pesquisa global em Kotlin por `putExtra("...")`, `getStringExtra("...")`, `getBooleanExtra("...")`, `getIntExtra("...")`, `getDoubleExtra("...")`, `hasExtra("...")` e `removeExtra("...")` nao encontrou literais restantes.
- Verificacao especifica de `UteisNavegacao`, `UteisSala` e `EsperaEliminadoActivity` confirmou que nao havia extras hardcoded a migrar nesses ficheiros.

### Pendentes

- Nenhum extra hardcoded ficou por migrar nas chamadas de Intent pesquisadas em Kotlin.

---

## Firebase Rules - Fase Baseline

### Ficheiros alterados

- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `TEST_REPORT.md`

### O que foi criado

- Criado `firebase-rules.json` com regras baseline para Firebase Realtime Database.
- As rules mantem a estrutura Firebase atual: `jogadores`, `salas`, `sala_1x1`, `sala_2x2`, `categorias`, `categoriasPublicas`, categorias personalizadas, amigos, pedidos e convites.
- Paths desconhecidos ficam bloqueados por defeito.
- `categorias` fica apenas de leitura.
- Nodes principais passam a validar tipos basicos e a rejeitar campos inesperados nos objetos principais.
- `jogadores/{nome}/password` fica validado como hash SHA-256 hexadecimal e nao pode ser trocado numa atualizacao normal de jogador existente.

### Mantido sem alterações

- Codigo Kotlin da app.
- Estrutura Firebase existente.
- Login manual por `jogadores/{nome}/password`.
- Fluxos de salas, amigos, convites, categorias, pontuacoes e estatisticas.

### Limitações assumidas

- Como a app ainda nao usa Firebase Auth, as rules nao conseguem provar a identidade real do jogador.
- Leituras em `jogadores` continuam abertas porque o login manual e alguns fluxos sociais precisam ler perfis/password hash.
- Escritas em salas e categorias publicas ainda dependem fortemente do cliente para respeitar regras de negocio.
- Seguranca forte exige migracao futura para Firebase Auth, com `auth.uid`, ownership nos dados e, idealmente, Cloud Functions/backend para resultados e estatisticas.

### Verificações executadas

- Analise estatica dos paths Firebase usados em Kotlin.
- `jq empty firebase-rules.json`
  - OK.
- Testes funcionais contra Firebase real ainda nao executados nesta fase.

---

## Utilitarios - Organizacao Baseline

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/utils/UteisValidacao.kt`
- `app/src/main/java/com/example/brainbrawl/routes/UteisNavegacao.kt`
- `app/src/main/java/com/example/brainbrawl/utils/CodigoSalaUtils.kt`
- `app/src/main/java/com/example/brainbrawl/utils/UteisPerguntas.kt`
- `app/src/main/java/com/example/brainbrawl/utils/UteisFirebase.kt`
- `app/src/main/java/com/example/brainbrawl/utils/UteisConquistas.kt`
- Imports das Activities/repositories que usavam estes helpers.
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### O que foi organizado

- `UteisValidacao` passou para `utils`.
- `UteisNavegacao` passou para `routes`.
- `gerarCodigoSala` foi separado para `CodigoSalaUtils`.
- `obterOpcoesAleatorias` foi separado para `UteisPerguntas`.
- `UteisFirebase` e `UteisConquistas` passaram para `utils`.
- `UteisJogo`, `UteisSala` e `UteisDicas` foram revistos e mantidos no package principal por ainda terem responsabilidades com UI, som, Firebase, repositories ou navegacao.

### Mantido sem alterações

- UI e layouts.
- Logica de jogo.
- Fluxos de navegacao.
- Estrutura Firebase.
- Repositories, services, models e Firebase Rules.

### Verificações executadas

- Pesquisa estatica para imports antigos de `UteisValidacao`, `UteisNavegacao`, `UteisConquistas`, `UteisFirebase`, `UteisSala.gerarCodigoSala` e `UteisJogo.obterOpcoesAleatorias`.
  - OK, sem imports antigos.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.

---

## Migracao UID - Bloco 1 Amigos

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/AmigosViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/PerfilAmigoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/AmigoAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/PedidoAmizadeAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Convidar1x1AmigoAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/Convidar2x2AmigoAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/models/UtilizadorSocial.kt`
- `app/src/main/java/com/example/brainbrawl/models/PedidoAmizade.kt`
- `app/src/main/java/com/example/brainbrawl/models/Convite.kt`
- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `firebase-rules.json`
- `TEST_REPORT.md`

### O que foi migrado

- Adicionado `UtilizadorSocial` para transportar `uid`, `nomeUtilizador`, chave real do perfil e chave de origem antiga.
- `AmigosRepository` passou a resolver jogadores por UID primeiro e por `nomeUtilizador` como fallback.
- Listas de amigos, pedidos e convites passam a usar identidade interna por UID quando existe.
- A UI continua a mostrar `nomeUtilizador` via `nomeDisplay`.
- Listeners sociais observam o caminho principal e tambem o caminho antigo por nome quando for diferente, para manter dados antigos visiveis.
- Pedidos de amizade carregam/removem chaves antigas e novas para evitar duplicados presos durante a fase hibrida.
- Convites recebidos mantem a chave original do convite para aceitar/remover convites antigos sem depender de todos os dados ja estarem migrados.
- `PerfilAmigoActivity` recebe opcionalmente `uidAmigo`, mas continua compatível com `nomeAmigo`.
- Fluxos de convite 1x1/2x2 usam UID para os nodes sociais; a sala criada continua com nomes para nao antecipar a migracao do bloco Salas.
- `firebase-rules.json` passou a permitir writes sociais autenticados quando `auth.uid` corresponde ao dono do node, a chave do outro utilizador ou ao `nomeUtilizador` legado resolvido a partir de `jogadores/{auth.uid}`.

### Mantido sem alterações

- UI, layouts, textos e navegacao visual.
- Extras existentes `nomeUtilizador` e `nomeAmigo`.
- Estrutura das salas `sala_1x1` e `sala_2x2`, para nao misturar este bloco com Salas/Jogo.
- Compatibilidade com amigos, pedidos e convites guardados com chave antiga por nome.

### Verificações executadas

- `./gradlew test`
  - Bloqueado no ambiente local com Java `25.0.2`, antes da compilacao do projeto: `JavaVersion.parse(25.0.2)`.
- `jq empty firebase-rules.json`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

### Ainda usa `nomeUtilizador`

- Salas e sala de espera.
- Jogo 1x1, 2x2 e grupo.
- Pontuacoes e estatisticas.
- Categorias personalizadas/publicas.
- Navegacao geral e extras de compatibilidade.
- Display de perfil, amigos, pedidos e convites.

### Proximo bloco sugerido

- Salas, antes de Jogo. As salas sao o contrato que liga convites, espera, admin, equipas e arranque do jogo; migrar esse ponto primeiro reduz o risco quando o bloco Jogo passar a usar UID.

---

## Correcao definitiva - convites 1x1/2x2 em fase hibrida UID/nome

### Causa exata

- O envio de convites misturava `chavePerfil` como dono do node social com `chavePrimaria` como subchave. Como `chavePrimaria` prefere `uid`, aceitar/remover podia procurar a copia enviada numa subchave diferente da que foi escrita.
- No 1x1, quando o perfil real ainda estava em `jogadores/{nomeUtilizador}` sem campo `uid`, o utilizador atual podia ser resolvido sem o `uid` do Firebase Auth. Assim `adminUid` ficava vazio e as rules podiam rejeitar todo o `updateChildren`, incluindo sala e convites.
- As Activities de convite mostravam sucesso e navegavam para a sala sem esperar pelo `Task` do Firebase, mascarando falhas de rules/paths.
- `firebase-rules.json` bloqueava campos novos em `convites_recebidos`/`convites_enviados` por causa de `$other: false`.

### Caminhos Firebase finais

- Convite recebido: `jogadores/{destinatario.chavePerfil}/convites_recebidos/{remetente.chaveConvite}`.
- Convite enviado: `jogadores/{remetente.chavePerfil}/convites_enviados/{destinatario.chaveConvite}`.
- `chaveConvite`: `uid` quando existe; fallback para `chavePerfil`; fallback final para `nomeUtilizador`/origem.
- Conteudo do convite: `estado`, `codigoSala`, `modo`, `nomeCategoria`, `remetenteUid`, `remetenteChavePerfil`, `remetenteNome`, `destinatarioUid`, `destinatarioChavePerfil`, `destinatarioNome`.
- Sala 1x1: `sala_1x1/{codigo}/jogadores/{chaveConvite}` com `uid`, `nome`, `nomeDisplay` e `nomeUtilizador` quando disponiveis.
- Sala 2x2: `sala_2x2/{codigo}/jogadores/{chaveConvite}` com os mesmos campos dos quatro jogadores.

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/models/UtilizadorSocial.kt`
- `app/src/main/java/com/example/brainbrawl/models/Convite.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/AmigosViewModel.kt`
- `firebase-rules.json`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`
- `FIREBASE_RULES_NOTES.md`

### Verificacoes executadas

- `jq empty firebase-rules.json`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.

### Checklist manual obrigatorio

1x1:
1. Conta A convida Conta B.
2. Confirmar no Firebase `jogadores/{chavePerfilB}/convites_recebidos/{chaveConviteA}` e `jogadores/{chavePerfilA}/convites_enviados/{chaveConviteB}`.
3. Confirmar `sala_1x1/{codigo}/jogadores` com A e B, `uid` quando existir, `nomeDisplay` e `nomeUtilizador`.
4. Conta B ve o convite.
5. Conta B aceita.
6. Ambos entram ou conseguem entrar na sala.
7. Sala mostra os nomes dos dois jogadores.
8. Admin inicia jogo.

2x2:
1. Conta A convida B, C e D.
2. Confirmar cada `jogadores/{chavePerfilDestinatario}/convites_recebidos/{chaveConviteA}`.
3. Confirmar `sala_2x2/{codigo}/jogadores` com 4 jogadores e `nomeDisplay`.
4. B, C e D veem o convite.
5. Todos aceitam.
6. Sala mostra os 4 nomes.
7. Admin inicia jogo.

---

## Matchmaking automatico 1x1/2x2

### Implementacao

Criado matchmaking automatico para jogadores autenticados:

- `1x1 Aleatorio` na Main cria/entra em fila 1x1.
- `2x2 Aleatorio` na Main cria/entra em fila 2x2.
- `MatchmakingActivity` mostra procura, loading e botao cancelar.
- Quando existe grupo suficiente, a fila e limpa e cada jogador recebe o resultado em `matchmaking/{modo}/resultados/{uid}`.
- O criador do match cria a sala por transacao; os restantes aguardam a sala existir antes de navegar.

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/MatchmakingActivity.kt`
- `app/src/main/java/com/example/brainbrawl/models/MatchmakingModels.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `app/src/main/res/layout/activity_matchmaking.xml`

### Ficheiros alterados

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/config/GameConstants.kt`
- `app/src/main/res/layout/activity_main.xml`
- `firebase-rules.json`
- `ARCHITECTURE_PLAN.md`
- `FIREBASE_RULES_NOTES.md`
- `TEST_REPORT.md`

### Estrutura Firebase

Fila:

- `matchmaking/1x1/fila/{uid}`
- `matchmaking/2x2/fila/{uid}`

Campos:

- `uid`
- `nomeUtilizador`
- `nomeDisplay`
- `avatar`
- `timestampEntrada`
- `estado = aguardando`

Resultado:

- `matchmaking/1x1/resultados/{uid}`
- `matchmaking/2x2/resultados/{uid}`

Campos:

- `uid`
- `codigoSala`
- `modo`
- `nomeCategoria`
- `criadorUid`
- `estado = encontrado`
- `timestampEntrada`
- `jogadores/{uidJogador}`

Salas criadas:

- `sala_1x1/{codigoSala}` com 2 jogadores, `admin`, `adminId`, `adminUid`, `estado = em_espera`, `nomeCategoria`.
- `sala_2x2/{codigoSala}` com 4 jogadores, `equipaA`, `equipaB`, `admin`, `adminId`, `adminUid`, `estado = em_espera`, `nomeCategoria`.

### Regras Firebase adicionadas

- `matchmaking` requer `auth != null`.
- `fila/{uid}` valida que `uid` do payload e igual a chave.
- `resultados/{uid}` valida `uid`, `codigoSala`, `modo`, `estado`, `timestampEntrada` e jogadores por UID.
- `avatar` passou a ser permitido nos jogadores de `sala_1x1` e `sala_2x2`.

### Protecoes de corrida/abuso

- UID e a chave da fila, evitando duplicacao simples do mesmo jogador.
- A selecao de grupo usa transacao em `matchmaking/{modo}`.
- A criacao de sala usa transacao no node da sala e so cria se estiver vazio.
- Cancelamento remove apenas a fila quando ainda nao existe resultado.
- `onDisconnect()` remove a entrada propria de fila se a ligacao cair.
- Entradas antigas sao limpas por clientes que entram depois.
- O jogador e removido da fila do outro modo ao entrar em nova procura.
- 1x1 exige outro UID; 2x2 exige mais 3 UIDs.

### Verificacoes executadas

- `jq empty firebase-rules.json`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

Observacoes:

- O build continua a mostrar o warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Testes manuais a executar

1x1:

1. Conta A toca em `1x1 Aleatorio`.
2. Confirmar `matchmaking/1x1/fila/{uidA}`.
3. Conta B toca em `1x1 Aleatorio`.
4. Confirmar que `sala_1x1/{codigo}` tem A e B.
5. Confirmar que `matchmaking/1x1/fila` fica sem A/B.
6. Confirmar que ambos entram na mesma `SalaDeEspera1x1Activity`.
7. Confirmar nomes e arranque normal do jogo.

2x2:

1. Contas A, B e C entram em `2x2 Aleatorio`.
2. Confirmar que nenhuma sala e criada com apenas 3 jogadores.
3. Conta D entra em `2x2 Aleatorio`.
4. Confirmar que `sala_2x2/{codigo}` tem 4 jogadores.
5. Confirmar `equipaA` com 2 jogadores e `equipaB` com 2 jogadores.
6. Confirmar que `matchmaking/2x2/fila` fica sem A/B/C/D.
7. Confirmar que todos entram na mesma `SalaDeEspera2x2Activity` e o jogo inicia normalmente.

Edge cases:

1. Mesmo jogador toca varias vezes rapidamente.
2. Jogador cancela durante procura.
3. Jogador fecha a app durante procura.
4. Jogador tenta entrar em 1x1 e 2x2.
5. Dois dispositivos com o mesmo UID entram ao mesmo tempo.
6. Jogador cancela depois da sala ja ter sido encontrada.
7. Entradas stale com mais de 2 minutos nao bloqueiam novos matches.

### Riscos que ficaram

- A arbitragem forte contra cliente malicioso continua limitada por Realtime Database client-side. Cloud Functions seriam o passo ideal para impedir totalmente claims fabricados.
- Codigo de sala de 6 caracteres pode colidir em teoria; a transacao impede sobrescrever sala existente e devolve erro nesse caso.
- Limpeza stale acontece quando outro cliente entra na fila; sem backend agendado nao ha limpeza periodica autonomamente.
- Ainda nao existe indice global de `salaAtual/{uid}`. A UI normal so volta a matchmaking depois de sair da sala, mas um cliente malicioso ainda poderia tentar escrever diretamente enquanto esta numa sala antiga.

---

## Historico de jogos

### Implementacao

Criado historico por jogador autenticado:

- Grava entradas em `historicoJogos/{uid}/{historicoId}`.
- Usa `historicoId` deterministico por modo/codigo da sala para evitar duplicacao ao reabrir pontuacao.
- Carrega os ultimos 50 jogos com `orderByChild(dataHora).limitToLast(50)`.
- Mostra ecran `HistoricoActivity` com lista do mais recente para o mais antigo.
- Adicionado atalho de historico na Main com icon `ic_book`.

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/HistoricoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/HistoricoAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/models/HistoricoJogo.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/HistoricoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/HistoricoViewModel.kt`
- `app/src/main/res/layout/activity_historico.xml`
- `app/src/main/res/layout/item_historico_jogo.xml`

### Ficheiros alterados

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/res/layout/activity_main.xml`
- `firebase-rules.json`
- `ARCHITECTURE_PLAN.md`
- `FIREBASE_RULES_NOTES.md`
- `TEST_REPORT.md`

### Estrutura Firebase

- `historicoJogos/{uid}/{historicoId}/modo`
- `historicoJogos/{uid}/{historicoId}/codigoSala`
- `historicoJogos/{uid}/{historicoId}/nomeCategoria`
- `historicoJogos/{uid}/{historicoId}/pontuacao`
- `historicoJogos/{uid}/{historicoId}/recordeFoiBatido`
- `historicoJogos/{uid}/{historicoId}/respostasCertas`
- `historicoJogos/{uid}/{historicoId}/totalPerguntas`
- `historicoJogos/{uid}/{historicoId}/venceu`
- `historicoJogos/{uid}/{historicoId}/empate`
- `historicoJogos/{uid}/{historicoId}/equipa`
- `historicoJogos/{uid}/{historicoId}/dataHora`
- `historicoJogos/{uid}/{historicoId}/jogadores`

### Regras Firebase adicionadas

- `historicoJogos/{uid}` so pode ser lido/escrito por `auth.uid == uid`.
- `indexOn` em `dataHora`.
- Validacao dos campos conhecidos e bloqueio de `$other`.

### Testes manuais a executar

1. Jogar modo classico com conta autenticada e confirmar entrada em `historicoJogos/{uid}`.
2. Jogar 1x1 com conta autenticada e confirmar `modo = 1x1`, adversario em `jogadores` e resultado correto.
3. Jogar 2x2 com conta autenticada e confirmar `modo = 2x2`, `equipa`, quatro jogadores e resultado correto.
4. Reabrir o ecra de pontuacao da mesma sala e confirmar que nao cria novo `historicoId`.
5. Abrir `HistoricoActivity` a partir da Main e confirmar ordem mais recente primeiro.
6. Jogar sem conta e confirmar que nao escreve em `historicoJogos`.
7. Criar mais de 50 entradas para o mesmo UID em ambiente de teste e confirmar que ficam so as 50 mais recentes.

### Verificacoes executadas

- `jq empty firebase-rules.json`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

Observacoes:

- O warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used` continua presente.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

## Fase 1 UI/UX - base visual segura

### Objetivo validado

Foi criada uma base visual reutilizavel e aplicada de forma conservadora aos ecras principais, sem alterar regras Firebase, repositories existentes, services existentes, ViewModels de jogo, pontuacao, rankings ou navegacao.

### Ficheiros visuais criados

- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/drawable/bg_app_gradient.xml`
- `app/src/main/res/drawable/bg_card_surface.xml`
- `app/src/main/res/drawable/bg_button_primary.xml`
- `app/src/main/res/drawable/bg_button_secondary.xml`
- `app/src/main/res/drawable/bg_button_danger.xml`
- `app/src/main/res/drawable/bg_segment_selected.xml`
- `app/src/main/res/drawable/bg_segment_unselected.xml`
- `app/src/main/res/drawable/bg_input_surface.xml`
- `app/src/main/res/drawable/bg_empty_state_card.xml`

### Ficheiros visuais alterados

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/activity_registar.xml`
- `app/src/main/res/layout/activity_ranking.xml`
- `app/src/main/res/layout/activity_meu_perfil.xml`
- `app/src/main/res/layout/activity_amigos.xml`
- `app/src/main/res/layout/activity_sala_de_espera_1x1.xml`
- `app/src/main/res/layout/activity_sala_de_espera2x2.xml`
- `app/src/main/res/layout/activity_pontuacao.xml`
- `app/src/main/res/layout/activity_pontuacao1x1.xml`
- `app/src/main/res/layout/activity_pontuacao_multi.xml`
- `app/src/main/res/layout/item_ranking_jogador.xml`
- `app/src/main/res/layout/item_podio.xml`

### Compatibilidade de build

Durante a validacao, o projeto ja referenciava `HistoricoActivity` e `MatchmakingActivity` no manifesto/Main, mas alguns ficheiros de binding/classes nao existiam no checkout. Foram repostos ficheiros minimos necessarios para compilacao e para manter os destinos declarados:

- `app/src/main/java/com/example/brainbrawl/HistoricoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/HistoricoAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/models/HistoricoJogo.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/HistoricoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/HistoricoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/MatchmakingActivity.kt`
- `app/src/main/res/layout/activity_historico.xml`
- `app/src/main/res/layout/item_historico_jogo.xml`
- `app/src/main/res/layout/activity_matchmaking.xml`

### Verificacoes executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK. Mantem o warning conhecido: `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK. Sem testes unitarios debug compilados neste checkout (`NO-SOURCE`).
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK. Mantem o warning conhecido de `adminHint` e o aviso generico de deprecated Gradle features.

### Checklist manual UI recomendada

1. Abrir Login e Registo em 360dp e confirmar que teclado nao corta campos essenciais.
2. Abrir Main com utilizador autenticado e convidado, validando botoes, cards e textos longos.
3. Abrir Ranking em todas as tabs e confirmar estado loading/empty/error.
4. Abrir Perfil e confirmar legibilidade de avatar, nivel, XP, pontuacao e recorde.
5. Abrir salas 1x1/2x2 e confirmar que listas/equipas cabem em ecra pequeno.
6. Abrir Podio/Pontuacao e confirmar contraste, botoes e nomes longos.
7. Abrir Historico e confirmar lista vazia, lista com itens e datas.
8. Testar toque rapido em botoes principais para confirmar feedback visual e sem deslocamentos.

## QA urgente pre-proxima fase - 2026-05-08

### Bugs confirmados no codigo

- Matchmaking 1x1/2x2 mantinha resultados persistidos em `matchmaking/{modo}/resultados/{playerKey}` depois da navegacao e a sala de espera podia interpretar uma leitura inicial inexistente como sala encerrada.
- Entrada em sala aceitava codigo em minusculas sem normalizacao centralizada.
- Salas mostravam codigo sem acao rapida de copiar.
- `AmigosViewModel` publicava o proprio utilizador como primeiro item da lista de amigos.
- Login Auth dependia quase so de `jogadores/{uid}` e nao fazia fallback suficiente para perfis Auth encontrados por `uid` como campo ou por email.
- Registo permitia avancar para a etapa de perfil sem cumprir todos os requisitos mostrados na UI.
- Icones de mostrar password em Login/Registo nao tinham acao ligada.
- Textos com nomes longos tinham risco de cortar/forcar layout em perfil, amigos, ranking e salas.

### Bugs corrigidos

- Matchmaking passou a consumir o resultado apos emitir a navegacao, ignorar cancelamento depois de match encontrado e manter listeners durante recriacao de Activity.
- Salas competitivas/grupo so emitem `SalaEncerrada` depois de a sala ja ter sido vista pelo listener pelo menos uma vez.
- Campo de codigo de sala normaliza para maiusculas, limita a 6 caracteres e filtra caracteres nao alfanumericos.
- Codigo mostrado em salas 1x1/2x2/grupo pode ser copiado para clipboard com feedback "Codigo copiado".
- Bottom nav reutilizavel aplicada a Perfil, Ranking, Historico e Amigos, preservando `uid`, `nomeUtilizador`, `nomeJogador` e `email`.
- Lista de amigos deixou de incluir o proprio utilizador.
- `AvatarUtils` normaliza nomes em lowercase e continua a aceitar `avatar_1`, `avatar_1_playstore`, `@drawable/...` e extensoes comuns.
- Badges de perfil ficam escondidas quando os thresholds nao sao cumpridos, incluindo perfil de amigo inexistente.
- Login Auth tenta carregar perfil por UID, por campo `uid` e por email; Main tambem tenta fallback por email.
- Registo valida email, password, confirmacao, username e avatar antes de criar Auth/DB.
- Mostrar/ocultar password funciona em Login, password de Registo e confirmacao, mantendo cursor no fim.

### Ficheiros criados

- `app/src/main/java/com/example/brainbrawl/routes/BottomNavHelper.kt`

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/java/com/example/brainbrawl/HistoricoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/LoginActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MatchmakingActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RankingActivity.kt`
- `app/src/main/java/com/example/brainbrawl/RegistarActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/utils/AvatarUtils.kt`
- `app/src/main/java/com/example/brainbrawl/utils/CodigoSalaUtils.kt`
- `app/src/main/java/com/example/brainbrawl/utils/UteisValidacao.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/AmigosViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/LoginViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/RegistarViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/SalaGrupoViewModel.kt`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_meu_perfil.xml`
- `app/src/main/res/layout/activity_perfil_amigo.xml`
- `app/src/main/res/layout/activity_registar.xml`
- `app/src/main/res/layout/activity_sala_de_espera2x2.xml`
- `app/src/main/res/layout/activity_sala_de_espera_1x1.xml`
- `app/src/main/res/layout/item_amigo.xml`
- `app/src/main/res/layout/item_jogador_sala.xml`
- `app/src/main/res/layout/item_ranking_jogador.xml`

### Regras Firebase

- `firebase-rules.json` nao foi alterado nesta ronda.
- `FIREBASE_RULES_NOTES.md` nao foi atualizado por nao haver alteracao de rules.

### Verificacoes executadas

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
  - OK.

Observacoes:

- Continua o warning conhecido `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Testes manuais recomendados

1. Matchmaking 1x1 com duas contas Auth: confirmar uma unica sala, ambos no mesmo codigo e sem loop entra/sai.
2. Matchmaking 1x1 com convidado/Auth e convidado/convidado, se convidados forem suportados no ambiente Firebase atual.
3. Cancelar matchmaking antes de match e depois de match encontrado.
4. Criar sala grupo, copiar codigo, entrar noutra sessao escrevendo o codigo em minusculas.
5. Navegar Main, Perfil, Ranking, Historico e Amigos pela bottom nav, confirmando extras preservados.
6. Criar/usar nomes longos em Main, Perfil, Amigos, Ranking e salas.
7. Abrir Amigos com duas contas que se adicionaram e confirmar que o proprio utilizador nao aparece.
8. Testar avatares `avatar_1_playstore`, `avatar_8_playstore`, vazio e invalido no Firebase.
9. Criar conta nova, fazer logout, login por email/password e reabrir app com sessao persistente.
10. Validar registo com email invalido, password curta, confirmacao diferente, username repetido/invalido.
11. Testar mostrar/ocultar password em Login, Registo e confirmacao.

### Riscos pendentes

- Nao foi possivel executar testes manuais de Firebase/emulador nesta ronda automatica.
- A bottom nav foi adicionada programaticamente aos ecras principais; deve ser validada visualmente em dispositivos pequenos.
- O warning antigo `adminHint` permanece fora do escopo desta ronda.
