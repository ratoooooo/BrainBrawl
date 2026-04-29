# BrainBrawl - Architecture Plan

## Mapa atual do projeto

BrainBrawl e uma app Android nativa em Kotlin, com UI em XML/ViewBinding e Firebase Realtime Database como backend. O ponto de entrada esta em `LoginActivity`, declarado como `MAIN/LAUNCHER` no `AndroidManifest.xml`.

Estrutura principal atual:

- `app/src/main/java/com/example/brainbrawl/`: Activities, adapters e utilitarios ainda no pacote raiz.
- `app/src/main/java/com/example/brainbrawl/models/`: modelos Firebase simples, criados com `data class` e valores por defeito para compatibilidade com Realtime Database.
- `app/src/main/java/com/example/brainbrawl/config/`: constantes de paths Firebase, extras de intents e modos/estados de jogo.
- `app/src/main/res/layout/`: ecras XML das Activities e itens de listas.
- `app/src/main/res/drawable/`, `mipmap-*`, `raw/`: avatares, icones, fundos e sons.
- `app/src/test/` e `app/src/androidTest/`: testes base gerados pelo Android Studio.
- `app/google-services.json`: configuracao Firebase.
- `build.gradle.kts`, `app/build.gradle.kts`: configuracao Gradle/Android/Firebase.

Ficheiros principais e responsabilidades:

- `LoginActivity.kt`: login manual por nome/password guardados em `jogadores/{nome}` e entrada como convidado.
- `RegistarActivity.kt`: cria jogador, hash SHA-256 da password e avatar inicial.
- `MainActivity.kt`: menu principal, cria sala, entra em sala, abre amigos e logout.
- `EscolherModoActivity.kt`, `TipoModoClassico.kt`, `EscolherCategoriaActivity.kt`, `EscolhaCategoriaModosActivity.kt`: selecao de modo/categoria.
- `SalaDeEsperaActivity.kt`: entrada por codigo em salas de grupo (`salas`).
- `SalaDeEsperaGrupoActivity.kt`: sala de espera dos modos de grupo em `salas`.
- `SalaDeEspera1x1Activity.kt`, `SalaDeEspera2x2Activity.kt`: salas competitivas dedicadas.
- `JogoActivity.kt`: jogo em grupo/classico/caotico/eliminatorias usando `salas`.
- `Jogo1x1Activity.kt`, `Jogo2x2Activity.kt`: jogos competitivos dedicados.
- `PontuacoesActivity.kt`, `Pontuacao1x1Activity.kt`, `Pontuacao2x2Activity.kt`: resultados e atualizacao de estatisticas.
- `AmigosActivity.kt`, adapters de amigos/convites/pedidos: amizade e convites.
- `UteisSala.kt`, `UteisNavegacao.kt`, `UteisJogo.kt`, `UteisValidacao.kt`, `UteisConquistas.kt`: logica partilhada.
- `models/Pergunta.kt`, `models/Convite.kt`, `models/Jogador.kt`, `models/SalaGrupo.kt`, `models/Sala1x1.kt`, `models/Sala2x2.kt`, `models/Categoria.kt`, `models/Pontuacao.kt`: modelos simples.

Fluxo principal:

1. `LoginActivity` autentica ou cria jogador temporario.
2. `MainActivity` recebe `nomeUtilizador` ou `nomeJogador`.
3. Criar sala: `EscolherModoActivity` -> `TipoModoClassico`/categoria -> cria dados no Firebase.
4. Entrar em sala: `SalaDeEsperaActivity` valida codigo e adiciona jogador a `salas/{codigo}/jogadores`.
5. Sala de espera observa jogadores e `estado`.
6. Quando `estado = em_jogo`, abre `JogoActivity`, `Jogo1x1Activity` ou `Jogo2x2Activity`.
7. Jogo carrega perguntas, gere timer, respostas e pontuacao.
8. Resultado abre a Activity de pontuacao e atualiza estatisticas.
9. Logout volta ao login e marca `estado = off` para utilizadores registados.

## Problemas da arquitetura atual

