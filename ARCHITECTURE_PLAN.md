# Pergunta o Luso - Architecture Plan

## Pré-beta: matchmaking, espera competitiva, pódio grupo eliminatórias, UI base

- **Matchmaking + salas 1x1/2x2**: o contrato de “pronto” em Firebase (`prontos/{chaveJogador}`) passou a ser coerente com presença real: valores `false`/chaves órfãs não contam; `onDisconnect` remove o nó de pronto em paralelo com `estado=off`; a contagem de chaves reais para reserva ignora jogadores `off`. O 2x2 ganhou o mesmo nó `prontos` e verificação antes de `guardarEquipas2x2` + `estado em_jogo`.
- **Eliminatórias grupo — fecho de resultados**: o fluxo de fim de jogo em grupo grava `terminado` + pontuação para **todos** os clientes em jogo não-solo, incluindo anfitrião com `admin=true`, para o resumo `resultadosGuardados/totalJogadores` em `PontuacaoRepository` fechar sem depender só dos eliminados (`eliminado`).
- **Ordenação pódio eliminatórias**: `ResultadoJogador` inclui `estadoPartida`; o pódio de eliminatórias ordena sobreviventes (`terminado`) antes de empates por pontos/respostas.
- **UI**: ecrãs principais de fluxo e pódio/histórico/perfil alinham o fundo raiz com `bg_main_premium` (mesma base visual que `MainActivity`).

## Correção ícones/badges/conquistas - 2026-05-15

- `UteisConquistas` passa a centralizar os ícones de badges por família com prefixes corretos: `pj`, `vt`, `xp`, `rc` e `cr`.
- As listas de marcos ficam ordenadas de forma decrescente para permitir escolher o maior badge desbloqueado com `firstOrNull`.
- A resolução de drawables usa nomes previsíveis e `Resources.getIdentifier`, devolvendo `null` quando o asset ainda não existe. Esta decisão evita `R.drawable.*` para ficheiros ausentes e permite importar imagens depois sem nova alteração de código.
- `PerfilAmigoActivity` deixou de calcular badges localmente e passou a chamar `UteisConquistas`.
- Não houve alteração de schema Firebase, rules, pontuação, XP, ranking, histórico, convites, salas, categorias ou matchmaking.

## Estado v1 final - Release Prep - 2026-05-14

- A v1 Android mantém arquitetura UID-first para dados autenticados novos, preservando compatibilidade legado por `nomeUtilizador` em leituras e fallbacks onde ainda existem dados antigos.
- `FirebasePaths` centraliza os nomes de paths/campos Firebase usados pelos repositories e viewmodels; `IntentExtras` centraliza chaves de navegação entre Activities.
- O padrão atual é MVVM parcial: Activities principais focam UI/ViewBinding/navegação, ViewModels concentram estado/orquestração, Repositories falam com Firebase e Services guardam lógica pura quando já migrada.
- `LoginActivity` continua como launcher no `AndroidManifest.xml`; a app label usa `@string/app_name` com nome visível `Pergunta o Luso`.
- `MatchmakingActivity` não está registada no manifest nem é aberta pela Main. Os ficheiros de matchmaking permanecem no repositório apenas como código desativado/histórico; o matchmaking aleatório só deve voltar numa branch nova.
- Convidados continuam temporários: podem jogar fluxos permitidos, mas os guardas de pontuação/histórico/badges/ranking impedem persistência indevida quando `isGuest`, `TIPO_JOGADOR_GUEST`, `uid` vazio ou `guest_`.
- Firebase Rules não foram alteradas no Release Prep. Riscos de segurança R1/R2/R3 continuam documentados em `FIREBASE_RULES_NOTES.md`.
- Manifest v1: apenas `LoginActivity` está exportada por ser launcher; activities internas continuam `exported=false`; a permissão de notificações do sistema foi removida por não haver API de notificações Android em uso.
- Assets v1: launcher icon aponta para `@mipmap/avatar_14`/`avatar_14_round`, ambos existentes; badges suportam assets locais por fallback, sem URLs remotos.

### Riscos pendentes para depois da v1

- Separar dados públicos/privados de jogadores e remover `jogadores.read=true` quando houver migração.
- Remover password/hash legado após migração completa para Firebase Auth.
- Mover fecho de pontuação, XP, ranking, histórico e conquistas para Cloud Functions.
- Usar Firebase Anonymous Auth ou contrato equivalente para convidados, reduzindo writes amplos em salas.
- Reintroduzir matchmaking aleatório apenas numa branch nova, com contrato atómico e backend/rules revistos.

### Próximas fases pós-v1 sugeridas

- V1 Release QA manual em dispositivo real.
- Firebase Security Hardening: jogadores públicos/privados, passwords legadas e rules.
- Backend autoritativo com Cloud Functions para pontuação/XP/ranking/histórico/conquistas.
- Firebase Anonymous Auth para convidados.
- Perguntas com imagem e pipeline de assets.
- Badges/assets finais com imagens locais completas.
- Versão iOS/Swift apenas depois da v1 Android estar fechada e testada.

## Perfil competitivo e badges v1 - 2026-05-14

- `BadgesService` concentra a logica pura de conquistas: define familias/thresholds, calcula progresso atual, marca badges bloqueadas/desbloqueadas e devolve a lista de badges novas a gravar.
- `BadgesRepository` e o unico ponto novo que fala com Firebase para conquistas. Le `conquistas/{uid}` e grava `conquistas/{uid}/{badgeId}` por transaction idempotente, sem sobrescrever timestamp existente.
- `MeuPerfilViewModel` passa a orquestrar perfil + stats + badges: carrega o perfil via `JogadorRepository`, calcula taxa de vitoria/derrotas derivadas, pede ao service a lista de badges e pede ao repository para persistir apenas as desbloqueadas novas.
- `MeuPerfilActivity` fica responsavel por renderizacao: avatar, resumo competitivo e grelha de badges. A Activity resolve imagens locais por nome de drawable com fallback seguro, mas nao decide thresholds nem escreve Firebase.
- `Badge`/`BadgeFamily` modelam id, familia, nome, descricao, condicao, estado, `drawableName`, progresso e objetivo.
- A estrategia de assets locais usa nomes previsiveis (`rc10`, `pj100`, `vt500`, etc.) em `res/drawable` ou `res/drawable-nodpi`. Como alguns assets especificos podem nao existir, a resolucao usa `resources.getIdentifier` nesta fase e fallback para `badge_default`/`badge_locked`, ou icones internos se os fallbacks estiverem ausentes.
- Conquistas sao UID-first e so persistem quando `FirebaseAuth.currentUser.uid` corresponde ao perfil carregado. Convidados e perfis sem Auth veem badges bloqueadas sem leitura/escrita em `conquistas`.
- O campo atual `totalRespostasCertas` ja existe no perfil e e atualizado pelo fluxo de estatisticas, por isso a familia RC fica ativa sem inventar dados.
- Pontuacao base, calculo de XP, ranking, historico, convites, salas, categorias e compatibilidade legado por `nomeUtilizador` nao foram alterados.
- Matchmaking aleatorio continua desativado e nao ganhou entrada nova.

## Atualizacao categorias personalizadas, modo por categoria e Main - 2026-05-11

- Categorias personalizadas passam a ser geridas em `ExplorarCategoriasActivity`, junto das categorias publicas e da criacao de categoria.
- O fluxo principal iniciado por `MainActivity -> EscolherModoActivity` fica focado em modos e categorias oficiais; `EscolherCategoriaActivity` deixa de abrir a gestao personalizada e encaminha para `Explorar Categorias`.
- Jogar uma categoria publica/personalizada passa por um chooser de modo especifico da categoria.
- `Caotico` nao entra nesse chooser porque o contrato atual mistura categorias/todas as perguntas; manter fora evita criar uma semantica ambigua.
- Grupo/eliminatorias continuam a criar salas por `UteisSala`; 1x1/2x2 continuam por convite via `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity`.
- O matchmaking aleatorio continua sem entrada de UI e nao foi reativado.
- `AmigosRepository` aceita metadados opcionais da categoria ao criar salas por convite, preservando `categoriaPublicaId`, `donoUid` e `donoCategoria`.
- `JogoCompetitivoRepository` passa a resolver perguntas de categorias publicas/personalizadas quando uma sala competitiva por convite nao traz perguntas pregravadas.
- A edicao de perguntas fica no par `AdicionarPerguntaActivity` + `EditarCategoriaViewModel` + `CategoriaRepository`: Activity trata UI/clicks, ViewModel valida e coordena estado, Repository escreve/le categorias Firebase.
- `perguntaId` passa a ser preservado no fluxo de edicao numerada; guardar uma pergunta selecionada atualiza o mesmo node.
- `Pergunta` ganhou `dificuldade` opcional; perguntas antigas sem esse campo continuam compativeis.
- `MainActivity` continua fina: os IDs de navegacao mantiveram-se e a mudanca principal ficou em XML, com `JOGAR AGORA` e `ENTRAR NUMA SALA` dentro do card principal.

