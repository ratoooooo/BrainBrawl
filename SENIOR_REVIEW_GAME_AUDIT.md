# Senior Review + Game Design Audit - Pergunta o Luso

Data da auditoria: 2026-05-17  
Projeto: `/Users/dinisrato/StudioProjects/BrainBrawl`  
Repo: `ratoooooo/BrainBrawl`  
Fase: review tecnica, QA malicioso, game design e decisao de beta fechada  

Nota: nesta fase nao alterei codigo, Firebase Rules, commits ou configuracao. O unico ficheiro criado e este relatorio. Nao executei `gradlew` nesta auditoria para evitar gerar ou alterar artefactos de build; a documentacao existente (`TEST_REPORT.md`) indica que `clean`, `assembleDebug`, `testDebugUnitTest`, `build` e `assembleRelease` passaram em fases anteriores.

## 1. Resumo executivo

Estado geral: o projeto esta num estado surpreendentemente completo para uma app Android Firebase feita em fases. Ja existe uma base jogavel com login/registo, convidados, salas, 1x1, 2x2, grupo, categorias, perfil, XP, historico, amigos, convites e badges. A arquitetura esta no caminho certo, mas ainda nao e uma arquitetura segura contra clientes modificados.

Nota tecnica: 7/10 para beta fechada controlada; 5/10 para beta publica.  
Nota produto/diversao: 7/10 como prototipo social de trivia; 8/10 de potencial se reforcar loops de retencao, identidade portuguesa e feedback de jogo.

Recomendacao beta fechada: sim, vale a pena mandar a 3-5 amigos, mas como teste fechado guiado, nao como beta publica. Eu faria uma micro-correcao antes se o objetivo for evitar confusao: alinhar a decisao sobre matchmaking aleatorio, porque a documentacao diz que esta desativado, mas `MainActivity.kt`, `AndroidManifest.xml` e `activity_main.xml` ainda o deixam acessivel para utilizadores autenticados.

Tres maiores riscos:

| Risco | Porque importa |
|---|---|
| Pontuacao/XP/ranking/historico sao client-authoritative | Um cliente modificado consegue escrever pontuacoes plausiveis e inflacionar progressao. Rules validam forma, nao verdade de jogo. |
| Multiplayer pode ficar bloqueado se alguem fecha a app | Pódios esperam 2/4 ou todos os jogadores; nao ha timeout robusto de abandono/reconexao em todos os modos. |
| Modelo hibrido UID + nome legado + convidados aumenta superficie de bugs | Compatibilidade por `nomeUtilizador`, fallback sem Auth e chaves temporarias tornam ownership, social e salas mais dificeis de provar. |

Tres maiores oportunidades:

| Oportunidade | Impacto |
|---|---|
| Fechar uma beta pequena com amigos | Vai revelar rapidamente se o jogo e divertido ou confuso nos fluxos reais de convite/sala. |
| Retencao leve: desafios diarios, streak, desforra mais visivel, ranking semanal | Grande ganho de vontade de voltar com baixo risco tecnico. |
| Identidade portuguesa forte | Distritos, expressoes, historia local, eventos tematicos e titulos tornam a app memoravel e menos generica. |

## 2. O que esta bom

Arquitetura:

- `FirebasePaths.kt`, `IntentExtras.kt` e `GameConstants.kt` reduzem bastante o caos de strings soltas. Ainda existem alguns campos literais antigos, mas a direcao esta certa.
- O padrao `Activity -> ViewModel -> Repository -> Service` ja esta presente em areas importantes: `MainViewModel.kt`, `LoginViewModel.kt`, `MeuPerfilViewModel.kt`, `JogoViewModel.kt`, `Jogo1x1ViewModel.kt`, `Jogo2x2ViewModel.kt`, `Pontuacao1x1ViewModel.kt`, `Pontuacao2x2ViewModel.kt` e `PontuacoesViewModel.kt`.
- Activities sensiveis ja estao muito mais finas do que o normal em projetos Firebase pequenos. `MainActivity.kt`, `LoginActivity.kt` e `MeuPerfilActivity.kt` fazem sobretudo ViewBinding, observers, navegacao e mensagens.
- Services como `ScoreService.kt`, `ScoreCompetitivoService.kt`, `ProgressaoService.kt`, `EstatisticasService.kt` e `BadgesService.kt` concentram logica pura e sao bons candidatos a mais testes.

