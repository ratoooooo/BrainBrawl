# Pergunta o Luso - TEST_REPORT

## Correção crítica — Matchmaking, fundos consistentes e pódio eliminatórias grupo (pré-beta)

### Causa real — matchmaking / sala de espera

- **Prontos 1x1**: `obterProntos1x1` contava todas as chaves em `prontos/`, mesmo com valor `false` ou chaves de jogadores já `off`, o que podia alinhar mal com os 2 jogadores realmente presentes.
- **Reserva de lugares**: `chavesJogadoresReais` contava jogadores com `estado == off`, podendo bloquear entradas ou dar sensação de “sala cheia” com fantasmas em salas abertas.
- **Desconexão**: `onDisconnect` só punha o jogador `off` em `jogadores/`, deixando entradas antigas em `prontos/` (pronto fantasma).
- **2x2**: não existia verificação de “prontos” antes de iniciar; só se verificava o número de jogadores.

### Correção aplicada — matchmaking / espera

- `JogoCompetitivoRepository`: `obterProntos1x1` só devolve chaves com valor booleano `true`; `obterProntos2x2` idem; `marcarPronto2x2` adicionado; `onDisconnect` remove também `prontos/{chave}`; `removerJogador2x2` limpa `prontos/{chave}`; `chavesJogadoresReais` ignora jogadores `off`.
- `Sala1x1ViewModel`: ao avançar, exige 2 prontos **cujas chaves pertencem aos 2 jogadores presentes**; debounce simples em `verificarProntosEAvancar`; evento `OponenteSaiu` quando a contagem de presentes desce após ter havido 2.
- `Sala2x2ViewModel`: ao entrar marca pronto; `iniciarJogo` só prossegue com 4 prontos válidos (presentes); eventos `JogadoresNaoProntos` e `OponenteSaiu`.
- Mensagens novas: `sala_espera_jogador_saiu`, `sala_espera_jogador_saiu_2x2`, `sala_2x2_nem_todos_prontos` (PT + EN/ES/FR/DE).

### Presença / ready-state (como ficou)

- **1x1**: presença = jogador `on` na sala; “pronto” = `prontos/{chave}=true` para esse jogador; o admin só inicia com 2 presentes e 2 prontos válidos; desligar/remove limpa pronto.
- **2x2**: cada jogador marca pronto ao entrar com sucesso; o admin só inicia com 4 presentes e 4 prontos válidos.

### Causa real — pódio eliminatórias grupo (sobrevivente em falta)

- Em `JogoViewModel.guardarResultadoEEnviarPontuacoes`, o ramo `if (!admin)` gravava `terminado` + pontuação em Firebase para participantes, mas **o admin/anfitrião que ainda jogava saltava o `guardarResultadoJogador`**. O sobrevivente costumava ser quem criou a sala → o pódio (`temResultadoGrupoGuardado`) ficava incompleto (ex.: 2/3).
- Eliminados já passavam por `marcarJogadorEliminado` com estado `eliminado`.

### Correção — sobrevivente / vencedor

- `guardarResultadoEEnviarPontuacoes`: para jogo em grupo (`!modoSolo`), **sempre** chama `guardarResultadoJogador` (inclui admin que joga), depois navega para pontuações.
- `ResultadoJogador` ganhou `estadoPartida`; `EstatisticasService.ordenarPodioGrupoEliminatorias` ordena: `terminado` primeiro, depois pontos, depois respostas certas.
- `PontuacoesViewModel` usa essa ordenação quando `modoJogo == eliminatorias`.

### Ranking intermédio (eliminados)

- `EsperaEliminadoViewModel`: “progresso fiável” considera também `pontos > 0` ou `respostasCertas > 0`, para não mostrar só “Em jogo” quando já há pontuação parcial real.

### Fundos (ecrãs uniformizados)

- Raiz dos layouts passou de `@drawable/bg_app_gradient` para `@drawable/bg_main_premium` (alinhado à Main) em: escolher modo/tipo/categoria, escolha categoria modos, explorar categorias, salas de espera (grupo/1x1/2x2), matchmaking, pódio (`activity_pontuacao*`), histórico, perfil/editar perfil, espera eliminado.

### Ficheiros alterados (lista principal)

- `JogoViewModel.kt`, `JogoCompetitivoRepository.kt`, `Sala1x1ViewModel.kt`, `SalaDeEspera1x1Activity.kt`, `Sala2x2ViewModel.kt`, `SalaDeEspera2x2Activity.kt`
- `EstatisticasService.kt`, `PontuacaoRepository.kt`, `PontuacoesViewModel.kt`, `EsperaEliminadoViewModel.kt`
- Layouts: `activity_escolher_modo.xml`, `activity_tipo_modo_classico.xml`, `activity_escolher_categoria.xml`, `activity_escolha_categoria_modos.xml`, `activity_explorar_categorias.xml`, `activity_sala_de_espera*.xml`, `activity_matchmaking.xml`, `activity_pontuacao.xml`, `activity_pontuacao_multi.xml`, `activity_pontuacao1x1.xml`, `activity_historico.xml`, `activity_meu_perfil.xml`, `activity_editar_perfil.xml`, `activity_espera_eliminado.xml`
- `values/strings.xml`, `values-en-rGB/strings.xml`, `values-es/strings.xml`, `values-fr/strings.xml`, `values-de-rDE/strings.xml`

### Comandos executados (validação)

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean assembleDebug testDebugUnitTest build` — **BUILD SUCCESSFUL** (debug + release compile, `testDebugUnitTest` OK).

### Testes manuais recomendados

- Matchmaking 1x1/2x2: fila → sala de espera → um jogador sai → todos veem estado e não iniciam sem contagem/prontos corretos; partida completa.
- Grupo eliminatórias: 3 jogadores, 2 eliminados, 1 sobrevivente (incl. anfitrião) → pódio 3/3 e ordem com vencedor em destaque.
- Fundos: percorrer Main vs ecrãs listados.
- Histórico: utilizador autenticado grava; convidado não.

### Riscos pendentes

- Se o dispositivo do sobrevivente crashar **antes** de `guardarResultadoJogador`, o pódio pode continuar incompleto sem Cloud Function de fecho autoritativo.
- `cancelarPorBackground` no `MatchmakingActivity.onStop` continua dependente do ciclo de vida Android; utilizador que minimiza durante a procura ainda pode sair da fila (comportamento intencional de limpeza).

## Correção ícones/badges/conquistas - 2026-05-15

### Objetivo

- Substituir o sistema antigo de badges com prefixes `j`/`v` por prefixes corretos `pj`, `vt`, `xp`, `rc` e `cr`.
- Centralizar a escolha do maior badge desbloqueado em `UteisConquistas`.
- Evitar crash/build fail quando algum asset de badge ainda não existe.

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/utils/UteisConquistas.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/test/java/com/example/brainbrawl/utils/UteisConquistasTest.kt`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Alterações aplicadas

- Removidos usos antigos de `jogosBadges`, `vitoriaBadges`, `respostasBadges`, `R.drawable.j*` e `R.drawable.v*`.
- Criadas listas ordenadas de forma decrescente: `partidasJogadasBadges`, `vitoriasBadges`, `xpBadges`, `respostasCertasBadges`, `creditosBadges`/`crBadges`.
- Criadas funções centralizadas para obter o drawable do maior marco atingido:
  - `obterBadgePartidasJogadas`
  - `obterBadgeVitorias`
  - `obterBadgeRespostasCertas`
  - `obterBadgeXp`
  - `obterBadgeCreditos`
- `PerfilAmigoActivity` passou a pedir os ícones a `UteisConquistas`, sem lógica local repetida.
- A resolução usa o nome do drawable e devolve `null` se o asset estiver ausente, evitando crash e permitindo adicionar PNGs depois.

### Drawables confirmados

- Presentes: `pj10`, `pj50`, `pj100`.
- Presentes: `vt50`, `vt100`.
- Presentes: `xp100`, `xp500`, `xp1000`, `xp2500`, `xp5000`, `xp10000`, `xp25000`, `xp50000`, `xp100000`, `xp250000`, `xp500000`, `xp1000000`.
- Presentes: `rc50`, `rc100`, `rc500`, `rc1000`.
- Presentes: `cr1`, `cr5`, `cr10`, `cr25`, `cr50`, `cr100`, `cr250`, `cr500`, `cr1000`.

### Drawables em falta

- `pj25.png`
- `vt5.png`
- `vt25.png`
- `rc200.png`

### Edge cases cobertos

- Valor negativo, zero e abaixo do primeiro marco devolvem `null`.
- Valor exatamente igual a um marco devolve esse marco.
- Valor entre dois marcos devolve o maior marco já atingido.
- Valor acima do maior marco devolve o maior badge disponível.
- Drawable ausente devolve `null` e a UI esconde a imagem em vez de crashar.
- Testes unitários cobrem os thresholds puros por nome.

### Testes manuais sugeridos

1. Abrir perfil de amigo com 0 jogos/vitórias/respostas e confirmar que não aparece badge.
2. Abrir perfil de amigo com valores abaixo do primeiro marco e confirmar que não aparece badge.
3. Abrir perfil com 10/50/100 jogos e confirmar `pj10`/`pj50`/`pj100`.
4. Adicionar manualmente `pj25.png`, `vt5.png`, `vt25.png` e `rc200.png` e confirmar que passam a aparecer sem alteração de código.
5. Confirmar que perfis antigos sem campos continuam a abrir sem crash.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test` - OK.

Observações:

- O build gera o relatório de lint em `app/build/reports/lint-results-debug.html`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

## V1 Release Prep - 2026-05-14

### Objetivo da fase

- Preparar a app Android v1 para teste interno/release local.
- Rever nome visivel, launcher label/icon, manifest, permissoes, matchmaking desativado, comandos de build e documentacao final.
- Nao alterar logica de pontuacao, XP, ranking, historico, convites, salas, categorias, login legado ou matchmaking.

### Verificacoes feitas

- Nome visivel: `app_name`, `brand_o_luso` e `brand_full_caps` mantem `Pergunta o Luso`/`PERGUNTA O LUSO` nos idiomas configurados.
- Manifest: `android:label="@string/app_name"`; `LoginActivity` continua `MAIN/LAUNCHER`; restantes activities internas estao `exported=false`.
- Matchmaking: `MatchmakingActivity` nao esta registada no `AndroidManifest.xml`; `MainActivity` nao tem referencia/listener para abrir matchmaking; os ids antigos `btn_matchmaking_1x1` e `btn_matchmaking_2x2` continuam apenas no XML da Main.
- Assets launcher: `@mipmap/avatar_14` e `@mipmap/avatar_14_round` existem, com foreground/background adaptativo em `mipmap-anydpi-v26` e variantes por densidade.
- Permissoes: `INTERNET` mantida por Firebase; `POST_NOTIFICATIONS` mantida porque o lint Android 13+ exige a permissao devido a `NotificationTarget` presente na dependencia Glide ja existente, mesmo sem fluxo novo de notificacoes do sistema nesta fase.
- Firebase Rules: nao foram alteradas nesta fase.

### Ficheiros alterados nesta fase

- `app/src/main/AndroidManifest.xml`
- `README.md`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease` - OK.

Observacoes:

- Uma primeira tentativa de `build` falhou quando `POST_NOTIFICATIONS` foi removida; o lint Android 13+ exige a permissao porque a dependencia Glide contem `NotificationTarget`. A permissao foi reposta e o `build` final passou.
- `build` gerou o relatorio de lint em `app/build/reports/lint-results-debug.html`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Resultado esperado de release

- `assembleRelease` gerou APK release local com sucesso.
- O projeto nao configura signing real nesta fase; o artefacto serve para validacao tecnica interna, nao para publicacao final em loja.
- Para distribuicao publica/fechada via Play Console sera necessario configurar signing real/seguro fora deste relatorio, sem commitar secrets.

### Checklist manual final recomendada

1. Abrir app.
2. Criar conta nova.
3. Login/logout.
4. Entrar como convidado.
5. Main.
6. Perfil.
7. Ranking.
8. Historico.
9. Amigos.
10. Pedido de amizade.
11. Convite 1x1.
12. Jogo 1x1 completo.
13. Convite 2x2.
14. Jogo 2x2 completo.
15. Sala grupo.
16. Classico.
17. Caotico.
18. Eliminatorias.
19. Pontuacao final.
20. Categorias publicas.
21. Categorias personalizadas.
22. Criar/editar/eliminar pergunta.
23. Confirmar convidado sem XP/historico/ranking.
24. Confirmar matchmaking invisivel.
25. Testar ecra pequeno.
26. Testar idioma ingles.
27. Fechar/reabrir app e confirmar sessao persistente.

### Riscos pendentes

- R1: `jogadores.read=true` e campo password/hash legado continuam bloqueador antes de beta publico.
- R2: pontuacao/XP/ranking/historico continuam client-authoritative.
- R3: salas com convidados ainda tem writes amplos.
- Solucao futura recomendada: Cloud Functions para fechos/estatisticas/conquistas e Auth anonimo para convidados.
- Matchmaking aleatorio continua desativado e so deve voltar numa branch nova.

### Decisao final

- V1 pronta para teste interno Android.
- Antes de beta publico, resolver os riscos R1/R2/R3 e configurar signing real fora do repositorio.

## V1 Textos + i18n + QA Final - 2026-05-14

### Objetivo da fase

- Uniformizar o nome visivel da app como `Pergunta o Luso`.
- Corrigir textos principais em portugues e mover hardcoded seguro para resources.
- Completar traducoes minimas nos idiomas existentes: `pt`, `en-rGB`, `es`, `fr`, `de-rDE`.
- Fazer verificacao final estatica/build sem alterar matchmaking, pontuacao, XP, ranking, historico, convites, salas ou categorias de forma estrutural.

### Textos/i18n corrigidos

- Nome visivel da app atualizado de `Pergunta ó Luso` para `Pergunta o Luso` em `app_name`, `brand_o_luso` e `brand_full_caps`.
- Títulos XML de convites 1x1/2x2, ecrã de eliminado e label `Admin` passaram a usar `@string`.
- Toasts principais de jogo, eliminatorias, convites 1x1/2x2, bónus de sequência, erros de sala/perguntas/pódio e espera de eliminado passaram para resources.
- Dialogs/dicas de categorias e modos passaram a usar strings localizadas.
- Cards de categorias públicas/personalizadas passaram a usar strings para `Jogar`, `Guardar`, `Avaliar`, estado pública/privada, criador e métricas.

### Idiomas atualizados

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en-rGB/strings.xml`
- `app/src/main/res/values-es/strings.xml`
- `app/src/main/res/values-fr/strings.xml`
- `app/src/main/res/values-de-rDE/strings.xml`

### Hardcoded movidos

- `EscolherCategoriaActivity.kt`: dicas, eventos, dialog de categorias personalizadas e confirmação de eliminação.
- `EscolhaCategoriaModosActivity.kt` e `EscolherModoActivity.kt`: dicas de categorias/modos.
- `ExplorarCategoriasActivity.kt`: botões e labels de cards principais.
- `JogoActivity.kt`, `Jogo1x1Activity.kt`, `Jogo2x2Activity.kt`, `UteisJogo.kt`: progresso, bónus e mensagens de erro/espera.
- `ConvidarAmigo1x1Activity.kt`, `ConvidarAmigo2x2Activity.kt`, `EsperaEliminadoActivity.kt`, `JogadoresSalaAdapter.kt`: mensagens visíveis.
- Layouts: `activity_convidar_amigo.xml`, `activity_convidar_amigo2x2.xml`, `activity_espera_eliminado.xml`, `activity_jogo.xml`.

### QA final por codigo

- Main nao regista listeners para `MatchmakingActivity`; os botoes antigos de matchmaking continuam apenas no XML como `gone`/inativos.
- `AndroidManifest.xml` nao expoe `MatchmakingActivity`.
- Persistencia de convidados continua bloqueada pelos guardas existentes em pontuacoes/historico/badges: `isGuest`, `TIPO_JOGADOR_GUEST` e `uid` vazio/`guest_`.
- 1x1/2x2 por convite mantem extras de categoria oficial/publica/personalizada; esta fase so alterou textos visiveis.
- Firebase Rules nao foram alteradas nesta fase.

### Ficheiros alterados nesta fase

- Kotlin: `ConvidarAmigo1x1Activity.kt`, `ConvidarAmigo2x2Activity.kt`, `EscolhaCategoriaModosActivity.kt`, `EscolherCategoriaActivity.kt`, `EscolherModoActivity.kt`, `EsperaEliminadoActivity.kt`, `ExplorarCategoriasActivity.kt`, `JogadoresSalaAdapter.kt`, `JogoActivity.kt`, `Jogo1x1Activity.kt`, `Jogo2x2Activity.kt`, `UteisJogo.kt`.
- Layouts: `activity_convidar_amigo.xml`, `activity_convidar_amigo2x2.xml`, `activity_espera_eliminado.xml`, `activity_jogo.xml`.
- Resources: `values/strings.xml`, `values-en-rGB/strings.xml`, `values-es/strings.xml`, `values-fr/strings.xml`, `values-de-rDE/strings.xml`.
- Docs: `TEST_REPORT.md`.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK durante a implementacao.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` - OK.

Observacoes:

- `build` gerou o relatorio de lint em `app/build/reports/lint-results-debug.html`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Checklist manual final v1

1. Abrir app sem crash.
2. Criar conta nova.
3. Login com conta existente.
4. Logout.
5. Entrar como convidado.
6. Ver Main em 360dp.
7. Confirmar nome visivel da app consistente.
8. Confirmar matchmaking invisivel/inacessivel.
9. Abrir Perfil.
10. Abrir Ranking.
11. Abrir Historico.
12. Abrir Amigos.
13. Enviar pedido de amizade.
14. Aceitar pedido de amizade.
15. Enviar convite 1x1.
16. Aceitar convite 1x1.
17. Iniciar e terminar 1x1.
18. Enviar convite 2x2.
19. Aceitar convites 2x2.
20. Iniciar e terminar 2x2.
21. Criar sala grupo.
22. Entrar numa sala por codigo.
23. Jogar classico.
24. Jogar caotico.
25. Jogar eliminatorias.
26. Ver pontuacao final.
27. Explorar categorias.
28. Criar categoria personalizada.
29. Adicionar pergunta.
30. Editar pergunta.
31. Eliminar pergunta.
32. Jogar categoria personalizada.
33. Confirmar convidado sem XP.
34. Confirmar convidado sem historico.
35. Confirmar convidado fora do ranking.
36. Testar idioma ingles.
37. Testar idioma espanhol, se possivel.
38. Testar idioma frances, se possivel.
39. Confirmar que textos principais nao ficam cortados em 360dp.
40. Confirmar que nao ha crashes ao voltar/navegar rapido entre ecras principais.

### Riscos pendentes

- `jogadores.read=true` e campo password/hash legado continuam bloqueador antes de beta publico.
- Pontuacao/XP/ranking/historico continuam client-authoritative.
- Salas com convidados ainda tem writes amplos.
- Versao robusta futura deve usar Cloud Functions/Auth anonimo.
- Matchmaking aleatorio continua desativado e so deve voltar numa branch nova.
- Ainda podem existir hardcoded residuais em areas tecnicas, logs, repositorios e ficheiros de matchmaking desativado; os textos principais ativos foram priorizados.