## Estado MVVM atual - 2026-05-08

- Padrao alvo mantido: `Activity -> ViewModel -> Repository -> Service -> Models`.
- Activities devem ficar restritas a ViewBinding, clicks, observers, Toast/dialog, navegacao e capacidades de UI como clipboard.
- ViewModels concentram `UiState`, eventos, validacoes leves, ciclo de vida de listeners e orquestracao de chamadas aos repositories.
- Repositories continuam donos de Firebase Realtime Database/Auth quando aplicavel, listeners, transactions e mapeamento de snapshots.
- Services mantem logica pura: pontuacao, XP/progressao, estatisticas, validacoes e helpers sem `Context`.
- Models continuam `data class` com defaults compativeis com Firebase.
- Arquitetura UID-first permanece: dados persistentes autenticados usam UID; `nomeUtilizador` fica como display/fallback legado; convidados continuam temporarios e sem estatisticas/XP/historico/ranking.
- Matchmaking aleatorio continua removido/desativado por instabilidade; nao foi reativado nesta migracao.

## Migracao MVVM pontuacoes e Main - 2026-05-08

- Auditoria de `*Activity.kt`: nao foram encontrados usos diretos de `FirebaseDatabase`, `FirebaseAuth`, `DatabaseReference` ou `ValueEventListener` em Activities. A maior divida era logica de negocio em Activities, nao acesso Firebase cru.
- `Pontuacao1x1Activity` ficou fina: le extras, configura observers/clicks, renderiza podio, mostra Toasts e navega para Main/desforra.
- `Pontuacao1x1ViewModel` foi expandido para escutar pontuacoes 1x1, preparar `Pontuacao1x1UiState`, identificar o jogador atual, gravar historico uma vez, pedir atualizacao de estatisticas uma vez e manter o fluxo de desforra.
- `Pontuacao2x2ViewModel` foi criado para escutar resultados 2x2, calcular podio/estado de espera/resultado final com `EstatisticasService`, emitir mensagem de recorde, gravar historico e atualizar estatisticas uma vez.
- `Pontuacao2x2Activity` passou a renderizar apenas `Pontuacao2x2UiState` e eventos.
- `PontuacoesViewModel` foi criado para resultados de grupo: escuta `PontuacaoRepository.escutarResultadosGrupo`, calcula podio/MVP/mensagens de espera e centraliza historico/estatisticas.
- `PontuacoesActivity` passou a inflar os itens visuais do podio a partir de `PontuacoesUiState`.
- `MainViewModel` foi criado para carregar perfil, avatar/nivel/XP, resolver UID-first com fallback por `nomeUtilizador`, gerir badge de pedidos/convites e remover listeners sociais no ciclo de vida.
- `MainActivity` ficou com navegacao principal, clicks, renderizacao de perfil/badge e logout visual.
- Repositories nao foram divididos nesta ronda: `PontuacaoRepository`, `JogoCompetitivoRepository`, `AmigosRepository` e `CategoriaRepository` permanecem grandes, mas a divisao ficou adiada para quando houver uma necessidade funcional clara.
- Firebase Rules nao foram alteradas nesta migracao MVVM.

Responsabilidades finais deste bloco:

- `Activity`: UI, observers, clicks, Toasts e navegacao.
- `Pontuacao*ViewModel`: estado de pontuacao, listeners, historico, estatisticas e anti-duplicacao local.
- `MainViewModel`: perfil principal, notificacoes sociais e cleanup de listeners.
- `PontuacaoRepository`: leitura realtime de resultados, atualizacao de estatisticas com anti-duplicacao Firebase e suporte a desforra.
- `HistoricoRepository`: escrita de historico por UID com transacao anti-duplicacao.
- `EstatisticasService`: ordenacao de podio, vencedor/empate, taxa de acertos, XP/progressao via `ProgressaoService`.

Pendencias arquiteturais:

- Criar ViewModels para `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity` numa fase propria, sem alterar o fluxo de convites.
- Avaliar segunda limpeza em categorias, principalmente validacoes de formulario e mensagens.
- Dividir `PontuacaoRepository` apenas quando houver testes dedicados para evitar regressao no fecho de jogo.
- Manter plano futuro de backend autoritativo para estatisticas/ranking/anti-cheat.

## Matchmaking aleatorio desativado temporariamente - 2026-05-08

- Decisao atual: opcao B. O matchmaking aleatorio 1x1/2x2 fica fora da experiencia do jogador ate ser refeito numa fase propria.
- `MainActivity` deixou de abrir `MatchmakingActivity`; os cards `1x1 Aleatorio` e `2x2 Aleatorio` ficam escondidos na Main.
- `MatchmakingActivity` deixou de estar registada no `AndroidManifest.xml`. Os ficheiros de matchmaking permanecem no repositorio apenas como codigo sem uso, para evitar uma remocao ampla e arriscada nesta fase.
- Convites 1x1/2x2 continuam a ser os fluxos competitivos ativos: `ConvidarAmigo1x1Activity`, `ConvidarAmigo2x2Activity`, `AmigosRepository`, `SalaDeEspera1x1Activity`, `SalaDeEspera2x2Activity` e `JogoCompetitivoRepository`.
- Pontuacao, XP, ranking, historico, categorias, login/registo, amigos, perfil e modos grupo/solo nao mudam por causa desta decisao.
- A futura reimplementacao do matchmaking deve comecar de contrato novo: apenas utilizadores autenticados no primeiro corte, selecao atomica de exatamente 2/4 jogadores, resultado por UID, sem loops de reentrada, sem dependencias de contagem em Firebase Rules e sem misturar convites com fila aleatoria.
- As secoes antigas de "matchmaking robusto/implementado" abaixo ficam como historico das tentativas anteriores, nao como arquitetura ativa.

## UX salas privadas e notificacoes sociais - 2026-05-08

- Salas competitivas por convite passam a gravar `origem=convite`, `entradaFechada=true`, `lotacaoMaxima` e `jogadoresPermitidos`, mantendo-as privadas para os jogadores ja listados.
- `SalaDeEspera1x1Activity` e `SalaDeEspera2x2Activity` consultam `JogoCompetitivoRepository.obterCodigoSalaInfo()` e escondem codigo/copiar quando a sala veio de convite, matchmaking ou esta fechada.
- O convite 2x2 passa a exigir 3 amigos selecionados, para que a sala privada ja nasca com os 4 jogadores esperados.
- Salas competitivas antigas sem `origem` e sem `entradaFechada` preservam o comportamento anterior; salas de grupo/manuais continuam a mostrar e copiar codigo.
- `MainActivity` tem indicador visual no item `Amigos`, alimentado por listeners de pedidos de amizade e convites recebidos pendentes. O listener e apenas para contas/perfis, usa UID-first com fallback legado e e removido em `onStop`.

## Matchmaking robusto e UI de sala - 2026-05-08