- Activities concentram UI, navegacao, regras de jogo e acesso Firebase no mesmo ficheiro.
- Existem varios nomes para a mesma informacao: `categoria`, `nomeCategoria`, `pontuacao`, `totalPontos`, `totalPerguntascertas`, `totalRespostasCertas`.
- A estrutura Firebase esta dividida em `salas`, `sala_1x1`, `sala_2x2` com contratos diferentes.
- A autenticacao e manual, sem Firebase Auth; isto simplifica testes, mas deixa passwords e permissoes dependentes de regras da Realtime Database.
- Muitos listeners sao anonimos e nao sao removidos em todas as Activities.
- As estatisticas sao atualizadas nas Activities de resultado e podem ser incrementadas mais de uma vez se a Activity recriar ou se listeners dispararem novamente.
- Modelos como `Pergunta` estao fora do pacote Kotlin, o que obriga imports de pacote default e dificulta organizacao futura.

## Arquitetura recomendada

Para este projeto, uma arquitetura em camadas inspirada em MVVM/MVC e mais adequada do que mover tudo para MVC puro de uma vez. As Activities podem ficar como Views/Controllers finos, enquanto Firebase e regras de jogo saem para services/repositories.

Estrutura criada para migracao futura:

- `config/`: constantes de Firebase, nomes de extras, chaves de database e configuracao global.
- `controllers/`: orquestracao de fluxos de ecras quando uma Activity ainda nao deve conter a regra completa.
- `models/`: data classes como `Pergunta`, `Convite`, `Jogador`, `Sala`, `Pontuacao`.
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
- A Activity continua responsavel por UI, toasts e navegacao.
- O repository ficou responsavel por criar/procurar sala, adicionar/remover jogador, apagar sala, atualizar estado, obter jogadores e gerir listeners de jogadores/estado/sala apagada.
- `JogadorRepository` ficou responsavel por obter perfil, obter avatar e atualizar estado `on`/`off`.
- `CategoriaRepository` ficou responsavel por paths `categorias`, `jogadores/{nome}/categoriasPersonalizadas` e `categoriasPublicas`, incluindo validacao simples de perguntas, transacoes de usos e avaliacao.
- `AmigosRepository` ficou responsavel por paths sociais existentes em `jogadores/{nome}/amigos`, `pedidos_amizade`, `convites_recebidos`, `convites_enviados`, alem da criacao de convites com salas `sala_1x1` e `sala_2x2`. A remocao de amizade agora remove os dois lados.
- `JogoRepository` ficou responsavel por paths de jogo de grupo em `salas/{codigoSala}`, mantendo os mesmos nomes de campos.
- `JogoCompetitivoRepository` ficou responsavel por paths competitivos existentes em `sala_1x1/{codigoSala}` e `sala_2x2/{codigoSala}`, mantendo os mesmos nomes de campos.
- `PontuacaoRepository` ficou responsavel por paths de resultados existentes em `salas/{codigoSala}/jogadores`, `sala_1x1/{codigoSala}/pontuacoes`, `sala_2x2/{codigoSala}/equipaA`, `equipaB`, `pontuacoes_A`, `pontuacoes_B`, `totalPerguntasCertas_A`, `totalPerguntasCertas_B` e por updates no node `jogadores/{nome}`.
- `EstatisticasService` ficou responsavel por calcular a nova media de `taxaAcertos`, decidir vencedores em solo/1x1/2x2, manter a regra antiga de empate 2x2 para estatisticas e montar o mapa de updates de estatisticas.
- `FirebasePaths` passou a ser usado nos repositories para os nodes `jogadores`, `salas`, `sala_1x1`, `sala_2x2`, `categorias`, `categoriasPersonalizadas`, `categoriasPublicas`, `amigos`, `pedidos_amizade`, `convites_recebidos` e `convites_enviados`.
- `GameConstants` passou a ser usado nos repositories/services para modos e estados como `classico`, `caotico`, `eliminado`, `1x1`, `2x2`, `pendente`, `aceite`, `on` e `off`.

Mantido sem alteracoes:

- Paths Firebase existentes, incluindo `salas/{codigoSala}`.
- Paths Firebase competitivos existentes, incluindo `sala_1x1/{codigoSala}` e `sala_2x2/{codigoSala}`.
- Nomes de campos como `jogadores`, `estado`, `isHostOnly`, `pontuacao`, `totalRespostasCertas` e `em_jogo`.
- Nomes de campos competitivos como `admin`, `prontos`, `nomeCategoria`, `perguntas`, `perguntaInicios`, `perguntaHoraInicio`, `pontuacoes`, `equipaA`, `equipaB`, `respostas`, `pontuacoes_A`, `pontuacoes_B`, `totalPerguntasCertas_A`, `totalPerguntasCertas_B`, `pontuacaoA` e `pontuacaoB`.
- Nomes de nodes de categorias, incluindo `categorias`, `categoriasPersonalizadas`, `categoriasPublicas`, `perguntas`, `usos`, `avaliacoes`, `ratingMedio` e `totalAvaliacoes`.
- Nomes de nodes sociais, incluindo `amigos`, `pedidos_amizade`, `convites_recebidos`, `convites_enviados`, `sala_1x1` e `sala_2x2`.
- Layouts, textos principais de UI, regras de inicio da sala de espera e navegacao para `JogoActivity`, `Jogo1x1Activity` e `Jogo2x2Activity`.
- UI de `JogoActivity`, `Jogo1x1Activity`, `Jogo2x2Activity`, categorias, regras dos modos classico/caotico/eliminatorias, adapters e layouts sociais.
- Perfis de convidados continuam sem ser criados: `PontuacaoRepository` so atualiza estatisticas quando `jogadores/{nome}` existe e tem `password`.
- Admin host-only em jogos de grupo continua fora do podio/estatisticas: jogadores com `isHostOnly=true`, nome vazio ou `admin` sao ignorados.
- Para evitar duplicacao por recriacao de Activity ou listeners repetidos, cada sala guarda marcadores transacionais em `estatisticasAtualizadas/{nomeJogador}` dentro da propria sala de resultado.
- Extras de intents continuam com os mesmos valores (`nomeUtilizador`, `nomeJogador`, `codigoSala`, `nomeCategoria`, `modoJogo`, `admin`, `pontuacao`, `totalPerguntas`, `totalRespostasCertas`). A constante `IntentExtras` ja existe, mas a substituicao nas Activities fica gradual.

Ainda falta migrar:

- Migracao mais profunda de `JogoActivity.kt`, se for necessaria, para reduzir mais estado local e callbacks da Activity.
- Substituir strings de intent extras nas Activities por `IntentExtras`, por familia de ecras, sem mudar navegacao.
- Avaliar se os modelos internos pequenos dos repositories (`JogadorSala`, `CategoriaPublica`, `ResultadoJogador`) devem sair para `models/` numa fase seguinte ou continuar como DTOs locais.
- UI para recusar pedidos/convites, caso seja criada mais tarde; os metodos Firebase de recusa/remocao ja existem em `AmigosRepository`, mas a UI atual continua igual.
- Testes manuais completos em dois dispositivos/sessoes para amigos, convites, jogo de grupo, modos competitivos e confirmacao visual das estatisticas no perfil.

Proxima fase recomendada:

- Validar manualmente amigos, pedidos, convites, jogo de grupo e modos 1x1/2x2 em dois dispositivos/sessoes.
- Se necessario depois dos testes manuais, afinar persistencia de respostas certas no 1x1 para guardar o total de cada jogador no node da sala, sem depender do intent de cada cliente.
- Considerar uma migracao mais profunda de estado interno de `JogoActivity.kt` apenas se aparecer necessidade real; o acesso Firebase principal de grupo ja esta em `JogoRepository`.

Fase 1 - Contratos e constantes:

- Criado `FirebasePaths`, `IntentExtras` e `GameConstants`.
- Trocar strings soltas gradualmente nas Activities que ainda usam extras diretamente.
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
3. `UteisValidacao.kt`, partes puras de `UteisJogo.kt` -> `utils/`.
4. Refinar repositories existentes antes de criar novos nomes paralelos.
5. Substituir extras hardcoded por `IntentExtras`, uma familia de Activities de cada vez.
6. Activities, uma familia de cada vez: auth, amigos, salas, jogo, pontuacoes.

## Cuidados para nao partir o projeto

- Alterar primeiro nomes internos, depois paths Firebase.
- Nao mover uma Activity sem atualizar `AndroidManifest.xml`.
- Nao mudar simultaneamente layout binding e classe da Activity.
- Manter compatibilidade temporaria com extras antigos, como `categoria` e `nomeCategoria`.
- Em Firebase, migrar dados com leitura tolerante a campos antigos antes de escrever apenas campos novos.
- Testar manualmente cada modo apos qualquer migracao de sala/jogo.