### Confirmacao

- Matchmaking continua desativado/inacessivel pela Main.
- Nao houve alteracao de Firebase Rules nesta fase.
- Nao houve alteracao de pontuacao, XP, ranking, historico, convites, salas, categorias estruturais ou login legado.

## V1 Polish Audit + UI Polish Pack - 2026-05-14

### Resumo da auditoria

- A Main mantem o matchmaking aleatorio invisivel/inacessivel: os cards antigos existem no XML como `gone`, sem foco/click, e `MainActivity` nao regista listeners para abrir `MatchmakingActivity`.
- Login e Registo ja tinham scroll e hierarquia visual aceitavel para v1; nao houve alteracao de autenticacao nem login legado.
- Meu Perfil ja tinha scroll e badges em grelha; nao houve alteracao de logica de stats, XP ou conquistas.
- Ranking tinha risco de corte em labels/valores no item em 360dp.
- Historico tinha hardcoded visivel em layout/adaptador e item com risco de aperto entre pontuacao/data/categoria.
- Amigos, pedidos e convites estavam funcionais, mas os cards de pedido/convite destoavam visualmente e a lista de amigos nao tinha estado vazio simples.
- Pontuacoes finais podiam transbordar em ecras pequenos por falta de scroll.
- Entrada por codigo tinha texto de estado com contraste fraco no fundo claro.
- Nao foi encontrada necessidade de alterar Firebase Rules.

### Problemas encontrados

- Hardcoded obvio em Historico: titulo, botao voltar e fallbacks do adapter.
- Cards de pedidos/convites com padding/cores/botoes menos consistentes que o resto da app.
- Falta de estado vazio em Amigos quando nao ha amigos carregados.
- Possivel corte em item de Ranking para labels/valores mais longos.
- Possivel overflow vertical em pontuacao final grupo/1x1/2x2.
- Contraste fraco do `txt_estado` em Entrada em sala.
- Warning simples de parametro `adminHint` nao usado em `SalaRepository`.

### Correcoes aplicadas

- Historico: removeu hardcoded, adicionou strings/fallbacks, melhorou elipses/maxLines e distribuiu data/pontuacao de forma mais segura.
- Amigos: adicionou estado vazio simples, padding nos recyclers e cards mais consistentes para pedidos/convites.
- Ranking: ajustou largura/tamanhos do item para reduzir risco de corte em 360dp.
- Pontuacoes: `activity_pontuacao.xml`, `activity_pontuacao1x1.xml` e `activity_pontuacao_multi.xml` passaram a ter scroll seguro.
- Entrada em sala: titulo ligeiramente mais contido, inputs com altura estavel e estado com cor legivel.
- SalaRepository: warning simples de parametro legado suprimido sem alterar comportamento.

### Ficheiros alterados nesta fase

- `app/src/main/res/layout/activity_historico.xml`
- `app/src/main/res/layout/item_historico_jogo.xml`
- `app/src/main/java/com/example/brainbrawl/HistoricoAdapter.kt`
- `app/src/main/res/layout/activity_amigos.xml`
- `app/src/main/res/layout/item_pedido_amizade.xml`
- `app/src/main/res/layout/item_convite.xml`
- `app/src/main/java/com/example/brainbrawl/AmigosActivity.kt`
- `app/src/main/res/layout/item_ranking_jogador.xml`
- `app/src/main/res/layout/activity_pontuacao.xml`
- `app/src/main/res/layout/activity_pontuacao1x1.xml`
- `app/src/main/res/layout/activity_pontuacao_multi.xml`
- `app/src/main/res/layout/activity_sala_de_espera.xml`
- `app/src/main/java/com/example/brainbrawl/repositories/SalaRepository.kt`
- `app/src/main/res/values/strings.xml` e variantes `en-rGB`, `es`, `fr`, `de-rDE`
- `TEST_REPORT.md`

### Firebase Rules

- Nao foram alteradas nesta fase.
- Riscos R1/R2/R3 continuam documentados e fora do escopo deste polish pack.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK durante a implementacao.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` - OK.

Observacoes:

- `build` gerou o relatorio de lint em `app/build/reports/lint-results-debug.html`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Testes manuais recomendados

1. Abrir app sem crash.
2. Login com conta existente.
3. Criar conta nova.
4. Entrar como convidado.
5. Ver Main em 360dp.
6. Confirmar matchmaking invisivel/inacessivel.
7. Abrir Perfil.
8. Abrir Ranking.
9. Abrir Historico.
10. Abrir Amigos.
11. Enviar/aceitar pedido de amizade.
12. Enviar convite 1x1.
13. Aceitar convite 1x1.
14. Iniciar e terminar 1x1.
15. Enviar convite 2x2.
16. Aceitar convites 2x2.
17. Iniciar e terminar 2x2.
18. Criar sala grupo.
19. Entrar numa sala por codigo.
20. Jogar classico.
21. Jogar caotico.
22. Jogar eliminatorias.
23. Ver pontuacao final.
24. Explorar categorias.
25. Criar categoria personalizada.
26. Adicionar pergunta.
27. Editar pergunta.
28. Eliminar pergunta.
29. Jogar categoria personalizada.
30. Confirmar convidado sem XP/historico/ranking.
31. Ver ecras principais em 360dp.
32. Ver se textos nao ficam cortados.

### Riscos pendentes

- Nao houve walkthrough visual real nesta execucao automatica; as correcoes foram por auditoria estatica e build.
- Algumas telas antigas ainda mantem estilos herdados/hardcoded fora do polish seguro desta fase.
- O matchmaking continua com ficheiros antigos no repo, mas sem entrada ativa na Main/manifest.
- Deprecated Gradle features continuam reportadas pelo Gradle.

### Confirmacao

- Matchmaking continua desativado/inacessivel pela Main.
- Nao houve alteracao de pontuacao, XP, ranking, historico, convites, salas, categorias, Firebase paths ou Firebase Rules.

## Perfil competitivo, stats avancadas e badges v1 - 2026-05-14

### Objetivo da fase

- Implementar o perfil competitivo com resumo de estatisticas, grelha de conquistas e suporte a imagens locais por nome de drawable.
- Criar badges v1 client-side para respostas certas, partidas jogadas e vitorias, gravadas em Firebase por UID quando o jogador esta autenticado.
- Preservar pontuacao base, XP, ranking, historico, convites, salas, categorias, convidados e matchmaking desativado.

### Ficheiros alterados nesta fase

- Modelos: `Badge.kt`, `BadgeFamily.kt`.
- Service: `BadgesService.kt`.
- Repository: `BadgesRepository.kt`.
- Perfil: `MeuPerfilViewModel.kt`, `MeuPerfilActivity.kt`, `activity_meu_perfil.xml`, `item_badge.xml`.
- Config/resources: `FirebasePaths.kt`, `strings.xml` e variantes `en-rGB`, `es`, `fr`, `de-rDE`.
- Tests/docs: `BadgesServiceTest.kt`, `firebase-rules.json`, `TEST_REPORT.md`, `ARCHITECTURE_PLAN.md`, `FIREBASE_RULES_NOTES.md`.

### Badges criadas

- RC - respostas certas: `RC_1`, `RC_10`, `RC_50`, `RC_100`, `RC_250`, `RC_500`, `RC_1000`, `RC_2500`, `RC_5000`.
- PJ - partidas jogadas/terminadas: `PJ_1`, `PJ_10`, `PJ_50`, `PJ_100`, `PJ_250`, `PJ_500`, `PJ_1000`, `PJ_2500`, `PJ_5000`.
- VT - vitorias: `VT_1`, `VT_10`, `VT_50`, `VT_100`, `VT_250`, `VT_500`, `VT_1000`, `VT_2500`, `VT_5000`.

### Assets locais

- A UI resolve drawables por `drawableName`, usando nomes previsiveis: `rc1`, `rc10`, `rc50`, `rc100`, `rc250`, `rc500`, `rc1000`, `rc2500`, `rc5000`, equivalentes `pj*` e `vt*`.
- Se o asset especifico nao existir, tenta `badge_default` ou `badge_locked`; se tambem nao existirem, usa icones internos existentes (`ic_trophy`/`ic_lock`) sem crash.
- Nao foram geradas imagens, nao foram usados URLs remotos e nao foi introduzida dependencia nova para imagens.

### Firebase

- Novo node: `conquistas/{uid}/{badgeId}`.
- Escrita client-side idempotente via transaction: se a badge ja existir, a transaction aborta sem sobrescrever `desbloqueadaEm`.
- Convidados e perfis sem Auth nao leem nem gravam conquistas; a UI mostra a grelha bloqueada sem persistencia.

### Comandos executados

- `python3 -m json.tool firebase-rules.json` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` - OK.

Observacoes:

- Mantem-se o warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Testes manuais recomendados

1. Conta nova: abrir Perfil, confirmar stats a zero e 27 badges bloqueadas sem crash.
2. Conta com jogos: confirmar partidas, vitorias, respostas certas, taxa de vitoria, XP e nivel; verificar desbloqueio conforme thresholds.
3. Assets em falta: remover/nao adicionar `rc10.png` ou outro e confirmar fallback visual sem crash.
4. Convidado: abrir perfil quando aplicavel e confirmar que nao aparece node em `conquistas`.
5. Badge ja desbloqueada: reabrir perfil e confirmar que nao duplica nem altera timestamp.
6. Matchmaking: confirmar que continua sem entrada ativa na UI.
7. Fluxos preservados: 1x1/2x2 por convite, salas, categorias, historico e ranking.

### Riscos pendentes

- Badges v1 continuam client-side; um cliente modificado poderia tentar gravar conquistas proprias validas em termos de formato. A versao robusta deve mover validacao/desbloqueio para Cloud Functions.
- A grelha mostra todas as 27 badges; em perfis com ecras pequenos fica dependente de scroll.
- Nao houve walkthrough manual Firebase real automatizado nesta ronda; os testes finais foram build/unit.

## UI polish e strings/i18n - 2026-05-12

### Ficheiros alterados nesta fase

- Kotlin/UI: `MainActivity.kt`, `LoginActivity.kt`, `RegistarActivity.kt`, `AmigosActivity.kt`, `RankingActivity.kt`, `RankingAdapter.kt`, `MeuPerfilActivity.kt`, `PerfilAmigoActivity.kt`, `SalaDeEsperaActivity.kt`, `SalaDeEsperaGrupoActivity.kt`, `SalaDeEspera1x1Activity.kt`, `SalaDeEspera2x2Activity.kt`, `ExplorarCategoriasActivity.kt`, `AdicionarPerguntaActivity.kt`, `ConviteAdapter.kt`, `EscolherModoActivity.kt`, `routes/BottomNavHelper.kt`.
- Layouts: `activity_main.xml`, `activity_login.xml`, `activity_registar.xml`, `activity_amigos.xml`, `activity_ranking.xml`, `item_ranking_jogador.xml`, `activity_meu_perfil.xml`, `activity_perfil_amigo.xml`, `activity_sala_de_espera.xml`, `activity_sala_de_espera_1x1.xml`, `activity_sala_de_espera2x2.xml`, `activity_pontuacao1x1.xml`, `activity_pontuacao_multi.xml`, `activity_explorar_categorias.xml`, `activity_escolher_categoria.xml`, `activity_adicionar_pergunta.xml`, `item_pedido_amizade.xml`, `item_podio.xml`.
- Resources: `values/strings.xml`, `values-en-rGB/strings.xml`, `values-es/strings.xml`, `values-fr/strings.xml`, `values-de-rDE/strings.xml`.

### Ecra revistos

- Revistos estaticamente: Login, Registo, Main, Ranking, Amigos, Perfil, Perfil de amigo, Entrar numa sala, Sala de espera grupo, Sala 1x1, Sala 2x2, Pontuacao 1x1, Pontuacao multi/grupo, Explorar Categorias, Criar/Editar Categoria.
- Revistos visualmente em emulador 360dp aproximado: Login, Registo, Main e Ranking.
- Abertura direta por `am start` de varias activities internas foi bloqueada pelo Android com `SecurityException` porque nao estao exportadas; por isso os restantes ecras ficaram validados por layout/build e nao por walkthrough completo.

### Problemas visuais corrigidos

- Main: o card `Vamos jogar?` ficou com a imagem principal do jogo, `JOGAR AGORA` centrado, `ENTRAR NUMA SALA` dentro do card e sem o botao separado por baixo.
- Main: bottom nav alinhada com contentores de icone consistentes e header estabilizado para nao colapsar em 360dp.
- Login/Registo: hints e textos movidos para resources; hint de password encurtado para evitar corte.
- Ranking: estatisticas de cada linha compactadas para evitar quebra visual em largura estreita.
- Perfil: card principal com scroll interno e altura constrangida para reduzir risco de corte em 360dp.
- Sala de espera: estado e botao voltar separados para evitar sobreposicao.
- Sala 2x2: linhas de jogadores passaram a usar pesos em vez de larguras fixas apertadas.
- Adicionar Pergunta: campos, botoes e espacamentos aproximados ao estilo visual existente.

### Strings e idiomas

- Portugues fica como base em `res/values/strings.xml`.
- Ingles usa a pasta existente `res/values-en-rGB/strings.xml`.
- Criadas `res/values-es/strings.xml` e `res/values-fr/strings.xml`.
- A pasta existente `res/values-de-rDE/strings.xml` foi mantida resource-complete para o lint/build; traducoes alemas antigas foram preservadas e novas chaves nao alvo foram preenchidas com fallback ingles.
- A app continua a depender apenas dos resource qualifiers normais do Android para abrir na lingua do telemovel. Nao foi criado seletor manual nem alteracao programatica de `Locale`.
- Foram corrigidos problemas de XML em resources, incluindo comentarios invalidos do tipo `//` e strings com aspas soltas.

### Textos hardcoded remanescentes

- Permanecem textos visiveis em areas fora do escopo seguro desta ronda: matchmaking desativado, jogo, convites 1x1/2x2, historico, espera de eliminado e alguns dialogs/adapters antigos.
- Estes textos nao foram movidos agora para evitar mexer em fluxos explicitamente protegidos pelo pedido: matchmaking, jogo, convites, historico, pontuacao/XP/ranking funcional.
- As strings principais dos ecras priorizados e da Main foram movidas para resources e existem nos idiomas suportados principais.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` - OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` - OK.

Observacoes:

- Mantem-se o warning existente `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Testes visuais e i18n

- Emulador usado com `wm size 720x1600` e `wm density 320`, equivalente aproximado a 360dp de largura.
- Screens revistos: Login, Registo, Main e Ranking.
- App instalada e aberta em debug sem crash nos ecras testados.
- Idiomas testados por app locale: `en-GB`, `es` e `fr`; os textos principais de Login/Registo mudaram conforme esperado. O locale foi depois reposto.

### Riscos pendentes

- Nao foi feito walkthrough visual completo de todos os ecras obrigatorios porque algumas activities internas nao sao exportadas e exigem dados/estado de fluxo.
- Nao houve teste multi-dispositivo nem teste Firebase real para salas, convites ou partidas.
- A pasta alema existente esta completa para build/lint, mas as novas chaves podem usar fallback ingles porque alemao nao era idioma alvo desta fase.
- Ainda existem hardcoded strings em zonas preservadas para uma fase futura controlada.

## Atualizacao categorias personalizadas, editor e Main - 2026-05-11

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/ExplorarCategoriasViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/EditarCategoriaViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/CategoriaRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/models/Pergunta.kt`
- `app/src/main/res/layout/activity_adicionar_pergunta.xml`
- `app/src/main/res/layout/activity_escolher_categoria.xml`
- `app/src/main/res/layout/activity_main.xml`
- `firebase-rules.json`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`
- `FIREBASE_RULES_NOTES.md`

### Novo fluxo de categorias

- A gestao de categorias personalizadas passou para `ExplorarCategoriasActivity`.
- `Explorar Categorias` mostra categorias publicas, minhas categorias e o botao `Criar Categoria`.
- O fluxo principal de `Jogar Agora` continua a escolher modo/categorias oficiais; o antigo botao de criar categoria em `EscolherCategoriaActivity` agora encaminha para `Explorar Categorias`.
- Convidados podem ver/jogar publicas se ja tiverem nome de jogador, mas criar/editar/guardar/avaliar continua a pedir conta.
- Nao houve migracao destrutiva nem limpeza de categorias antigas.

### Jogar categoria -> escolher modo

- Ao tocar em `Jogar` numa categoria publica ou personalizada, aparece o chooser: `Classico/grupo`, `1x1 por convite`, `2x2 por convite`, `Eliminatorias`.
- `Caotico` nao aparece neste chooser porque continua a representar mistura de categorias/todas as perguntas, nao uma categoria especifica.
- Grupo/eliminatorias criam sala com as perguntas da categoria escolhida.
- 1x1/2x2 continuam a passar por `ConvidarAmigo1x1Activity`/`ConvidarAmigo2x2Activity`; nao foi reativado matchmaking aleatorio.
- Extras preservados/adicionados para categoria: `uid`, `nomeUtilizador`, `nomeJogador`, `nomeCategoria`, `categoriaPublicaId`, `donoUid`, `donoCategoria`, `modoJogo`, `admin`.

### Edicao por numero de pergunta

- `AdicionarPerguntaActivity` mostra `Total de perguntas: X` e botoes numerados `1..N` vindos das perguntas reais carregadas do Firebase.
- Ao tocar num numero, o formulario e preenchido com pergunta, opcoes, resposta correta, imagem e dificuldade quando existirem.
- Guardar com uma pergunta selecionada reutiliza o `perguntaId` original e atualiza esse node, sem criar duplicado.
- `Nova pergunta` limpa o formulario e volta ao estado de criacao.
- `Eliminar atual` pede confirmacao e remove apenas a pergunta selecionada.
- Categorias antigas sem `imagem` ou `dificuldade` continuam validas.

### Dificuldade

- Foi adicionado o campo opcional `dificuldade` com valores `facil`, `media`, `dificil`.
- O editor mostra `media` por defeito visual.
- O campo ainda nao altera pontuacao, tempo, XP, matchmaking, ranking ou historico.

### Main

- `ENTRAR NUMA SALA` foi movido para dentro do card principal `Vamos jogar?`.
- O botao separado abaixo foi removido.
- O card passou a usar `@mipmap/avatar_14_foreground` como imagem do jogo.
- `JOGAR AGORA` continua a abrir `EscolherModoActivity`.
- `ENTRAR NUMA SALA` continua a abrir `SalaDeEsperaActivity`.

### Firebase Rules

- `firebase-rules.json` foi alterado apenas para aceitar `imagem` e `dificuldade` em perguntas e metadados de categoria publica/personalizada em `sala_1x1`/`sala_2x2`.
- `dificuldade` e opcional; quando existe tem de ser `facil`, `media` ou `dificil`.
- Nao foram usadas validacoes com `childrenCount`.
- Nao foram abertas permissoes globais.

### Comandos executados

- `python3 -m json.tool firebase-rules.json`
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

- Continua o warning antigo `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

