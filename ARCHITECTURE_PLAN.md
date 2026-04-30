# BrainBrawl - Architecture Plan

## Fase final UID/Auth hardening

Estado atualizado em 2026-04-30:

- `uid` fica como identificador primario para novos dados autenticados.
- `nomeUtilizador` continua apenas como display/fallback legado enquanto existirem dados antigos por nome.
- `adminUid` foi adicionado como campo novo de sala, mantendo `adminId` e `admin` por compatibilidade.
- As salas novas autenticadas escrevem `adminUid`/`adminId` com o UID do Firebase Auth.
- O jogo e as pontuacoes continuam a resolver jogadores por UID primeiro e por nome/chave antiga apenas quando a sala ou perfil antigo ainda precisa disso.
- `MeuPerfilActivity`/`MeuPerfilViewModel` passaram a carregar o perfil por UID primeiro.
- `UteisNavegacao` e ecras de modo/categoria/pontuacao/categorias publicas recuperam o UID tambem de `FirebaseAuth.currentUser` quando o extra nao veio na Intent.
- `CategoriaRepository` deixou de expor overloads publicos que aceitavam apenas `nomeUtilizador`; os metodos publicos de categorias personalizadas/publicas recebem `uid` e mantem fallback interno por nome.
- `AmigosRepository` passou a considerar `uid` tambem nas chaves de leitura social hibrida.
- `RankingActivity` adiciona um ranking global simples acessivel pela `MainActivity`, com `RankingViewModel`, `RankingRepository`, `RankingJogador` e `RecyclerView`.
- O ranking le `jogadores`, ordena por `pontuacao`, limita inicialmente a top 100 e mantém compatibilidade com perfis novos por UID e perfis legados por `nomeUtilizador`.
- O ranking passou a suportar multiplos tipos com o mesmo fluxo (`GLOBAL`, `SOLO`, `1x1`, `2x2`), mudando apenas o campo de ordenacao Firebase e mantendo fallback para `0` em campos ausentes.
- `firebase-rules.json` foi preparado para `auth.uid` em `jogadores/{uid}`, `salas`, `sala_1x1`, `sala_2x2` e `categoriasPublicas`, com excecoes legadas explicitas para convidados/dados antigos.

Decisao de compatibilidade:

- Dados antigos em `jogadores/{nomeUtilizador}` nao sao apagados nem migrados em massa.
- As leituras continuam tolerantes a `nomeUtilizador`, `nomeJogador`, `nomeDisplay`, `adminId` e chaves antigas de sala.
- Escritas novas com Auth devem cair em UID; se nao houver Auth ou UID, o fallback por nome continua limitado aos fluxos legados/convidados.

## Mapa atual do projeto

BrainBrawl e uma app Android nativa em Kotlin, com UI em XML/ViewBinding, Firebase Authentication e Firebase Realtime Database como backend. O ponto de entrada esta em `LoginActivity`, declarado como `MAIN/LAUNCHER` no `AndroidManifest.xml`.

Estrutura principal atual:

- `app/src/main/java/com/example/brainbrawl/`: Activities, adapters e utilitarios ligados a UI/Firebase ainda no pacote raiz.
- `app/src/main/java/com/example/brainbrawl/models/`: modelos Firebase simples, criados com `data class` e valores por defeito para compatibilidade com Realtime Database.
- `app/src/main/java/com/example/brainbrawl/config/`: constantes de paths Firebase, extras de intents e modos/estados de jogo.
- `app/src/main/java/com/example/brainbrawl/routes/`: helpers de navegacao e montagem de intents.
- `app/src/main/java/com/example/brainbrawl/utils/`: helpers puros, validacao, conversoes Firebase simples e constantes partilhadas.
- `app/src/main/res/layout/`: ecras XML das Activities e itens de listas.
- `app/src/main/res/drawable/`, `mipmap-*`, `raw/`: avatares, icones, fundos e sons.
- `app/src/test/` e `app/src/androidTest/`: testes base gerados pelo Android Studio.
- `app/google-services.json`: configuracao Firebase.
- `build.gradle.kts`, `app/build.gradle.kts`: configuracao Gradle/Android/Firebase.

Ficheiros principais e responsabilidades:

- `LoginActivity.kt`: login com Firebase Auth por email/password, fallback temporario para login antigo por nome/password e entrada como convidado.
- `RegistarActivity.kt`: cria conta Firebase Auth por email/password e perfil principal em `jogadores/{uid}`.
- `MainActivity.kt`: menu principal, cria sala, entra em sala, abre ranking global, abre amigos e logout.
- `RankingActivity.kt`: lista global de jogadores ordenada por pontuacao decrescente.
- `EscolherModoActivity.kt`, `TipoModoClassico.kt`, `EscolherCategoriaActivity.kt`, `EscolhaCategoriaModosActivity.kt`: selecao de modo/categoria.
- `SalaDeEsperaActivity.kt`: entrada por codigo em salas de grupo (`salas`).
- `SalaDeEsperaGrupoActivity.kt`: sala de espera dos modos de grupo em `salas`.
- `SalaDeEspera1x1Activity.kt`, `SalaDeEspera2x2Activity.kt`: salas competitivas dedicadas.
- `JogoActivity.kt`: jogo em grupo/classico/caotico/eliminatorias usando `salas`.
- `Jogo1x1Activity.kt`, `Jogo2x2Activity.kt`: jogos competitivos dedicados.
- `PontuacoesActivity.kt`, `Pontuacao1x1Activity.kt`, `Pontuacao2x2Activity.kt`: resultados e atualizacao de estatisticas.
- `AmigosActivity.kt`, adapters de amigos/convites/pedidos: amizade e convites.
- `UteisSala.kt`, `UteisJogo.kt`, `UteisDicas.kt`: helpers ainda ligados a Firebase, contexto Android, UI ou som.
- `routes/UteisNavegacao.kt`: helpers de navegacao e montagem de intents.
- `utils/UteisValidacao.kt`, `utils/CodigoSalaUtils.kt`, `utils/UteisPerguntas.kt`, `utils/UteisFirebase.kt`, `utils/UteisConquistas.kt`: helpers puros/seguros e constantes partilhadas.
- `models/Pergunta.kt`, `models/Convite.kt`, `models/Jogador.kt`, `models/SalaGrupo.kt`, `models/Sala1x1.kt`, `models/Sala2x2.kt`, `models/Categoria.kt`, `models/Pontuacao.kt`: modelos simples.

