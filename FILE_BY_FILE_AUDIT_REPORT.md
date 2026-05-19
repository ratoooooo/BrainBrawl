# Auditoria ficheiro-a-ficheiro + Robustez + Anti-abuso

Data: 2026-05-18  
Projeto: BrainBrawl / Pergunta o Luso  
Escopo: Android Kotlin/XML, Firebase Realtime Database, Firebase Auth, MVVM, UID-first com compatibilidade legado por nomeUtilizador.

## 1. Resumo executivo

Estado geral: a app está suficientemente sólida para beta fechada pequena, desde que a beta seja tratada como teste funcional real e não como lançamento público. A arquitetura está coerente com o padrão Activity/ViewModel/Repository/Service, os paths centrais estão bastante usados, convidados estão protegidos nos fluxos persistentes principais, e já existe idempotência razoável para histórico/estatísticas.

Bloqueadores para beta fechada: não encontrei bloqueadores técnicos depois das correções desta ronda. Encontrei riscos que bloqueiam beta pública, sobretudo porque pontuação, XP, ranking, rating e conquistas continuam client-authoritative.

Principais riscos:
- Segurança forte ainda depende de cliente: resultados/XP/ranking/rating podem ser forjados por alguém com ferramentas.
- Multiplayer ainda tem riscos residuais de abandono/reconexão e salas presas em casos extremos.
- Avaliações funcionam melhor para contas Firebase UID-first; contas legado sem Firebase Auth continuam limitadas pelas rules.

Correções aplicadas nesta ronda:
- Badges XP alinhadas com os assets reais `xp100`, `xp500`, `xp1000`, `xp2500`, `xp5000`, `xp10000`, `xp25000`, `xp50000`, `xp100000`, `xp250000`, `xp500000`, `xp1000000`.
- Ecrã Editar Perfil passa a abrir com o avatar atual selecionado.
- Login e registo bloqueiam duplo clique simples durante tentativa de autenticação/registo.
- Testes unitários atualizados/adicionados para badges XP e parsing de avatar.

## 2. Ficheiros analisados