### Testes manuais recomendados

1. Abrir Main e confirmar que `ENTRAR NUMA SALA` esta dentro do card principal.
2. Tocar em `JOGAR AGORA` e confirmar que abre a escolha de modo.
3. Tocar em `ENTRAR NUMA SALA` e confirmar que abre entrada por codigo.
4. Abrir `Explorar Categorias` e confirmar `Categorias publicas`, `Minhas categorias` e `Criar Categoria`.
5. Criar categoria personalizada e adicionar 5 perguntas.
6. Confirmar `Total de perguntas: 5` e botoes `1 2 3 4 5`.
7. Tocar na pergunta 2 e confirmar que pergunta/opcoes/resposta/imagem/dificuldade carregam.
8. Editar a pergunta 2 e confirmar no Firebase que atualizou o mesmo `perguntaId`.
9. Confirmar que nao foi criado duplicado.
10. Usar `Nova pergunta` e criar uma pergunta nova.
11. Eliminar uma pergunta com confirmacao.
12. Jogar categoria personalizada e confirmar o chooser de modo.
13. Jogar categoria publica e confirmar o chooser de modo.
14. Confirmar que 1x1/2x2 continuam por convite.
15. Confirmar que matchmaking aleatorio nao aparece.

### Riscos pendentes

- Nao executei testes manuais com Firebase real/emulador nesta ronda automatica.
- O chooser de categoria especifica exclui `Caotico` por decisao de produto; se no futuro houver caotico por categoria, sera preciso novo contrato de perguntas.
- A edicao remove pergunta individualmente; nao reordena nem reescreve a lista toda.


Data: 2026-05-08

## Verificacao pos-MVVM - 2026-05-08

### Escopo

- Ronda apenas de verificacao apos a migracao MVVM.
- Nao foram implementadas funcionalidades novas.
- Nao foi feito refactor novo.
- Nao foram alteradas Firebase Rules.
- Nao foram encontrados bugs causados pela migracao MVVM que exigissem correcao de codigo.

### Validacao por fluxo

- Login/registo/logout: codigo revisto; `LoginActivity` e `RegistarActivity` continuam com os seus ViewModels existentes e nao foram alterados nesta ronda. Smoke test em emulador iniciou a app e confirmou sessao persistente abrindo a Main sem crash. Logout continua a chamar `MainViewModel.terminarSessao()`, que marca offline e termina Auth.
- Main: validado por build, analise de `MainActivity`/`MainViewModel` e smoke test em emulador. Main abre sem crash; perfil/badge estao isolados no ViewModel; Activity mantem clicks/navegacao/renderizacao.
- Amigos/pedidos/convites: validado por analise de listeners. `MainViewModel` usa `AmigosRepository.observarPedidosRecebidos()` e `observarConvitesRecebidos()`, remove listeners em `pararNotificacoes()`/`onCleared()` e nao cria listeners duplicados para o mesmo identificador.
- Convite 1x1: fluxo de `ConvidarAmigo1x1Activity` -> `AmigosRepository` -> `SalaDeEspera1x1Activity` revisto; nao foi alterado pela ronda MVVM.
- Convite 2x2: fluxo de `ConvidarAmigo2x2Activity` -> `AmigosRepository` -> `SalaDeEspera2x2Activity` revisto; nao foi alterado pela ronda MVVM.
- Sala grupo: `SalaDeEsperaActivity`/`SalaDeEsperaGrupoActivity` continuam com `SalaGrupoViewModel`; paths/extras preservados.
- Jogo classico/caotico/eliminatorias: `JogoActivity` continua a enviar extras esperados para `PontuacoesActivity`; eliminados continuam a passar por `EsperaEliminadoActivity`.
- Pontuacao grupo: `PontuacoesActivity` recebe os mesmos extras e renderiza `PontuacoesUiState`; `PontuacoesViewModel` escuta `PontuacaoRepository.escutarResultadosGrupo()` e preserva historico/estatisticas uma vez.
- Pontuacao 1x1: `Pontuacao1x1Activity` recebe os mesmos extras; `Pontuacao1x1ViewModel` preserva listener de pontuacoes, historico, estatisticas, convidado sem persistencia e desforra.
- Pontuacao 2x2: `Pontuacao2x2Activity` recebe os mesmos extras; `Pontuacao2x2ViewModel` preserva podio, espera por 4 jogadores, recorde, historico e estatisticas.
- Historico: `HistoricoRepository.guardarHistoricoUmaVez()` continua a ser chamado apenas com UID valido pelos ViewModels de pontuacao.
- Ranking: nao foi alterado; as atualizacoes de estatisticas continuam a passar por `PontuacaoRepository.atualizarEstatisticasSalaUmaVez()`.
- Perfil: `MainViewModel` carrega perfil por UID-first com fallback por `nomeUtilizador`/email; `MeuPerfilActivity` nao foi alterada.
- Convidados sem estatisticas: validado nas guards `podeGravarPersistente()` em 1x1/2x2 e `uid.isNotBlank()` no grupo, mantendo convidado no podio sem historico/XP/ranking.
- Matchmaking aleatorio: continua removido/desativado; `MainActivity` nao tem navegacao para `MatchmakingActivity`.

### Verificacoes tecnicas executadas

- Pesquisa em Activities migradas por imports/acesso direto a repositories/listeners Firebase:
  - `MainActivity.kt`, `Pontuacao1x1Activity.kt`, `Pontuacao2x2Activity.kt` e `PontuacoesActivity.kt` nao contem `PontuacaoRepository`, `HistoricoRepository`, `EstatisticasService`, `AmigosRepository`, `JogadorRepository`, `FirebaseDatabase` ou `ValueEventListener`.
- Pesquisa de matchmaking na Main/manifest:
  - Sem `MatchmakingActivity`, `abrirMatchmaking` ou listeners ativos na Main/manifest. Os ids XML escondidos continuam sem click.
- Smoke test em emulador:
  - `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`: OK.
  - `adb -s emulator-5554 shell am start -n com.example.brainbrawl/.LoginActivity`: OK.
  - `dumpsys activity activities`: app resumida em `MainActivity`, confirmando sessao persistente e arranque sem crash.

### Build e testes

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK; `BUILD SUCCESSFUL`.

Avisos mantidos: `SalaRepository.kt:84 Parameter 'adminHint' is never used` e deprecated Gradle features.

### Limites da verificacao

- Nao foi executado teste manual completo com criacao real de contas, pedidos de amizade e convites entre varios utilizadores nesta ronda.
- Nao foram jogadas partidas completas 1x1/2x2/grupo no Firebase real durante esta verificacao.
- A validacao funcional profunda desses fluxos deve ser feita manualmente com varios dispositivos/contas, mas a verificacao pos-MVVM nao encontrou regressao estatica, de build, de arranque ou de contrato de extras/listeners.

## Auditoria e migracao MVVM - 2026-05-08

### Resumo da auditoria

- Activities com `FirebaseDatabase`, `FirebaseAuth`, `DatabaseReference` ou `ValueEventListener` diretos: nenhuma encontrada em `*Activity.kt` por pesquisa automatica. O acesso Firebase esta concentrado em repositories/services.
- Activities com logica pesada antes desta ronda: `Pontuacao1x1Activity`, `Pontuacao2x2Activity`, `PontuacoesActivity` e `MainActivity`.
- Logica pesada encontrada em pontuacoes: listeners de resultados, persistencia de historico, atualizacao de estatisticas/XP/ranking indireto, anti-duplicacao local e identificacao de convidado/auth estavam nas Activities.
- Logica pesada encontrada na Main: leitura de perfil, resolucao de UID/nome, XP/avatar/nivel e listeners de pedidos/convites estavam na Activity.
- Activities ja razoavelmente alinhadas com MVVM e nao migradas nesta ronda: login/registo, ranking, historico, amigos/perfil, jogos, salas de espera e categorias principais ja usam ViewModels/repositories.
- Repositories grandes identificados: `PontuacaoRepository`, `JogoCompetitivoRepository`, `AmigosRepository` e `CategoriaRepository`. Nao foram divididos porque a divisao nao era necessaria para esta migracao segura.
- Services reutilizados: `EstatisticasService`, `ProgressaoService`, `ScoreService`, `ScoreCompetitivoService` e `AuthService`.
- ViewModels existentes preservados: `Pontuacao1x1ViewModel` foi expandido em vez de duplicado; `Sala1x1ViewModel`, `Sala2x2ViewModel`, `SalaGrupoViewModel`, `AmigosViewModel`, `CategoriasViewModel`, `ExplorarCategoriasViewModel`, `EditarCategoriaViewModel`, `LoginViewModel`, `RegistarViewModel`, `MeuPerfilViewModel`, `PerfilAmigoViewModel`, `RankingViewModel` e `HistoricoViewModel` nao foram duplicados.
- Listeners movidos para ViewModel: resultados/pontuacoes 1x1, 2x2 e grupo; pedidos de amizade e convites recebidos da Main.
- UiState/Event que faltavam: adicionados para pontuacoes competitivas/grupo e para Main.
- Navegacao misturada com dados: reduzida em pontuacoes e Main; as Activities mantem apenas clique, renderizacao, Toast e navegacao.
- Pontos de risco com convidados: a persistencia de pontuacoes continua bloqueada por `uid` valido e `tipoJogador != guest`; convidado aparece no podio, mas nao grava estatisticas, XP, historico nem ranking.

### Ficheiros migrados/criados

- Criado `app/src/main/java/com/example/brainbrawl/viewmodels/Pontuacao2x2ViewModel.kt`.
- Criado `app/src/main/java/com/example/brainbrawl/viewmodels/PontuacoesViewModel.kt`.
- Criado `app/src/main/java/com/example/brainbrawl/viewmodels/MainViewModel.kt`.
- Expandido `app/src/main/java/com/example/brainbrawl/viewmodels/Pontuacao1x1ViewModel.kt`.
- Migradas `Pontuacao1x1Activity.kt`, `Pontuacao2x2Activity.kt`, `PontuacoesActivity.kt` e `MainActivity.kt` para observar estado e eventos.

### Responsabilidades apos migracao

- `Pontuacao1x1Activity`: renderiza dois jogadores, observa estado de desforra, mostra mensagens e navega.
- `Pontuacao2x2Activity`: renderiza podio 2x2, estado de espera/final e mensagens.
- `PontuacoesActivity`: renderiza lista/podio de grupo com mensagem de espera/erro.
- `MainActivity`: configura botoes, navega, renderiza perfil/XP/avatar/badge e executa logout visual.
- `Pontuacao1x1ViewModel`: escuta pontuacoes, gere historico/estatisticas uma vez, calcula identidade do jogador atual e preserva fluxo de desforra.
- `Pontuacao2x2ViewModel`: escuta resultados 2x2, calcula podio/estado/recorde, grava historico e estatisticas uma vez.
- `PontuacoesViewModel`: escuta resultados de grupo, prepara podio/MVP/mensagens e grava historico/estatisticas uma vez.
- `MainViewModel`: carrega perfil principal, resolve UID-first com fallback legado, gere badge social e remove listeners no ciclo de vida.

### Ficheiros que ficaram pendentes

- `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity` ainda usam repository diretamente; nao foram migradas para evitar mexer no fluxo de convites agora.
- `EscolherModoActivity` e `EscolhaCategoriaModosActivity` ainda usam `AuthService`/extras diretamente; sao fluxo de navegacao leve.
- `AdicionarPerguntaActivity`, `EscolherCategoriaActivity` e `ExplorarCategoriasActivity` ja usam ViewModels, mas podem receber uma segunda limpeza futura de validacoes/UI.
- `PontuacaoRepository` continua grande; uma futura divisao por leitura de podio, historico/estatisticas e desforra pode fazer sentido quando houver testes dedicados.

### Verificacoes executadas nesta ronda

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK em verificacao intermedia; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK; `BUILD SUCCESSFUL`.

Avisos mantidos: `SalaRepository.kt:84 Parameter 'adminHint' is never used` e deprecated Gradle features.

### Testes manuais pendentes

- Nao foram executados testes manuais multi-dispositivo nesta ronda dentro do ambiente atual.
- Continuam recomendados os testes manuais de Auth, convidado, Main, amigos/convites, salas, 1x1, 2x2, pontuacoes e categorias descritos no pedido.

### Riscos pendentes

- O fecho de estatisticas/historico continua cliente-side, ainda que centralizado em ViewModels/repositories.
- A Main agora remove e recria listeners sociais por ciclo de vida; deve ser validada manualmente com contas reais recebendo pedidos/convites.
- A migracao manteve os paths Firebase e a formula de pontuacao; bugs preexistentes de dados antigos continuam possiveis por compatibilidade legado.
- Matchmaking aleatorio continua removido/desativado.

## Decisao final - Matchmaking aleatorio desativado - 2026-05-08

### Opcao escolhida

- Opcao B: remover/desativar temporariamente o matchmaking aleatorio 1x1/2x2 da experiencia do jogador.
- Motivo: o fluxo atual ja acumulava varias correcoes concorrentes em fila, resultado, sala, `playerKey`, convidados e Firebase Rules. Mesmo com transacoes cliente-side, a arbitragem continua vulneravel a estados intermedios, clientes duplicados e regras que nao conseguem provar a lotacao real. Para esta fase, a decisao mais segura e entregar a app sem botoes que levam a uma funcionalidade instavel.
- Esta decisao substitui as tentativas anteriores documentadas abaixo. As secoes antigas ficam como historico tecnico do que foi tentado, nao como estado recomendado atual.

### Alteracoes aplicadas

- `MainActivity.kt`: removida a navegacao para `MatchmakingActivity`; os listeners de `1x1 Aleatorio` e `2x2 Aleatorio`, a flag `matchmakingAbrindo` e `abrirMatchmaking()` foram retirados.
- `activity_main.xml`: os cards `1x1 Aleatorio` e `2x2 Aleatorio` ficaram escondidos (`visibility="gone"`) e nao clicaveis; o card `Explorar Categorias` ocupa a linha sem margem inicial.
- `AndroidManifest.xml`: `MatchmakingActivity` deixou de estar registada, impedindo navegacao interna acidental para o ecrã de matchmaking.
- `firebase-rules.json`: removido o uso de `childrenCount()` em `jogadoresPermitidos`; as rules mantem apenas estrutura/booleanos e a lotacao fica a cargo do Kotlin/repository.
- `FIREBASE_RULES_NOTES.md` e `ARCHITECTURE_PLAN.md`: atualizados para refletir que o matchmaking aleatorio esta temporariamente fora da experiencia.

### Fluxos preservados

- Convites 1x1 continuam no fluxo `ConvidarAmigo1x1Activity` -> `AmigosRepository.enviarConvite1x1()` -> `SalaDeEspera1x1Activity`.
- Convites 2x2 continuam no fluxo `ConvidarAmigo2x2Activity` -> `AmigosRepository.enviarConvite2x2()` -> `SalaDeEspera2x2Activity`.
- Pontuacao, XP, ranking, historico, categorias, login/registo, amigos, perfil e modos grupo/solo nao foram alterados nesta decisao.
- Convidados nao perderam os modos que ja usavam fora do matchmaking aleatorio; o corte foi apenas dos botoes aleatorios da Main e da rota para `MatchmakingActivity`.

### Verificacoes executadas

- `python3 -m json.tool firebase-rules.json`
  - OK; JSON valido.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK; `BUILD SUCCESSFUL`.

Avisos mantidos: `SalaRepository.kt:84 Parameter 'adminHint' is never used` e deprecated Gradle features.

### Riscos pendentes

- Os ficheiros de matchmaking continuam no codigo para futura reimplementacao, mas sem entrada de UI nem manifesto.
- Nao foi feito teste manual multi-dispositivo de convites Firebase nesta ronda; a garantia atual vem da analise dos caminhos, de nao tocar nesses fluxos e de build/test.
- Nodes antigos de `matchmaking` que existam no Firebase podem continuar na base de dados, mas a app ja nao oferece caminho de jogador para consumi-los.
- A reativacao futura deve ser tratada como reimplementacao nova e simples, idealmente com backend autoritativo ou transacao Kotlin muito pequena, sem convidados ate o fluxo autenticado estar estavel.

## UX - salas privadas por convite e indicador social - 2026-05-08

### Alteracoes aplicadas

- Salas competitivas criadas por convite passam a gravar `origem=convite`, `entradaFechada=true`, `lotacaoMaxima` e `jogadoresPermitidos` no momento de `enviarConvite1x1()`/`enviarConvite2x2()`.
- `SalaDeEspera1x1Activity` e `SalaDeEspera2x2Activity` carregam a origem/privacidade da sala antes de expor o codigo. Se a sala for `convite`, `matchmaking` ou estiver fechada, mostram apenas `Partida por convite` ou `Partida automática` e escondem o botao de copiar.
- Salas antigas sem `origem` e sem `entradaFechada` continuam com o comportamento atual, mostrando codigo. Salas de grupo/manual continuam a mostrar e copiar codigo pela `SalaDeEsperaGrupoActivity`.
- `JogoCompetitivoRepository` reconhece `origem=convite` como sala fechada e valida a entrada pelos jogadores ja listados, mantendo compatibilidade por `uid`, `playerKey`, nome e display legado.
- `ConvidarAmigo2x2Activity` passou a exigir 3 amigos selecionados, porque uma sala 2x2 privada precisa nascer com os 4 jogadores definidos e ja nao depende de partilha de codigo.
- `MainActivity` passou a observar pedidos de amizade e convites recebidos pendentes para contas/perfis autenticados, usando `AmigosRepository` com UID-first e fallback por `nomeUtilizador`.
- O botao `Amigos` na Main mostra um badge vermelho com contador (`9+` no limite visual) quando ha pedidos/convites pendentes; convidados nao veem indicador social.
- Os listeners sociais da Main sao criados em `onStart`, evitam duplicacao por identificador e sao removidos em `onStop`.

### Ficheiros alterados nesta ronda UX

- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/drawable/bg_notification_badge.xml`
- `app/src/main/java/com/example/brainbrawl/repositories/AmigosRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/res/layout/activity_sala_de_espera_1x1.xml`
- `app/src/main/res/layout/activity_sala_de_espera2x2.xml`
- `app/src/main/java/com/example/brainbrawl/config/GameConstants.kt`

### Verificacoes executadas

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK; `BUILD SUCCESSFUL`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK; `BUILD SUCCESSFUL`.

Avisos mantidos: `SalaRepository.kt:84 Parameter 'adminHint' is never used` e deprecated Gradle features.

### Testes manuais recomendados

- Criar convite 1x1 e confirmar que a sala mostra `Partida por convite` sem codigo nem copiar.
- Criar convite 2x2 e confirmar que a sala mostra `Partida por convite` sem codigo nem copiar.
- Criar/entrar numa sala de grupo por codigo e confirmar que codigo e copiar continuam visiveis.
- Receber pedido de amizade e confirmar badge em `Amigos` na Main.
- Receber convite 1x1/2x2 e confirmar badge em `Amigos` na Main.
- Aceitar/remover pendencias e confirmar que o badge desaparece quando a lista fica vazia.

## Issue #1 - Matchmaking loop e UI da sala de espera - 2026-05-08

### Analise do fluxo

- Export real analisado (`project-34e54-default-rtdb-export (1).json`): nao existe node `matchmaking`; existem `salas`, `sala_2x2` e `jogadores` UID-first. Logo, a estrutura de matchmaking ainda precisa de ser criada pela app e bater exatamente com as rules.
- Entrada na fila: `MatchmakingViewModel.entrarComJogador()` chama `MatchmakingRepository.entrarNaFila()` e escreve em `matchmaking/{modo}/fila/{playerKey}`.
- Observacao da fila: `MatchmakingViewModel.observarFila()` escuta a fila para UI/contador e tenta formar match quando existem jogadores suficientes.
- Criacao de sala: `MatchmakingRepository.tentarCriarMatch()` reclama os jogadores em transacao; depois `criarSalaEPublicarResultados()` cria `sala_1x1/{codigo}` ou `sala_2x2/{codigo}` e so entao publica `resultados/{playerKey}`.
- Admin/criador: e o jogador cujo `playerKey` corresponde ao `criadorKey` que ganhou/chamou a transacao, alinhando writer Firebase, claim e sala.
- Remocao da fila: os jogadores selecionados sao removidos no mesmo update que publica resultados; cancelar remove apenas fila ainda `aguardando`.
- `onDisconnect`: continua registado ao entrar na fila e agora e cancelado explicitamente ao cancelar ou ao navegar para uma sala.
- Listeners: fila/resultado sao removidos em navegacao, `onDestroy`/`onCleared`; salas competitivas/grupo mantem os handles existentes.
- Resultado antigo: entrada nova apaga resultado anterior; alem disso, resultado precisa apontar para sala existente antes de navegar.
- Navegacao unica: `MatchmakingActivity` e `MatchmakingViewModel` mantem flags explicitas; `MainActivity` tambem bloqueia taps repetidos nos botoes de matchmaking.

### Causa encontrada

- O cancelamento antigo fazia `get()` ao resultado e depois removia fila fora de uma transacao. Se um match marcasse a fila como `encontrado` mas ainda nao tivesse publicado o resultado, o cancelamento podia remover a propria fila enquanto a sala ja estava a ser criada.
- `matches/{matchId}` era calculado apenas com `playerKey`; um match antigo entre os mesmos jogadores podia bloquear futuras tentativas do mesmo par/grupo.
- Resultados nao tinham identidade de sessao, por isso duas instancias com o mesmo UID podiam observar o mesmo resultado.
- A navegacao consumia resultado sem confirmar primeiro que a sala ja existia.
- Causa especifica confirmada nesta ronda: quando o segundo cliente ganhava a transacao, o codigo podia criar a sala com `admin/adminId/adminUid` do primeiro jogador da fila. As rules exigem que o writer seja o admin gravado, por isso a criacao da sala podia falhar com `permission_denied` depois de a fila ja estar em `encontrado`.
- Causa do erro ao entrar na fila: o payload local tinha sido expandido com `sessionId`, enquanto a estrutura recomendada e rules com `$other=false` exigem campos conhecidos. Isto e compatível com `permission_denied` logo no write inicial da fila; o campo foi removido do Firebase.
- Causa da fase atual "Erro ao criar sala": o fluxo ainda marcava jogadores selecionados como `estado=encontrado` dentro do claim antes de confirmar sala+resultados. Quando a criacao/publicacao falhava, o cliente recuperava com mensagem generica e podia deixar estado intermedio dificil de cancelar.
- Causa do caso de 3 jogadores no 1x1: `JogoCompetitivoRepository.adicionarJogador()` apenas reutilizava uma chave existente quando reconhecia a identidade; se um terceiro chegasse por resultado/codigo antigo ou identidade diferente, nao havia guarda de lotacao antes do `setValue()`.
- Causa exata ainda observada apos a primeira correcao: a guarda de lotacao em `adicionarJogador()` fazia `get()` dos jogadores e so depois `setValue()`. Dois clientes podiam ler a mesma lotacao antes da escrita final, permitindo corrida. Alem disso, salas vindas de matchmaking nao estavam marcadas como fechadas a lista original de `playerKey`, entao uma Activity aberta por resultado/codigo antigo ainda podia tentar adicionar uma nova entrada.
- O payload inicial da sala 1x1 tambem escrevia `prontos` para os jogadores durante a criacao. Esse estado foi retirado da criacao da sala: cada cliente marca o seu proprio pronto ao entrar, usando o fluxo ja existente e compativel com as rules.

### Solucao implementada

- `tentarCriarMatch()` guarda `criadorSelecionado = jogadoresSelecionados.first { it.playerKey == criadorKey }` e usa esse mesmo jogador em `matches`, `salaMap()` e `resultadoMap()`.
- `criarSalaEPublicarResultados()` recebe o `criador` explicitamente e deixou de calcular `jogadores.first()` internamente.
- O rollback pós-claim remove `matches/{matchId}` e as entradas `fila/{playerKey}` dos jogadores sem resultado publicado. Se a sala tinha sido criada e falhou a publicacao dos resultados, remove tambem a sala criada pelo mesmo admin.
- O claim transacional agora cria apenas `matches/{matchId}`; a fila so e removida quando a sala foi criada com o numero exato de jogadores e os resultados foram publicados.
- `tentarCriarMatch()` valida `size == 2` no 1x1 e `size == 4` no 2x2, com `playerKey` nao vazio e sem duplicados. A mesma validacao repete antes da escrita da sala.
- Depois da transacao da sala, o repository confirma que `jogadores` tem exatamente o limite esperado antes de publicar resultados.
- `salaMap()` deixou de escrever `prontos` inicialmente no 1x1; a sala nasce apenas com `jogadores`, `admin/adminId/adminUid`, `estado` e `nomeCategoria`.
- Salas criadas por matchmaking agora nascem fechadas: `origem=matchmaking`, `entradaFechada=true`, `lotacaoMaxima=2/4` e `jogadoresPermitidos/{playerKey}=true`.
- `JogoCompetitivoRepository.adicionarJogador()` passou a tratar sala fechada antes de escrever: se o jogador atual nao corresponde a um `playerKey`/`uid` ja presente em `jogadores`, a entrada e bloqueada e nenhum `setValue()` e executado.
- Para salas abertas/convites, `adicionarJogador()` usa reserva transacional em `jogadoresPermitidos` antes de escrever um jogador novo, evitando a corrida `get()` + `setValue()`. Reabrir a Activity pelo mesmo jogador reutiliza a chave existente.
- `Sala1x1ViewModel` e `Sala2x2ViewModel` emitem `EntradaBloqueada`; as Activities mostram mensagem de sala cheia e voltam ao menu sem adicionar jogador extra.
- `MatchmakingViewModel` deixou de navegar apenas por existencia da sala; agora confirma que `sala_1x1/{codigo}/jogadores/{playerKey}` existe ou que a sala contem esse `playerKey`/`uid`. Resultado antigo que aponte para sala cheia/errada e consumido e ignorado.
- Logs adicionados/fortalecidos: path da fila, path da sala, modo, codigo, adminId/adminUid, lista/quantidade de selecionados, campos do payload, paths dos resultados, colisao de codigo e erro Firebase exato, com destaque para `permission_denied`.
- Cancelar agora limpa resultado/fila quando encontra resultado sem sala valida ou fila `encontrado` sem resultado/sala valida, evitando ficar preso em "Partida já encontrada".
- A UI mostra "A criar sala..." quando o jogador atual ja esta em `estado=encontrado` ou quando o cliente esta a criar o match; se a criacao falhar, mostra erro amigavel.
- Cancelamento validado contra resultado e sala: se existe resultado com sala valida, nao apaga; se nao ha resultado/sala valida, limpa fila/resultado do jogador para permitir voltar.
- O payload de fila/resultados foi simplificado e deixa de enviar `sessionId`, evitando rejeicao por campos inesperados nas rules.
- `matchId` inclui `playerKey` e `timestampEntrada`, permitindo rematches dos mesmos jogadores sem reaproveitar claim antigo.
- Antes de navegar, `MatchmakingViewModel` verifica a existencia da sala; se nao existir, remove/ignora o resultado antigo e continua sem loop.
- Ao navegar, remove listeners, cancela `onDisconnect`, consome resultado e emite um unico evento de abertura.
- Botao Voltar de entrada em sala ficou com texto branco.
- Codigo de sala em 1x1/2x2/grupo usa icone pequeno de copiar ao lado do codigo, com `contentDescription="Copiar código"` e Toast `Código copiado`.
- Codigo de sala recebido por Intent em salas de espera e entrada manual continua normalizado para maiusculas com `CodigoSalaUtils`.
- Firebase Rules foram ajustadas de forma especifica para aceitar `origem`, `lotacaoMaxima`, `entradaFechada` e `jogadoresPermitidos`, mantendo os limites 2/4 nesses nodes.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/MainActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/models/MatchmakingModels.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/java/com/example/brainbrawl/config/GameConstants.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/res/layout/activity_sala_de_espera.xml`
- `app/src/main/res/layout/activity_sala_de_espera_1x1.xml`
- `app/src/main/res/layout/activity_sala_de_espera2x2.xml`
- `app/src/main/res/drawable/ic_copy.xml`
- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `ARCHITECTURE_PLAN.md`
- `TEST_REPORT.md`

### Estrutura Firebase final do matchmaking

- `matchmaking/{modo}/fila/{playerKey}`: `playerKey`, `uid`, `tipoJogador`, `nomeDisplay`, `nomeUtilizador`, `nomeJogador`, `avatar`, `timestampEntrada`, `estado`, `isGuest`.
- `matchmaking/{modo}/resultados/{playerKey}`: `playerKey`, `uid`, `tipoJogador`, `codigoSala`, `modo`, `nomeCategoria`, `criadorId`, `criadorUid`, `estado=encontrado`, `timestampEntrada`, `jogadores`.
- `matchmaking/{modo}/matches/{matchId}`: claim transacional com `estado`, `codigoSala`, `modo`, `criadorId`, `criadorUid`, `timestampEntrada`.
- Salas reais continuam em `sala_1x1/{codigo}` e `sala_2x2/{codigo}`.
- `sala_1x1/{codigo}` e `sala_2x2/{codigo}` recebem `admin`, `adminId` e, quando existir, `adminUid` do mesmo `criadorSelecionado` que escreveu o claim.
- Matchmaking 1x1 publica exatamente 2 jogadores em `sala_1x1/{codigo}/jogadores`; 2x2 publica exatamente 4 em `sala_2x2/{codigo}/jogadores`. Jogadores extra permanecem na fila para um match seguinte.
- Salas de matchmaking adicionam `origem=matchmaking`, `entradaFechada=true`, `lotacaoMaxima` e `jogadoresPermitidos`. Em sala fechada, a sala de espera so confirma jogador que ja esta listado; nao adiciona terceiro.
- `jogadoresPermitidos` tambem funciona como reserva transacional de lotacao para salas abertas/convites: limite 2 em `sala_1x1` e limite 4 em `sala_2x2`.

### Verificacoes executadas

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK em verificacao intermedia.
- `python3 -m json.tool firebase-rules.json`
  - OK; JSON valido.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew clean`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - OK.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew build`
  - OK.

Avisos mantidos: `SalaRepository.kt:84 Parameter 'adminHint' is never used` e deprecated Gradle features.

### Testes manuais recomendados

- 1x1 Auth A+B: confirmar uma unica sala, ambos na mesma sala, sem entrada/saida em loop e sem navegacao duplicada apos rotacao/refresh.
- 1x1 Auth A+B+C quase simultaneo: confirmar que a primeira sala tem exatamente 2 jogadores e o terceiro fica fora dessa sala.
- 1x1 convidado + Auth: confirmar sala unica, convidado com nome correto e sem writes persistentes de estatisticas/XP/historico.
- 2x2 A/B/C/D: com tres jogadores nao cria sala; ao quarto cria uma unica `sala_2x2`.
- 2x2 A/B/C/D/E quase simultaneo: confirmar que a primeira sala tem exatamente 4 jogadores e o quinto fica fora dessa sala.
- Cancelamento: antes do match remove a propria fila; durante/depois do match nao apaga sala e nao volta a entrar em fila.
- Resultado antigo: fechar/voltar e entrar outra vez nao deve navegar com resultado anterior.
- UI grupo: botao Voltar legivel a branco, codigo em maiusculas, icone copia ao lado e entrada por codigo aceita minusculas.

### Riscos pendentes

- Matchmaking continua cliente-side; Cloud Functions ainda seriam a solucao autoritativa contra cliente modificado ou claim abandonado depois de `estado=criando`.
- Testes manuais multi-dispositivo/Firebase realtime ainda precisam de ser executados num ambiente com contas/convidados reais.

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

## Security Patch Pre-Walkthrough - 2026-05-14

### Causa real confirmada

- `jogadores/{id}/password` ainda nao e codigo morto: e escrito por `JogadorRepository.criarJogador`, lido por `JogadorRepository.toPerfilJogador` e usado por `LoginViewModel.entrarLegado`.
- Firebase Auth ja e usado nos fluxos novos de email/password, mas o login legado por `nomeUtilizador/password` continua ativo.
- Por isso, remover o campo `password` ou fechar totalmente `jogadores.read=true` nesta fase poderia quebrar compatibilidade legado, perfis, ranking ou amigos.

### Correcoes aplicadas

- Reforcado o guard de persistencia do resultado de grupo: `PontuacoesInput.podeGravarPersistente()` agora bloqueia `guest_`, `isGuest=true` e `tipoJogador=guest`, alem de exigir UID nao vazio.
- `PontuacoesActivity` passa `tipoJogador` e `isGuest` para o ViewModel quando esses extras existem.
- Adicionados limites conservadores nas Firebase Rules para categorias publicas e categorias personalizadas: nome, descricao, pergunta, resposta correta, opcoes, imagem e dificuldade.
- `imagem` e `dificuldade` continuam opcionais; `dificuldade`, quando existe, continua limitada a `facil`, `media` ou `dificil`.
- Valores antigos ja existentes e inalterados continuam aceites pelas rules, para reduzir risco de quebrar categorias antigas durante updates noutros campos.
- Logs residuais de matchmaking/salas competitivas deixam de expor diretamente codigo de sala, `playerKey`, UID e paths completos de updates.
- Matchmaking nao foi reativado nem alterado como fluxo.

### Ficheiros alterados nesta ronda

- `firebase-rules.json`
- `FIREBASE_RULES_NOTES.md`
- `TEST_REPORT.md`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/PontuacoesViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`

### Riscos pendentes

- R1: `jogadores.read=true` continua a expor dados de perfil e hash legado. Correccao completa deve esperar por migracao/split publico-privado e remocao planeada do login legado.
- R2: pontuacao, XP, vitorias, ranking e historico continuam client-authoritative. Correccao robusta deve ser feita com Cloud Functions/backend.
- R3: writes amplos em salas continuam necessarios enquanto convidados nao tiverem Auth anonimo ou backend autoritativo.
- Limites de perguntas copiadas para `salas`, `sala_1x1` e `sala_2x2` ficaram pendentes para evitar risco de quebrar fluxos de jogo/convite antes do walkthrough.
- Badges/conquistas futuras devem herdar o mesmo guard de convidados antes de persistirem progresso.

### Checklist manual recomendada

1. Criar conta nova.
2. Fazer login/logout.
3. Jogar como autenticado e confirmar XP, historico e ranking.
4. Jogar como convidado e confirmar que aparece em sala/podio, nao ganha XP, nao aparece no ranking, nao grava historico e nao cria `jogadores/{guestKey}`.
5. Criar categoria personalizada com texto normal.
6. Tentar criar pergunta vazia.
7. Tentar criar pergunta/opcao muito grande.
8. Jogar categoria personalizada.
9. Enviar convite 1x1.
10. Enviar convite 2x2.
11. Confirmar que matchmaking continua inacessivel.

### Validacoes desta ronda

- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.

Observacoes:

- Mantem-se o warning conhecido `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

## Beta Prep — UI/UX Polish + Anti-abuso leve - 2026-05-17

### Ecras auditados

- Login, perfil proprio, perfil publico de amigo, amigos/pedidos/convites.
- Escolher modo, escolher tipo, escolher categoria, categorias por modo.
- Explorar categorias, criar/editar categoria, adicionar/editar/eliminar pergunta.
- Matchmaking, sala de espera manual, sala 1x1, sala 2x2.
- Jogo classico/local, 1x1 e 2x2.

### Problemas encontrados

- Ainda existiam botoes com `@android:style/Widget.Button`, `@android:drawable` e fundos antigos `botao_branco_arredondado`/`botao_voltar`.
- Cartoes de categorias e perguntas misturavam cores hardcoded com o novo visual `bb_*`.
- Convites e pedidos de amizade tinham apenas acao positiva visivel no item; recusar ficava pouco claro no fluxo.
- O perfil publico escondia badges de resumo quando nao havia drawable resolvido, criando espacos visualmente inconsistentes.
- `UteisConquistas` referia assets inexistentes (`pj25`, `vt5`, `vt25`, `rc200`) apesar de existirem familias novas (`pj1`, `vt1`, `rc1`, `*250`, `*2500`, `*5000`).
- Perguntas personalizadas aceitavam duplicados com diferencas de maiusculas/minusculas e podiam guardar resposta correta fora das quatro opcoes se o estado do radio group ficasse incoerente.
- Convites podiam ser aceites sem confirmar primeiro se a sala ainda existia.
- Alguns botoes de sala podiam receber toques repetidos antes do estado Firebase responder.

### Correcoes visuais aplicadas

- Substituidos os botoes antigos/default por `BrainBrawlButton`, `bg_button_primary`, `bg_button_secondary` e `bg_button_danger` nos fluxos principais auditados.
- Polidos os cards dinamicos de `ExplorarCategoriasActivity`, incluindo cores `bb_text_*`, `bg_card_surface` e botoes com destaque/ perigo.
- Polido `activity_adicionar_pergunta.xml` e a lista dinamica de perguntas numeradas, com estados vazios e selecao visual consistentes.
- Polidos itens de pedidos, convites e convidar amigo, incluindo botoes aceitar/recusar/desafiar consistentes.
- Polidos Login, escolher modo/tipo/categoria, sala manual, sala 1x1, sala 2x2, perfil proprio, perfil de amigo e ecras de jogo 1x1/2x2/local.
- Removidas referencias visuais a drawables Android nativos nos ecras auditados.

### Correcoes de badges/imagens

- Criados fallbacks `badge_default.xml` e `badge_locked.xml`.
- Atualizado o mapping de `UteisConquistas` para apontar apenas para drawables existentes.
- Atualizados testes unitarios de `UteisConquistas`.
- `PerfilAmigoActivity` passa a mostrar fallback bloqueado nos badges de resumo em vez de esconder as imagens.
- `BadgeGridRenderer` ja tinha fallback centralizado; a nova ronda completou os assets que faltavam.

### Melhorias no matchmaking UX