Funcionalidades:

- O conjunto funcional e forte: Auth, convidados, amigos, pedidos, convites, categorias publicas/personalizadas, modos de jogo, XP, historico, ranking e badges.
- `PontuacaoRepository.kt` tem idempotencia local importante em `estatisticasAtualizadas`, historico por id estavel e transacoes para desforra.
- `JogoCompetitivoRepository.kt` usa transacoes para reservar entrada e para guardar perguntas da sala uma so vez. Isto ajuda bastante em corridas normais.
- `BadgesRepository.kt` grava conquistas por transaction, evitando sobrescrever timestamps ja existentes.

Jogo:

- O conceito de "Pergunta o Luso" e claro e comercialmente melhor do que um quiz generico.
- Modos classico, caotico, eliminatorias, 1x1 e 2x2 dao variedade suficiente para uma beta pequena.
- O bonus por sequencia e a pontuacao por tempo criam tensao simples e compreensivel.
- A desforra 1x1 e uma excelente feature de retencao.

UX:

- Main tem um primeiro fluxo claro: `JOGAR AGORA` e `ENTRAR NUMA SALA`.
- Perfil ja mostra nivel, XP, estatisticas e conquistas, o que ajuda a dar sentido a jogar varias vezes.
- O badge visual de pedidos/convites em `MainViewModel.kt` e boa decisao para puxar o jogador de volta para social.

Identidade portuguesa:

- O nome e forte.
- Categorias como Historia, Geografia, Cultura Geral, Desporto e Gentilicos encaixam bem.
- Ainda ha muito espaco para reforcar portugalidade, mas a base ja suporta isso via categorias e eventos.

## 3. Problemas tecnicos prioritarios

| Prioridade | Area | Ficheiros provaveis | Problema | Impacto | Correcao recomendada | Risco de quebrar |
|---|---|---|---|---|---|---|
| P0 | Produto/estado real | `MainActivity.kt`, `AndroidManifest.xml`, `activity_main.xml`, `MatchmakingActivity.kt` | Documentacao diz que matchmaking aleatorio esta desativado, mas a Main volta a mostrar botoes para utilizadores com UID e o manifest regista a Activity. | Testers podem cair no fluxo mais instavel sem saber. Contradiz `README.md`, `TEST_REPORT.md` e `ARCHITECTURE_PLAN.md`. | Decidir: ou esconder/desativar de novo, ou assumir como beta feature e testar com script dedicado. | Medio |
| P0 | Anti-cheat | `PontuacaoRepository.kt`, `firebase-rules.json`, `Jogo*ViewModel.kt` | Pontuacao, XP, vitorias, historico e badges dependem do cliente. | Ranking manipulavel por cliente modificado. Bloqueia beta publica. | Para beta fechada, aceitar risco e monitorizar. Para publico, Cloud Functions para fechar jogo e atualizar stats. | Alto |
| P0 | Multiplayer bloqueado | `Jogo1x1ViewModel.kt`, `Jogo2x2ViewModel.kt`, `Pontuacao*ViewModel.kt`, `JogoCompetitivoRepository.kt` | Pódios esperam todos; se alguem fecha a app antes de gravar pontuacao, outros ficam em espera. | Frustracao imediata em testes reais. | Timeout visivel: "jogador saiu", botao voltar seguro, resultado parcial/abandono. | Medio |
| P1 | Repositories grandes | `JogoCompetitivoRepository.kt` 1047 linhas, `PontuacaoRepository.kt` 816, `CategoriaRepository.kt` 789, `AmigosRepository.kt` 751 | Responsabilidades misturadas: Firebase, mapping, regras, compatibilidade e fluxos. | Dificil testar e mexer sem regressao. | Dividir por subdominio quando tocar: sala, perguntas, pódio, stats, convites. | Medio |
| P1 | Convidados e Auth hibrido | `firebase-rules.json`, `JogoCompetitivoRepository.kt`, `PontuacaoRepository.kt`, `SalaRepository.kt` | Varias writes aceitam `auth == null` em salas para convidados/legado. | Necessario para v1, mas fragil contra abuso. | Migrar convidados para Firebase Anonymous Auth; remover fallbacks sem Auth. | Alto |
| P1 | Validacao de pergunta incompleta na UI | `EditarCategoriaViewModel.kt`, `CategoriaRepository.kt`, `firebase-rules.json` | ViewModel valida tamanho e opcoes distintas, mas nao confirma explicitamente que `respostaCorreta` pertence as quatro opcoes. | Perguntas invalidas podem ser gravadas se UI permitir valor incoerente. Repository filtra depois, causando "categoria sem perguntas validas". | Validar `respostaCorreta in opcoes`. | Baixo |
| P1 | Possivel duplicacao/inconsistencia social | `AmigosRepository.kt` | Convites/pedidos usam multipath e, nalguns casos, atualizacao essencial + secundaria. | Copias podem ficar divergentes se a segunda falhar. | Para beta, aceitar e criar limpeza manual. Futuro: Cloud Function ou esquema single-source-of-truth. | Medio |
| P1 | UX de erro generica | varias Activities/ViewModels | Muitas mensagens sao "Erro ao carregar..." sem acao concreta. | Testers nao sabem se devem tentar outra vez, voltar, recriar sala ou reportar. | Mensagens por situacao: sala cheia, convite expirado, adversario saiu, perguntas insuficientes. | Baixo |
| P2 | Testes limitados | `app/src/test` | Existem testes para badges/conquistas e exemplos, mas quase nada para pontuacao, progressao, empate, categorias, convites e race conditions. | Regressao facil ao mexer. | Adicionar unit tests puros para Services e tests fake/repository-light para mapping. | Baixo/medio |
| P2 | Hardcoded legacy residual | `JogoCompetitivoRepository.kt`, `CategoriaRepository.kt`, `PontuacoesViewModel.kt`, `JogoViewModel.kt` | Ainda ha strings literais como `categoriaPersonalizada`, `donoCategoria`, textos de eventos e mensagens. | Pequena deriva entre constants/rules/client. | Migrar gradualmente para `FirebasePaths` e strings resources. | Baixo |
| P2 | Performance social/categorias | `AmigosRepository.kt`, `CategoriaRepository.kt`, `RankingRepository.kt` | Leitura global de `jogadores` e categorias publicas sem paginacao. | Aceitavel em beta pequena, fraco quando crescer. | Paginacao, indices dedicados e perfis publicos separados. | Medio |

