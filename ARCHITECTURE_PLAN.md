# BrainBrawl - Architecture Plan

## Mapa atual do projeto

BrainBrawl e uma app Android nativa em Kotlin, com UI em XML/ViewBinding e Firebase Realtime Database como backend. O ponto de entrada esta em `LoginActivity`, declarado como `MAIN/LAUNCHER` no `AndroidManifest.xml`.

Estrutura principal atual:

- `app/src/main/java/com/example/brainbrawl/`: Activities, adapters, modelos simples e utilitarios misturados no mesmo pacote.
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
- `Perguntas.kt`, `Convite.kt`: modelos simples.

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

### Fase atual - repository de salas de grupo

Criado:

- `app/src/main/java/com/example/brainbrawl/repositories/SalaRepository.kt`, como primeira camada de acesso Firebase para salas no node Firebase `salas`.
- `app/src/main/java/com/example/brainbrawl/repositories/JogadorRepository.kt`, como camada pequena para leituras simples do node Firebase `jogadores`.
- Estrutura base de pacotes `models/`, `repositories/`, `services/`, `controllers/` e `utils/` mantida para a migracao gradual.

Movido nesta fase:

- Em `SalaDeEsperaGrupoActivity.kt`, as chamadas diretas a `FirebaseDatabase` para a sala de grupo foram substituidas por chamadas a `SalaRepository`.
- Em `SalaDeEsperaActivity.kt`, a procura de sala por codigo, a validacao de existencia/nome repetido e a adicao do jogador em `salas/{codigoSala}/jogadores` passaram para `SalaRepository`.
- Em `SalaDeEsperaActivity.kt`, a leitura de avatar do jogador registado passou para `JogadorRepository`.
- Em `UteisSala.kt`, a criacao de salas de grupo em `salas/{codigoSala}` passou a usar `SalaRepository.criarSala`.
- Em `LoginActivity.kt` e `MainActivity.kt`, a leitura de perfil para login e a atualizacao de estado online/offline passaram para `JogadorRepository`.
- Em `MeuPerfilActivity.kt` e `PerfilAmigoActivity.kt`, a leitura de perfil, avatar e estatisticas passou para `JogadorRepository`.
- Em `AmigosActivity.kt`, leituras simples de perfil/avatar/estado usadas na lista de amigos e na pesquisa passaram para `JogadorRepository`; convites e escritas de amizade ficaram inalterados.
- A Activity continua responsavel por UI, toasts e navegacao.
- O repository ficou responsavel por criar/procurar sala, adicionar/remover jogador, apagar sala, atualizar estado, obter jogadores e gerir listeners de jogadores/estado/sala apagada.
- `JogadorRepository` ficou responsavel por obter perfil, obter avatar e atualizar estado `on`/`off`.

Mantido sem alteracoes:

- Paths Firebase existentes, incluindo `salas/{codigoSala}`.
- Nomes de campos como `jogadores`, `estado`, `isHostOnly`, `pontuacao`, `totalRespostasCertas` e `em_jogo`.
- Layouts, textos principais de UI, regras de inicio da sala de espera e navegacao para `JogoActivity`.
- `JogoActivity`, categorias e logica de pontuacao.

Ainda falta migrar:

- Firebase de jogo em `JogoActivity.kt`.
- Salas competitivas `sala_1x1` e `sala_2x2`.
- Categorias, perguntas, convites e amigos.
- Atualizacoes de estatisticas nas Activities de pontuacao, que continuam fora desta fase.
- Escritas de amizade/remocao de amigo em `PerfilAmigoActivity.kt` e restantes operacoes sociais.
- Leituras diretas de listas sociais (`amigos`, `convites_recebidos`, `pedidos_amizade`) em `AmigosActivity.kt`, que devem sair numa fase propria de amigos/convites.
- Leituras de perguntas/categorias em `UteisSala.kt`, que continuam no utilitario ate existir um repository proprio para categorias/perguntas.

Proxima fase recomendada:

- Migrar Firebase de `JogoActivity.kt` para um repository/service proprio, mantendo regras de jogo e pontuacao intactas.
- Em alternativa, antes do jogo, criar repositories pequenos para jogadores/categorias e remover as leituras restantes de `SalaDeEsperaActivity.kt` e `UteisSala.kt`.

Fase 1 - Contratos e constantes:

- Criar objetos em `config/` para nomes de extras e paths Firebase.
- Trocar strings soltas gradualmente.
- Adicionar testes para conversao de extras e paths.

Fase 2 - Modelos:

- Mover `Pergunta` e `Convite1x1` para `models/`.
- Criar modelos `JogadorSala`, `SalaGrupo`, `Sala1x1`, `Sala2x2`.
- Manter typealiases ou imports temporarios para nao partir Activities.

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

1. `Perguntas.kt` -> `models/Pergunta.kt`.
2. `Convite.kt` -> `models/Convite.kt`.
3. `UteisValidacao.kt`, partes puras de `UteisJogo.kt` -> `utils/`.
4. Firebase de amigos -> `repositories/UserRepository.kt`.
5. Firebase de salas -> `repositories/RoomRepository.kt`.
6. Perguntas/categorias -> `repositories/QuestionRepository.kt`.
7. Pontuacao/estatisticas -> `services/ScoreService.kt`.
8. Activities, uma familia de cada vez: auth, amigos, salas, jogo, pontuacoes.

## Cuidados para nao partir o projeto

- Alterar primeiro nomes internos, depois paths Firebase.
- Nao mover uma Activity sem atualizar `AndroidManifest.xml`.
- Nao mudar simultaneamente layout binding e classe da Activity.
- Manter compatibilidade temporaria com extras antigos, como `categoria` e `nomeCategoria`.
- Em Firebase, migrar dados com leitura tolerante a campos antigos antes de escrever apenas campos novos.
- Testar manualmente cada modo apos qualquer migracao de sala/jogo.