- O ecrã de matchmaking foi preservado e continua com loading, contador, tempo, lista de jogadores e cancelamento.
- A ronda manteve o ViewModel/repository existentes e nao alterou o algoritmo.
- O polish incidiu no ecossistema do fluxo: salas 1x1/2x2, botoes de sair/iniciar, convites e entrada em sala.
- Aceitar convite agora valida se a sala ainda existe antes de navegar; convite expirado mostra mensagem amigavel e remove o convite.

### Validacoes anti-abuso leves

- Duplo toque em aceitar/recusar convites e pedidos fica bloqueado no item enquanto a acao corre.
- Duplo toque em guardar pergunta fica bloqueado ate receber evento do ViewModel.
- Duplo toque em iniciar/sair de sala 1x1/2x2 fica bloqueado localmente.
- Perguntas personalizadas rejeitam opcoes duplicadas ignorando caixa, campos vazios, campos longos e resposta correta fora das opcoes.
- Categorias publicas/copias passam a filtrar perguntas sem opcoes validas, com opcoes duplicadas ou resposta correta fora das opcoes.
- Convites expirados ou com sala apagada deixam de navegar para sala invalida.

### Ficheiros alterados nesta ronda

- Kotlin/UI: `AdicionarPerguntaActivity.kt`, `ExplorarCategoriasActivity.kt`, `AmigosActivity.kt`, `PerfilAmigoActivity.kt`, `SalaDeEspera1x1Activity.kt`, `SalaDeEspera2x2Activity.kt`.
- Kotlin/adapters: `ConviteAdapter.kt`, `PedidoAmizadeAdapter.kt`.
- Kotlin/dados: `AmigosRepository.kt`, `CategoriaRepository.kt`, `AmigosViewModel.kt`, `EditarCategoriaViewModel.kt`, `UteisConquistas.kt`.
- Layouts: login, perfil, perfil amigo, amigos, convidar 2x2, escolher modo/tipo/categoria, explorar categorias, adicionar pergunta, salas, jogo local/1x1/2x2, itens de convite/pedido/convidar.
- Resources: `badge_default.xml`, `badge_locked.xml`, strings de convite expirado/erro em PT/EN/ES/FR/DE.
- Testes: `UteisConquistasTest.kt`.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git status`: executado no fim da ronda.

Observacoes:

- Gradle continua a mostrar o aviso conhecido de funcionalidades deprecated para Gradle 9.0.
- `build` gerou relatorio lint debug e terminou com `BUILD SUCCESSFUL`.

### Testes manuais recomendados

1. Abrir Login.
2. Abrir Registo.
3. Abrir Main.
4. Abrir Perfil.
5. Abrir Ranking.
6. Abrir Historico.
7. Abrir Amigos.
8. Abrir Explorar Categorias.
9. Criar/editar categoria.
10. Adicionar/editar/eliminar pergunta.
11. Verificar botoes/cards em ecra pequeno.
12. Ver badges no proprio perfil.
13. Ver badges no perfil de amigo.
14. Confirmar imagens/fallback.
15. Enviar pedido de amizade.
16. Aceitar/recusar.
17. Tentar pedido duplicado.
18. Tentar pedido para si proprio.
19. Criar pergunta invalida.
20. Criar categoria sem perguntas.
21. Publicar categoria.
22. Jogar categoria publica.
23. Entrar matchmaking.
24. Cancelar matchmaking.
25. Convite 1x1.
26. Convite 2x2.
27. Sala grupo.
28. Sair/voltar atras em sala.
29. Terminar jogo e ver podio.
30. Confirmar XP/historico nao duplicam.
31. Confirmar convidado sem ranking/historico/XP/badges.

### Riscos pendentes

- Esta ronda nao endurece seguranca profunda; pontuacao/XP/ranking/historico continuam client-authoritative ate v2.0/Cloud Functions.
- Matchmaking continua client-side, apesar de mais claro e preservado.
- Ainda falta QA manual multi-conta real para corrida de convites, sala cheia, app fechada e reconexao.
- Nao foram alteradas Firebase Rules nesta fase.

## Matchmaking audit + controlled fixes - 2026-05-15

### Bugs/riscos encontrados

- `MatchmakingActivity` existia no codigo, mas nao estava registada no Manifest e os cards da Main estavam escondidos/inativos.
- O claim transacional criava `matches/{matchId}`, mas nao reservava os jogadores selecionados na fila dentro da mesma transacao. Em cenarios 1x1 com 3 jogadores ou 2x2 com 5 jogadores, outros clientes podiam ficar a tentar reclamar o mesmo grupo enquanto a sala ainda estava a ser criada.
- A procura nao tinha timeout visual nem contador de espera.
- O cancelamento por back/botao existia, mas o ViewModel nao tentava limpar a fila em `onCleared` quando a Activity era destruida fora do caminho normal.
- As rules de matchmaking continuam dependentes de transacao client-side no node `matchmaking/{modo}`, o que reduz duplicacao mas nao substitui um backend autoritativo.

### Bugs corrigidos

- A transacao de `MatchmakingRepository.tentarCriarMatch` agora marca os jogadores selecionados em `fila/{playerKey}` como `estado=encontrado`, com `codigoSala` e `criadorId`, antes de sair da transacao.
- Como os jogadores reclamados deixam de estar `aguardando`, outros clientes passam a escolher os proximos jogadores livres em vez de repetir o mesmo grupo.
- `MatchmakingViewModel` passou a ter timer de procura, timeout de 90 segundos, estado de cancelamento e limpeza defensiva em `onCleared`.
- O botao cancelar fica desativado durante cancelamento/preparacao da sala, reduzindo cliques repetidos.
- A Main voltou a expor os cards 1x1/2x2 aleatorios apenas quando existe UID autenticado.
- `MatchmakingActivity` foi registada como `exported=false`.

### Firebase Rules

- `firebase-rules.json` foi alterado de forma minima para permitir que o cliente criador marque outros jogadores da fila como `encontrado` durante o claim transacional, exigindo `criadorId == auth.uid`.
- `python3 -m json.tool firebase-rules.json`: OK.

### Testes manuais recomendados

1. Conta A abre matchmaking 1x1 sozinha e ve contador/tempo.
2. Conta B entra em 1x1 e ambas navegam para a mesma `sala_1x1`.
3. Conta A cancela antes de encontrar e a entrada sai da fila.
4. Tres contas entram em 1x1 quase ao mesmo tempo; apenas duas formam a primeira sala e a terceira nao e emparelhada com jogador ja reclamado.
5. Quatro contas entram em 2x2 e formam uma unica `sala_2x2` com 4 UIDs/playerKeys unicos.
6. Cinco contas entram em 2x2; quatro formam sala e a quinta fica em fila/aguarda novo grupo.
7. Spam no botao cancelar durante preparacao nao cria navegacao duplicada.
8. Back press cancela a fila quando ainda nao ha partida.
9. Fechar Activity antes de match tenta limpar a propria fila.
10. Confirmar que convites 1x1/2x2 continuam a abrir salas por convite.
11. Confirmar que pontuacao, XP, ranking, perfil e conquistas nao mudaram.

### Riscos restantes

- O matchmaking continua client-side. Um cliente malicioso ainda pode tentar abusar de writes amplos em `matchmaking/{modo}` enquanto a criacao da sala nao estiver em Cloud Functions.
- Nao existe `activeRooms/{uid}` autoritativo; a UI reduz entrada duplicada, mas controlo forte de "uma sala ativa por UID" deve ir para backend.
- `onDisconnect` ajuda contra perda de ligacao, mas varias sessoes com a mesma conta continuam a ser um caso imperfeito em Firebase client-side.
- Limpeza de jogadores fantasma continua baseada em timeout/stale do cliente, nao num job servidor.

### Validacoes desta ronda

- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK preliminar.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: falhou inicialmente por `MissingTranslation` nas novas strings de matchmaking; corrigido ao completar EN/ES/FR/DE.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK apos correcao.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test`: OK.

## Matchmaking estabilizacao + polish + QA - 2026-05-15

### Estado auditado

- Entrada na fila: `MatchmakingViewModel.entrarComJogador()` chama `MatchmakingRepository.entrarNaFila()`.
- Saida da fila: cancelamento normal, timeout, `onCleared` e limpeza stale do repository removem `matchmaking/{modo}/fila/{playerKey}` e o resultado local.
- Criacao de sala: `MatchmakingRepository.tentarCriarMatch()` reclama jogadores por transacao e `criarSalaEPublicarResultados()` cria `sala_1x1` ou `sala_2x2`.
- UI recebe match por listener em `matchmaking/{modo}/resultados/{playerKey}` e navega em `MatchmakingActivity.abrirSala()`.
- Listeners ativos: fila e resultado; ambos sao removidos antes de recriar e ao navegar/cancelar/destruir.

### Problemas encontrados

- O estado operacional ainda estava implicito em strings e flags, dificultando QA de `searching`, `creating`, `navigating`, `timeout` e `error`.
- A Main podia abrir mais de uma `MatchmakingActivity` com taps muito rapidos nos cards.
- Ao receber resultado antigo/invalido, o timer ja estava parado e a UI podia ficar sem estado claro.
- Ao ir para background durante procura, a fila nao era cancelada explicitamente enquanto a Activity continuava viva.
- Havia textos hardcoded no layout de matchmaking.

### Correcoes aplicadas

- Adicionado `MatchmakingStatus` com estados explicitos: `IDLE`, `SEARCHING`, `MATCH_FOUND`, `CREATING_ROOM`, `NAVIGATING`, `CANCELLING`, `CANCELLED`, `TIMEOUT` e `ERROR`.
- `MatchmakingUiState` passou a transportar o status operacional para a Activity.
- `MatchmakingActivity` ajusta loading, botao Cancelar/Voltar e textos com base no estado recebido.
- Main ganhou guard `matchmakingAberturaEmCurso` para evitar spam nos cards 1x1/2x2.
- `MatchmakingViewModel.cancelarPorBackground()` cancela silenciosamente a fila quando a app vai para background sem navegacao iniciada.
- Resultado antigo, sala invalida ou erro de confirmacao deixam agora um estado de erro claro e permitem voltar/tentar novamente.
- Textos hardcoded do layout foram movidos para strings e completados nos idiomas existentes.
- Firebase Rules nao foram alteradas nesta ronda de estabilizacao.

### Testes manuais recomendados

1. 1x1: jogador A entra sozinho e ve `SEARCHING` com tempo a contar.
2. 1x1: jogador B entra e ambos veem preparacao/navegacao para a mesma sala.
3. 1x1: A cancela antes de B entrar e sai da fila.
4. 1x1: A cancela no momento em que B entra.
5. 1x1: clicar 20 vezes no card de matchmaking na Main e confirmar uma unica Activity.
6. 1x1: back durante procura.
7. 1x1: background durante procura e confirmar limpeza da fila.
8. 2x2: 1, 2 e 3 jogadores ficam em espera.
9. 2x2: 4 jogadores formam sala.
10. 2x2: 5 jogadores entram e apenas 4 formam a primeira sala.
11. Spam no botao cancelar durante preparacao.
12. Rotacao de ecra durante procura.
13. Convites 1x1/2x2 continuam funcionais.
14. Partida, pontuacao, XP/CR, ranking, perfil e conquistas sem regressao.

### Riscos restantes

- A criacao de sala e a escolha de jogadores continuam client-side; Cloud Functions continuariam mais seguras.
- Nao existe `activeRooms/{uid}` autoritativo, portanto varias sessoes com a mesma conta ainda nao ficam 100% controladas.
- O cancelamento em background favorece nao deixar filas presas, mas pode surpreender se o utilizador alternar apps durante a procura.
- Perda de internet depende de `onDisconnect` e timeout/stale; limpeza perfeita exige backend.

### Validacoes desta ronda

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK preliminar.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test`: OK.

## Perfil de amigos - conquistas e polish visual - 2026-05-15

### Bug corrigido

- O perfil do proprio utilizador ja usava `MeuPerfilViewModel`, `BadgesService` e `BadgesRepository` para calcular conquistas e, quando aplicavel, persistir desbloqueios em `conquistas/{uid}`.
- O perfil de amigo so mostrava tres icones de destaque calculados diretamente na Activity, sem uma lista completa de conquistas para o amigo.
- Como `conquistas/{uid}` continua privado por rules, o perfil de amigo nao deve ler `conquistas/{friendUid}`. A correcao foi calcular as conquistas do amigo a partir das estatisticas publicas carregadas por `JogadorRepository.obterPerfil(utilizador.chavePrimaria)`.
- Falhas de resolucao/leitura do amigo deixam agora de causar loading infinito e caem num estado seguro de perfil desconhecido.

### Correcoes aplicadas

- `PerfilAmigoViewModel` passou a montar `BadgeProgress` com estatisticas do amigo e a chamar `BadgesService.calcularBadges` com desbloqueio local apenas para visualizacao.
- `PerfilAmigoActivity` passou a renderizar a grelha completa de conquistas do amigo, mantendo os icones de destaque existentes com `UteisConquistas`.
- `MeuPerfilActivity` e `PerfilAmigoActivity` passaram a partilhar `BadgeGridRenderer`, evitando logica duplicada de grelha/drawable/fallback nas Activities.
- `BadgeGridRenderer` agrupa conquistas por partidas jogadas, vitorias e respostas certas, usa fallback local seguro para drawables ausentes e diferencia bloqueadas/desbloqueadas por alpha/escala.
- As dicas de categorias/modos passaram a usar cards mais compactos, scroll em dialogs longos, marcador visual e botao OK do sistema.
- Firebase Rules nao foram alteradas.
- Matchmaking nao foi alterado nem reativado.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/viewmodels/PerfilAmigoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/PerfilAmigoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/MeuPerfilActivity.kt`
- `app/src/main/java/com/example/brainbrawl/utils/BadgeGridRenderer.kt`
- `app/src/main/java/com/example/brainbrawl/UteisDicas.kt`
- `app/src/main/res/layout/activity_perfil_amigo.xml`
- `app/src/main/res/layout/item_badge.xml`
- `app/src/main/res/drawable/bg_dica_card.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en-rGB/strings.xml`
- `app/src/main/res/values-es/strings.xml`
- `app/src/main/res/values-fr/strings.xml`
- `app/src/main/res/values-de-rDE/strings.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Edge cases cobertos

- Perfil proprio continua a usar conquistas persistidas quando o utilizador autenticado e o dono do perfil coincidem.
- Perfil de amigo mostra conquistas do amigo calculadas pelo perfil publico resolvido por UID/chave legado, nao pelo UID autenticado.
- Amigo com estatisticas nulas/ausentes recebe progresso 0 pelos fallbacks existentes do repository.
- Amigo com 0 estatisticas ve badges bloqueados.
- Falha de permissao ou leitura do perfil de amigo deixa de manter loading sem fim.
- Abrir perfis diferentes re-renderiza a grelha limpando views anteriores.
- Drawable de badge em falta usa `badge_default`, `badge_locked`, `ic_trophy` ou `ic_lock` sem crash.

### Testes manuais recomendados

1. Abrir o proprio perfil com estatisticas a 0.
2. Abrir o proprio perfil com algumas conquistas desbloqueadas.
3. Abrir perfil de amigo com jogos/vitorias/respostas certas e confirmar que os badges sao dele.
4. Abrir amigo A e depois amigo B e confirmar que a grelha nao fica trocada.
5. Abrir amigo antigo sem alguns campos de estatisticas.
6. Confirmar que `conquistas/{friendUid}` nao e lido nem exposto publicamente.
7. Confirmar que convidados continuam sem persistir conquistas.
8. Ver conquistas em ecra pequeno e maior.
9. Ver dicas em categorias, escolha de modo e modos de jogo.
10. Confirmar que matchmaking continua invisivel/inacessivel.

### Validacoes desta ronda

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test`: OK.

### Riscos pendentes

- As conquistas de amigos sao derivadas de estatisticas publicas; nao mostram timestamp real de desbloqueio porque `conquistas/{uid}` permanece privado por design.
- A grelha completa de conquistas continua alinhada com `BadgesService` v1: RC, PJ e VT. XP/CR existem em `UteisConquistas` como sistema de icones, mas nao foram ligados a persistencia de conquistas nesta correcao.
- Riscos de seguranca ja documentados permanecem: `jogadores.read=true` com password/hash legado, estatisticas client-authoritative e writes amplos em salas.

## Estado Atual — Matchmaking + Badges + Regressao Geral - 2026-05-15

### Objetivo

Validar o estado real depois da reativacao/estabilizacao do matchmaking e da integracao de badges/assets, sem criar funcionalidades novas e sem mexer em pontuacao, XP, ranking, historico, convites, salas ou categorias.

### Estado do matchmaking 1x1

- A entrada na fila usa `MatchmakingViewModel.entrarComJogador()` e `MatchmakingRepository.entrarNaFila()`.
- Path principal: `matchmaking/1x1/fila/{playerKey}`.
- Resultado individual: `matchmaking/1x1/resultados/{playerKey}`.
- Claim de jogadores: transacao em `matchmaking/1x1`.
- Sala criada em `sala_1x1/{codigoSala}` com exatamente 2 jogadores.
- A fila do modo oposto e limpa ao entrar, reduzindo risco de estar em 1x1 e 2x2 em simultaneo.
- `MatchmakingActivity` navega para `SalaDeEspera1x1Activity` apenas depois de confirmar que o jogador pertence a sala.
- `navegacaoIniciada` no ViewModel e `navegando` na Activity reduzem navegacao duplicada.
- Timeout de procura: 90 segundos, com limpeza da fila e mensagem ao utilizador.

### Estado do matchmaking 2x2

- A entrada na fila usa o mesmo fluxo controlado do 1x1, com limite de 4 jogadores.
- Path principal: `matchmaking/2x2/fila/{playerKey}`.
- Resultado individual: `matchmaking/2x2/resultados/{playerKey}`.
- Claim de jogadores: transacao em `matchmaking/2x2`.
- Sala criada em `sala_2x2/{codigoSala}` com exatamente 4 jogadores e `jogadoresPermitidos`.
- O repository valida quantidade e unicidade de `playerKey` antes de criar/publicar resultados.
- A Activity navega para `SalaDeEspera2x2Activity` apenas quando o resultado e a sala sao consistentes.

### Correcoes feitas nesta validacao

- Corrigido um edge case em `MatchmakingViewModel.cancelarPorBackground()`: se a app for para background quando a sala ja existe, o ViewModel deixa de apagar listeners/estado como se tivesse cancelado com sucesso. Agora mantem `MATCH_FOUND` e permite concluir o fluxo em vez de perder uma partida ja criada.
- Firebase Rules nao foram alteradas.

### Estado dos badges

- `BadgesService` continua a calcular a grelha persistente v1 para RC, PJ e VT a partir de `BadgeProgress`.
- Conquistas persistidas continuam UID-first em `conquistas/{uid}`.
- `BadgesRepository` bloqueia leitura/escrita para convidados e usa transacao por badge para nao sobrescrever timestamp existente.
- Perfil proprio le/grava conquistas apenas quando o `authUid` e o dono do perfil coincidem.
- Perfil de amigo calcula badges visualmente a partir de estatisticas publicas do amigo; nao le `conquistas/{friendUid}` porque esse node permanece privado.
- `UteisConquistas` contem resolucao de icones para PJ, VT, XP, RC e CR.
- `BadgeGridRenderer` usa `resources.getIdentifier` com fallback seguro para `badge_default`, `badge_locked`, `ic_trophy` ou `ic_lock`.