Fluxo principal:

1. `LoginActivity` autentica por Firebase Auth, reusa `currentUser`, aceita fallback legado por nome/password ou cria jogador temporario.
2. `MainActivity` recebe `uid`/`email` quando ha Firebase Auth e continua a receber `nomeUtilizador` ou `nomeJogador` para compatibilidade.
3. Ranking: `MainActivity` -> `RankingActivity` -> `RankingViewModel` -> `RankingRepository` consulta `jogadores` por `pontuacao` (global) ou por vitorias por modo (`totalVitoriasModoSolo`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2`).
4. Criar sala: `EscolherModoActivity` -> `TipoModoClassico`/categoria -> cria dados no Firebase.
5. Entrar em sala: `SalaDeEsperaActivity` valida codigo e adiciona jogador a `salas/{codigo}/jogadores`.
6. Sala de espera observa jogadores e `estado`.
7. Quando `estado = em_jogo`, abre `JogoActivity`, `Jogo1x1Activity` ou `Jogo2x2Activity`.
8. Jogo carrega perguntas, gere timer, respostas e pontuacao.
9. Resultado abre a Activity de pontuacao e atualiza estatisticas.
10. Logout volta ao login e marca `estado = off` para utilizadores registados.

## Problemas da arquitetura atual

- Activities concentram UI, navegacao, regras de jogo e acesso Firebase no mesmo ficheiro.
- Existem varios nomes para a mesma informacao: `categoria`, `nomeCategoria`, `pontuacao`, `totalPontos`, `totalPerguntascertas`, `totalRespostasCertas`.
- A estrutura Firebase esta dividida em `salas`, `sala_1x1`, `sala_2x2` com contratos diferentes.
- A autenticacao esta em migracao hibrida: novas contas usam Firebase Auth e perfil em `jogadores/{uid}`, mas varios fluxos ainda usam `nomeUtilizador` como chave temporaria.
- Muitos listeners sao anonimos e nao sao removidos em todas as Activities.
- As estatisticas sao atualizadas nas Activities de resultado e podem ser incrementadas mais de uma vez se a Activity recriar ou se listeners dispararem novamente.
- Modelos como `Pergunta` estao fora do pacote Kotlin, o que obriga imports de pacote default e dificulta organizacao futura.

## Arquitetura recomendada

Para este projeto, uma arquitetura em camadas inspirada em MVVM/MVC e mais adequada do que mover tudo para MVC puro de uma vez. As Activities podem ficar como Views/Controllers finos, enquanto Firebase e regras de jogo saem para services/repositories.

Estrutura criada para migracao futura:

- `config/`: constantes de Firebase, nomes de extras, chaves de database e configuracao global.
- `controllers/`: orquestracao de fluxos de ecras quando uma Activity ainda nao deve conter a regra completa.
- `models/`: data classes como `Pergunta`, `Convite`, `Jogador`, `Sala`, `Pontuacao`.
- `viewmodels/`: estado simples de ecras migrados para MVVM leve, mantendo Activities como UI/navegacao.
- `views/`: Activities/Fragments quando forem migrados do pacote raiz.
- `services/`: regras de negocio, por exemplo `GameService`, `ScoreService`, `AuthService`.
- `repositories/`: leitura/escrita Firebase, por exemplo `RoomRepository`, `UserRepository`, `QuestionRepository`.
- `routes/`: helpers de navegacao e nomes de extras.
- `utils/`: funcoes pequenas e puras, validacao, formatacao, hashing.
- `middlewares/`: validacoes transversais, guards de sessao/sala.
- `hooks/`: observadores/listeners reutilizaveis, util se a app evoluir para Compose ou componentes reativos.
- `components/`: UI reutilizavel, adapters e widgets.
- `assets/`: assets nao Android-res, se forem necessarios.
- `tests/`: testes unitarios por camada.

## Plano de migracao por fases

### Fase atual - models, config e repositories Firebase incrementais

Criado:

- `app/src/main/java/com/example/brainbrawl/models/Pergunta.kt`, movido do pacote default para `models`.
- `app/src/main/java/com/example/brainbrawl/models/Convite.kt`, substituindo o modelo antigo de convite por um modelo gradual `Convite`.
- `app/src/main/java/com/example/brainbrawl/models/Jogador.kt`.
- `app/src/main/java/com/example/brainbrawl/models/SalaGrupo.kt`.
- `app/src/main/java/com/example/brainbrawl/models/Sala1x1.kt`.
- `app/src/main/java/com/example/brainbrawl/models/Sala2x2.kt`.
- `app/src/main/java/com/example/brainbrawl/models/Categoria.kt`.
- `app/src/main/java/com/example/brainbrawl/models/Pontuacao.kt`.
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`, com nomes de nodes/fields Firebase mantendo exatamente os valores atuais.
- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`, com os nomes atuais dos extras de navegação.
- `app/src/main/java/com/example/brainbrawl/config/GameConstants.kt`, com estados, modos e identificadores partilhados.
- `app/src/main/java/com/example/brainbrawl/repositories/SalaRepository.kt`, como primeira camada de acesso Firebase para salas no node Firebase `salas`.
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`, como camada pequena para leituras simples do node Firebase `jogadores`.
- `app/src/main/java/com/example/brainbrawl/repositories/CategoriaRepository.kt`, como camada para categorias oficiais, categorias personalizadas, categorias publicas e perguntas.
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`, como camada pequena para amigos, pedidos de amizade e convites.
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`, como camada pequena para Firebase do jogo de grupo em `salas/{codigoSala}`.
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`, como camada pequena para Firebase competitivo em `sala_1x1/{codigoSala}` e `sala_2x2/{codigoSala}`.
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`, como camada pequena para leitura de resultados/podio e escrita de estatisticas dos jogadores.
- `app/src/main/java/com/example/brainbrawl/services/GameService.kt`, para regras puras pequenas de tempo/modos e eliminatorias.
- `app/src/main/java/com/example/brainbrawl/services/ScoreService.kt`, para o calculo puro de pontuacao do jogo de grupo.
- `app/src/main/java/com/example/brainbrawl/services/ScoreCompetitivoService.kt`, para o calculo puro de pontuacao dos modos 1x1 e 2x2.
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`, para calculo puro de taxa de acertos, vencedor por modo, podio 2x2 e validacao anti-duplicacao.
- `app/src/main/java/com/example/brainbrawl/services/AuthService.kt`, como wrapper pequeno de Firebase Auth para `currentUser`, registo, login e logout.
- `app/src/main/java/com/example/brainbrawl/viewmodels/AmigosViewModel.kt`, como ViewModel leve para expor amigos, pedidos de amizade, convites e eventos sociais simples.
- `app/src/main/java/com/example/brainbrawl/viewmodels/CategoriasViewModel.kt`, como ViewModel leve para categorias personalizadas, publicacao/remocao publica e eliminacao de categoria.
- `app/src/main/java/com/example/brainbrawl/viewmodels/ExplorarCategoriasViewModel.kt`, como ViewModel leve para observar categorias publicas, guardar copia e avaliar categoria.
- `app/src/main/java/com/example/brainbrawl/viewmodels/EditarCategoriaViewModel.kt`, como ViewModel leve para carregar, validar, guardar e eliminar perguntas de categorias personalizadas.
- `app/src/main/java/com/example/brainbrawl/viewmodels/LoginViewModel.kt`, como ViewModel leve para login Firebase Auth por email/password, sessao persistente por `currentUser`, fallback legado por nome/password, entrada como convidado e estado online.
- `app/src/main/java/com/example/brainbrawl/viewmodels/RegistarViewModel.kt`, como ViewModel leve para validacao de registo, criacao de conta Firebase Auth e criacao do perfil em `jogadores/{uid}`.
- `app/src/main/java/com/example/brainbrawl/viewmodels/MeuPerfilViewModel.kt`, como primeiro ViewModel leve para expor avatar e estatisticas do proprio perfil.
- `app/src/main/java/com/example/brainbrawl/viewmodels/PerfilAmigoViewModel.kt`, como primeiro ViewModel leve para expor avatar/estatisticas do amigo e evento simples de remocao de amigo.
- `app/src/main/java/com/example/brainbrawl/viewmodels/SalaGrupoViewModel.kt`, como ViewModel leve para entrada/sala de espera de grupo, jogadores, estado, inicio e saida da sala.
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`, como ViewModel leve para sala competitiva 1x1, prontos, jogadores, estado, inicio e saida.
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`, como ViewModel leve para sala competitiva 2x2, equipas, jogadores, estado, inicio e saida.
- `app/src/main/java/com/example/brainbrawl/viewmodels/EsperaEliminadoViewModel.kt`, como ViewModel leve para observar o fim da sala em eliminatorias.
- `app/src/main/java/com/example/brainbrawl/viewmodels/JogoViewModel.kt`, como ViewModel leve para jogo de grupo/classico/caotico/eliminatorias.
- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo1x1ViewModel.kt`, como ViewModel leve para jogo competitivo 1x1.
- `app/src/main/java/com/example/brainbrawl/viewmodels/Jogo2x2ViewModel.kt`, como ViewModel leve para jogo competitivo 2x2.
- Estrutura base de pacotes `models/`, `repositories/`, `services/`, `controllers/` e `utils/` mantida para a migracao gradual.

Movido nesta fase:

- Em `SalaDeEsperaGrupoActivity.kt`, as chamadas diretas a `FirebaseDatabase` para a sala de grupo foram substituidas por chamadas a `SalaRepository`.
- Em `SalaDeEsperaActivity.kt`, a procura de sala por codigo, a validacao de existencia/nome repetido e a adicao do jogador em `salas/{codigoSala}/jogadores` passaram para `SalaRepository`.
- Em `SalaDeEsperaActivity.kt`, a leitura de avatar do jogador registado passou para `JogadorRepository`.
- Em `UteisSala.kt`, a criacao de salas de grupo em `salas/{codigoSala}` passou a usar `SalaRepository.criarSala`.
- Em `LoginActivity.kt` e `MainActivity.kt`, a leitura de perfil para login e a atualizacao de estado online/offline passaram para `JogadorRepository`.
- Em `MeuPerfilActivity.kt` e `PerfilAmigoActivity.kt`, a leitura de perfil, avatar e estatisticas passou para `JogadorRepository`.
- Em `AmigosActivity.kt`, leituras simples de perfil/avatar/estado usadas na lista de amigos continuam em `JogadorRepository`; lista de amigos, pesquisa de jogador, envio/aceitacao de pedidos e carregamento/aceitacao de convites passaram para `AmigosRepository`.
- Em `AmigosActivity.kt`, amigos, pedidos de amizade e convites recebidos passaram a usar listeners em tempo real via `AmigosRepository`, removidos em `onStop`/`onDestroy`.
- Em `PerfilAmigoActivity.kt`, a remocao de amigo passou para `AmigosRepository`, mantendo a navegacao e UI na Activity.
- Em `ConvidarAmigo1x1Activity.kt`, carregar amigos, criar sala `sala_1x1` e escrever convites recebidos/enviados passaram para `AmigosRepository`.
- Em `ConvidarAmigo2x2Activity.kt`, carregar amigos, criar sala `sala_2x2` e escrever convites recebidos/enviados passaram para `AmigosRepository`.
- Em `JogoActivity.kt`, leitura de admin/modo, leitura de perguntas, listener de indice da pergunta, offset do servidor, escrita de respostas, atualizacao da pergunta atual, limpeza de respostas, jogadores restantes, eliminacao de jogador, estado da sala e resultado final do jogador passaram para `JogoRepository`.
- Em `JogoActivity.kt`, o calculo de tempo por modo passou para `GameService` e o calculo de pontuacao/bonus passou para `ScoreService`.
- Em `SalaDeEspera1x1Activity.kt`, a entrada do jogador, prontos, admin, listeners de jogadores/estado/sala apagada, verificacao de prontos, arranque do jogo e saida da sala passaram para `JogoCompetitivoRepository`.
- Em `Jogo1x1Activity.kt`, a leitura de categoria da sala, carregamento/criacao transacional das perguntas, offset do servidor, sincronizacao do inicio da pergunta, pontuacao final e espera pelo podio passaram para `JogoCompetitivoRepository`.
- Em `Jogo1x1Activity.kt`, a formula de pontuacao competitiva passou para `ScoreCompetitivoService`, mantendo os mesmos valores e toasts de bonus na Activity.
- Em `SalaDeEspera2x2Activity.kt`, a entrada do jogador, admin, listeners de jogadores/estado/sala apagada, escrita de `equipaA`, `equipaB`, `pontuacaoA`, `pontuacaoB`, arranque do jogo e saida da sala passaram para `JogoCompetitivoRepository`.
- Em `Jogo2x2Activity.kt`, a leitura de categoria da sala, identificacao da equipa, carregamento/criacao transacional das perguntas, respostas, resultado por equipa, offset do servidor, sincronizacao do inicio da pergunta e espera pelo podio passaram para `JogoCompetitivoRepository`.
- Em `Jogo2x2Activity.kt`, a formula de pontuacao competitiva passou para `ScoreCompetitivoService`, mantendo os mesmos valores e toasts de bonus na Activity.
- Em `PontuacoesActivity.kt`, a leitura de pontuacoes finais de grupo e a atualizacao de estatisticas dos jogadores passaram para `PontuacaoRepository`; a ordenacao do podio e vencedores passou para `EstatisticasService`.
- Em `Pontuacao1x1Activity.kt`, a leitura em tempo real de pontuacoes finais passou para `PontuacaoRepository`; a Activity mantem apenas UI, desforra e navegacao. A estatistica do proprio jogador e atualizada uma vez por jogador/sala para preservar as respostas certas vindas do intent.
- Em `Pontuacao2x2Activity.kt`, a leitura em tempo real das equipas/pontuacoes/respostas certas e a atualizacao de estatisticas passaram para `PontuacaoRepository`; a decisao de podio, vencedor da equipa e texto de empate/vitoria passou para `EstatisticasService`.
- Em `UteisSala.kt`, o carregamento de perguntas oficiais, personalizadas e publicas passou para `CategoriaRepository`; a escrita da sala continua em `SalaRepository`.
- Em `AdicionarPerguntaActivity.kt`, carregar perguntas editaveis, guardar pergunta e eliminar pergunta de categoria personalizada passaram para `CategoriaRepository`.
- Em `EscolherCategoriaActivity.kt`, listar categorias personalizadas/publicas, eliminar categoria, publicar/atualizar categoria publica e remover publicacao passaram para `CategoriaRepository`.
- Em `ExplorarCategoriasActivity.kt`, escutar categorias publicas, guardar copia e avaliar categoria passaram para `CategoriaRepository`.
- Em `AmigosActivity.kt`, o carregamento/observacao da lista de amigos, pedidos de amizade e convites recebidos passou para `AmigosViewModel`, que continua a chamar `AmigosRepository` e `JogadorRepository`.
- Em `AmigosActivity.kt`, pesquisa de jogador, envio de pedido, aceitar/recusar pedido e aceitar/recusar/remover convite passaram para `AmigosViewModel`; toasts, adapters e navegacao continuam na Activity.
- Em `AmigosActivity.kt`, os listeners sociais continuam a ser removidos em `onStop`/`onDestroy`, agora delegando em `AmigosViewModel`, que tambem remove em `onCleared`.
- Em `EscolherCategoriaActivity.kt`, listar categorias personalizadas/publicas, publicar categoria, remover publicacao e eliminar categoria passaram para `CategoriasViewModel`; dialog, toasts, criacao de sala e navegacao continuam na Activity.
- Em `ExplorarCategoriasActivity.kt`, observar categorias publicas, guardar copia de categoria publica e avaliar categoria passaram para `ExplorarCategoriasViewModel`; cards, dialog de avaliacao, toasts e navegacao continuam na Activity.
- Em `AdicionarPerguntaActivity.kt`, carregar perguntas editaveis, validar pergunta, guardar pergunta e eliminar pergunta passaram para `EditarCategoriaViewModel`; formulario, lista visual, limpar campos e navegacao continuam na Activity.
- Em `LoginActivity.kt`, validacao de campos, Firebase Auth por email/password, deteccao de `currentUser`, fallback legado por nome/password, entrada como convidado e marcacao online passaram para `LoginViewModel`; toasts e navegacao continuam na Activity.
- Em `RegistarActivity.kt`, validacao de nome/email/password, criacao de conta Firebase Auth, montagem do nome do avatar e criacao do perfil em `jogadores/{uid}` passaram para `RegistarViewModel`/`JogadorRepository`; grelha de avatares, toasts e navegacao continuam na Activity.
- Em `MainActivity.kt`, o logout passou a chamar `AuthService.terminarSessao()` alem de marcar o jogador offline; `uid` e `email` sao preservados nos extras de base quando disponiveis.
- Em `JogadorRepository.kt`, foram adicionados metodos pequenos para criar perfil autenticado, obter perfil/avatar por `uid` ou por `nomeUtilizador` e atualizar estado de forma hibrida, mantendo suporte aos perfis antigos por nome.
- Em `MeuPerfilActivity.kt`, o carregamento de perfil, avatar e estatisticas passou para `MeuPerfilViewModel`, que continua a chamar `JogadorRepository`.
- Em `PerfilAmigoActivity.kt`, o carregamento de perfil, avatar e estatisticas passou para `PerfilAmigoViewModel`, que continua a chamar `JogadorRepository`.
- Em `PerfilAmigoActivity.kt`, a remocao de amigo passou a ser iniciada pelo `PerfilAmigoViewModel`, mantendo toast e navegacao na Activity.
- Em `SalaDeEsperaActivity.kt`, a validacao de entrada por codigo, procura de sala, verificacao de nome repetido, carregamento de avatar e adicao do jogador passaram para `SalaGrupoViewModel`; a Activity mantem campos, toasts e navegacao.
- Em `SalaDeEsperaGrupoActivity.kt`, garantir jogador na sala, observar jogadores em tempo real, observar estado, observar sala apagada, validar condicoes de inicio, iniciar jogo e sair/apagar sala passaram para `SalaGrupoViewModel`.
- Em `SalaDeEspera1x1Activity.kt`, adicionar jogador, marcar pronto, obter admin, observar jogadores/estado/sala apagada, verificar prontos, iniciar jogo e sair/apagar sala passaram para `Sala1x1ViewModel`.
- Em `SalaDeEspera2x2Activity.kt`, adicionar jogador, obter admin, observar jogadores/estado/sala apagada, calcular/guardar equipas, iniciar jogo e sair/apagar sala passaram para `Sala2x2ViewModel`.
- Em `EsperaEliminadoActivity.kt`, o listener de estado da sala em eliminatorias passou para `EsperaEliminadoViewModel`; a Activity mantem o texto, toasts e abertura do podio.
- Em `JogoActivity.kt`, carregar perguntas, observar `perguntaAtualIndex`, observar fim de eliminatorias, sincronizar `serverTimeOffset`/`perguntaHoraInicio`, enviar respostas, calcular pontuacao, obter jogadores restantes, eliminar jogadores, avancar perguntas, guardar resultado final e detectar fim de jogo passaram para `JogoViewModel`.
- Em `Jogo1x1Activity.kt`, leitura de categoria, carregamento/criacao das perguntas, sincronizacao de inicio de pergunta, offset do servidor, calculo de pontuacao, guardar pontuacao final, espera pelo podio e deteccao de fim de jogo passaram para `Jogo1x1ViewModel`.
- Em `Jogo2x2Activity.kt`, leitura de categoria, identificacao da equipa, carregamento/criacao das perguntas, sincronizacao de inicio de pergunta, offset do servidor, envio de resposta, calculo de pontuacao, guardar resultado por equipa, espera pelo podio e deteccao de fim de jogo passaram para `Jogo2x2ViewModel`.
- Bloco UID 3 - Jogo: `JogoActivity`, `Jogo1x1Activity` e `Jogo2x2Activity` passaram a receber/preservar `uid` quando existe Firebase Auth, mantendo `nomeUtilizador`/`nomeJogador` como display e fallback para convidados/dados antigos.
- Bloco UID 3 - Jogo: `JogoRepository` resolve a chave efetiva do jogador em `salas/{codigoSala}/jogadores` por `uid`, `nomeUtilizador`, `nomeJogador`, `nomeDisplay` ou chave antiga antes de gravar respostas, eliminacao e resultado final.
- Bloco UID 3 - Jogo: `JogoCompetitivoRepository` passou a guardar/ler jogadores competitivos como identidade hibrida, cria convites 1x1/2x2 com `uid` como chave principal quando disponivel, preserva `admin` para display e adiciona `adminId` como identificador autenticado.
- Bloco UID 3 - Jogo: respostas 2x2, pontuacoes 1x1, pontuacoes por equipa 2x2, prontidao, equipas e remocao de jogador usam a chave efetiva resolvida da sala, evitando duplicar jogadores quando uma sala antiga ainda esta por nome.
- Bloco UID 3 - Jogo: `UteisSala`, Activities de sala/categoria e helpers de navegacao preservam o extra `uid` ate ao jogo e ate as Activities de pontuacao, sem alterar UI, navegacao, tempos ou regras de pontuacao.
- Bloco UID 4 - Pontuacoes e Estatisticas: `EstatisticasService.ResultadoJogador` passou a transportar `uid`, chave real da sala, `nomeUtilizador` e `nomeJogador`; `nome` fica como display e `identificadorEstatisticas` escolhe `uid` quando existe.
- Bloco UID 4 - Pontuacoes e Estatisticas: `PontuacaoRepository` le resultados finais de grupo/1x1/2x2 juntando pontuacoes com metadados dos jogadores, resolve o perfil global por `uid` primeiro e por `nomeUtilizador`/nome como fallback legado.
- Bloco UID 4 - Pontuacoes e Estatisticas: updates de `pontuacao`, `totalJogos`, vitorias por modo, `totalRespostasCertas` e `taxaAcertos` escrevem no perfil resolvido em `jogadores/{uid}` quando existe, sem criar perfil para convidados.
- Bloco UID 4 - Pontuacoes e Estatisticas: marcadores anti-duplicacao em `estatisticasAtualizadas` usam `uid` quando disponivel e caem para chave/nome nos dados convidados ou legados; a transacao continua a impedir duplicacao por recriacao da Activity.
- Bloco UID 4 - Pontuacoes e Estatisticas: `Pontuacao1x1Activity` mantem UI/desforra, mas passa a identificar jogador/adversario por identidade hibrida e cria a sala de desforra com `uid` como chave principal quando possivel.
- Bloco UID 4 - Pontuacoes e Estatisticas: `Pontuacao2x2Activity` continua a mostrar o podio igual, mas a deteccao de recorde local procura o jogador por `uid`/chaves de compatibilidade.
- Bloco UID 5 - Categorias: `CategoriaRepository` passou a resolver categorias personalizadas por `uid` primeiro e por `nomeUtilizador` como fallback, juntando categorias antigas e novas sem duplicar nomes.
- Bloco UID 5 - Categorias: criacao, edicao, eliminacao e perguntas de categorias personalizadas escrevem em `jogadores/{uid}/categoriasPersonalizadas` quando existe Auth, preservando leitura/escrita na chave antiga quando a categoria ja esta em `jogadores/{nomeUtilizador}`.
- Bloco UID 5 - Categorias: categorias publicas guardam `criadorUid`, `criadorId`, `nomeUtilizador` e `nomeDisplay`; a publicacao procura ids publicos antigos e novos antes de criar, evitando duplicar categorias publicas existentes.
- Bloco UID 5 - Categorias: guardar copia de categoria publica e avaliacoes usam `uid` como chave principal quando disponivel, com fallback por `nomeUtilizador`; convidados continuam bloqueados para criar, publicar, guardar copia e avaliar.
- Bloco UID 5 - Categorias: `UteisSala` carrega perguntas de categorias personalizadas por identidade hibrida e guarda `donoUid` nos metadados da sala quando existe, sem alterar regras de jogo, UI ou navegacao.
- `JogoActivity.kt`, `Jogo1x1Activity.kt` e `Jogo2x2Activity.kt` continuam responsaveis por UI, opcoes aleatorias visuais, timers visuais, progress bar, animacao/sons, toasts e navegacao.
- Os ViewModels de salas guardam os handles dos listeners e removem-nos em chamadas explicitas das Activities e tambem em `onCleared`.
- Os ViewModels de jogo guardam os handles de listeners Firebase e removem-nos em `removerListeners()` e `onCleared`, evitando listeners duplicados na migracao de jogo.
- A Activity continua responsavel por UI, toasts e navegacao.
- O repository ficou responsavel por criar/procurar sala, adicionar/remover jogador, apagar sala, atualizar estado, obter jogadores e gerir listeners de jogadores/estado/sala apagada.
- `JogadorRepository` ficou responsavel por obter perfil, obter avatar, criar perfil Auth em `jogadores/{uid}` e atualizar estado `on`/`off`, aceitando `uid` ou `nomeUtilizador` durante a fase hibrida.
- `CategoriaRepository` ficou responsavel por paths `categorias`, `jogadores/{uidOuNome}/categoriasPersonalizadas` e `categoriasPublicas`, incluindo validacao simples de perguntas, transacoes de usos, avaliacao e fallback para categorias antigas por nome.
- `AmigosRepository` ficou responsavel por paths sociais existentes em `jogadores/{nome}/amigos`, `pedidos_amizade`, `convites_recebidos`, `convites_enviados`, alem da criacao de convites com salas `sala_1x1` e `sala_2x2`. A remocao de amizade agora remove os dois lados.
- `JogoRepository` ficou responsavel por paths de jogo de grupo em `salas/{codigoSala}`, mantendo os mesmos nomes de campos.
- `JogoCompetitivoRepository` ficou responsavel por paths competitivos existentes em `sala_1x1/{codigoSala}` e `sala_2x2/{codigoSala}`, mantendo os mesmos nomes de campos.
- `PontuacaoRepository` ficou responsavel por paths de resultados existentes em `salas/{codigoSala}/jogadores`, `sala_1x1/{codigoSala}/pontuacoes`, `sala_2x2/{codigoSala}/equipaA`, `equipaB`, `pontuacoes_A`, `pontuacoes_B`, `totalPerguntasCertas_A`, `totalPerguntasCertas_B` e por updates no perfil global resolvido em `jogadores/{uid}` com fallback por nome legado.
- `EstatisticasService` ficou responsavel por calcular a nova media de `taxaAcertos`, decidir vencedores em solo/1x1/2x2, manter a regra antiga de empate 2x2 para estatisticas e montar o mapa de updates de estatisticas.
- `FirebasePaths` passou a ser usado nos repositories para os nodes `jogadores`, `salas`, `sala_1x1`, `sala_2x2`, `categorias`, `categoriasPersonalizadas`, `categoriasPublicas`, `amigos`, `pedidos_amizade`, `convites_recebidos` e `convites_enviados`.
- `GameConstants` passou a ser usado nos repositories/services para modos e estados como `classico`, `caotico`, `eliminado`, `1x1`, `2x2`, `pendente`, `aceite`, `on` e `off`.
- Bloco 1 de extras: `LoginActivity`, `MainActivity`, `UteisNavegacao`, `EscolherModoActivity` e `EscolherCategoriaActivity` passaram a usar `IntentExtras` para ler/escrever extras, mantendo os mesmos valores e a mesma navegacao.
- Bloco 2 de extras: `SalaDeEsperaActivity`, `SalaDeEsperaGrupoActivity`, `SalaDeEspera1x1Activity`, `SalaDeEspera2x2Activity` e `EsperaEliminadoActivity` passaram a usar `IntentExtras` para os extras de sala, jogador, categoria, modo, admin e resultados transportados pela espera de eliminado. Os extras legados `categoria` e `respostasCertas` foram centralizados sem mudar os valores.
- Bloco 3 de extras: `JogoActivity`, `Jogo1x1Activity` e `Jogo2x2Activity` passaram a usar `IntentExtras` nas leituras iniciais e no redirecionamento para espera/pontuacoes, preservando os mesmos extras de pontuacao e jogador.
- Bloco 4 de extras: `PontuacoesActivity`, `Pontuacao1x1Activity` e `Pontuacao2x2Activity` passaram a usar `IntentExtras` nas leituras de resultados e na navegacao de desforra 1x1, mantendo os mesmos valores e os updates de estatisticas.
- Bloco 5 de extras: `AmigosActivity`, `PerfilAmigoActivity`, `MeuPerfilActivity`, `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity` passaram a usar `IntentExtras` nos fluxos sociais/perfil e convites. O extra `nomeAmigo` foi centralizado mantendo o mesmo valor.
- Bloco 6 de extras: `AdicionarPerguntaActivity`, `ExplorarCategoriasActivity`, `EscolhaCategoriaModosActivity` e `TipoModoClassico` passaram a usar `IntentExtras` nos fluxos de categorias, criacao/edicao de categorias e escolha de categoria para grupo/1x1/2x2. Os modos `classico`, `1x1` e `2x2` tocados no bloco usam `GameConstants`, mantendo os mesmos valores.
- Bloco 7 de extras: revisao final de chamadas `putExtra`, `getStringExtra`, `getBooleanExtra`, `getIntExtra`, `getDoubleExtra`, `hasExtra` e `removeExtra` com strings literais. Os extras restantes em `RegistarActivity` e `AmigoAdapter` passaram a usar `IntentExtras`; a pesquisa global em Kotlin nao encontrou mais literais de extras nessas chamadas.

Mantido sem alteracoes:

- Paths Firebase existentes, incluindo `salas/{codigoSala}`.
- Paths Firebase competitivos existentes, incluindo `sala_1x1/{codigoSala}` e `sala_2x2/{codigoSala}`.
- Nomes de campos como `jogadores`, `estado`, `isHostOnly`, `pontuacao`, `totalRespostasCertas` e `em_jogo`.
- Nomes de campos competitivos como `admin`, `prontos`, `nomeCategoria`, `perguntas`, `perguntaInicios`, `perguntaHoraInicio`, `pontuacoes`, `equipaA`, `equipaB`, `respostas`, `pontuacoes_A`, `pontuacoes_B`, `totalPerguntasCertas_A`, `totalPerguntasCertas_B`, `pontuacaoA` e `pontuacaoB`.
- Nomes de nodes de categorias, incluindo `categorias`, `categoriasPersonalizadas`, `categoriasPublicas`, `perguntas`, `usos`, `avaliacoes`, `ratingMedio` e `totalAvaliacoes`.
- Nomes de nodes sociais, incluindo `amigos`, `pedidos_amizade`, `convites_recebidos`, `convites_enviados`, `sala_1x1` e `sala_2x2`.
- Layouts, textos principais de UI, regras de inicio da sala de espera e navegacao para `JogoActivity`, `Jogo1x1Activity` e `Jogo2x2Activity`.
- Layouts e estrutura visual de `LoginActivity`, `RegistarActivity`, `AmigosActivity`, `EscolherCategoriaActivity`, `ExplorarCategoriasActivity`, `AdicionarPerguntaActivity`, `MeuPerfilActivity` e `PerfilAmigoActivity`.
- UI de `JogoActivity`, `Jogo1x1Activity`, `Jogo2x2Activity`, categorias, regras dos modos classico/caotico/eliminatorias, adapters e layouts sociais.
- Perfis de convidados continuam sem ser criados: `PontuacaoRepository` so atualiza estatisticas quando encontra um perfil existente em `jogadores/{uid}` ou no fallback legado por nome.
- Admin host-only em jogos de grupo continua fora do podio/estatisticas: jogadores com `isHostOnly=true`, nome vazio ou `admin` sao ignorados.
- Para evitar duplicacao por recriacao de Activity ou listeners repetidos, cada sala guarda marcadores transacionais em `estatisticasAtualizadas/{uidOuNome}` dentro da propria sala de resultado.
- Extras de intents continuam com os mesmos valores (`nomeUtilizador`, `nomeJogador`, `codigoSala`, `nomeCategoria`, `modoJogo`, `admin`, `pontuacao`, `totalPerguntas`, `totalRespostasCertas`) e as chamadas de Intent conhecidas em Kotlin usam `IntentExtras`.
- Novos extras `uid` e `email` foram adicionados para preparar a migracao completa sem remover `nomeUtilizador`.
- O login antigo por `jogadores/{nome}/password` continua disponivel como fallback quando o identificador nao e email.

Ainda falta migrar:

- Migrar gradualmente os ultimos metadados de display/compatibilidade para ficarem apenas como fallback; amigos, salas, jogo, pontuacoes, estatisticas e categorias ja estao em modo hibrido com fallback.
- Publicar e validar as rules novas num projeto Firebase de teste antes de usar em producao.
- Continuar apenas ajustes pequenos depois de validar perfil/amigos/categorias/autenticacao/salas/jogo manualmente, sem refactor total.
- Revisao final de extras hardcoded concluida; nao foram encontrados literais restantes nas chamadas de Intent pesquisadas em Kotlin.
- Avaliar se os modelos internos pequenos dos repositories (`JogadorSala`, `CategoriaPublica`, `ResultadoJogador`) devem sair para `models/` numa fase seguinte ou continuar como DTOs locais.
- UI para recusar pedidos/convites, caso seja criada mais tarde; os metodos Firebase de recusa/remocao ja existem em `AmigosRepository`, mas a UI atual continua igual.
- Testes manuais completos em dois dispositivos/sessoes para amigos, convites, salas de grupo, grupo/classico/caotico/eliminatorias, modos competitivos e confirmacao visual das estatisticas no perfil.

Proxima fase recomendada:

- Cloud Functions: mover ranking, estatisticas finais, validacao de resultados e anti-cheat para backend confiavel.
- Validar manualmente amigos, pedidos, convites, salas, jogo, pontuacoes, estatisticas e categorias em dois dispositivos/sessoes antes de remover fallbacks legados.
- Mapear e migrar writes legados que ainda podem cair em `jogadores/{nomeUtilizador}` por compatibilidade antes de remover fallback.
- Se necessario depois dos testes manuais, afinar persistencia de respostas certas no 1x1 para guardar o total de cada jogador no node da sala, sem depender do intent de cada cliente.

Fase 1 - Contratos e constantes:

- Criado `FirebasePaths`, `IntentExtras` e `GameConstants`.
- Bloco 1 concluido em `LoginActivity`, `MainActivity`, `UteisNavegacao`, `EscolherModoActivity` e `EscolherCategoriaActivity`.
- Bloco 2 concluido em `SalaDeEsperaActivity`, `SalaDeEsperaGrupoActivity`, `SalaDeEspera1x1Activity`, `SalaDeEspera2x2Activity` e `EsperaEliminadoActivity`.
- Bloco 3 concluido em `JogoActivity`, `Jogo1x1Activity` e `Jogo2x2Activity`.
- Bloco 4 concluido em `PontuacoesActivity`, `Pontuacao1x1Activity` e `Pontuacao2x2Activity`.
- Bloco 5 concluido em `AmigosActivity`, `PerfilAmigoActivity`, `MeuPerfilActivity`, `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity`.
- Bloco 6 concluido em `AdicionarPerguntaActivity`, `ExplorarCategoriasActivity`, `EscolhaCategoriaModosActivity` e `TipoModoClassico`.
- Bloco 7 concluido em `RegistarActivity` e `AmigoAdapter`, fechando a revisao global de extras hardcoded nas chamadas de Intent pesquisadas.
- Adicionar testes para conversao de extras e paths.

Fase 2 - Modelos:

- `Pergunta`, `Convite`, `Jogador`, `SalaGrupo`, `Sala1x1`, `Sala2x2`, `Categoria` e `Pontuacao` foram criados em `models/`.
- Migrar uso dos modelos novos apenas quando reduzir acoplamento, sem obrigar todos os DTOs locais a sair dos repositories de uma vez.

Fase 3 - Repositories:

- Extrair leituras/escritas de `jogadores`, `categorias`, `salas`, `sala_1x1`, `sala_2x2`.
- Centralizar remocao de listeners.
- Deixar Activities a chamar metodos claros como `roomRepository.observePlayers(...)`.

Fase 4 - Services:

- Extrair pontuacao, timer, selecao de perguntas e estatisticas.
- Garantir que atualizacao de estatisticas acontece uma vez por jogador/jogo.

Fase 5 - Views/Controllers:

- Mover Activities para `views/` apenas depois de estabilizar imports e Manifest.
- Usar controllers pequenos para navegacao/fluxos, ou migrar para ViewModels se for adotado Jetpack Lifecycle.

## Ordem recomendada para mover ficheiros

1. Concluido: `Perguntas.kt` -> `models/Pergunta.kt`.
2. Concluido: `Convite.kt` -> `models/Convite.kt`.
3. Concluido: `UteisValidacao.kt`, `UteisFirebase.kt`, `UteisConquistas.kt`, `gerarCodigoSala` e `obterOpcoesAleatorias` -> `utils/`.
4. Refinar repositories existentes antes de criar novos nomes paralelos.
5. Concluido: substituir extras hardcoded por `IntentExtras`, uma familia de Activities de cada vez.
6. Activities, uma familia de cada vez: auth, amigos, salas, jogo, pontuacoes.

## Cuidados para nao partir o projeto

- Alterar primeiro nomes internos, depois paths Firebase.
- Nao mover uma Activity sem atualizar `AndroidManifest.xml`.
- Nao mudar simultaneamente layout binding e classe da Activity.
- Manter compatibilidade temporaria com extras antigos, como `categoria` e `nomeCategoria`.
- Em Firebase, migrar dados com leitura tolerante a campos antigos antes de escrever apenas campos novos.
- Testar manualmente cada modo apos qualquer migracao de sala/jogo.

## Fase Utilitarios

- `UteisValidacao.kt` foi movido para `app/src/main/java/com/example/brainbrawl/utils/`, mantendo `validarCampos` e `hashPassword` sem alteracao de comportamento.
- `UteisNavegacao.kt` foi movido para `app/src/main/java/com/example/brainbrawl/routes/`, por concentrar criacao de intents e navegacao entre Activities.
- `gerarCodigoSala` saiu de `UteisSala` para `utils/CodigoSalaUtils.kt`, mantendo o mesmo alfabeto, tamanho e uso de aleatoriedade.
- `obterOpcoesAleatorias` saiu de `UteisJogo` para `utils/UteisPerguntas.kt`, mantendo a mesma copia mutavel e `shuffle`.
- `UteisFirebase.kt` e `UteisConquistas.kt` foram movidos para `utils/`, porque sao helpers/constantes sem ownership de fluxo.
- `UteisSala.kt` ficou no package principal porque ainda orquestra repositories, Firebase e navegacao para salas.
- `UteisJogo.kt` ficou no package principal com helpers ligados a UI/som/pontuacao com `Context`.
- `UteisDicas.kt` ficou no package principal por construir dialog/UI Android.

Mantido sem alteracoes nesta fase:

- UI, layouts e textos.
- Logica de jogo, pontuacao e salas.
- Estrutura Firebase e nomes de paths/campos.
- Repositories, services, models e rules.