## 4. Problemas de UX/game design

| Ecra/fluxo | Problema | Consequencia para jogador | Melhoria sugerida | Prioridade |
|---|---|---|---|---|
| Main | "Jogar Agora", "Explorar Categorias", matchmaking e entrar sala competem entre si. | Jogador novo pode nao perceber o melhor primeiro passo. | Primeiro uso com uma recomendacao discreta: "Comeca por Classico" ou "Criar sala para amigos". | P1 |
| Matchmaking | Estado contraditorio com docs. Pode aparecer como modo normal. | Testers podem avaliar a app pelo fluxo mais arriscado. | Tratar como "experimental" ou esconder. | P0 |
| Sala de espera | Se alguem sai ou fecha app, o feedback pode ser insuficiente. | Parece bugado ou preso. | Estado de presenca, timeout e botao "recriar sala". | P0 |
| Convites | Aceitar convite marca estado, mas o jogador precisa perceber onde vai e se a sala ainda existe. | Convite duplicado/antigo confunde. | Mostrar modo, categoria, remetente, validade e estado da sala antes de entrar. | P1 |
| Resultado final | Pódio funciona, mas pode ser mais emocional. | Menos vontade de rejogar. | Mostrar "ganhaste por X", "ficaste a Y pontos", MVP, melhor sequencia, botao Desforra maior. | P1 |
| Perfil | Estatisticas existem, mas conquistas ainda parecem sistema tecnico. | Menos motivacao. | Badges com progresso visivel: "32/50 respostas certas". | P1 |
| Categorias publicas | Sem moderacao/report, qualidade pode variar muito. | Uma categoria fraca estraga percecao do jogo. | "Reportar pergunta", rating por pergunta e curadoria. | P1 |
| Criar pergunta | Sem preview da pergunta como aparece em jogo. | Criadores nao veem erros de legibilidade. | Preview antes de guardar/publicar. | P2 |
| Modos | Classico, caotico e eliminatorias podem nao ser autoexplicativos. | Jogador hesita. | Microcopy curta no seletor de modo, sem tutorial pesado. | P1 |
| Historico/ranking | Bons sistemas, mas faltam objetivos temporais. | Pouca urgencia para voltar amanha. | Ranking semanal, streak diaria, "melhor distrito da semana". | P1 |

