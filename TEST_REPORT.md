# BrainBrawl - TEST_REPORT

Data: 2026-04-29

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
