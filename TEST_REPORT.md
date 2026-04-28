# BrainBrawl - TEST_REPORT

Data: 2026-04-28

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