## 5. Seguranca e jogador malicioso

| Exploit | Como aconteceria | Impacto | Mitigacao agora | Solucao robusta futura |
|---|---|---|---|---|
| Falsificar XP/ranking | Cliente modificado escreve stats em `jogadores/{uid}` com valores validos. | Ranking e progressao perdem confianca. | Beta pequena e logs/monitorizacao manual. | Cloud Functions calculam stats a partir de eventos assinados/fecho de sala. |
| Falsificar pontuacao da sala | Cliente escreve `pontuacoes`, `pontuacoes_A/B` ou `pontuacao` com valor alto. | Vence partidas e ganha XP. | Rules limitam tipo/ownership, nao verdade. | Servidor valida respostas, tempo e pontuacao. |
| Duplicar historico | Cliente tenta gravar historico repetido. | Historico inflado. | `HistoricoRepository` usa id estavel e transaction; bom para app normal. | Historico escrito so pelo backend. |
| Duplicar badges | Cliente tenta gravar `conquistas/{uid}`. | Conquistas falsas. | Transaction idempotente e rules validam forma/progresso, mas progresso vem de stats manipulaveis. | Backend desbloqueia badges. |
| Entrar em sala cheia | Corrida em `jogadoresPermitidos` ou manipular playerKey. | 1x1 com 3 jogadores, 2x2 com 5 ou duplicados. | `JogoCompetitivoRepository` reserva com transaction e dedupe. | Backend/transaction autoritativa e Auth anonimo para todos. |
| Iniciar sala sem permissao | Cliente escreve `estado=em_jogo` se rules permitem por admin/fallback. | Partidas arrancam sem todos. | UID admin ajuda, mas `auth == null` ainda enfraquece. | Remover writes sem Auth; start via Cloud Function. |
| Aceitar convite duplicado/antigo | Convite pendente aponta para sala inexistente ou ja usada. | UI confusa, falha de entrada. | Verificar existencia da sala ao aceitar/abrir. | Convites com TTL e estado centralizado. |
| Sair a meio para quebrar sala | Fechar app antes de gravar resultado. | Pódio fica preso. | Botao de sair e mensagens melhores. | Presenca, timeout, abandono computado pelo servidor. |
| Manipular categoria publica | Criador/cliente escreve perguntas fracas/gigantes dentro dos limites. | Qualidade baixa ou abuso. | Limits nas rules e Auth obrigatorio para publicar. | Moderacao, report, reputacao e fila de aprovacao. |
| Guest gravar dados persistentes | Usar `guest_` ou sem Auth para escrever perfil/stats. | Contamina ranking. | Rules bloqueiam `jogadores/guest_*`; Kotlin evita stats guest. | Anonymous Auth + claims/tokens e sem writes persistentes sem servidor. |

## 6. Multiplayer/race conditions

Convites:

- `AmigosRepository.kt` cria salas 1x1/2x2 fechadas com `origem=convite`, `entradaFechada=true`, `lotacaoMaxima` e `jogadoresPermitidos`. Isto e bom.
- Convites sao duplicados em `convites_recebidos` e `convites_enviados`; `aceitarConvite` atualiza uma copia essencial e depois a secundaria. Se a secundaria falhar, pode ficar divergencia temporaria.
- Falta uma nocao clara de expiracao/TTL. Convites antigos podem sobreviver a salas apagadas ou partidas ja jogadas.

Salas:

- `Sala1x1ViewModel.kt` e `Sala2x2ViewModel.kt` removem listeners e tratam sala apagada, o que e bom.
- Se o admin sai, a sala e apagada. Em beta pequena isto e aceitavel, mas UX deve explicar "o anfitriao saiu".
- Se nao-admin fecha app sem clicar sair, nao ha garantia de cleanup imediato nos fluxos competitivos por convite.

1x1:

- Entrada e limitada por `lotacaoMaxima=2` e reserva transacional.
- `escutarPodio1x1` espera 2 pontuacoes. Se o adversario fecha a app antes de `guardarPontuacao1x1`, o jogador fica a aguardar.
- Desforra esta bem pensada: flags por jogador, `novaSalaDesforra` por transaction e sala criada uma vez. Ainda assim precisa de UX de timeout: "adversario nao respondeu".

2x2:

- Sala exige 4 jogadores e `Sala2x2ViewModel.kt` divide equipas por ordem de entrada (`take(2)` e `drop(2)`). Funciona, mas pode parecer arbitrario.
- `escutarPodio2x2` espera 2 por equipa e pontuacoes/respostas de todos. Um abandono bloqueia resultado final.
- Empate 2x2 esta tratado em `EstatisticasService.vencedores`: equipa empatada nao recebe vitoria. Boa decisao.

Matchmaking:

- `MatchmakingRepository.kt` tem um desenho relativamente robusto: fila, resultados, matches, claim transacional, rollback, stale cleanup e verificacao de jogador na sala.
- O problema principal e produto/risco: estava documentado como desativado, mas parece ativo. Se for testado, deve ser tratado como feature experimental.
- O path `matchmaking/{modo}` nas rules ainda permite write amplo no nivel do modo para preservar transaction cliente-side. E melhor do que nada, mas nao e garantia autoritativa.

Saida/reconexao:

- Listeners sao removidos em `onCleared`, `onDestroy` ou metodos dedicados em varios ViewModels.
- Falta uma estrategia uniforme de reconexao: se a app fecha e volta durante jogo, nao ha fluxo claro para regressar a sala/partida em curso.
- Falta estado de abandono com timeout em pódios e salas.

Duplicacao de XP/historico:

- Ha boas defesas locais: flags como `estatisticasAtualizadas`, `historicoGuardado` e transacao em `estatisticasAtualizadas/{identificador}`.
- O risco continua porque o cliente pode escolher que resultado escreve e quando escreve. Isto e suficiente para amigos, nao para publico.

## 7. Firebase/rules

Riscos atuais:

- `jogadores` tem `.read=true`. Isto suporta ranking, social e legado, mas expoe dados publicos e potencialmente hashes de password antigos.
- `jogadores/{uid}` permite ao proprio utilizador escrever stats numericas desde que validas. Isto permite inflacionar `pontuacao`, `xpTotal`, `totalVitorias` etc.
- `salas`, `sala_1x1`, `sala_2x2` e `matchmaking` mantem varias condicoes `auth == null` para convidados/legado.
- `categoriasPublicas` esta melhor, mas rating/usos continuam client-side. A media pode ser influenciada por clientes dentro do contrato permitido.
- Rules validam forma, ownership parcial e tipos; nao validam regras de jogo, tempo, respostas ou vencedor.

O que esta aceitavel para beta fechada:

- Leitura publica de categorias e salas.
- Writes temporarios de sala para convidados, desde que testers sejam conhecidos.
- Stats client-side, se todos souberem que e beta fechada e que ranking nao e anti-cheat.
- Login legado temporario, desde que nao haja promocao publica nem base grande.

O que bloqueia beta publica:

- `jogadores.read=true` com compatibilidade de password/hash legado.
- Writes client-side em ranking/XP/historico/vitorias.
- Writes `auth == null` em salas e paths competitivos.
- Matchmaking client-side como autoridade de selecao/criacao.
- Ausencia de moderacao/report em categorias publicas.

Sugestoes futuras:

1. Separar `perfisPublicos/{uid}` de `jogadoresPrivados/{uid}`.
2. Encerrar login legado por `nomeUtilizador/password`.
3. Migrar convidados para Firebase Anonymous Auth.
4. Cloud Functions para fechar jogos, calcular pontuacao, XP, ranking, historico e badges.
5. TTL/cleanup automatico para salas, convites e matchmaking.
6. Rules passarem a permitir ao cliente escrever apenas intents/eventos temporarios, nao resultados finais.

## 8. Beta fechada

Recomendo mandar a amigos? Sim, com controlo. Eu escolheria 3-5 pessoas, idealmente 2 tecnicas/pacientes e 3 jogadores normais. Nao vender como "esta pronto"; vender como "quero descobrir onde isto parte e onde e divertido".

Fluxos a testar:

- Criar conta nova, login, logout e voltar a entrar.
- Entrar como convidado e confirmar que joga mas nao ganha XP/historico.
- Criar sala grupo, outro jogador entra por codigo, jogar classico ate ao pódio.
- Modo caotico e eliminatorias em sala grupo.
- Enviar pedido de amizade, aceitar, remover amigo.
- Enviar convite 1x1, aceitar, jogar, pódio, desforra.
- Enviar convite 2x2 com 4 jogadores, jogar, pódio e empate se possivel.
- Explorar categoria publica, guardar copia, criar pergunta, editar, eliminar, publicar.
- Perfil, badges, ranking e historico depois de varias partidas.

Perguntas para testers:

- Em que momento ficaste sem saber o que fazer?
- Qual foi o fluxo mais divertido?
- Qual foi o ecra mais confuso?
- Percebeste a diferenca entre Classico, Caotico, Eliminatorias, 1x1 e 2x2?
- O jogo pareceu justo?
- O pódio deu vontade de jogar outra vez?
- O que te faria voltar amanha?
- Que tema portugues gostavas de jogar?

Bugs a procurar:

- Sala presa a aguardar jogador/pódio.
- Convite aceite mas sala inexistente/cheia.
- Jogador duplicado em sala.
- Pódio incompleto.
- XP/historico duplicado.
- Convidado aparecer no ranking.
- Categoria criada mas sem perguntas jogaveis.
- Erros ao rodar app, bloquear ecra ou fechar/reabrir.
- Textos cortados em ecra pequeno.

Como recolher feedback:

- Criar um Google Form curto com escala 1-5 para diversao, clareza, vontade de voltar e estabilidade.
- Pedir screen recording de 2 minutos quando algo falhar.
- Pedir que cada tester mande uma nota livre: "o momento mais fixe" e "o momento mais irritante".
- Anotar modelo do telemovel, Android version e se estava em Wi-Fi/dados.

## 9. Roadmap recomendado

### v1.2.x - estabilizacao/beta fechada

Objetivo: reduzir friccao e crashes/frustracao sem mexer na arquitetura profunda.

- Decidir estado do matchmaking: esconder ou marcar como experimental.
- Adicionar timeouts e mensagens para pódio/sala quando jogador abandona.
- Verificar resposta correta dentro das opcoes ao criar/editar pergunta.
- Melhorar mensagens de convite expirado, sala cheia, sala apagada e perguntas insuficientes.
- Checklist manual em 2 dispositivos reais.
- Criar formulario de feedback e guiao de beta.

### v1.3 - melhorias rapidas de retencao

Objetivo: fazer jogadores quererem repetir.

- Ranking semanal.
- Desforra com destaque no resultado.
- Progresso visual de badges.
- Streak diaria simples.
- Desafios diarios de 5 perguntas.
- Melhor resumo final: melhor sequencia, pergunta mais rapida, comparacao com adversario.

### v1.4 - perguntas com imagem/moderacao

Objetivo: melhorar conteudo e qualidade.

- Perguntas com imagem usando campo `imagem` ja previsto.
- Preview de pergunta antes de publicar.
- Reportar pergunta/categoria.
- Fila de moderacao basica para categorias publicas.
- Rating por pergunta alem de rating da categoria.
- Filtros por tema, dificuldade e popularidade.

### v2.0 - backend/Cloud Functions/seguranca forte

Objetivo: tornar beta publica viavel.

- Cloud Functions para fecho de jogo.
- Cliente escreve respostas/eventos; servidor calcula pontuacao, XP, historico e badges.
- Anonymous Auth para convidados.
- Separar perfis publicos e privados.
- Remover login legado/password da Realtime Database.
- TTL para salas, convites e matchmaking.
- Regras Firebase fechadas para writes sensiveis.

### Futuro - iOS/TestFlight/temporadas/torneios

Objetivo: escala e comunidade.

- iOS/TestFlight apenas depois de v1 Android estar estavel.
- Temporadas mensais.
- Ligas por distrito/regiao.
- Torneios tematicos.
- Eventos especiais: Santos Populares, 25 de Abril, Eurovisao, futebol europeu, feriados nacionais.
- Moderacao/reputacao de criadores.

## 10. Ideias fora da caixa