Assets verificados:

- Presentes: `rc1`, `rc10`, `rc50`, `rc100`, `rc250`, `rc500`, `rc1000`, `rc2500`, `rc5000`, `pj1`, `pj10`, `pj50`, `pj100`, `pj250`, `pj500`, `pj1000`, `pj2500`, `pj5000`, `vt1`, `vt10`, `vt50`, `vt100`, `vt250`, `vt500`, `vt1000`, `vt2500`, `vt5000`, todos os `xp*` esperados e todos os `cr*` esperados.
- Ausentes nesta copia local: assets opcionais/legado `rc200`, `pj25`, `vt5`, `vt25`. Os fallbacks `badge_default` e `badge_locked` foram adicionados na ronda de beta prep.
- Os assets ausentes nao quebram build porque os utilitarios devolvem `null` quando o drawable nao existe e a grelha cai para `ic_trophy`/`ic_lock`.

### Regressao principal verificada por codigo

- Login/registo e nome visivel continuam com `Pergunta o Luso`.
- `MatchmakingActivity` esta no Manifest como `exported=false`.
- Main so mostra matchmaking quando existe UID autenticado.
- Convites 1x1/2x2 continuam separados do matchmaking automatico.
- Salas por codigo continuam nos paths existentes.
- Categorias e perguntas continuam fora desta alteracao.
- Convidados continuam sem persistencia de conquistas; regras de XP/historico/ranking de convidados permanecem dependentes dos repositories/fluxos ja existentes.

### Firebase Rules

- `python3 -m json.tool firebase-rules.json`: OK, JSON valido.
- Regras nao foram alteradas nesta fase.
- `conquistas/{uid}` permite leitura/escrita apenas ao proprio utilizador autenticado.
- `matchmaking/{modo}` continua client-side e depende de validacoes/transactions no cliente.

### Ficheiros alterados nesta validacao

- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

Nota: ja existiam alteracoes pendentes de fases anteriores em Main, Matchmaking, layouts e strings; esta validacao nao as reverteu.

### Comandos executados

- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.

Observacoes:

- Gradle continua a mostrar aviso conhecido de funcionalidades deprecated para Gradle 9.0.
- `build` executou tambem lint/testes agregados e gerou APK release/debug sem falha.

### Checklist manual recomendada

1. Login com conta A.
2. Login com conta B.
3. Matchmaking 1x1 entre A e B.
4. Cancelar fila 1x1.
5. Terminar jogo 1x1.
6. Confirmar pontuacao/historico/XP.
7. Confirmar badges apos jogo.
8. Matchmaking 2x2 com quatro contas.
9. Cancelar fila 2x2.
10. Terminar jogo 2x2.
11. Confirmar equipa vencedora.
12. Confirmar convites 1x1 continuam funcionais.
13. Confirmar convites 2x2 continuam funcionais.
14. Criar sala por codigo.
15. Jogar modo classico.
16. Jogar modo caotico.
17. Jogar eliminatorias.
18. Criar categoria personalizada.
19. Jogar categoria personalizada.
20. Entrar como convidado.
21. Confirmar convidado sem XP/historico/ranking/badges.
22. Abrir perfil.
23. Confirmar badges e imagens.
24. Abrir ranking.
25. Abrir historico.
26. Fechar e reabrir app.

### Riscos pendentes

- Matchmaking continua client-side; Cloud Functions seriam mais fortes para criacao autoritativa de sala, anti-spam, `activeRooms/{uid}` e limpeza global de jogadores fantasma.
- Firebase Rules continuam a permitir bastante escrita em matchmaking/salas para suportar transacoes cliente-side.
- Riscos ja conhecidos permanecem: `jogadores.read=true` com password/hash legado, pontuacao/XP/ranking/historico client-authoritative e writes amplos em salas.
- Alguns assets opcionais/legado de badges estao ausentes (`rc200`, `pj25`, `vt5`, `vt25`), embora haja fallback sem crash.

### Decisao

Com base na validacao estatica e nos comandos de build/teste, e seguro tratar esta versao como candidata para teste interno, com a ressalva de executar o checklist manual multi-conta antes de fechar release publica/beta.

## Bugfix critico pre-walkthrough - convites 1x1/2x2 perguntas - 2026-05-14

### Causa real confirmada

- Nos convites 1x1/2x2, uma categoria oficial sem `categoriaPublicaId`, `donoUid` ou `donoCategoria` explicitos era marcada como `categoriaPersonalizada=true` porque o fallback usava `nomeUtilizador` do host.
- Com isso, uma categoria oficial como `Cultura Geral` podia ser procurada em `jogadores/{host}/categoriasPersonalizadas/Cultura Geral/perguntas` em vez de `categorias/Cultura Geral/perguntas`.
- O erro aparecia ao iniciar o jogo porque o carregamento competitivo nao encontrava perguntas validas no path personalizado errado.

### Correcoes aplicadas

- `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity` so marcam `categoriaPersonalizada=true` quando `donoUid` ou `donoCategoria` chegam explicitamente no intent.
- Categorias oficiais por convite ficam sem metadados de publica/personalizada e voltam a usar `categorias/{nomeCategoria}/perguntas`.
- `JogoCompetitivoRepository` passou a devolver erro claro quando a sala/categoria existe mas nao tem perguntas validas.
- O parser competitivo de perguntas le sempre por `snapshot.children`, aceitando perguntas guardadas como array/lista ou objeto/map, e ignora entradas invalidas.
- Para o ecrã competitivo atual continuam a ser usadas 4 opcoes por pergunta, porque `Jogo1x1Activity` e `Jogo2x2Activity` têm 4 botoes fixos.
- Firebase Rules nao foram alteradas.
- Matchmaking nao foi alterado nem reativado.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `TEST_REPORT.md`

### Testes manuais obrigatorios

1. Criar/usar conta A e conta B.
2. Conta A convida conta B para 1x1.
3. Conta B aceita.
4. Conta A inicia.
5. Confirmar que o jogo abre com perguntas.
6. Jogar ate ao fim e confirmar pontuacao.
7. Repetir com 2x2 usando quatro contas/jogadores.
8. Testar categoria oficial, por exemplo `Cultura Geral`.
9. Testar categoria oficial que tenha perguntas como objeto/map, por exemplo `Geografia`.
10. Testar categoria publica/personalizada se o fluxo permitir.
11. Confirmar que o erro "Erro a ir buscar perguntas" desapareceu.
12. Confirmar que matchmaking nao aparece nem foi reativado.

### Validacoes desta ronda

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.

Observacoes:

- Mantem-se o warning conhecido `SalaRepository.kt:84 Parameter 'adminHint' is never used`.
- Gradle continua a avisar sobre deprecated features para Gradle 9.0.

## Beta Prep UI Fixes — Categorias, Perfil, Badges, Main e Salas - 2026-05-18

### Bugs encontrados

- Adicionar/editar perguntas personalizadas dependia de uma escrita composta no nó da categoria, o que tornava a operação frágil quando a categoria ainda não existia ou quando a pergunta já existia.
- As Firebase Rules de `categoriasPersonalizadas/{categoria}/perguntas/{perguntaId}` aceitavam criação, mas bloqueavam edição real dos campos existentes porque exigiam `newData == data` quando o campo já existia.
- O perfil principal estava a renderizar a grelha completa de conquistas, deixando a página pesada.
- Perfil público/amigo também mostrava demasiadas conquistas e não apresentava nível/XP de forma clara.
- O ecrã de convidar amigo não tinha botão voltar nem empty state útil.
- Salas 1x1/2x2 mostravam o texto/card `Partida por convite`, que não trazia valor ao jogador.
- Main tinha avatar com camada circular atrás do asset e a seta do botão `JOGAR AGORA` podia parecer desalinhada.
- Info cards/dicas usavam marcador com amarelo forte/torrado.

### Causa real do bug de adicionar pergunta

- A gravação usava `categoriaRef.updateChildren(...)` para atualizar metadados da categoria e escrever `perguntas/{perguntaKey}` no mesmo passo.
- Para edição, as rules de perguntas personalizadas bloqueavam alterações de `pergunta`, `respostaCorreta`, `opcoes` e `imagem` quando o campo já existia.
- Resultado: criação podia ficar inconsistente e edição podia falhar no Firebase, apesar de a UI parecer válida.

### Correções aplicadas

- `CategoriaRepository.guardarPerguntaPersonalizada` agora garante primeiro os metadados da categoria e depois grava diretamente `perguntas/{perguntaKey}` com `setValue`.
- A edição usa o mesmo `perguntaId`, portanto não duplica pergunta.
- As rules de perguntas personalizadas foram ajustadas minimamente para permitir edição pelo dono, mantendo limites de tamanho e campos fechados.
- Validações leves mantidas: campos vazios, opções duplicadas ignorando caixa, resposta correta fora das opções e limites de tamanho.
- `activity_adicionar_pergunta.xml` foi aliviado visualmente com card de formulário, pergunta multiline e dificuldade em bloco mais limpo.

### Perfil e conquistas

- Perfil principal passa a mostrar só melhores conquistas por família.
- Criado `ConquistasActivity` com grelha completa, scroll e famílias RC/PJ/VT/XP/CR.
- `BadgesService` passou a calcular XP e CR além de RC/PJ/VT.
- `BadgeGridRenderer` ganhou `renderMelhores(...)` para perfil resumido e mantém `render(...)` para grelha completa.
- Criado `EditarPerfilActivity` para edição segura de avatar; nome/email/password não foram alterados nesta fase.

### Amigos/badges

- Perfil público de amigo passa a mostrar nível, XP e melhores conquistas, não grelha completa.
- Botão `Ver conquistas` abre as conquistas públicas calculadas pelas estatísticas do amigo.
- Fallback de badges continua centralizado por `BadgeGridRenderer` e `badge_default`/`badge_locked`.

### Salas e convites

- Salas 1x1 e 2x2 escondem o card/código quando a sala é privada por convite.
- Sala de grupo/código mantém informação útil para copiar/partilhar código.
- `ConvidarAmigo1x1Activity` e `ConvidarAmigo2x2Activity` receberam botão voltar.
- Ecrãs de convite mostram empty state quando não há amigos disponíveis.

### Main/avatar e dicas

- Main removeu o círculo/fundo atrás do avatar.
- A seta do botão `JOGAR AGORA` foi trocada por seta alinhada num content box fixo.
- Removidos fundos circulares em avatar selecionado/registo/dialog e no adapter de avatares.
- `UteisDicas` passou para visual mais discreto: fundo claro, título azul escuro, marcador azul escuro com contorno dourado subtil.

### Estado das avaliações

- O fluxo de avaliação foi revisto e mantido ativo.
- `ExplorarCategoriasViewModel` já bloqueia avaliação sem login e `CategoriaRepository.avaliarCategoria` usa transação para impedir avaliação duplicada por UID/chave compatível.
- Não foi criado sistema novo de avaliação.
- Teste manual obrigatório: avaliar categoria pública com conta autenticada, tentar avaliar de novo e confirmar mensagem de já avaliada.

### Firebase Rules

- Rules foram alteradas nesta fase por bug real:
  - perguntas personalizadas agora podem ser editadas pelo dono mantendo validações de tipo/tamanho;
  - `conquistas/{uid}/{badgeId}/familia` aceita também `XP` e `CR`.
- `python3 -m json.tool firebase-rules.json` deve ser executado nesta ronda.

### Ficheiros alterados nesta ronda

- Categorias/perguntas: `AdicionarPerguntaActivity.kt`, `EditarCategoriaViewModel.kt`, `CategoriaRepository.kt`, `activity_adicionar_pergunta.xml`, `firebase-rules.json`.
- Perfil/badges: `MeuPerfilActivity.kt`, `PerfilAmigoActivity.kt`, `MeuPerfilViewModel.kt`, `PerfilAmigoViewModel.kt`, `BadgesService.kt`, `BadgeGridRenderer.kt`, `Badge.kt`, `BadgeFamily.kt`, `activity_meu_perfil.xml`, `activity_perfil_amigo.xml`, `activity_conquistas.xml`, `ConquistasActivity.kt`.
- Editar perfil: `EditarPerfilActivity.kt`, `activity_editar_perfil.xml`, `JogadorRepository.kt`, `AvatarGridAdapter.kt`.
- Amigos/convites: `ConvidarAmigo1x1Activity.kt`, `ConvidarAmigo2x2Activity.kt`, `activity_convidar_amigo.xml`, `activity_convidar_amigo2x2.xml`.
- Salas/Main/dicas: `SalaDeEspera1x1Activity.kt`, `SalaDeEspera2x2Activity.kt`, `activity_main.xml`, `activity_registar.xml`, `dialog_selecionar_avatar.xml`, `UteisDicas.kt`, `bg_dica_card.xml`, `colors.xml`.
- Manifest/strings/testes/docs: `AndroidManifest.xml`, `strings.xml`, `BadgesServiceTest.kt`, `TEST_REPORT.md`, `ARCHITECTURE_PLAN.md`, `FIREBASE_RULES_NOTES.md`.

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK durante a implementação.
- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK após adicionar traduções das novas strings.
- `git status`: executado no fim da ronda.

Observações:

- A primeira execução de `build` falhou por `MissingTranslation` nas novas strings de perfil/conquistas; foram adicionadas traduções em EN/ES/FR/DE e o `build` passou.
- Gradle mantém o aviso conhecido de funcionalidades deprecated para Gradle 9.0.

### Testes manuais recomendados

1. Criar categoria personalizada nova.
2. Adicionar pergunta válida e confirmar que aparece na lista.
3. Editar a mesma pergunta e confirmar que não duplica.
4. Eliminar pergunta e confirmar que desaparece.
5. Começar jogo com categoria personalizada recém-criada.
6. Tentar guardar pergunta vazia, opções vazias, duplicadas e resposta sem opção selecionada.
7. Publicar categoria válida e abrir categoria pública.
8. Avaliar categoria pública uma vez e tentar avaliar de novo.
9. Abrir perfil próprio e confirmar só melhores badges.
10. Abrir `Ver conquistas` e confirmar grelha completa RC/PJ/VT/XP/CR.
11. Abrir `Editar perfil`, mudar avatar, guardar e voltar ao perfil.
12. Abrir perfil de amigo e confirmar melhores badges/imagens.
13. Abrir conquistas de amigo.
14. Abrir convidar amigo 1x1/2x2, testar voltar e empty state.
15. Criar convite 1x1 e 2x2.
16. Confirmar sala 1x1/2x2 sem `Partida por convite`.
17. Confirmar sala de grupo/código mantém código.
18. Abrir Main em ecrã pequeno/grande e confirmar avatar limpo e seta alinhada.
19. Abrir dicas de modo/categoria e confirmar novo visual.

### Riscos pendentes

- Editar perfil nesta fase só altera avatar; mudar nome/email/password fica pendente por risco de compatibilidade UID-first/social/legado.
- Avaliação continua client-side; robustez forte exigiria backend/Cloud Functions.
- XP/CR em badges são calculados a partir dos dados locais disponíveis (`xpTotal` e pontuação/créditos competitivos), não de um sistema económico novo.
- Segurança profunda de pontuação/XP/ranking/histórico continua pendente para v2.0/backend.

## Auditoria ficheiro-a-ficheiro + Robustez + Anti-abuso - 2026-05-18

### Áreas auditadas

- Config/constants: `FirebasePaths.kt`, `IntentExtras.kt`, `GameConstants.kt`.
- Auth/sessão: `LoginActivity.kt`, `LoginViewModel.kt`, `RegistarActivity.kt`, `RegistarViewModel.kt`, `AuthService.kt`, `JogadorRepository.kt`.
- Main/perfil/badges: `MainActivity.kt`, `MainViewModel.kt`, `MeuPerfilActivity.kt`, `MeuPerfilViewModel.kt`, `PerfilAmigoActivity.kt`, `PerfilAmigoViewModel.kt`, `ConquistasActivity.kt`, `EditarPerfilActivity.kt`, `BadgesService.kt`, `BadgesRepository.kt`, `AvatarUtils.kt`, `BadgeGridRenderer.kt`.
- Pontuação/histórico: `PontuacaoRepository.kt`, `HistoricoRepository.kt`, `Pontuacao1x1ViewModel.kt`, `Pontuacao2x2ViewModel.kt`, `PontuacoesViewModel.kt`, `ProgressaoService.kt`, `EstatisticasService.kt`, `ScoreService.kt`, `ScoreCompetitivoService.kt`.
- Jogo/modos: `JogoActivity.kt`, `JogoViewModel.kt`, `Jogo1x1Activity.kt`, `Jogo1x1ViewModel.kt`, `Jogo2x2Activity.kt`, `Jogo2x2ViewModel.kt`, `JogoRepository.kt`, `JogoCompetitivoRepository.kt`.
- Categorias/social/salas/matchmaking: `CategoriaRepository.kt`, `AdicionarPerguntaActivity.kt`, `ExplorarCategoriasActivity.kt`, `ExplorarCategoriasViewModel.kt`, `AmigosRepository.kt`, `AmigosViewModel.kt`, `SalaRepository.kt`, `SalaGrupoViewModel.kt`, `Sala1x1ViewModel.kt`, `Sala2x2ViewModel.kt`, `MatchmakingRepository.kt`, `MatchmakingViewModel.kt`.
- Firebase/documentação: `firebase-rules.json`, `FIREBASE_RULES_NOTES.md`, `ARCHITECTURE_PLAN.md`, `README.md`.

### Correções aplicadas

- Badges XP: `BadgesService.kt` agora usa marcos que existem como drawables reais (`xp100`, `xp500`, `xp1000`, `xp2500`, `xp5000`, `xp10000`, `xp25000`, `xp50000`, `xp100000`, `xp250000`, `xp500000`, `xp1000000`).
- Testes de badges atualizados para 48 badges totais e 12 badges XP.
- `AvatarUtils.kt` ganhou `indicePorNomeAvatar` para resolver nomes guardados no Firebase para índice da grelha.
- `EditarPerfilActivity.kt` carrega o avatar atual antes de guardar, evitando sobrescrever acidentalmente com o avatar 1.
- `LoginActivity.kt` bloqueia botões de entrada/guest/registo durante tentativa.
- `RegistarActivity.kt` bloqueia o botão de registo durante tentativa.
- Criado `AvatarUtilsTest.kt` para validar parsing dos nomes de avatar.

### Bugs/riscos encontrados

- Bug real corrigido: badges XP apontavam para IDs sem asset correspondente, causando fallback/ícone errado.
- Bug real corrigido: editar perfil abria sempre com seleção visual no avatar 1.
- Risco corrigido: duplo clique em login/registo.
- Risco pendente: rating continua client-side e dependente de Auth UID-first; contas legado sem Firebase Auth podem falhar ao avaliar.
- Risco pendente: pontuação/XP/ranking/histórico/conquistas continuam client-authoritative.
- Risco pendente: pódios 1x1/2x2 podem aguardar indefinidamente se jogador abandonar antes de gravar resultado.