| Ficheiro/área | Estado | Problemas encontrados | Ação tomada | Pendência |
|---|---|---|---|---|
| `config/FirebasePaths.kt` | OK | Cobertura boa dos nodes/campos principais; alguns paths compostos continuam naturais nos repositories. | Sem alteração. | Manter disciplina de novas constantes. |
| `config/IntentExtras.kt` | OK | Extras principais centralizados. | Sem alteração. | Sem pendência crítica. |
| `config/GameConstants.kt` | OK | Estados/modos consistentes. | Sem alteração. | Sem pendência crítica. |
| `services/AuthService.kt` | OK | Wrapper simples e claro. | Sem alteração. | Sem pendência crítica. |
| `repositories/JogadorRepository.kt` | OK | UID-first com fallback por nome; edição de avatar segura. | Sem alteração direta. | Password legado continua risco documentado. |
| `viewmodels/LoginViewModel.kt` | OK | Fluxo Firebase + legado preservado. | Sem alteração. | Sem loading state formal. |
| `LoginActivity.kt` | Corrigido | Duplo clique podia disparar múltiplas tentativas. | Botões de entrada/registo/guest desativados enquanto há tentativa. | Loading visual mais explícito pode vir depois. |
| `viewmodels/RegistarViewModel.kt` | OK | Validações fortes de email/password/avatar. | Sem alteração. | Rollback de Auth se perfil falhar é simples, mas aceitável. |
| `RegistarActivity.kt` | Corrigido | Duplo clique podia disparar registo repetido. | Botão de registo desativado durante tentativa e reativado em evento. | Loading visual pode ser melhorado depois. |
| `MainActivity.kt` | OK | Navegação e guard de matchmaking por UID estão consistentes. | Sem alteração. | Matchmaking não disponível para convidados por regra atual. |
| `viewmodels/MainViewModel.kt` | OK | Listeners sociais são removidos; estado guest protegido. | Sem alteração. | Notificações dependem de resolver perfis cliente-side. |
| `MeuPerfilActivity.kt` | OK | Perfil principal mostra melhores badges e liga conquistas/editar perfil. | Sem alteração. | Sem pendência crítica. |
| `viewmodels/MeuPerfilViewModel.kt` | OK | Guest guard para conquistas e progressão coerente. | Sem alteração. | CR usa pontuação como proxy de créditos. |
| `PerfilAmigoActivity.kt` | OK | Melhores badges e avatar/fallback OK. | Sem alteração. | Sem pendência crítica. |
| `viewmodels/PerfilAmigoViewModel.kt` | OK | Carrega stats/badges públicos via perfil resolvido. | Sem alteração. | Dados públicos dependem de rules/leitura existente. |
| `ConquistasActivity.kt` | OK | Ecrã separado de conquistas funciona para próprio/amigo. | Sem alteração. | Sem paginação, aceitável para volume atual. |
| `EditarPerfilActivity.kt` | Corrigido | Abria sempre com avatar 1, podendo guardar avatar errado sem o jogador perceber. | Carrega perfil atual e seleciona avatar existente. | Só edita avatar por decisão segura. |
| `utils/AvatarUtils.kt` | Corrigido | Faltava parser de índice para avatar guardado. | Adicionado `indicePorNomeAvatar`. | Sem pendência crítica. |
| `utils/BadgeGridRenderer.kt` | OK | Fallback de badge existe; grelha por família coerente. | Sem alteração. | Textos XP/Créditos ainda hardcoded no renderer. |
| `services/BadgesService.kt` | Corrigido | XP thresholds não batiam com assets reais. | Thresholds XP alinhados com drawables existentes. | CR continua proxy de pontuação. |
| `repositories/BadgesRepository.kt` | OK | Usa transaction para evitar duplicação. | Sem alteração. | Client-side, não autoritativo. |
| `repositories/HistoricoRepository.kt` | OK | `guardarHistoricoUmaVez` usa transaction e limita histórico. | Sem alteração. | Idempotência depende de `historicoId`. |
| `repositories/PontuacaoRepository.kt` | OK | `estatisticasAtualizadas` por sala/jogador reduz duplicação. | Sem alteração. | Client-authoritative até Cloud Functions. |
| `viewmodels/Pontuacao1x1ViewModel.kt` | OK | Guards de guest, histórico e stats presentes. | Sem alteração. | Abandono adversário ainda precisa teste manual. |
| `viewmodels/Pontuacao2x2ViewModel.kt` | OK | Espera 4 resultados e guarda histórico uma vez. | Sem alteração. | Pódio pode esperar indefinidamente se alguém não terminar. |
| `viewmodels/PontuacoesViewModel.kt` | OK | Grupo espera resultados completos e bloqueia guests. | Sem alteração. | Salas grupo podem ficar presas se jogador abandonar. |
| `services/ProgressaoService.kt` | OK | Lógica pura e testável. | Sem alteração. | Fórmula mantida. |
| `services/EstatisticasService.kt` | OK | Cálculo centralizado e modo-aware. | Sem alteração. | Empates solo/1x1 dão vencedor pelo topo; aceitável, mas rever design. |
| `services/ScoreService.kt` | OK | Fórmula base preservada. | Sem alteração. | Sem pendência crítica. |
| `services/ScoreCompetitivoService.kt` | OK | Fórmula base preservada. | Sem alteração. | Sem pendência crítica. |
| `JogoActivity.kt` | OK com risco | Listeners removidos; respostas bloqueiam depois de responder. | Sem alteração. | `botaoCorreto!!` depende de perguntas válidas. |
| `viewmodels/JogoViewModel.kt` | OK | Admin/jogador separados; listeners removidos. | Sem alteração. | Eliminatórias devem ser stressadas manualmente. |
| `Jogo1x1Activity.kt` | OK com risco | Bloqueio de opções após resposta. | Sem alteração. | Perguntas inválidas poderiam crashar via `botaoCorreto!!`. |
| `viewmodels/Jogo1x1ViewModel.kt` | OK | Perguntas partilhadas e pódio completo. | Sem alteração. | Sem timeout de pódio. |
| `Jogo2x2Activity.kt` | OK com risco | Fluxo igual ao 1x1. | Sem alteração. | Perguntas inválidas poderiam crashar via `botaoCorreto!!`. |
| `viewmodels/Jogo2x2ViewModel.kt` | OK | Identifica equipa antes de pontuar. | Sem alteração. | Sem timeout de pódio. |
| `repositories/JogoRepository.kt` | OK | Usa server offset e listeners removíveis. | Sem alteração. | Abandono em grupo ainda é risco. |
| `repositories/JogoCompetitivoRepository.kt` | OK | Transactions para reservas/início/perguntas. | Sem alteração. | Código grande; merece auditoria dedicada antes de beta pública. |
| `repositories/CategoriaRepository.kt` | OK | Paths UID-first com fallback legado; valida perguntas ao jogar/publicar. | Sem alteração nesta ronda. | Rating para legado sem auth fica limitado. |
| `AdicionarPerguntaActivity.kt` | OK | Validações e confirmação de eliminar presentes. | Sem alteração. | Sem pendência crítica. |
| `viewmodels/EditarCategoriaViewModel.kt` | OK | Validação de categoria/pergunta/opções/resposta correta. | Sem alteração. | Erros de carregar/eliminar podiam expor mais feedback. |
| `ExplorarCategoriasActivity.kt` | OK com risco | Avaliação exposta; guest bloqueado. | Sem alteração. | Avaliação depende de Auth UID-first. |
| `viewmodels/ExplorarCategoriasViewModel.kt` | OK | Guards de login para guardar/avaliar/gerir. | Sem alteração. | Legacy sem Auth pode falhar rules ao avaliar. |
| `repositories/AmigosRepository.kt` | OK | Remove listeners; convites/pedidos determinísticos reduzem duplicados. | Sem alteração. | Aceitar convite não valida lotação no cliente, depende da sala/rules. |
| `viewmodels/AmigosViewModel.kt` | OK | Bloqueia pedido para si próprio/amigo existente. | Sem alteração. | Pedido pendente duplicado não tem mensagem própria. |
| `AmigosActivity.kt` | OK | Listeners no `onStart/onStop`; adapters bloqueiam ações rápidas. | Sem alteração. | Sem pendência crítica. |
| `ConvidarAmigo1x1Activity.kt` | OK | Botão voltar e empty state existentes. | Sem alteração. | Convite duplicado sobrescreve, não avisa. |
| `ConvidarAmigo2x2Activity.kt` | OK | Valida seleção antes de convidar. | Sem alteração. | Testar 3 amigos e cancelamento. |
| `ConviteAdapter.kt` | OK | Desativa aceitar/recusar após clique. | Sem alteração. | Sem pendência crítica. |
| `PedidoAmizadeAdapter.kt` | OK | Desativa aceitar/recusar após clique. | Sem alteração. | Sem pendência crítica. |
| `SalaRepository.kt` | OK com risco | Listeners removíveis; entrada por código valida duplicados. | Sem alteração. | Sala grupo permite iniciar com 1 jogador real. |
| `SalaGrupoViewModel.kt` | OK com risco | Remove listeners; admin apaga sala ao sair. | Sem alteração. | `MINIMO_JOGADORES_GRUPO = 1` deve ser decisão de produto. |
| `Sala1x1ViewModel.kt` | OK | Exige exatamente 2 para iniciar. | Sem alteração. | Sem pendência crítica. |
| `Sala2x2ViewModel.kt` | OK | Exige exatamente 4 e evita duplo início. | Sem alteração. | Sem pendência crítica. |
| `SalaDeEspera1x1Activity.kt` | OK | Botões iniciar/sair desativam no clique. | Sem alteração. | Testar sala apagada pelo host. |
| `SalaDeEspera2x2Activity.kt` | OK | Botões iniciar/sair desativam no clique. | Sem alteração. | Testar equipas após saída. |
| `SalaDeEsperaGrupoActivity.kt` | OK | Início bloqueado por estado do ViewModel. | Sem alteração. | Reavaliar mínimo de jogadores. |
| `SalaDeEsperaActivity.kt` | OK | Entrada por código desativa botão durante tentativa. | Sem alteração. | Sem pendência crítica. |
| `repositories/MatchmakingRepository.kt` | OK com risco | Transactions, stale cleanup e rollback existem. | Sem alteração. | Logs ainda verbosos; algoritmo merece teste multi-dispositivo. |
| `viewmodels/MatchmakingViewModel.kt` | OK | Timeout/cancelamento/listeners tratados. | Sem alteração. | Cancelamento em background deve ser testado em rede instável. |
| `MatchmakingActivity.kt` | OK | Cancela por botão/background; navegação controlada. | Sem alteração. | Sem pendência crítica. |
| `firebase-rules.json` | OK para beta fechada | Rules fecham root e bloqueiam guests em perfis/conquistas; ainda client-authoritative. | Sem alteração nesta fase; JSON validado. | Cloud Functions antes de beta pública. |
| `TEST_REPORT.md` | Atualizado | Faltava secção desta fase. | Atualizado. | Continuar a registar testes manuais. |
| `FIREBASE_RULES_NOTES.md` | OK | Já documenta limites client-side. | Sem alteração. | Atualizar quando rules mudarem. |
| `ARCHITECTURE_PLAN.md` | OK | Sem alteração arquitetural nesta ronda. | Sem alteração. | Sem pendência. |
| `README.md` | OK | Não exigiu mudança nesta ronda. | Sem alteração. | Pode ganhar guia de beta testers. |

