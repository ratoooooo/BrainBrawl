# BrainBrawl - TEST_REPORT

Data: 2026-04-30

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
  - `Solo` por `totalVitoriasModoSolo`
  - `1x1` por `totalVitoriasModo1x1`
  - `2x2` por `totalVitoriasModo2x2`
- Compatibilidade mantida com dados antigos: campos ausentes passam a `0`, sem quebrar perfis legados.
- Convidados/perfis invalidos continuam ignorados no ranking.
- `firebase-rules.json` atualizado com `.indexOn` para `pontuacao`, `totalVitoriasModoSolo`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2`.

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