- `MatchmakingRepository` continua a ser o dono do contrato Firebase do matchmaking. O cliente que ganha a transacao e chama `tentarCriarMatch(criadorKey=...)` passa a ser tambem o `admin/adminId/adminUid` da sala criada, evitando `permission_denied` quando o segundo jogador ganha o claim.
- O claim transacional de matchmaking agora reserva apenas `matches/{matchId}`. A fila deixa de ser marcada como `encontrado` antes de a sala existir; `fila/{playerKey}` so e removida no mesmo update que publica `resultados/{playerKey}`.
- A selecao de jogadores e um invariante do repository: 1x1 exige exatamente 2 `playerKey` distintos e 2x2 exige exatamente 4. A validacao ocorre antes da sala e e confirmada depois da transacao da sala antes de publicar resultados.
- Salas criadas por matchmaking sao salas competitivas fechadas: `origem=matchmaking`, `entradaFechada=true`, `lotacaoMaxima=2/4` e `jogadoresPermitidos/{playerKey}=true` para os selecionados.
- Se a criacao da sala ou a publicacao de resultados falhar depois do claim, o repository remove `matches/{matchId}` e as entradas de fila dos jogadores selecionados que ainda nao tinham resultado publicado. Isto evita deixar jogadores presos em `estado=encontrado`; optou-se por limpar a fila em vez de reescrever entradas de outros UIDs, para nao abrir Firebase Rules.
- O cancelamento passou a validar resultado+sala: se ambos existem, nao apaga a sala; se existe resultado antigo sem sala, ou fila `encontrado` sem resultado/sala valida, limpa a propria fila/resultado para permitir voltar.
- O payload Firebase do matchmaking voltou a estrutura simples compativel com as rules reais: `playerKey`, `uid`, `tipoJogador`, nomes, avatar, `timestampEntrada` e `estado`. O campo `sessionId` foi removido do Firebase porque pode ser rejeitado por rules com `$other=false` e estava alinhado com o erro ao entrar na fila.
- O `matchId` transacional passou a incluir o timestamp de entrada alem dos `playerKey`, para nao bloquear futuras partidas entre o mesmo par/grupo depois de um match antigo ficar em `matches`.
- A sala continua a ser criada antes dos resultados; a navegacao agora verifica explicitamente se o `playerKey` atual existe em `sala_1x1/{codigo}/jogadores` ou `sala_2x2/{codigo}/jogadores` antes de abrir a sala de espera.
- `sala_1x1` ja nao recebe `prontos` no payload inicial do matchmaking. Cada cliente marca o proprio pronto ao entrar na sala, mantendo a responsabilidade no fluxo da sala de espera.
- `JogoCompetitivoRepository.adicionarJogador()` passou a ser a barreira de lotacao da sala: em salas fechadas apenas confirma jogador ja listado; em salas abertas/convites usa reserva transacional em `jogadoresPermitidos` antes de escrever jogador novo. Reentrada do mesmo jogador reutiliza a chave existente.
- `MatchmakingViewModel` mantem flags para uma unica criacao de match pendente, uma unica navegacao e cancelamento sem reentrada na fila. Ao navegar, remove listeners, cancela o `onDisconnect` da fila e remove o resultado consumido.
- `MainActivity` bloqueia taps repetidos nos botoes 1x1/2x2 enquanto abre a Activity de matchmaking, reduzindo Activitys duplicadas do mesmo jogador.
- Salas de espera continuam a receber `playerKey`, `uid`, `tipoJogador` e avatar como antes; convidados preservam `uid` vazio e continuam temporarios, sem tocar em estatisticas/XP/historico/ranking.
- A UI de codigo de sala passou a usar um pequeno `ImageButton` junto ao codigo nos layouts competitivos e no layout reutilizado pela sala de grupo. A entrada por codigo continua normalizada por `CodigoSalaUtils.normalizarCodigo(trim().uppercase())`.

## Firebase Rules hardening - 2026-05-08

- A arquitetura de dados continua UID-first para perfis autenticados: `jogadores/{uid}` e `historicoJogos/{uid}` sao os paths persistentes sensiveis.
- Convidados permanecem restritos a dados temporarios de sala e matchmaking; as rules bloqueiam `jogadores/guest_*` e historico exige `auth.uid`.
- `playerKey` continua separado de `uid`: Auth usa UID como chave principal; guest usa chave temporaria `guest_...` apenas em sala/matchmaking.
- `categoriasPublicas` passa a ser um path publico de leitura, mas writes exigem Auth; nao-criadores nao podem alterar perguntas/metadados principais.
- `salas`, `sala_1x1`, `sala_2x2` e `matchmaking` continuam com algumas permissoes tolerantes porque convidados e transacoes cliente-side ainda fazem parte da arquitetura atual.
- A fronteira de seguranca real para resultados, XP e ranking continua pendente: o plano recomendado e mover esses writes para Cloud Functions e deixar o cliente apenas produzir eventos temporarios de jogo.

## Fecho competitivo 1x1/2x2 - 2026-05-08

- `Pontuacao1x1Activity` ficou mais leve: renderiza podio, observa estado/eventos de desforra, preserva extras de identidade e navega; deixou de criar sala de desforra diretamente e deixou de apagar a sala ao voltar.
- `Pontuacao1x1ViewModel` concentra o estado da desforra, evita navegacao duplicada e coordena os listeners com `PontuacaoRepository`.
- `PontuacaoRepository` passou a expor operacoes pequenas para 1x1: marcar desforra, escutar pedidos, escutar `novaSalaDesforra`, criar/reutilizar sala de desforra via transacao e limpar flags de desforra sem apagar a sala antiga.
- A desforra separa `playerKey` de `uid`: autenticados preservam UID e convidados preservam a chave temporaria/`tipoJogador=guest`; nenhum convidado e convertido em perfil persistente.
- `Pontuacao2x2Activity` deixou de apagar `sala_2x2/{codigoSala}` no Voltar e passou a renderizar resultado/espera em TextViews fixas, removendo Toasts repetidos do listener realtime.
- As Activities de pontuacao competitiva mantem flags locais para evitar repeticao de estatisticas, historico, recorde e navegacao quando listeners Firebase disparam mais de uma vez.
- A persistencia de estatisticas/historico continua condicionada a `uid` valido e jogador nao guest; o repository tambem ignora resultados sem UID antes de atualizar perfis.
- A limpeza definitiva de salas antigas ficou documentada como responsabilidade futura fora do botao Voltar, preferencialmente por Cloud Functions/TTL administrativo.

## Matchmaking aleatorio implementado - 2026-05-08

- `MatchmakingActivity` ficou como UI fina: le extras, observa `MatchmakingViewModel`, mostra contador/lista da fila, cancela e navega.
- `MatchmakingViewModel` prepara a identidade, entra na fila, observa fila/resultado, evita navegacao duplicada e emite eventos para abrir `SalaDeEspera1x1Activity` ou `SalaDeEspera2x2Activity`.
- `MatchmakingRepository` concentra Firebase: escreve/remover fila, configura `onDisconnect`, observa resultados, limpa entradas stale, faz claim transacional em `matchmaking/{modo}` e cria a sala em `sala_1x1` ou `sala_2x2`.
- A identidade separa `playerKey` de `uid`: autenticados usam `playerKey = uid`; convidados usam `playerKey = guest_{nome}_{timestamp}` e `uid` vazio.
- `JogadorSalaIdentidade` passou a aceitar `playerKey`, `tipoJogador` e `avatar` como campos opcionais, preservando os fluxos antigos por UID/nome.
- Convidados entram em matchmaking e salas, aparecem no podio, mas nao criam perfil em `jogadores` e nao gravam estatisticas/XP/historico/ranking porque os fechos de pontuacao exigem `uid` valido.
- `AvatarUtils` centraliza resolucao de avatares, aceitando `avatar_1_playstore`, `avatar_1`, nomes com extensao e prefixos `@drawable/`/`@mipmap/`, com fallback para `avatar_1_playstore`.

## Nota UI - Registo em 2 passos - 2026-05-07

- `RegistarActivity` continua a ser o unico ecrã de registo e mantem o mesmo `RegistarViewModel`, repository/Firebase Auth e criacao de perfil em `jogadores/{uid}`.
- A mudanca e apenas visual/UX: o XML separa a conta (`page_conta`) do perfil (`page_perfil`) no mesmo layout com ViewBinding.
- `btnContinuarRegisto` faz apenas validacoes locais de preenchimento e confirmacao da palavra-passe antes de mostrar o passo de perfil.
- `btnRegistar` continua a executar o fluxo existente: recolhe nome, email, password e avatar selecionado e chama `viewModel.registar(...)`.
- A grelha `grid_avatars` e o destaque `img_avatar_selecionado` foram preservados para manter o fluxo de selecao de avatar.

## Auditoria pre-funcionalidades - 2026-05-03

Estado arquitetural geral:

- As camadas principais estao no caminho certo para Kotlin + XML/ViewBinding + Firebase: Activities tratam UI/navegacao, ViewModels guardam estado de ecras migrados, Repositories concentram Firebase e Services concentram regras reutilizaveis.
- Os fluxos mais sensiveis de jogo e pontuacao ja usam repositories (`SalaRepository`, `JogoRepository`, `JogoCompetitivoRepository`, `PontuacaoRepository`) e services (`EstatisticasService`, `ProgressaoService`, `ScoreService`, `ScoreCompetitivoService`).
- Listeners principais de salas, jogos, amigos, categorias publicas e pontuacoes usam handles e sao removidos em `onDestroy`, `onStop` ou `onCleared`, conforme o fluxo.
- `uid` esta a funcionar como chave principal para dados novos autenticados; `nomeUtilizador` deve continuar como display/fallback legado enquanto houver perfis antigos e convidados.

Pontos a manter sob observacao:

- `CategoriaRepository.kt`, `AmigosRepository.kt`, `JogoCompetitivoRepository.kt` e `PontuacaoRepository.kt` estao grandes. A recomendacao e dividir por subdominio apenas quando uma proxima funcionalidade tocar naturalmente nesses limites.
- Algumas Activities ainda fazem efeitos Firebase pequenos, por exemplo desforra/remocao de sala nas Activities de pontuacao. Isto nao exige refactor agora, mas deve ser reduzido gradualmente.
- A regra de empate 2x2 foi alinhada nesta auditoria: UI mostra empate e `EstatisticasService.vencedores()` nao atribui vitoria a nenhuma equipa quando os totais empatam.
- Estatisticas e ranking continuam calculados no cliente. Para anti-cheat forte, mover fecho de jogo e atualizacao de perfil para Cloud Functions.

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
- O ranking le `jogadores`, ordena por `pontuacao` no Global ou por `recordePontuacao` no Recorde, limita inicialmente a top 100 e mantém compatibilidade com perfis novos por UID e perfis legados por `nomeUtilizador`.
- O ranking passou a suportar multiplos tipos com o mesmo fluxo (`GLOBAL`, `RECORDE`, `SOLO`, `1x1`, `2x2`), mudando apenas o campo de ordenacao Firebase e mantendo fallback para `0` em campos ausentes.
- Foi adicionado sistema progressivo de XP/Niveis por jogador (`xpTotal`, `nivel`, `xpNoNivelAtual`, `xpNecessarioProximoNivel`) calculado no fecho de jogo junto com estatisticas, sem alterar a pontuacao.
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
- `RankingActivity.kt`: lista jogadores por pontuacao acumulada, melhor jogo ou vitorias por modo.
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
3. Ranking: `MainActivity` -> `RankingActivity` -> `RankingViewModel` -> `RankingRepository` consulta `jogadores` por `pontuacao` (global), `recordePontuacao` (melhor jogo) ou por vitorias por modo (`totalVitoriasModoSolo`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2`).
4. Criar sala: `EscolherModoActivity` -> `TipoModoClassico`/categoria -> cria dados no Firebase.
5. Entrar em sala: `SalaDeEsperaActivity` valida codigo e adiciona jogador a `salas/{codigo}/jogadores`.
6. Sala de espera observa jogadores e `estado`.
7. Quando `estado = em_jogo`, abre `JogoActivity`, `Jogo1x1Activity` ou `Jogo2x2Activity`.
8. Jogo carrega perguntas, gere timer, respostas e pontuacao.
9. Resultado abre a Activity de pontuacao e atualiza estatisticas, incluindo progressao de XP/Niveis por perfil.
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

## Contrato atual de convites competitivos 1x1/2x2

- `UtilizadorSocial.chavePerfil` representa o node real do perfil em `jogadores/{...}`.
- `UtilizadorSocial.chaveConvite` representa a subchave operacional de convites/salas: `uid` quando existe, depois `chavePerfil`, depois `nomeUtilizador`.
- Convites recebidos vivem em `jogadores/{destinatario.chavePerfil}/convites_recebidos/{remetente.chaveConvite}`.
- Convites enviados vivem em `jogadores/{remetente.chavePerfil}/convites_enviados/{destinatario.chaveConvite}`.
- O conteudo do convite guarda uid/chavePerfil/nome dos dois lados para permitir leitura, aceitacao, remocao e rules em perfis Auth e legados.
- Salas competitivas usam a mesma `chaveConvite` nos nodes `sala_1x1/{codigo}/jogadores` e `sala_2x2/{codigo}/jogadores`.
- Cada jogador de sala deve manter `nome`, `nomeDisplay`, `uid` e `nomeUtilizador` quando disponiveis, para a sala de espera mostrar nomes e o jogo resolver identidade.

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

## Matchmaking automatico 1x1/2x2

Nova camada adicionada sem alterar convites de amigos:

- `MatchmakingRepository` concentra entrada/cancelamento de fila, transacoes, listeners e criacao segura de sala.
- `MatchmakingViewModel` expoe estado simples para a UI e eventos de navegacao.
- `MatchmakingActivity` mostra procura, loading e cancelamento.
- `MainActivity` passa a oferecer `1x1 Aleatorio` e `2x2 Aleatorio`.

Contrato Firebase:

- `matchmaking/1x1/fila/{uid}` e `matchmaking/2x2/fila/{uid}` guardam o jogador autenticado em espera.
- `matchmaking/1x1/resultados/{uid}` e `matchmaking/2x2/resultados/{uid}` entregam `codigoSala`, `criadorUid`, `nomeCategoria` e lista de jogadores encontrados.
- Salas continuam nos paths existentes: `sala_1x1/{codigo}` e `sala_2x2/{codigo}`.

Decisoes de concorrencia:

- A selecao de jogadores corre por transacao em `matchmaking/{modo}`.
- A criacao da sala corre por transacao no codigo gerado e so escreve se a sala ainda nao existir.
- A fila usa UID como chave, removendo duplicacao do mesmo jogador.
- Ao entrar num modo, a fila do outro modo e limpa para reduzir presenca simultanea.
- Entradas antigas da fila sao removidas por clientes que entram depois; `onDisconnect()` remove a propria entrada quando a ligacao cai.

Limites assumidos:

- Sem Cloud Functions, as rules nao conseguem provar toda a intencao de negocio de um cliente malicioso. As validacoes bloqueiam formas erradas e exigem UID coerente, mas a arbitragem forte de matchmaking continuaria melhor num backend confiavel.

## Historico de jogos

Nova camada adicionada para historico por jogador autenticado:

- `HistoricoRepository` grava uma entrada por jogo e carrega os ultimos 50 jogos por UID.
- `HistoricoViewModel` expoe estado de loading/lista/mensagem para a UI.
- `HistoricoActivity` mostra a lista ordenada do mais recente para o mais antigo.
- `HistoricoAdapter` renderiza resumo de modo, resultado, pontuacao, categoria, data e jogadores.

Contrato Firebase:

- `historicoJogos/{uid}/{historicoId}`
- `historicoId` e deterministico por modo/codigo da sala, por exemplo `1x1_ABC123`, evitando duplicacao ao reabrir a pontuacao.
- Campos gravados: `modo`, `codigoSala`, `nomeCategoria`, `pontuacao`, `recordeFoiBatido`, `respostasCertas`, `totalPerguntas`, `venceu`, `empate`, `equipa`, `dataHora` e `jogadores`.

Pontos de integracao:

- `PontuacoesActivity` grava historico de modos de grupo/classico/caotico/eliminatorias quando todos os resultados da sala estao completos.
- `Pontuacao1x1Activity` grava historico quando existem dois resultados.
- `Pontuacao2x2Activity` grava historico quando existem quatro resultados.
- Convidados ficam sem historico porque a escrita exige UID autenticado.
- Pontuacao, XP, rankings, matchmaking e convites nao foram alterados.

## Fase 1 UI/UX - base visual segura

Objetivo arquitetural:

- Criar uma camada de tokens visuais reutilizaveis em XML, sem introduzir novas dependencias e sem mudar fluxos Firebase, regras de jogo, repositories existentes, services existentes ou navegacao.

Tokens e recursos criados:

- `colors.xml` passou a expor cores `bb_*` para fundo, superficies, texto, acoes, perigo, equipas e outlines.
- `dimens.xml` centraliza espacos, raios, alturas de botoes/inputs e tamanhos de texto.
- `themes.xml` define pequenos defaults globais de fonte/cor/status bar e o estilo reutilizavel `BrainBrawlButton`.
- Drawables `bg_*` centralizam gradiente da app, cards, inputs, botoes primario/secundario/perigo, segmentos selecionados/nao selecionados e empty states.

Aplicacao conservadora:

- Layouts principais receberam fundos, superficies, paddings e botoes consistentes mantendo IDs existentes.
- Foram priorizados Main, Login, Registo, Ranking, Perfil, Amigos, salas de espera 1x1/2x2 e pontuacao/podio.
- A mudanca evita redesign grande: os ecras continuam com a mesma estrutura funcional, mas com contraste, espacamento e hierarquia visual mais consistentes.

Limites da fase:

- Nao foram alteradas regras Firebase.
- Nao foram alteradas formulas de jogo, pontuacao, XP, rankings, convites ou salas.
- Nao foram adicionadas bibliotecas Material novas.
- Responsividade foi melhorada por dimens e larguras `0dp`/`match_parent`, mas ainda deve ser validada visualmente em emulador 360dp.

Nota de compatibilidade:

- O checkout ja tinha referencias a Historico/Matchmaking no manifesto e na Main. Foram repostos os ficheiros necessarios para os bindings dessas Activities existirem e o build passar, mantendo esses destinos dentro da base visual.

## QA urgente pre-proxima fase - navegacao, matchmaking e auth

Mudancas arquiteturais pequenas e intencionais:

- `BottomNavHelper` centraliza a barra inferior programatica dos ecras principais secundarios: Perfil, Ranking, Historico e Amigos.
- O helper preserva `uid`, `nomeUtilizador`, `nomeJogador` e `email`, mantendo Main com a barra XML ja existente.
- `CodigoSalaUtils` passou a expor normalizacao/validacao de codigo de sala, mantendo a geracao no mesmo helper.
- `MatchmakingRepository` passou a ter consumo explicito de resultado apos navegacao para evitar eventos antigos/repetidos.
- `MatchmakingViewModel` passou a ignorar cancelamentos depois de match encontrado e a remover listeners antes da emissao final de navegacao.
- ViewModels de sala competitiva/grupo passaram a distinguir leitura inicial ainda nao confirmada de sala realmente encerrada.
- `JogadorRepository` passou a resolver perfis por chave direta, por campo `uid`, por `nomeUtilizador` legado e por email, mantendo UID-first para contas autenticadas.

Limites preservados:

- Sem alteracao de formula de pontuacao, XP, rankings, categorias ou Cloud Functions.
- Sem alteracao de Firebase Rules nesta ronda.
- Convidados continuam sem ser tratados como UID nem como perfil em `jogadores/{guestKey}`.

## Perfil e conquistas de amigos

Decisao v1:

- `conquistas/{uid}` continua privado e UID-first: apenas o proprio utilizador autenticado le/grava as suas conquistas persistidas.
- O perfil de amigo nao tenta ler `conquistas/{friendUid}`. Para visualizacao publica, `PerfilAmigoViewModel` resolve o amigo por UID/chave legado, carrega o perfil publico via `JogadorRepository` e calcula badges com `BadgesService` a partir de `totalJogos`, `totalVitorias` e `totalRespostasCertas`.
- Esta abordagem evita abrir dados privados nas Firebase Rules e mantem compatibilidade com contas antigas resolvidas por `nomeUtilizador`.
- `MeuPerfilViewModel` mantem o fluxo persistente: calcula progresso, le conquistas do proprio `authUid` quando permitido, e grava apenas novos desbloqueios do proprio utilizador.

UI:

- `BadgeGridRenderer` centraliza a renderizacao da grelha de conquistas para o proprio perfil e perfil de amigo.
- O renderer agrupa por partidas jogadas, vitorias e respostas certas, limpa a grelha antes de renderizar e usa fallback seguro para drawables de badge em falta.
- `UteisDicas` continua como helper de UI Android, agora com dialog de dicas mais compacto, com scroll e cards consistentes.

Limites preservados:

- Sem alteracao de Firebase schema ou rules.
- Sem alteracao de matchmaking, pontuacao, XP, ranking, historico, convites, salas ou categorias.
- XP/CR permanecem em `UteisConquistas` como resolucao de icones, mas a persistencia/listagem completa de conquistas v1 continua limitada ao contrato atual de `BadgesService`.

## Matchmaking reativado de forma controlada - 2026-05-15

Estado v1:

- `MatchmakingActivity` voltou a estar registada no Manifest como `exported=false`.
- A Main mostra os cards 1x1/2x2 aleatorios apenas para utilizadores com UID autenticado.
- O fluxo continua separado dos convites; salas por convite e salas manuais mantem os paths e responsabilidades existentes.

Contrato Firebase preservado:

- `matchmaking/{modo}/fila/{playerKey}`
- `matchmaking/{modo}/matches/{matchId}`
- `matchmaking/{modo}/resultados/{playerKey}`
- `sala_1x1/{codigoSala}`
- `sala_2x2/{codigoSala}`

Correcao de concorrencia:

- O claim transacional em `MatchmakingRepository.tentarCriarMatch` continua a correr em `matchmaking/{modo}`.
- Dentro da transacao, alem de criar `matches/{matchId}`, os jogadores selecionados sao marcados na fila como `estado=encontrado`, com `codigoSala` e `criadorId`.
- Outros clientes filtram apenas `estado=aguardando` para criar novo match, evitando que 3 jogadores no 1x1 ou 5 jogadores no 2x2 fiquem a repetir o mesmo grupo reclamado.
- A sala e criada depois do claim, e os resultados sao publicados em seguida. Se a criacao/publicacao falhar, o rollback remove match/fila/resultado conforme o estado existente.

UI/ViewModel:

- `MatchmakingViewModel` passou a gerir timer, timeout de 90 segundos, estados de cancelamento/preparacao e limpeza defensiva em `onCleared`.
- `MatchmakingActivity` continua fina: observa estado/eventos, mostra jogadores, tempo e contador, e navega para `SalaDeEspera1x1Activity` ou `SalaDeEspera2x2Activity`.

Limites:

- A seguranca forte contra abuso, anti-spam, `activeRooms/{uid}` e limpeza de fantasma deve ser movida para Cloud Functions numa fase futura.
- As Firebase Rules ainda precisam permitir write amplo no nivel `matchmaking/{modo}` porque a transacao cliente-side le e escreve fila/matches no mesmo node.

## Matchmaking estabilizado - 2026-05-15

Estado de UI:

- `MatchmakingViewModel` expoe `MatchmakingStatus` no `MatchmakingUiState`.
- Estados suportados: `IDLE`, `SEARCHING`, `MATCH_FOUND`, `CREATING_ROOM`, `NAVIGATING`, `CANCELLING`, `CANCELLED`, `TIMEOUT` e `ERROR`.
- A Activity deixa de inferir o fluxo apenas por strings/flags e passa a ajustar loading, botao e mensagens com base no estado vindo da ViewModel.

Cancelamento e limpeza:

- Cancelar/back continuam a chamar o ViewModel, que remove fila/resultado quando ainda nao existe sala valida.
- `cancelarPorBackground()` limpa a fila quando a Activity vai para background sem navegacao para sala.
- Resultado antigo ou sala invalida passam para `ERROR`, limpam resultado local e permitem voltar/tentar novamente.

Anti-duplicacao UI:

- `MainActivity` usa `matchmakingAberturaEmCurso` para bloquear taps repetidos nos cards 1x1/2x2 ate a Activity voltar a `onResume`.
- `MatchmakingActivity` mantem guard local `navegando` para evitar navegacao dupla.

Limites preservados:

- Sem alteracao de core da partida, pontuacao, XP, CR, ranking, perfil, conquistas ou convites.
- Sem nova alteracao de Firebase Rules nesta estabilizacao.

## Validacao estado atual - matchmaking, badges e regressao - 2026-05-15

Estado validado:

- `MatchmakingRepository` continua a concentrar fila, cancelamento, transacoes, criacao de sala, resultados e limpeza de listeners.
- `MatchmakingViewModel` continua a ser a fonte de verdade de UI com `MatchmakingStatus`, timer, timeout, cancelamento e eventos de navegacao.
- `MatchmakingActivity` permanece fina: observa estado/eventos, renderiza procura e navega para sala 1x1/2x2.
- `MainActivity` expoe matchmaking apenas a utilizadores autenticados com UID.
- Fluxos por convite e salas manuais continuam separados do matchmaking automatico.

Paths ativos:

- `matchmaking/1x1/fila/{playerKey}`
- `matchmaking/1x1/matches/{matchId}`
- `matchmaking/1x1/resultados/{playerKey}`
- `matchmaking/2x2/fila/{playerKey}`
- `matchmaking/2x2/matches/{matchId}`
- `matchmaking/2x2/resultados/{playerKey}`
- `sala_1x1/{codigoSala}`
- `sala_2x2/{codigoSala}`

Badges/conquistas:

- `BadgesService` e `BadgesRepository` mantem o contrato persistente v1 em `conquistas/{uid}`.
- Persistencia completa v1: RC, PJ e VT.
- Perfil proprio usa UID-first e grava apenas conquistas novas do proprio utilizador autenticado.
- Perfil de amigo calcula badges visualmente por estatisticas publicas, sem abrir `conquistas/{friendUid}`.
- `UteisConquistas` resolve icones locais para PJ, VT, XP, RC e CR, devolvendo `null` se o drawable esperado nao existir.
- `BadgeGridRenderer` centraliza grelha/fallback visual e evita logica de drawable nas Activities.

Correcao pequena aplicada na validacao:

- `MatchmakingViewModel.cancelarPorBackground()` deixa de limpar listeners/estado quando o repository indica que a partida ja foi encontrada e existe sala valida. Nesse caso, a UI mantem `MATCH_FOUND` para permitir concluir a navegacao quando a app volta.

Riscos e proximas fases:

- Manter checklist multi-conta para validar manualmente 1x1, 2x2, convites, salas e categorias.
- Migrar matchmaking autoritativo para Cloud Functions numa fase futura, incluindo anti-spam, `activeRooms/{uid}` e limpeza global de filas fantasma.
- Separar dados publicos/privados de jogadores e remover password/hash legado antes de beta publico.
- Endurecer pontuacao/XP/ranking/historico com backend confiavel.
- Completar assets opcionais/legado de badges se voltarem a ser usados visualmente: `rc200`, `pj25`, `vt5`, `vt25`.

## Beta Prep UI/UX polish - 2026-05-17

Decisao de UI:

- Manter o design system leve existente em vez de criar um sistema novo.
- Reutilizar `BrainBrawlButton`, `bg_button_primary`, `bg_button_secondary`, `bg_button_danger`, `bg_card_surface`, `bg_empty_state_card` e cores `bb_*`.
- Remover nos ecras principais auditados as referencias visuais a `@android:style/Widget.Button`, `@android:drawable`, `botao_branco_arredondado` e `botao_voltar`.

Badges:

- `UteisConquistas` deve apontar apenas para assets realmente existentes.
- `badge_default` e `badge_locked` passam a existir como fallback visual local.
- `BadgeGridRenderer` continua a ser o ponto central para fallback em grelha; os perfis tambem devem mostrar fallback quando o badge de resumo ainda nao foi atingido.

Anti-abuso leve:

- Bloqueio de duplo toque fica na camada de UI/adapter quando o risco e apenas repeticao de acao.
- Validacao de integridade de perguntas fica no `EditarCategoriaViewModel` e no parser/filtro de `CategoriaRepository`.
- Convite aceite valida existencia da sala no `AmigosRepository` antes de navegar, sem alterar schema Firebase.

Limites preservados:

- Sem refactor geral.
- Sem alteracao de Firebase Rules.
- Sem alteracao de schema.
- Sem alteracao do calculo base de pontuacao, XP ou ranking.
- Matchmaking automatico permanece ativo e separado dos convites.

## Beta Prep UI Fixes - perfil, conquistas e categorias - 2026-05-18

Categorias/perguntas:

- `CategoriaRepository.guardarPerguntaPersonalizada` passa a separar a preparação da categoria da gravação da pergunta.
- A categoria é garantida primeiro; a pergunta é gravada diretamente em `perguntas/{perguntaId}`.
- Edição preserva `perguntaId`, evitando duplicação.
- Firebase Rules foram ajustadas apenas para permitir edição real de perguntas personalizadas pelo dono.

Perfil/conquistas:

- `MeuPerfilActivity` passa a ser resumo de perfil, não página completa de conquistas.
- `ConquistasActivity` é o ecrã próprio para todas as conquistas.
- `BadgeGridRenderer.renderMelhores` serve perfis resumidos; `BadgeGridRenderer.render` mantém grelha completa.
- `BadgesService` passa a calcular RC/PJ/VT/XP/CR. Persistência continua UID-first em `conquistas/{uid}`.
- Perfil público de amigo calcula conquistas a partir de estatísticas públicas, sem ler `conquistas/{friendUid}`.

Editar perfil:

- `EditarPerfilActivity` suporta alteração segura de avatar.
- Nome/email/password ficam fora de escopo porque podem quebrar compatibilidade social/legado e autenticação.
- `JogadorRepository.atualizarAvatar` resolve UID/nome legado e grava apenas o campo `avatar`.

UI:

- Main remove fundos circulares estranhos nos avatares e alinha a seta do CTA principal.
- Salas 1x1/2x2 escondem o código/card quando a sala é privada por convite; sala de grupo mantém código.
- Dicas usam marcador azul escuro com dourado subtil, sem amarelo torrado dominante.

## Solo vs Grupo - mínimo de sala - 2026-05-18

Decisão de fluxo:

- Solo é o fluxo individual e continua fora de salas.
- Grupo/Sala é o fluxo multiplayer por código e exige pelo menos 2 participantes ativos presentes antes de iniciar.
- 1x1 e 2x2 continuam com regras competitivas próprias.
- Matchmaking continua separado e não foi alterado.

Responsabilidades:

- `SalaGrupoViewModel` é a fonte local da regra de início da sala grupo: observa jogadores, calcula participantes ativos, expõe estado de UI e valida de novo no Firebase antes de mudar a sala para `em_jogo`.
- `SalaDeEsperaGrupoActivity` apenas apresenta contador/mensagem e liga o botão `Iniciar` ao estado exposto pela ViewModel.
- `SalaRepository` permanece como camada Firebase; não houve alteração de schema nem de Firebase Rules.

## Pré-beta - Solo, eliminatórias e competitividade - 2026-05-18

Modo Solo:

- Solo é fluxo individual local dentro de `JogoActivity`/`JogoViewModel`.
- Solo não cria `salas/{codigo}`, não mostra código e não espera outros jogadores.
- `partidaId` é usado apenas para histórico/idempotência local do resultado, não como código de sala.
- Convidados podem jogar Solo, mas continuam sem histórico, XP, ranking ou badges persistentes.

Grupo/Sala:

- Grupo/Sala continua multiplayer por código e exige mínimo de 2 participantes.
- 1x1, 2x2, convites e matchmaking mantêm fluxos próprios.

Eliminatórias:

- Eliminatórias Solo usam todas as perguntas válidas disponíveis e terminam ao primeiro erro ou quando esgotam perguntas.
- Eliminatórias Grupo deixam de aplicar limite fixo de 8 perguntas quando a sala é criada/carregada.
- Jogadores eliminados aguardam em `EsperaEliminadoActivity`, com ranking/estado parcial até ao pódio final.

Competitividade de categorias:

- Apenas categorias oficiais contam para ranking, recordes e vitórias competitivas.
- Categorias personalizadas e públicas criadas por jogadores são não competitivas.
- O histórico pode registar esses jogos com `competitivo=false`.
- `PontuacaoRepository` bloqueia estatísticas competitivas para salas com metadados de categoria pública/personalizada.

Histórico:

- `HistoricoRepository` remove entradas antigas do próprio `historicoJogos/{uid}` com mais de `GameConstants.HISTORICO_RETENCAO_DIAS`.
- Entradas sem timestamp válido são preservadas.
- Retenção autoritativa futura deve ser Cloud Functions/TTL.

## Ajuste final pré-beta - fluxo, presença e vitória Solo - 2026-05-18

Fluxo de modos:

- `EscolherModoActivity` é apenas a escolha do modo base: Clássico, Caótico ou Eliminatórias.
- `TipoModoClassico` passou a funcionar como ecrã comum de escolha de tipo para todos os modos base.
- 1x1/2x2 permanecem disponíveis apenas no modo Clássico para preservar os fluxos competitivos atuais; Caótico/Eliminatórias expõem Solo e Grupo.
- Matchmaking continua na Main e não foi movido para este fluxo.

Vitórias Solo:

- `EstatisticasService` mantém Solo como jogo pontuável/experienciável conforme regra atual, mas Solo não incrementa `totalVitorias` nem `totalVitoriasModoSolo`.
- Badges/Ranking baseados em `totalVitorias` ficam protegidos contra vitórias individuais artificiais.

Matchmaking/presença:

- Salas competitivas fechadas atualizam presença no Firebase com `jogadores/{chave}/estado = on`.
- `onDisconnect` marca o jogador como `off`; as ViewModels de sala ignoram jogadores `off` ao calcular presença e início.
- 1x1 mantém o nó `prontos` existente; 2x2 usa presença ativa/exatamente 4 jogadores como bloqueio simples.
- Um ready-state manual e autoritativo por jogador fica para versão futura/backend, para evitar redesenhar convites/matchmaking nesta fase.

## Correções finais pré-beta - matchmaking, eliminatórias e histórico - 2026-05-18

Matchmaking e salas fechadas:

- Salas vindas de matchmaking continuam em `sala_1x1`/`sala_2x2` com `origem=matchmaking`, `entradaFechada=true`, `lotacaoMaxima` e `jogadoresPermitidos`.
- A entrada numa sala fechada passou a aceitar o jogador se a chave existir em `jogadoresPermitidos`, mesmo que o nó em `jogadores` tenha sido removido ao sair da sala de espera.
- Ao reentrar por essa autorização, o cliente recria `jogadores/{chave}` com a mesma identidade e marca presença `estado=on`.
- A lotação efetiva continua a ser presença real: 2 jogadores ativos no 1x1, 4 no 2x2. O fluxo de matchmaking permanece na Main.

Eliminatórias grupo:

- O nó `salas/{codigo}/jogadores/{chave}` passa a receber progresso parcial a cada resposta: `pontuacao`, `totalRespostasCertas`, `totalPerguntas` e `estado=em_jogo`.
- Resultado final de grupo só é considerado final quando `estado=terminado` ou `estado=eliminado`.
- O ecrã de eliminado usa ranking intermédio em cards com estados humanos: `Em jogo`, `Eliminado` e `Terminou`.
- Se um jogador vivo ainda não tiver progresso parcial confirmado, a UI mostra `Em jogo` em vez de um 0 falso.

Histórico:

- `historicoJogos/{uid}` continua UID-first e bloqueado para convidados.
- O bug de histórico/pódio prematuro vinha de campos iniciais `pontuacao=0` e `totalRespostasCertas=0` serem tratados como resultado final.
- A decisão atual é preservar histórico para categorias oficiais, personalizadas e públicas; competitividade continua separada por `competitivo=false` quando a categoria não é oficial.
- Ranking, recorde, vitórias e XP competitivo continuam bloqueados para categorias não oficiais pela camada de pontuação/estatísticas.

## Refactor controlado do matchmaking - fila, sala, presença e pronto - 2026-05-19

Contrato:

- O matchmaking continua a partir da Main e mantém os paths atuais: `matchmaking/{modo}/fila`, `matchmaking/{modo}/matches`, `matchmaking/{modo}/resultados`, `sala_1x1/{codigo}` e `sala_2x2/{codigo}`.
- A seleção de jogadores continua cliente-side com transação em `matchmaking/{modo}`: 2 jogadores para 1x1, 4 jogadores para 2x2.
- A sala criada por matchmaking é fechada com `origem=matchmaking`, `entradaFechada=true`, `lotacaoMaxima` e `jogadoresPermitidos/{playerKey}=true`.
- Os jogadores selecionados são copiados para `jogadores/{playerKey}` com `estado=off`; presença real só existe quando a Activity da sala de espera abre e marca o próprio jogador como `on`.
- O nó `prontos/{playerKey}` não nasce no payload inicial da sala, para manter compatibilidade com as validações atuais de sala. Cada cliente cria/atualiza o próprio pronto ao entrar e ao clicar em `Pronto`.

Separação de fluxos:

- Salas de `origem=matchmaking` usam ready-state manual: o jogo só inicia com exatamente N jogadores presentes e N prontos.
- Salas por convite preservam o comportamento antigo: presença/ready automático ao entrar e início pelo admin quando a sala está completa.
- `MatchmakingActivity.onStop` já não cancela a fila por background; cancelamento acontece por ação explícita do utilizador ou por saída da sala de espera antes do jogo.

Saída e inconsistência:

- Se um jogador sai de uma sala de matchmaking antes de `em_jogo`, a sala é cancelada/apagada e os restantes recebem aviso de encerramento.
- A validação de lotação considera apenas jogadores reais presentes: sem `estado=off`, sem placeholders, sem duplicados e com autorização por `jogadoresPermitidos` quando a sala é fechada.
- A reentrada é idempotente: antes de escrever na fila, o ViewModel procura sala ativa de matchmaking onde o jogador já esteja autorizado e navega para ela.

Limites:

- Sem Cloud Functions, a arbitragem continua dependente de transações e limpeza client-side.
- `onDisconnect` cobre o caso normal de perda de ligação, mas não substitui limpeza autoritativa/TTL futura.
- 2x2 deve continuar a ser validado manualmente com 4 contas/dispositivos reais antes da beta fechada.

## Resultados grupo: visual vs persistente - 2026-05-19

Contrato clarificado:

- Resultado visual temporário de Grupo Clássico, Caótico e Eliminatórias vive em `salas/{codigo}/jogadores/{jogadorId}`.
- O pódio de grupo lê o mesmo path e considera resultado guardado apenas quando o jogador está `terminado` ou `eliminado` e tem pontuação/respostas.
- Jogadores `off` antes de começar não entram no total esperado do pódio.
- O placeholder técnico `admin` continua ignorado; o host real da sala não deve ser escondido por `isHostOnly`.

Separação de responsabilidades:

- Resultado visual, contador do pódio, ordenação final e feedback ao jogador nunca dependem de a categoria ser competitiva.
- Estatísticas persistentes, XP, ranking, recordes e vitórias competitivas continuam condicionados por autenticação e competitividade da categoria.
- Convidados podem aparecer no pódio visual da sala, mas não escrevem histórico/estatísticas persistentes.

Rules:

- `salas/{codigo}/jogadores/{jogadorId}` precisa aceitar os campos realmente escritos pelo jogo: `pontuacao`, `totalRespostasCertas`, `totalPerguntas` e `estado` com `on/off/em_jogo/terminado/eliminado`.
- Esta fase mantém o schema atual e não cria Cloud Functions.

## QA jogável: pódio, histórico e navegação - 2026-05-19

Pódio e resultados de sala:

- O fluxo de grupo mantém o contrato em que o resultado visual é escrito no próprio jogador da sala, não num nó separado de estatística persistente.
- `perguntaAtual/respostas` é estado transitório de sincronização da pergunta atual; o admin pode limpar o nó inteiro ao avançar a pergunta.
- O pódio de grupo não deve fechar automaticamente só porque alguns resultados ainda não chegaram; deve permanecer no estado de espera até os jogadores esperados terminarem ou serem eliminados.

Histórico:

- `HistoricoJogo` transporta `competitivo` para distinguir histórico visual de efeitos competitivos.
- Categorias não oficiais podem aparecer no histórico e no pódio visual, mas não devem inflacionar ranking, recordes, vitórias competitivas ou XP competitivo.

Navegação/back:

- Em Activities de jogo (`JogoActivity`, `Jogo1x1Activity`, `Jogo2x2Activity`), o Back físico é uma ação bloqueada por regra de produto enquanto a partida está ativa e deve mostrar mensagem clara.
- Em ecrãs navegacionais como Histórico, Perfil e Amigos, o botão visual de voltar deve permanecer visível e chamar `finish()`.

## Fluxo de convite após seleção de categoria - 2026-05-23

Contrato:

- O fluxo `Clássico -> 1x1 -> categoria -> Convidar amigo` chega ao convite com `IntentExtras.MODO_JOGO=MODO_1X1` e `IntentExtras.NOME_CATEGORIA` já definidos.
- O fluxo `Clássico -> 2x2 -> categoria -> Convidar amigos` chega ao convite com `IntentExtras.MODO_JOGO=MODO_2X2` e `IntentExtras.NOME_CATEGORIA` já definidos.
- Os ecrãs de convite são pós-seleção: não mostram seletor de categoria, não mostram seletor 1x1/2x2 e não permitem alterar esse contexto.
- `ConvidarAmigo1x1Activity` seleciona exatamente 1 amigo online e encaminha para `SalaDeEspera1x1Activity` com código de sala, utilizador, modo e categoria preservados.
- `ConvidarAmigo2x2Activity` seleciona 3 amigos online, mantendo o utilizador atual como quarto jogador, e encaminha para `SalaDeEspera2x2Activity` com código de sala, utilizador, modo e categoria preservados.

Limites:

- A atribuição de equipas 2x2 continua automática e dependente da lógica existente de criação/entrada na sala; não existe seleção manual de equipas neste fluxo.
- Categorias públicas/personalizadas continuam a transportar os extras atuais (`CATEGORIA_PUBLICA_ID`, `DONO_UID`, `DONO_CATEGORIA`) para manter a origem da categoria sem alterar regras de competitividade.

## Arranque 1x1/2x2 por convite e contexto partilhado - 2026-05-23

Contrato:

- Salas competitivas por convite continuam a iniciar por alteração de estado em `sala_1x1/{codigo}/estado` ou `sala_2x2/{codigo}/estado` para `em_jogo`.
- O estado `em_jogo` é o último sinal de arranque para os clientes: antes dele, o repositório competitivo deve garantir que `nomeCategoria` e `perguntas` existem; no 2x2, `equipaA` e `equipaB` também devem ser escritos no mesmo `updateChildren` que publica o estado.
- A sala de espera resolve a chave real do jogador ao entrar e passa essa chave para `Jogo1x1Activity`/`Jogo2x2Activity` através de `IntentExtras.PLAYER_KEY`.
- `NOME_CATEGORIA` deixa de depender apenas do Intent local do admin; as salas de espera e os jogos competitivos leem o nome partilhado em `sala_1x1/{codigo}/nomeCategoria` ou `sala_2x2/{codigo}/nomeCategoria`.
- A UI de jogo pode mostrar uma categoria inicial recebida por Intent, mas deve substituir pelo valor carregado da sala assim que o ViewModel publica a primeira pergunta.
- Dados de avatar em convites passam a ser escritos no snapshot inicial da sala, para que waiting rooms e placares usem cada jogador real em vez de repetir fallback/local host.
- Iniciar jogo e navegar para `Jogo1x1Activity`/`Jogo2x2Activity` nunca deve ser tratado como saída da sala. Cleanup/remocao de jogador fica reservado para `Sair da Sala`, Back antes do start ou cancelamento explícito.
- O start privado só deve escrever campos aceites pelo schema/rules de `sala_1x1` e `sala_2x2`; `modoJogo` continua a ser contexto de Intent/origem de fluxo, não metadata escrita no nó privado enquanto as rules não o aceitarem.
- A distinção de tipo de sala é sempre explícita por `origem`: `convite` para salas privadas por convite e `matchmaking` para salas automáticas. A ausência de `origem` não deve ser interpretada como matchmaking.
- Mensagens e fallback de matchmaking só podem aparecer quando `origem=matchmaking`; salas `origem=convite` usam comportamento privado e não entram em fallback para fila automática.
- Quedas de presença observadas durante `inicioJogoEmCurso`/`aIniciarJogo` ou depois de `estado=em_jogo` são ignoradas pela sala de espera, porque já pertencem à navegação para jogo e não a uma saída manual.
- `RoomFlowType` é o adaptador interno para o contrato Firebase existente: `MATCHMAKING` lê `origem=matchmaking`, `INVITE` lê `origem=convite`, e `PRIVATE` cobre `origem=manual` ou origem ausente para compatibilidade legado.
- Os ViewModels competitivos podem partilhar a Activity/layout, mas a semântica de start/cleanup/mensagens deve ramificar por `RoomFlowType`. Não é permitido chamar cleanup ou fallback de matchmaking a partir de `INVITE`/`PRIVATE`.
- `MatchmakingActivity` e ecrãs de convite propagam `IntentExtras.ORIGEM_SALA` para reduzir ambiguidade entre navegação local e metadata Firebase; a metadata Firebase continua autoritativa.

Limites:

- A identificação continua compatível com UID, `playerKey`, nome de utilizador e nome legado. A chave resolvida pela sala é a preferida para reduzir falhas ao carregar equipas 2x2.
- O retry de identificação de equipa no jogo 2x2 é deliberadamente curto e único; falhas persistentes continuam a ser erros reais de estado da sala.
- Não foram alteradas regras de limpeza de matchmaking, pódio de grupo, histórico, ranking ou competitividade de categorias.

## UI polish pódio/histórico/jogo - 2026-05-24

Contrato mantido:

- Pódio continua UID-first/compatível: a ordenação final permanece nos serviços/viewmodels existentes (`ordenarPodio`, `ordenarPodioGrupoEliminatorias`, `ordenarPodio2x2`). A UI apenas renderiza a lista já ordenada.
- `ResultadoJogador.avatar` é um campo de apresentação opcional lido dos snapshots já existentes; ausência de avatar usa fallback local via `AvatarUtils`.
- Pódio top 3 e continuação usam `PodioUiRenderer`/`PodioContinuationAdapter` para evitar lógica duplicada nas Activities.
- Histórico mantém a carga por `HistoricoRepository`; filtros são locais e baseados no campo existente `competitivo`.
- Gameplay mantém timers, submissão imediata da resposta e validação no ViewModel/serviços. A alteração remove só o botão visual de voltar e reequilibra XML.

Limites:

- Não foi introduzido novo schema Firebase, Cloud Function, regra de segurança ou ranking alternativo.
- `Jogar novamente` permanece ligado ao fluxo existente de desforra no 1x1; outros pódios preservam apenas o voltar quando não há fluxo de rematch seguro.
- A precisão no resumo usa os dados de respostas/perguntas já passados no fluxo atual, sem recalcular resultados globais da sala.

## Logic polish ranking/timers/records - 2026-05-24

Contrato:

- O ranking visível deixa de tratar Solo como ranking de vitórias. O separador é `Grupo` e usa o campo legado `totalVitoriasModoSolo` como contador de vitórias de grupo para não introduzir schema novo.
- `RankingTipo.GRUPO`, `RankingTipo.MODO_1X1`, `RankingTipo.MODO_2X2` e `RankingTipo.RECORDE` são a fonte única de ordenação da UI de ranking: grupo/1x1/2x2 por vitórias, recorde por `recordePontuacao`.
- `EstatisticasService.Modo.SOLO` continua a atualizar pontos totais, recorde, jogos, acertos e progressão, mas não vitórias.
- `EstatisticasService.Modo.GRUPO` incrementa `totalVitorias` e o contador legado `totalVitoriasModoSolo` quando o jogador vence.
- O vencedor de `Modo.GRUPO` respeita a ordem final recebida do fluxo de pontuações, para preservar a regra de eliminatórias já aplicada antes da escrita estatística.
- `recordePontuacao` é sempre monotónico por jogador: só sobe quando a pontuação de uma partida supera o valor anterior.
- Timers clássicos de solo/grupo/categorias usam constantes centrais de 15s; caótico usa 10s; eliminatórias usam 15s; matchmaking competitivo mantém 20s.
- A pontuação ao vivo 2x2 é escrita no nó canónico vivo `sala_2x2/{codigo}/jogadores/{playerKey}/pontuacao`; `equipaA` e `equipaB` ficam como membership/identidade da equipa, e o placar junta esses jogadores com a pontuação atual de `jogadores`.

Limites:

- Não foi adicionada migração para dados históricos de grupo que já possam ter sido gravados sem contador específico.
- O nome Firebase `totalVitoriasModoSolo` permanece por compatibilidade, apesar de a UI passar a apresentá-lo como Grupo.
- Edição de nome de perfil ficou deferida até haver contrato de atualização canónica/fan-out e validação de unicidade.
- Firebase Rules não foram alteradas.

## Fix pontuação ao vivo 2x2 matchmaking - 2026-05-24

Contrato:

- 2x2 escreve pontuação incremental em `sala_2x2/{codigo}/jogadores/{playerKey}/pontuacao`, alinhado com o padrão 1x1.
- `equipaA` e `equipaB` continuam a definir composição visual da equipa e compatibilidade com salas privadas/matchmaking.
- O listener de placar 2x2 observa `sala_2x2/{codigo}`, lê membership de `equipaA`/`equipaB`, lê pontuação viva de `jogadores` e faz merge por `chave`, `uid`, `playerKey` ou nomes legados.
- A pontuação exibida da equipa é sempre a soma das pontuações vivas dos membros dessa equipa.
- Resultado final/pódio mantém os nós finais existentes; esta correção não muda ranking, timers, regras Firebase nem layout.

Limites:

- Validado em runtime que o caminho antigo de equipa falhava por permissão e que o caminho `jogadores/{playerKey}/pontuacao` escreve sem `Permission denied`.
- A confirmação visual positiva em todos os clientes deve ser repetida manualmente porque a automação por `uiautomator` falhou intermitentemente durante a janela de resposta.

## UI dedicada para pódios 1x1/2x2 - 2026-05-24

Contrato:

- `Pontuacao1x1Activity` e `Pontuacao2x2Activity` passam a renderizar layouts dedicados ao tipo de resultado, sem reutilizar a composição visual de pódio de grupo.
- O pódio 1x1 continua alimentado pela ordenação final de `Pontuacao1x1ViewModel`; a Activity apenas faz binding de vencedor/segundo jogador, avatares, pontuação e estado visual.
- O pódio 2x2 continua alimentado por `EstatisticasService.ordenarPodio2x2`; `Pontuacao2x2ViewModel` expõe uma estrutura de apresentação por equipa para evitar lógica de layout dentro do XML.
- `PodioUiRenderer` permanece disponível para o pódio de grupo e outros ecrãs que ainda usam top 3/continuação.
- Não há alteração de schema Firebase, regras, ranking, histórico, matchmaking, salas privadas ou cálculo de pontuação.

Limites:

- A desforra real continua implementada apenas no fluxo 1x1 existente.
- O 2x2 apresenta o CTA visual de novo jogo, mas usa retorno seguro ao início para não inventar um fluxo de revanche.

## Desforra mode-aware 1x1/2x2 - 2026-05-24

Contrato:

- A entrada genérica `SalaDeEsperaActivity` continua a representar entrada em sala de grupo; fluxos 1x1/2x2 não devem usá-la como fallback de desforra.
- Desforra 1x1 permanece em `sala_1x1`: os dois jogadores aceitam, uma nova sala 1x1 é criada/reutilizada por `novaSalaDesforra`, e a Activity abre explicitamente `SalaDeEspera1x1Activity`.
- Desforra 2x2 passa a existir em `sala_2x2`: cada jogador marca `jogadores/{playerKey}/desforra=true`; quando os quatro jogadores reais aceitam, é criada/reutilizada uma nova sala 2x2.
- A nova sala 2x2 copia jogadores e composição de `equipaA`/`equipaB`, preserva categoria, fecha entrada por `jogadoresPermitidos`, reinicia prontos/pontuações e abre `SalaDeEspera2x2Activity`.
- Matchmaking concluído não volta automaticamente à fila; a desforra cria uma sala privada com os mesmos participantes, preservando UID/playerKey e equipas.
- Não há alteração de ranking, histórico, regras Firebase, pontuação, XP, timers ou limpeza de matchmaking.

Limites:

- O fluxo 2x2 exige aceitação dos quatro jogadores para criar a revanche.
- Não foi adicionada UI dedicada para gerir rejeição/cancelamento de desforra 2x2; erros reativam o botão e mantêm o jogador no pódio.