### Relatório criado

- `FILE_BY_FILE_AUDIT_REPORT.md` criado com resumo executivo, tabela de ficheiros analisados, bugs corrigidos, riscos pendentes, segurança/anti-abuso, multiplayer/race conditions, Firebase/rules, checklist manual e conclusão.

### Comandos executados

- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git status`: executado no fim da ronda.

Observação: Gradle continua a emitir aviso de deprecated features para Gradle 9.0; não bloqueia beta fechada.

### Testes manuais recomendados adicionais

1. Login com duplo clique em `Entrar`.
2. Entrada como convidado com duplo clique em `Jogar sem conta`.
3. Registo novo com duplo clique em `Registar`.
4. Editar perfil e confirmar que a grelha abre no avatar atual.
5. Guardar outro avatar e confirmar persistência ao voltar ao perfil/Main.
6. Abrir conquistas e verificar imagens XP 100/500/1000/2500/5000+.
7. Avaliar categoria pública com conta Firebase e tentar segunda avaliação.
8. Testar pódio 1x1/2x2 com um jogador a fechar a app antes de terminar.
9. Matchmaking 1x1/2x2 com cancelamento em background.
10. Sala grupo com um jogador real: decidir se este comportamento é aceitável para beta.

## Solo vs Grupo — mínimo 2 jogadores em sala grupo - 2026-05-18

### Causa da alteração

- A sala de grupo ainda tinha `MINIMO_JOGADORES_GRUPO = 1` em `SalaGrupoViewModel`.
- A dica do modo clássico também comunicava que o admin podia iniciar quando houvesse pelo menos 1 jogador na sala.
- Isto deixava o fluxo de Grupo demasiado próximo de Solo e permitia uma sala de grupo arrancar sem o mínimo de 2 participantes presentes.

### Decisão de produto

- Solo continua a ser o fluxo individual.
- Grupo/Sala é fluxo multiplayer por código e exige pelo menos 2 participantes presentes.
- 1x1, 2x2 e matchmaking mantêm as regras próprias e não foram alterados.

### Correções aplicadas

- `SalaGrupoViewModel` passou a exigir 2 participantes ativos antes de permitir `salas/{codigo}/estado = em_jogo`.
- A validação é feita em tempo real para ativar/desativar o botão e novamente antes de iniciar, lendo os jogadores atuais no Firebase.
- Jogadores com `estado = off` e placeholder legado `admin/admin` não contam para o mínimo.
- `SalaDeEsperaGrupoActivity` passou a mostrar contador mínimo, estado de espera e botão `Iniciar` visualmente desativado enquanto faltar jogador.
- A mensagem de erro passou a dizer claramente que são necessários pelo menos 2 jogadores para iniciar uma sala de grupo.
- A dica do modo Grupo foi ajustada para comunicar sala com código e mínimo de 2 jogadores presentes.

### Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/viewmodels/SalaGrupoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en-rGB/strings.xml`
- `app/src/main/res/values-es/strings.xml`
- `app/src/main/res/values-fr/strings.xml`
- `app/src/main/res/values-de-rDE/strings.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git status`: executado no fim da ronda.

### Testes manuais recomendados

1. Criar sala grupo como host e confirmar que sozinho não consegue iniciar.
2. Confirmar contador `1/2 jogadores mínimos` e mensagem de espera.
3. Entrar com segundo participante e confirmar botão `Iniciar` ativo.
4. Sair com o segundo participante antes de iniciar e confirmar botão volta a ficar desativado.
5. Testar duplo clique no botão `Iniciar`.
6. Repetir em clássico, caótico e eliminatórias.
7. Jogar Solo sozinho e confirmar que continua independente da sala grupo.
8. Confirmar 1x1, 2x2 e matchmaking sem alterações de fluxo.

### Riscos pendentes

- A validação continua client-side/Firebase Realtime Database; proteção autoritativa forte fica pendente para backend/Cloud Functions.
- Salas antigas com dados legados podem ter representações diferentes de admin, mas a validação ignora apenas o placeholder literal `admin/admin` e conta participantes ativos reais.

## Pré-beta — Modo Solo, Eliminatórias, Categorias não competitivas e Histórico 3 dias - 2026-05-18

### Decisões de produto

- Solo é fluxo individual: não cria sala, não mostra código e não espera jogadores.
- Grupo/Sala continua multiplayer por código e mantém mínimo de 2 participantes.
- 1x1, convites 1x1/2x2 e matchmaking não foram movidos nem redesenhados nesta fase.
- Categorias oficiais são competitivas.
- Categorias personalizadas e categorias públicas criadas por jogadores são jogáveis, mas não contam para ranking, recordes ou vitórias competitivas.
- Histórico autenticado fica retido por 3 dias via limpeza client-side/repository enquanto não houver Cloud Functions/TTL.

### Regras implementadas

- `TipoModoClassico` ganhou opção explícita `Solo` para clássico.
- `EscolherModoActivity` passou a perguntar Solo ou Grupo para caótico e eliminatórias.
- `EscolherCategoriaActivity` abre `JogoActivity` em modo local quando recebe `MODO_SOLO=true`.
- `ExplorarCategoriasActivity` permite jogar categorias públicas/personalizadas em Solo clássico, Solo caótico e Solo eliminatórias.
- `JogoViewModel` suporta modo Solo local, carrega perguntas por categoria sem `salas/{codigo}` e navega para pontuação com `partidaId`.
- Solo clássico/caótico limita a partida a 8 perguntas; Solo eliminatórias usa todas as perguntas válidas disponíveis.
- Eliminatórias grupo deixam de truncar perguntas para 8 no carregamento da sala e no `JogoRepository`.
- Eliminatórias terminam por erro, por fim das perguntas disponíveis ou pelas regras de fim da sala.
- `EsperaEliminadoActivity` mostra estado/ranking parcial dos jogadores enquanto aguarda o pódio final.
- `PontuacaoRepository` bloqueia atualizações competitivas quando a sala tem `categoriaPublica`, `categoriaPublicaId`, `categoriaPersonalizada` ou `donoUid`.
- `PontuacoesViewModel` grava histórico de categorias não competitivas, mas não atualiza pontuação global/recorde/vitórias.
- `HistoricoRepository` remove entradas do próprio UID com mais de 3 dias ao carregar histórico ou gravar novo histórico.
- Entradas sem `dataHora` válida não são apagadas automaticamente.

### Ficheiros alterados nesta fase

- `app/src/main/java/com/example/brainbrawl/EscolherModoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/JogoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/EsperaEliminadoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/EsperaEliminadoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/PontuacoesViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/HistoricoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/HistoricoAdapter.kt`
- `app/src/main/java/com/example/brainbrawl/UteisSala.kt`
- `app/src/main/java/com/example/brainbrawl/config/GameConstants.kt`
- `app/src/main/java/com/example/brainbrawl/config/IntentExtras.kt`
- `app/src/main/java/com/example/brainbrawl/config/FirebasePaths.kt`
- `app/src/main/res/layout/activity_tipo_modo_classico.xml`
- `app/src/main/res/layout/activity_espera_eliminado.xml`
- `app/src/main/res/values*/strings.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Testes feitos

- `git diff --check`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git status`: executado no fim da ronda.

Observação: Gradle continua a emitir aviso de deprecated features para Gradle 9.0; não bloqueia esta fase.

### Testes manuais recomendados

1. Jogar Solo clássico com categoria oficial e confirmar que não há sala/código.
2. Jogar Solo caótico com categoria oficial.
3. Jogar Solo eliminatórias, acertar várias e confirmar que só termina ao erro ou fim das perguntas.
4. Jogar categoria personalizada em Solo e confirmar histórico marcado como não competitivo.
5. Jogar categoria pública em Solo e confirmar que ranking/recorde/vitórias não mudam.
6. Criar sala grupo eliminatórias com 2+ jogadores e confirmar que há mais de 8 perguntas quando disponíveis.
7. Errar em eliminatórias grupo e confirmar estado/ranking parcial no ecrã de eliminado.
8. Confirmar pódio final normal após fim da sala.
9. Abrir Histórico com entradas antigas de teste e confirmar remoção apenas do próprio UID.
10. Confirmar regressão: 1x1 por convite, 2x2 por convite e matchmaking continuam nos mesmos sítios.

### Riscos pendentes

- A distinção competitiva ainda é client-side; anti-farm robusto deve ir para Cloud Functions.
- A limpeza de histórico por 3 dias é oportunista/client-side; solução futura ideal é TTL/Cloud Functions.
- Solo usa a UI de jogo existente; uma UI/pódio solo dedicado pode melhorar clareza visual numa versão futura.

## Ajuste final pré-beta — fluxo de modos, matchmaking ready-state, sala grupo e histórico visual - 2026-05-18

### Inconsistência encontrada

- `Clássico` abria o ecrã de escolha de tipo, mas `Caótico` e `Eliminatórias` usavam um fluxo separado/dialog ou entrada direta, criando uma experiência inconsistente.
- No ecrã de tipo, `Solo` parecia selecionado por defeito por usar botão preenchido, enquanto 1x1/2x2/Grupo usavam estilo secundário.
- O botão `Voltar` em escolha de modo/tipo usava o drawable castanho `bg_button_danger`, pouco coerente com o resto da UI.
- Salas competitivas fechadas reaproveitavam jogadores já criados sem refrescar presença, permitindo que um jogador que saísse continuasse visível para o outro.

### Solução aplicada

- `Clássico`, `Caótico` e `Eliminatórias` passam a abrir sempre o mesmo ecrã de escolha de tipo.
- Para `Caótico` e `Eliminatórias`, o ecrã mostra `Solo` e `Todos/Grupo`; 1x1/2x2 ficam escondidos para não alterar os fluxos competitivos atuais.
- `Solo` passou para o mesmo estilo secundário dos restantes botões; `Voltar` também usa botão secundário claro.
- A sala grupo removeu o texto pesado `Aguardando jogadores...` e mantém apenas contador mínimo, lista de jogadores, estado curto e botão iniciar ativo/desativo.
- Solo continua a gravar pontuação/XP/histórico conforme regra atual, mas deixou de incrementar `totalVitorias` e `totalVitoriasModoSolo`.
- O histórico passou a colorir resultado: vitória verde, derrota vermelha, empate dourado.

### Matchmaking/presença

- `JogoCompetitivoRepository` passa a marcar jogadores de salas fechadas como `estado=on` quando entram na sala de espera.
- O mesmo repositório configura `onDisconnect` para colocar `estado=off` se a ligação Firebase cair.
- `Sala1x1ViewModel` e `Sala2x2ViewModel` filtram jogadores `off` antes de calcular presença, botão iniciar e equipas.
- Os ecrãs de espera 1x1/2x2 removem o jogador quando o utilizador sai pelo botão ou pelo back.
- 1x1 continua a usar o ready-state existente em `prontos`; 2x2 ficou protegido por presença ativa/exatamente 4 jogadores. Um botão manual “Pronto” para todos fica como melhoria futura para não redesenhar o fluxo nesta fase.

### Ficheiros alterados nesta fase

- `app/src/main/java/com/example/brainbrawl/EscolherModoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/services/EstatisticasService.kt`
- `app/src/main/java/com/example/brainbrawl/HistoricoAdapter.kt`
- `app/src/main/res/layout/activity_escolher_modo.xml`
- `app/src/main/res/layout/activity_tipo_modo_classico.xml`
- `app/src/main/res/values*/strings.xml`
- `app/src/test/java/com/example/brainbrawl/ExampleUnitTest.kt`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git status`: executado no fim da ronda.

Observação: Gradle continua a emitir aviso de deprecated features para Gradle 9.0; não bloqueia esta fase.

### Testes manuais recomendados

1. Abrir `Jogar Agora`, escolher Clássico, Caótico e Eliminatórias e confirmar o mesmo ecrã de tipo.
2. Confirmar que `Solo` e `Voltar` usam o mesmo estilo dos botões secundários.
3. Confirmar que Caótico/Eliminatórias mostram Solo e Grupo sem dialog antigo.
4. Criar sala grupo e confirmar código visível, contador mínimo e mensagem curta sem `Aguardando jogadores...`.
5. Jogar Solo e confirmar que pontuação/histórico continuam, mas vitórias competitivas não aumentam.
6. Matchmaking 1x1: A e B chegam à sala, B sai pelo back, A deve ver B desaparecer e não conseguir iniciar.
7. Repetir em 2x2 quando houver 4 testers/dispositivos.
8. Confirmar histórico com vitória, derrota e empate.

### Riscos pendentes

- `onDisconnect` é best-effort client-side; limpeza totalmente autoritativa de salas fantasma continua a pertencer a backend/Cloud Functions.
- 2x2 ainda não tem botão manual “Pronto” por jogador; nesta ronda ficou protegido por presença ativa e contagem exata.
- Fluxos 1x1/2x2 por convite foram preservados; validar manualmente porque partilham sala de espera com partidas vindas de matchmaking.

## Correções finais pré-beta — Matchmaking, Eliminatórias grupo, Histórico e Explorar Categorias - 2026-05-18

### Causas reais encontradas

- Matchmaking/sala cheia: salas fechadas criadas por matchmaking mantinham `jogadoresPermitidos`, mas a validação de reentrada só procurava o jogador em `jogadores`. Quando um jogador saía/removia a presença, continuava autorizado mas já não era reconhecido como selecionado; a Activity recebia falha genérica e mostrava “sala cheia”.
- Eliminatórias grupo/0 falso: os jogadores vivos só gravavam pontuação final ao terminar/morrer. Como o payload inicial do jogador já tinha `pontuacao=0` e `totalRespostasCertas=0`, o ecrã do eliminado mostrava 0 como se fosse real.
- Histórico grupo: o leitor de resultados considerava esses campos iniciais `pontuacao=0`/`totalRespostasCertas=0` como resultado final. Isso podia disparar pódio/histórico cedo e bloquear a gravação final idempotente.
- Explorar Categorias: os cards juntavam 3 botões na mesma linha e o drawable de perigo ainda tinha castanho hardcoded.

### Correções aplicadas

- `JogoCompetitivoRepository` aceita reentrada em salas fechadas quando o jogador existe em `jogadoresPermitidos`; se já não estiver em `jogadores`, recria o nó do jogador com a mesma chave autorizada e marca `estado=on`.
- O ready-state/presença mantém `onDisconnect` para `estado=off`; iniciar continua bloqueado por presença real: 2 jogadores no 1x1 e 4 no 2x2.
- `JogoViewModel` publica progresso parcial a cada resposta em `salas/{codigo}/jogadores/{chave}`: pontuação, respostas certas, perguntas respondidas e estado `em_jogo`.
- `JogoRepository.guardarResultadoJogador` marca jogadores terminados com `estado=terminado`; eliminados continuam com `estado=eliminado`.
- `PontuacaoRepository` só conta resultado de grupo como final quando o jogador está `terminado` ou `eliminado`, evitando histórico/pódio prematuro.
- `EsperaEliminadoActivity` passou de lista textual para cards de ranking parcial, com top 3 destacado, estado `Em jogo`/`Eliminado`/`Terminou` e sem 0 falso quando ainda não há progresso confirmado.
- `ExplorarCategoriasActivity` agrupa botões em linhas de no máximo 2, melhora largura do botão `Criar Categoria` e usa paleta clara/azul escura com danger vermelho.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/PontuacaoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/JogoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/EsperaEliminadoViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/EsperaEliminadoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/ExplorarCategoriasActivity.kt`
- `app/src/main/res/layout/activity_espera_eliminado.xml`
- `app/src/main/res/layout/activity_explorar_categorias.xml`
- `app/src/main/res/drawable/bg_button_danger.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values*/strings.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Comandos executados

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: falhou primeiro por `android.widget.Space` em falta; corrigido.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: falhou primeiro por tradução em falta de `aguarda_restantes_jogadores`; corrigido em EN/ES/FR/DE.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git diff --check`: OK, sem whitespace errors.
- `git status --short`: executado no fim; a working tree já tinha várias alterações pré-existentes e ficou sem commit automático.

Observação: Gradle continua a emitir aviso de deprecated features para Gradle 9.0; não bloqueia esta fase.

### Testes manuais recomendados

1. Matchmaking 1x1: A entra, B entra, ambos chegam à sala; B sai; A vê B sair e não inicia sozinho.
2. Matchmaking 1x1: repetir com A a sair e confirmar que B volta à Main/estado correto.
3. Matchmaking 2x2: quatro jogadores chegam à sala, saída de um remove presença e bloqueia iniciar.
4. Reentrar depois de sair de uma sala fechada autorizada e confirmar que não aparece “sala cheia” indevidamente.
5. Eliminatórias grupo: um jogador erra, vê ranking parcial em cards, e vivos aparecem com progresso real ou “Em jogo”.
6. Terminar eliminatórias grupo e confirmar pódio final e histórico do auth user.
7. Histórico: auth em oficial, personalizada e pública cria entrada em `historicoJogos/{uid}`; convidado não cria.
8. Confirmar que categoria personalizada/pública aparece no histórico mas não atualiza ranking/recorde/vitórias competitivas.
9. Explorar Categorias em ecrã pequeno: Criar Categoria alinhado; Jogar/Guardar/Avaliar/Editar/Eliminar/Tornar pública sem cortes.
10. Regressão rápida: login, main, solo, grupo, 1x1, 2x2, ranking, perfil, badges, amigos, pedidos e convites.

### Riscos pendentes

- Presença/offline continua best-effort do cliente via Firebase `onDisconnect`; perda de rede sem disconnect imediato ainda pode deixar estado temporário até nova ação/listener.
- Matchmaking continua client-side, sem Cloud Functions; seleção e presença estão mais defensivas, mas autoridade total exigiria backend.
- O ranking intermédio usa progresso parcial gravado pelo cliente; ainda não é prova antifraude.

## Refactor controlado do matchmaking — fila, sala, presença e pronto - 2026-05-19

### Causa real encontrada

- A sala criada pelo matchmaking copiava todos os jogadores para `sala_1x1/{codigo}/jogadores` ou `sala_2x2/{codigo}/jogadores` já com `estado=on`.
- Isto fazia a sala de espera contar jogadores como presentes antes de as respectivas Activities abrirem e confirmarem presença real.
- Em paralelo, o `onStop` de `MatchmakingActivity` ainda podia chamar limpeza de background quando a Activity estava em transição, o que misturava cancelamento com navegação.
- Como a lotação olhava para jogadores já copiados e/ou prontos fantasma, uma entrada legítima podia cair no caminho de erro genérico e a UI mostrava “sala cheia”.

### Porque as correções pequenas anteriores não resolveram

- Marcar `onDisconnect` e aceitar `jogadoresPermitidos` reduzia sintomas de reentrada, mas não separava a fase “match selecionado” da fase “jogador realmente entrou na sala”.
- O ready-state continuava implicitamente pronto/auto-presente em alguns pontos, sobretudo 1x1.
- A sala de matchmaking precisava de contrato próprio, não apenas validações laterais.