| Ideia | Impacto esperado | Esforco | Risco | Prioridade | Identidade portuguesa |
|---|---|---|---|---|---|
| Modo "Mapa de Portugal" | Muito alto | Alto | Medio | Alta futura | Forte: conquistar distritos/ilhas por acertos. |
| Liga dos Distritos | Alto | Medio | Medio | Alta | Cada jogador escolhe distrito; ranking semanal por distrito. |
| "Desafio da Terrinha" | Alto | Medio | Baixo | Alta | Perguntas locais, gentilicos, pratos, monumentos. |
| Pergunta relampago diaria | Alto | Baixo | Baixo | Alta | Uma pergunta portuguesa por dia, streak. |
| Titulos de perfil | Medio | Baixo | Baixo | Alta | "Mestre dos Pasteis", "Sabio do Minho", "Conquistador do Alentejo". |
| Molduras de perfil sem dinheiro real | Medio | Medio | Baixo | Media | Molduras por regioes, festas e conquistas. |
| Conquistas secretas | Medio | Baixo | Baixo | Media | "Acertar 5 perguntas de bacalhau", "Vencer por 1 ponto". |
| Modo "Contra o Relogio da CP" | Medio | Baixo | Baixo | Media | Humor portugues leve: comboio atrasado, tens de responder rapido. |
| Eventos tematicos semanais | Alto | Medio | Medio | Alta | Semana dos Descobrimentos, Euro, Festas Populares, Historia local. |
| Perguntas com audio | Medio | Alto | Medio | Futura | Fado, sotaques, hinos, sons tradicionais. |
| Cartas de poder no modo caotico | Alto | Medio/alto | Alto | Futura | "Trocar pergunta", "50/50", "Roubar 5 segundos". Precisa equilibrar. |
| Torneio de cafe | Alto | Alto | Medio | Futura | Brackets rapidos para grupos de amigos. |

## 11. Checklist antes de enviar a amigos

- [ ] Confirmar se matchmaking fica escondido ou explicitamente testado.
- [ ] Instalar APK em pelo menos 2 telemoveis fisicos.
- [ ] Criar 4 contas novas.
- [ ] Confirmar login/logout e sessao persistente.
- [ ] Confirmar convidado sem XP, historico, ranking e badges persistentes.
- [ ] Jogar sala grupo classico do inicio ao pódio.
- [ ] Jogar caotico.
- [ ] Jogar eliminatorias com alguem a falhar uma pergunta.
- [ ] Enviar e aceitar pedido de amizade.
- [ ] Enviar convite 1x1, aceitar, jogar e pedir desforra.
- [ ] Enviar convite 2x2 para 3 amigos, jogar com 4 jogadores.
- [ ] Fechar app a meio de 1x1 e ver se o outro fica preso.
- [ ] Fechar app antes do pódio 2x2 e registar comportamento.
- [ ] Criar categoria personalizada.
- [ ] Criar pergunta invalida de proposito e verificar feedback.
- [ ] Publicar categoria e jogar categoria publica.
- [ ] Ver ranking, historico, perfil e badges depois dos jogos.
- [ ] Testar ecra pequeno/teclado aberto em login, registo e criar pergunta.
- [ ] Registar todos os bugs com screenshot/video curto.

## 12. Conclusao

Vale a pena continuar? Sim. O projeto ja passou o ponto de "prototipo tecnico" e tem uma app real por baixo. O que falta nao e inventar mais features sem fim; e estabilizar os fluxos sociais/multiplayer, clarificar UX e preparar uma fronteira de seguranca para quando deixar de ser so amigos.

Vale a pena mandar a amigos? Sim, para 3-5 amigos, com guiao e expectativas certas. Eu nao mandaria ainda como beta publica nem a um grupo grande. Para beta fechada pequena, os riscos sao aceitaveis e ate desejaveis: vais aprender depressa onde o jogo prende, onde diverte e onde confunde.

O que fazer a seguir:

1. Alinhar o estado do matchmaking.
2. Testar abandono/reconexao/pódios com 2 a 4 pessoas.
3. Melhorar mensagens de sala/convite/erro.
4. Adicionar validacao simples extra nas perguntas.
5. Preparar o ciclo v1.2.x como beta fechada guiada, antes de investir em Cloud Functions e features grandes.