## 3. Bugs corrigidos

| Bug | Causa real | Ficheiros alterados | Validação |
|---|---|---|---|
| Badges XP podiam cair em fallback/ícone errado | `BadgesService` definia marcos `XP_300`, `XP_600`, `XP_2000`, mas os drawables reais são `xp500`, `xp2500`, etc. | `BadgesService.kt`, `BadgesServiceTest.kt` | `testDebugUnitTest` e `build` passaram. |
| Editar Perfil podia guardar avatar errado sem intenção | O ecrã começava sempre no avatar 1 em vez de carregar o avatar atual do perfil. | `EditarPerfilActivity.kt`, `AvatarUtils.kt`, `AvatarUtilsTest.kt` | Teste unitário de parsing de avatar; build passou. |
| Login podia receber cliques repetidos rápidos | Botões continuavam ativos enquanto o ViewModel autenticava. | `LoginActivity.kt` | `assembleDebug` e `build` passaram. |
| Registo podia receber cliques repetidos rápidos | Botão de registo continuava ativo durante criação de conta/perfil. | `RegistarActivity.kt` | `assembleDebug` e `build` passaram. |

## 4. Riscos pendentes

| Risco | Impacto | Porque não foi corrigido agora | Fase sugerida |
|---|---|---|---|
| Pontuação/XP/ranking client-authoritative | Jogador técnico pode falsificar stats. | Resolver corretamente exige backend/Cloud Functions e possível migração. | v2.0 segurança forte. |
| Rating client-side e limitado por Auth | Contas legado sem Firebase Auth podem falhar ao avaliar; jogador técnico pode manipular rating se rules permitirem edge cases. | Mudar isto bem exige função autoritativa e decisão sobre legado. | v1.4/v2.0 moderação/rating. |
| Pódios 1x1/2x2 podem esperar indefinidamente se jogador fecha app | Fricção em beta multiplayer. | Timeout robusto pode alterar UX e regras de vitória. | v1.3 robustez multiplayer. |
| Sala grupo inicia com 1 jogador real | Pode confundir se a promessa for "grupo". | Pode ser intencional para teste/solo; mudar agora podia quebrar fluxo existente. | Decisão produto antes da beta pública. |
| Logs de matchmaking ainda verbosos | Ruído e potencial exposição de metadados em debug. | IDs estão mascarados; para beta fechada é aceitável. | Antes de release pública. |
| Repositórios competitivos grandes | Manutenção difícil e risco de regressão escondida. | Refactor geral está fora do escopo. | Pós-beta com testes de integração. |