### Novo fluxo

- `matchmaking/{modo}/fila/{playerKey}` continua a ser a fila.
- Antes de escrever na fila, o ViewModel procura uma sala ativa de matchmaking onde o jogador já esteja autorizado; se existir, navega para lá em vez de duplicar fila.
- A formação do match continua transacional em `matchmaking/{modo}` e seleciona exatamente 2 jogadores para 1x1 ou 4 para 2x2.
- A sala criada por matchmaking agora nasce com:
  - `origem=matchmaking`
  - `entradaFechada=true`
  - `lotacaoMaxima=2/4`
  - `jogadoresPermitidos/{playerKey}=true`
  - `jogadores/{playerKey}/estado=off`
- `prontos/{playerKey}` é criado/atualizado por cada cliente ao entrar e ao clicar em `Pronto`, não no payload inicial da sala.
- Ao abrir a sala de espera, cada cliente marca `estado=on` para si e configura `onDisconnect` para `estado=off` e remoção de `prontos/{playerKey}`.
- Em matchmaking, todos veem botão `Pronto`; o jogo só inicia com N jogadores presentes e N prontos.
- Em convite 1x1/2x2, o fluxo antigo foi preservado: a sala continua a usar presença/ready automático e o admin inicia.

### Cancelamento e saída

- Cancelar na fila remove a entrada e volta à Main.
- `MatchmakingActivity.onStop` deixou de limpar a fila agressivamente; saída real usa botão Cancelar/back, e navegação para sala não é tratada como cancelamento.
- Se alguém sai de uma sala de matchmaking antes do jogo começar, a sala é apagada e os restantes recebem encerramento/aviso.
- Não se tenta preencher a mesma sala com novo jogador nesta fase.

### Como evita “sala cheia” indevida

- Jogador já autorizado por `jogadoresPermitidos` pode entrar/reentrar.
- Jogadores `off`, placeholders e duplicados não contam como presença real.
- A sala só fica completa quando cada jogador abre a sala de espera e marca `estado=on`.
- `prontos` só conta quando o valor é `true` e pertence a um jogador presente.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/MatchmakingViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/MatchmakingActivity.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala1x1ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/viewmodels/Sala2x2ViewModel.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/res/values*/strings.xml`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`

### Testes feitos

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK durante validação intermédia.

### Testes manuais obrigatórios

1. 1x1: A entra, B entra, ambos chegam à sala, ambos clicam `Pronto`, jogo inicia.
2. 1x1: B sai antes de pronto; A vê saída/encerramento e não aparece “sala cheia”.
3. 1x1: cancelar e reentrar sem ficar preso em sala antiga.
4. 2x2: quatro contas entram, todos clicam `Pronto`, jogo inicia.
5. 2x2: um sai antes do jogo; restantes veem encerramento e jogo não inicia.
6. Confirmar 1x1 por convite.
7. Confirmar 2x2 por convite.
8. Confirmar sala grupo.
9. Confirmar que matchmaking continua acessível pela Main.

### Riscos pendentes

- A formação do match continua cliente-side; Cloud Functions seriam o caminho autoritativo para produção pública.
- `onDisconnect` continua best-effort em perda de rede abrupta.
- 2x2 precisa de validação manual real com 4 dispositivos/contas para confirmar timings de presença/pronto em rede real.

## Debug real do matchmaking com contas de teste - 2026-05-19

### Contas usadas

- Foram usados emuladores autenticados como `jogador3` e `jogador4` para o 1x1.
- Para 2x2 foram usados quatro emuladores autenticados como `jogador1`, `jogador2`, `jogador3` e `jogador4`.
- Nenhuma password foi guardada em código, documentação ou comandos persistidos.

### Cenário reproduzido

- 1x1 com `jogador3` + `jogador4` reproduziu o bug original: a UI ficou em `Erro ao criar sala` e `Jogadores à procura: 0/2`.
- Logcat mostrou `Permission denied` na transação de criação de `/sala_1x1/{codigo}`.
- Depois da primeira correção, a sala já era criada, mas a entrada na sala de espera falhava com `Permission denied` em `jogadores/{uid}` ao marcar presença.

### Causa real

- O payload inicial da sala de matchmaking escrevia campos no nó raiz da sala que as rules atuais bloqueiam: `codigoSala`, `modo` e `dataCriacao`.
- O payload inicial também tentava criar `prontos` dentro da sala; isso é seguro no 1x1 quando escrito pelo jogador, mas não estava permitido no payload raiz e nem existia nas rules do 2x2.
- A presença escrevia `timestampEntrada` dentro de `sala_1x1/sala_2x2/{codigo}/jogadores/{uid}`, mas esse campo não é permitido pelas rules dos jogadores de sala.
- A transação ampla em `matchmaking/{modo}` podia ficar presa sem `onComplete` neste cenário multi-cliente; o claim no nó inteiro era demasiado grande para o fluxo real.

### Correção aplicada

- `MatchmakingRepository.tentarCriarMatch()` passou a ler a fila e fazer claim transacional apenas em `matchmaking/{modo}/matches/{matchId}`.
- O `matchId` é determinístico a partir dos jogadores selecionados, por isso dois clientes que veem o mesmo grupo disputam o mesmo claim e só um cria a sala.
- A sala de matchmaking deixou de escrever `codigoSala`, `modo`, `dataCriacao` e `prontos` no payload inicial.
- A presença em sala de espera passou a atualizar apenas `estado=on`; deixou de escrever `timestampEntrada` em `jogadores/{uid}`.
- `firebase-rules.json` local ganhou `sala_2x2/{codigo}/prontos/{jogadorId}` com a mesma regra do 1x1, necessária para o pronto manual no 2x2.

### Testes manuais feitos

- 1x1 com `jogador3` + `jogador4`: sala criada, ambos navegaram para `SalaDeEspera1x1Activity`, ambos apareceram como presentes, ambos clicaram `Pronto`, jogo iniciou em `Jogo1x1Activity`.
- 1x1: não voltou a aparecer `Erro ao criar sala`, `sala cheia` ou `Jogadores à procura: 0/2` após a correção.
- 2x2 com quatro emuladores: sala criada com quatro jogadores e todos navegaram para `SalaDeEspera2x2Activity`.
- 2x2: pronto ainda falha no Firebase remoto atual com `Permission denied` em `sala_2x2/{codigo}/prontos/{uid}` até as rules locais corrigidas serem publicadas.

### Ficheiros alterados nesta ronda

- `app/src/main/java/com/example/brainbrawl/repositories/MatchmakingRepository.kt`
- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`
- `firebase-rules.json`
- `TEST_REPORT.md`
- `ARCHITECTURE_PLAN.md`
- `FIREBASE_RULES_NOTES.md`

### Comandos executados

- `adb devices`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` nos emuladores de teste.
- `adb shell monkey -p com.example.brainbrawl 1`
- `adb shell input tap ...`
- `adb logcat -d -v time`
- `curl` read-only aos nós de matchmaking para confirmar limpeza/estado.
- `python3 -m json.tool firebase-rules.json`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`
- `git diff --check`
- `git status --short`

### Riscos pendentes

- A alteração em `firebase-rules.json` precisa ser publicada no Firebase antes de o `Pronto` do 2x2 funcionar contra a base remota.
- O matchmaking continua client-side; Cloud Functions continuam recomendadas para arbitragem autoritativa futura.

## Correção 2x2 matchmaking - erro ao iniciar após quatro prontos - 2026-05-19

### Cenário reproduzido

- Quatro emuladores entraram no matchmaking 2x2 com contas autenticadas.
- Os quatro jogadores chegaram à `SalaDeEspera2x2Activity`.
- Depois dos quatro `Pronto`, o admin podia cair em `Erro ao iniciar jogo` durante a tentativa de gravar equipas/iniciar sala.

### Causa real

- `guardarEquipas2x2()` escrevia `equipaA/{uid}` e `equipaB/{uid}` usando `JogadorCompetitivo.toFirebaseMap()`.
- Esse mapa passou a incluir `estado`, que é válido em `jogadores/{uid}`, mas não é permitido pelas rules de `equipaA/equipaB`.
- Resultado: o write de equipas podia ser rejeitado por Firebase Rules e o admin via erro ao iniciar.

### Correção aplicada

- `JogadorCompetitivo.toFirebaseMap()` deixou de incluir `estado` quando usado para gravar `equipaA/equipaB`.
- Presença continua a viver em `jogadores/{uid}/estado`; equipas guardam apenas identidade do jogador.
- Não foi necessária nova alteração de rules para este erro específico.

### Teste real feito

- Reinstalada a build debug nos quatro emuladores.
- Quatro jogadores entraram no 2x2, apareceram na sala de espera, clicaram `Pronto` e navegaram para `Jogo2x2Activity`.
- Não apareceu `Erro ao iniciar jogo` após a correção.

### Ficheiro alterado nesta correção

- `app/src/main/java/com/example/brainbrawl/repositories/JogoCompetitivoRepository.kt`

### Rules

- Não foi necessária nova alteração de Firebase Rules para o erro de iniciar após quatro prontos.
- Mantém-se a alteração local anterior em `firebase-rules.json` para permitir `sala_2x2/{codigo}/prontos/{jogadorId}`, necessária ao botão `Pronto` manual do matchmaking 2x2 caso ainda não esteja publicada no Firebase remoto.

### Comandos executados

- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git diff --check`: OK.
- `git status --short`: executado no fim.

## Correção crítica — resultados grupo Clássico/Caótico/Eliminatórias, pódio 0/3 e Firebase Rules - 2026-05-19

### Confirmação do bug

- O bug foi reportado em Grupo Clássico e Grupo Caótico: o pódio ficava em `A aguardar resultados... 0/3`.
- A análise de código mostrou que Grupo Eliminatórias também usava o mesmo path de resultado visual e podia ficar afetado quando o resultado final/progresso era rejeitado pelas rules.
- Não foram feitos testes reais multi-dispositivo nesta ronda sem credenciais/passwords de teste disponíveis na automação; os cenários reais abaixo ficam pendentes para execução manual após publicar as rules.

### Causa real

- O jogo grava o resultado visual de grupo em `salas/{codigo}/jogadores/{playerKeyOuUid}`.
- O pódio de grupo lê o mesmo path: `salas/{codigo}/jogadores`.
- O write de resposta/finalização escreve `pontuacao`, `totalRespostasCertas`, `totalPerguntas` e `estado`.
- `firebase-rules.json` bloqueava esse payload em `salas/{codigo}/jogadores/{jogadorId}` porque:
  - `estado` só aceitava `on`, `off` e `eliminado`, mas o jogo escreve `em_jogo` durante a partida e `terminado` no fim;
  - `totalPerguntas` não estava permitido nesse nó.
- Como o `updateChildren` era rejeitado por Firebase Rules, nenhum jogador conseguia gravar o resultado visual final; o listener do pódio via 0 resultados guardados.

### Correção aplicada

- `firebase-rules.json` passou a aceitar `estado=em_jogo` e `estado=terminado` em jogadores de `salas/{codigo}`.
- `firebase-rules.json` passou a validar `totalPerguntas` em jogadores de `salas/{codigo}`.
- `PontuacaoRepository` mantém a separação entre resultado visual e estatística persistente:
  - resultado visual temporário continua em `salas/{codigo}/jogadores`;
  - ranking/recordes/vitórias/XP continuam bloqueados para categorias públicas/personalizadas.
- A contagem esperada do pódio de grupo deixou de contar jogadores com `estado=off`.
- O pódio/espera de eliminatórias deixam de esconder o host real por `isHostOnly`; apenas o placeholder `admin` é ignorado.
- Novas salas caóticas deixam de criar o host real com `isHostOnly=true`.
- `SalaRepository.garantirJogadorNaSala()` deixa de marcar o criador real como host-only invisível ao pódio.

### Firebase Rules

- Rules verificadas para `salas/{codigo}/jogadores`, `perguntaAtual/respostas`, `estatisticasAtualizadas`, `perguntas` e `estado`.
- O bloqueio confirmado estava em `salas/{codigo}/jogadores/{jogadorId}/estado` e ausência de `totalPerguntas`.
- Alteração mínima e segura: só foram adicionados valores/campo que o cliente já escrevia no contexto da própria sala.
- `python3 -m json.tool firebase-rules.json`: OK.
- Não foi usado `childrenCount`.

### Fundos corrigidos

- `activity_jogo.xml`
- `activity_jogo1x1.xml`
- `activity_jogo2x2.xml`
- `activity_ranking.xml`
- `activity_perfil_amigo.xml`
- Já estavam alinhados com `bg_main_premium` nos ecrãs prioritários: escolher categoria, escolher modo/tipo, explorar categorias, sala de espera, pódio, histórico, meu perfil e matchmaking.

### Testes técnicos/comandos

- `python3 -m json.tool firebase-rules.json`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: OK.
- `git diff --check`: OK.
- `adb devices`: não executável nesta shell porque `adb` não está no PATH.
- `git status --short`: executado no fim.

### Testes reais pendentes

- Grupo Clássico com 3 jogadores: confirmar pódio `3/3` e todos com pontuação.
- Grupo Caótico com 3 jogadores: confirmar pódio `3/3` e todos com pontuação.
- Grupo Eliminatórias com 3 jogadores: confirmar eliminados e sobrevivente no pódio `3/3`.
- Auth user: confirmar histórico recente guardado.
- Convidado: confirmar pódio visual sem histórico persistente.
- Categoria pública/personalizada: confirmar pódio visual e ausência de ranking/recorde/vitórias competitivas.
- Regressão: matchmaking 1x1, matchmaking 2x2, convites 1x1/2x2 e sala grupo mínimo 2.

### Riscos pendentes

- As Firebase Rules corrigidas precisam ser publicadas no projeto Firebase antes de o bug deixar de acontecer na base remota.
- O admin/host real fica visível no pódio; no fluxo atual de grupo, o ecrã do admin continua a controlar o relógio/perguntas e não responde como jogador normal.
- A validação autoritativa de resultados continua client-side; Cloud Functions/backend ficam para fase futura.

## QA total jogável — pódio grupo, voltar e regressão pré-beta - 2026-05-19

### Contas e dispositivos

- Teste real executado em 4 emuladores Android (`emulator-5554`, `emulator-5556`, `emulator-5558`, `emulator-5560`).
- Contas usadas sem documentar passwords: `jogador1@gmail.com`, `jogador3@gmail.com` e `jogador4@gmail.com`.
- O emulador preparado para o segundo jogador estava autenticado como `jogador4`; não foi confirmada existência de emails com typo `ogador2@gmail.com`/`ogador4@gmail.com` nesta ronda.

### Testes reais executados

- Instalação do APK debug nos quatro emuladores: OK.
- Grupo Clássico com 3 contas reais:
  - sala criada por `jogador1`;
  - `jogador3` e `jogador4` entraram na sala;
  - jogo iniciado e terminado nos três clientes;
  - `PontuacoesActivity` abriu e permaneceu aberta nos três emuladores;
  - pódio mostrou 3/3 jogadores com pontuação (`jogador4`, `jogador3`, `jogador1`).
- Histórico: aberto em emulador autenticado; o botão `Voltar` estava visível e regressou à Main.
- Perfil: aberto em emulador autenticado; o botão `Voltar` estava visível e regressou à Main.
- Solo Clássico: navegação real até `JogoActivity`; o botão Back do Android já não sai silenciosamente da partida e mantém o jogo aberto.

### Pódio grupo

- O bug atual descrito como "pódio entra e sai logo" não foi reproduzido no teste real de Grupo Clássico desta build: o pódio abriu e ficou estável.
- O bug anterior de `A aguardar resultados... 0/3` continua explicado pela secção anterior: as rules locais bloqueavam os writes reais de `estado=em_jogo/terminado` e `totalPerguntas` em `salas/{codigo}/jogadores/{jogadorId}`.
- Durante o teste real, os logs revelaram dois bloqueios adicionais de Firebase remoto:
  - o admin não conseguia remover o nó inteiro `salas/{codigo}/perguntaAtual/respostas` ao avançar pergunta;
  - o histórico do auth user era rejeitado porque `historicoJogos/{uid}/{historicoId}` recebia o campo `competitivo`, mas as rules não o validavam.
- Não foi confirmado nesta ronda que Caótico e Eliminatórias reproduzem o "entra e sai logo"; ficam como teste manual obrigatório após publicar as rules locais.

### Correções aplicadas

- `firebase-rules.json` passou a permitir ao admin limpar/remover `salas/{codigo}/perguntaAtual/respostas` como nó inteiro, mantendo a validação booleana por resposta individual.
- `firebase-rules.json` passou a aceitar `competitivo` booleano em `historicoJogos/{uid}/{historicoId}`.
- `JogoActivity`, `Jogo1x1Activity` e `Jogo2x2Activity` passaram a bloquear o Back físico durante a partida com mensagem clara: `Termina a partida para voltar ao menu.`
- `HistoricoActivity`, `MeuPerfilActivity` e `AmigosActivity` deixaram de esconder os botões de voltar que os layouts já tinham.

### Firebase Rules

- Rules verificadas/alteradas localmente para:
  - `salas/{codigo}/jogadores/{jogadorId}`;
  - `salas/{codigo}/perguntaAtual/respostas`;
  - `historicoJogos/{uid}/{historicoId}`;
  - paths de pontuação/resultados usados pelo pódio de grupo.
- As alterações são locais no ficheiro `firebase-rules.json`; precisam ser publicadas no Firebase para remover os `Permission denied` no ambiente real.
- Não foi usado `childrenCount`.

### Fundos

- Reconfirmado por inspeção de layout que os ecrãs prioritários desta fase já usam a base visual `bg_main_premium` ou tinham sido alinhados na fase anterior: jogo, pódio, escolher categoria, sala de espera, explorar categorias, histórico, perfil, ranking e matchmaking.
- Não foi feito redesign novo.

### Testes pendentes/riscos

- Grupo Caótico com 3 jogadores reais: pendente.
- Grupo Eliminatórias com 3 jogadores reais: pendente.
- Matchmaking 1x1/2x2 e convites 1x1/2x2: pendentes nesta ronda após a correção de rules/histórico.
- Histórico em Firebase remoto só fica validável depois de publicar as rules locais.
- A app continua client-authoritative para resultados; sem Cloud Functions/backend por decisão desta fase.

### Comandos executados nesta ronda

- `$HOME/Library/Android/sdk/platform-tools/adb devices`
- `$HOME/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk` nos emuladores disponíveis
- `$HOME/Library/Android/sdk/platform-tools/adb shell monkey -p com.example.brainbrawl -c android.intent.category.LAUNCHER 1`
- `$HOME/Library/Android/sdk/platform-tools/adb shell dumpsys activity activities`
- `$HOME/Library/Android/sdk/platform-tools/adb exec-out uiautomator dump /dev/tty`
- `python3 -m json.tool firebase-rules.json`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`: OK.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build`: falhou primeiro por `MissingTranslation` da nova string `voltar_bloqueado_jogo`; corrigidas traduções EN/ES/FR/DE e repetido com OK.
- `git status --short`: executado no fim.