## 5. Segurança/anti-abuso

Exploits avaliados mentalmente:
- Guest a gravar XP/histórico/badges: mitigado por guards em `Pontuacao*Input`, `PontuacoesInput`, `BadgesRepository` e rules em `conquistas`.
- Reabrir pontuação e duplicar histórico: mitigado por `guardarHistoricoUmaVez` com transaction por `historicoId`.
- Duplicar stats ao reabrir pódio: mitigado por `estatisticasAtualizadas/{identificador}` em sala.
- Duplicar badges: mitigado por transaction em `conquistas/{uid}/{badgeId}`.
- Duplo clique em login/registo: mitigado nesta ronda.
- Pergunta inválida: mitigada no editor e filtrada ao carregar/publicar categorias; risco residual em dados legados/oficiais malformados.
- Pedido/convite duplicado: mitigação parcial por chaves determinísticas; ainda sem feedback específico.

Mitigações aplicadas:
- Bloqueio simples de duplo clique em login/registo.
- Badges XP só usam drawables existentes, reduzindo fallback errado.
- Avatar atual é resolvido antes de guardar edição de perfil.

Pendências:
- Autoridade real de jogo e anti-cheat exigem backend.
- Validações Firebase não provam que uma pontuação é justa.
- Falhas de rede/abandono ainda precisam teste multi-dispositivo.

## 6. Multiplayer/race conditions

Convites:
- Convites 1x1/2x2 usam chaves determinísticas para enviado/recebido.
- Aceitar convite verifica se a sala existe antes de abrir.
- Pendência: não há validação visual forte de convite expirado por lotação/estado avançado além da existência da sala.

Salas:
- 1x1 exige 2 jogadores para iniciar.
- 2x2 exige 4 jogadores e bloqueia duplo início com `aIniciarJogo`.
- Grupo remove listeners e apaga sala se admin sai.
- Pendência: sala grupo com jogador a fechar app pode ficar com presença velha.

Matchmaking:
- Mantido.
- Usa transactions para reclamar fila e criar match.
- Tem cleanup stale, timeout, cancelamento e rollback.
- Pendência: testar em dois/quatro dispositivos reais com app em background, rede desligada e cancelamento simultâneo.

Pódios:
- Histórico e stats têm idempotência.
- Pódio 1x1/2x2 espera resultados completos.
- Pendência: sem timeout/forfeit robusto quando alguém abandona.

Abandono:
- Há limpeza parcial por `onDisconnect` no matchmaking e remoção em sala.
- Não há sistema autoritativo de desistência/vitória por abandono.

## 7. Firebase/rules

Alterações nesta fase: nenhuma.

Validação executada:
- `python3 -m json.tool firebase-rules.json`: OK.

O que está aceitável para beta fechada:
- Root fechado.
- `conquistas/{uid}` exige Auth e bloqueia `guest_`.
- Perfis guest não devem ser criados por rules.
- Categorias têm limites básicos de string e ownership.
- Matchmaking/salas têm validações de formato e Auth nos fluxos principais.

O que bloqueia beta pública:
- Stats, XP, ranking, histórico, rating e badges continuam escritos pelo cliente.
- Sem Cloud Functions, rules não conseguem provar resultados.
- Categorias públicas e avaliações precisam moderação/abuso mais forte.

## 8. Testes manuais recomendados

- Registar conta nova e fazer duplo clique no botão de registo.
- Fazer login com email e com conta legado; tentar duplo clique em Entrar.
- Entrar como convidado e confirmar que perfil/ranking/histórico/badges não persistem.
- Editar avatar, sair do perfil, voltar e confirmar persistência.
- Abrir conquistas e confirmar imagens XP 100/500/1000/2500+ sem fallback.
- Criar categoria personalizada, adicionar/editar/eliminar pergunta, jogar essa categoria.
- Publicar categoria e tentar avaliar com conta Firebase.
- Tentar avaliar como convidado e confirmar mensagem de login.
- Enviar pedido de amizade para si próprio, amigo existente e jogador inexistente.
- Aceitar/recusar pedido rapidamente.
- Enviar convite 1x1 e 2x2, aceitar, recusar e tentar convite expirado.
- Matchmaking 1x1: entrar, cancelar, entrar de novo, fechar app em procura.
- Matchmaking 2x2: quatro jogadores reais/emuladores até sala completa.
- Sala grupo: host sai, convidado sai, iniciar com 1 jogador e decidir se UX faz sentido.
- Terminar 1x1/2x2 e reabrir pódio para confirmar sem duplicação de XP/histórico.

## 9. Conclusão

Recomendo avançar para beta fechada com 3-5 amigos depois de uma aprovação visual rápida desta build. Não encontrei bloqueadores novos para beta fechada. As correções desta ronda são pequenas e seguras, e a validação técnica passou.

Próximos passos:
- Fazer teste manual dos fluxos listados acima.
- Recolher feedback dos testers sobre clareza de salas/matchmaking/conquistas.
- Antes de beta pública, planear Cloud Functions para resultados, XP, ranking, rating e conquistas.

## Validação técnica executada

| Comando | Resultado |
|---|---|
| `python3 -m json.tool firebase-rules.json` | OK |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean` | OK |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` | OK |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` | OK |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build` | OK |

Observação: o Gradle continua a avisar sobre deprecated features incompatíveis com Gradle 9.0; não é bloqueador para esta beta.
