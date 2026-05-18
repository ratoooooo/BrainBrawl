# Firebase Realtime Database Rules

Este ficheiro acompanha `firebase-rules.json` e documenta o estado atual de seguranca do BrainBrawl.

## Badges v1 / conquistas - 2026-05-14

- Foi criado o node `conquistas/{uid}/{badgeId}` para conquistas UID-first.
- Cada conquista gravada contem `id`, `familia`, `nome`, `descricao`, `objetivo`, `progressoAoDesbloquear`, `drawableName`, `desbloqueadaEm` e `origem`.
- Permissoes:
  - leitura apenas quando `auth != null` e `auth.uid == $uid`;
  - escrita apenas quando `auth != null` e `auth.uid == $uid`;
  - chaves `guest_` ficam bloqueadas em `conquistas`.
- Convidados nao leem nem gravam conquistas, e o cliente tambem evita chamar o repository para convidados/perfis sem Auth.
- As rules validam formato, familia (`RC`, `PJ`, `VT`), drawable local com nome simples e progresso suficiente para o objetivo.
- Nao foram abertas permissoes globais e nao foi usado `childrenCount` nem `childrenCount()`.
- Limitacao importante: badges v1 sao client-side. As rules protegem ownership e formato, mas nao provam que as estatisticas sao legitimas.
- Futuro ideal: mover desbloqueio de conquistas para Cloud Functions junto ao fecho autoritativo de jogos/estatisticas, escrevendo `conquistas/{uid}` no servidor.

## Atualizacao categorias e dificuldade opcional - 2026-05-11

- `firebase-rules.json` foi atualizado para aceitar campos opcionais em perguntas:
  - `imagem` como string.
  - `dificuldade` como string limitada a `facil`, `media` ou `dificil`.
- A validacao foi aplicada a perguntas em:
  - `categoriasPublicas/{categoriaId}/perguntas/{perguntaId}`
  - `jogadores/{uidOuLegado}/categoriasPersonalizadas/{nomeCategoria}/perguntas/{perguntaId}`
  - `salas/{codigoSala}/perguntas/{perguntaId}`
  - `sala_1x1/{codigoSala}/perguntas/{perguntaId}`
  - `sala_2x2/{codigoSala}/perguntas/{perguntaId}`
- `sala_1x1` e `sala_2x2` passaram a aceitar metadados opcionais de categoria especifica por convite:
  - `categoriaPersonalizada`
  - `donoCategoria`
  - `donoUid`
  - `categoriaPublica`
  - `categoriaPublicaId`
- Nao foram abertas permissoes globais.
- Nao foi usado `childrenCount` nem `childrenCount()`.
- `python3 -m json.tool firebase-rules.json` executado com sucesso.

## Atualizacao - matchmaking aleatorio desativado - 2026-05-08

Alteracoes desta ronda:

- O matchmaking aleatorio 1x1/2x2 foi retirado da experiencia do jogador; os paths `matchmaking` podem continuar nas rules por compatibilidade, mas ficam sem entrada de UI ativa.
- `childrenCount()` foi removido de `firebase-rules.json`, porque Realtime Database Rules nao suporta essa validacao.
- `jogadoresPermitidos` continua a aceitar apenas filhos booleanos, mas as rules ja nao tentam contar 2/4 entradas. A lotacao operacional fica no Kotlin, em `JogoCompetitivoRepository`, atraves da reserva transacional antes de escrever jogadores em salas abertas/convites.
- Esta alteracao evita rules invalidas sem mexer em pontuacao, XP, ranking, historico, categorias, login/registo ou convites 1x1/2x2.

## Revisao de hardening - 2026-05-08

Alteracoes aplicadas nesta ronda:

- `jogadores/{jogadorId}` passou a bloquear chaves com prefixo `guest_`, impedindo criacao direta de perfis persistentes para convidados.
- `categoriasPublicas/{categoriaId}` deixou de aceitar writes sem Auth. Convidados podem ler/jogar categorias publicas, mas nao publicar, avaliar nem incrementar usos.
- Campos estruturais de categorias publicas (`nome`, `descricao`, `criador*`, `perguntas`, datas) passaram a ficar imutaveis para nao-criadores. Um utilizador autenticado que nao e criador so consegue participar no fluxo de avaliacao/usos sem alterar perguntas ou metadados principais.
- `totalAvaliacoes` e `usos` passaram a aceitar manutencao do valor atual ou incremento de uma unidade; valores negativos continuam bloqueados.
- `jogadores/{uid}/categoriasPersonalizadas` passou a exigir Auth no proprio UID ou fallback legado explicito por `nomeUtilizador`; sem Auth, so fica permitido se existir perfil legado com `password`.
- `sala_2x2/{codigo}/equipaA` e `equipaB` deixaram de aceitar campos arbitrarios em jogadores de equipa. Agora aceitam apenas campos conhecidos de identidade temporaria: `nome`, `nomeDisplay`, `uid`, `playerKey`, `tipoJogador`, `isGuest`, `nomeUtilizador`, `nomeJogador` e `avatar`.
- `sala_1x1/{codigo}` e `sala_2x2/{codigo}` passaram a aceitar metadados de sala competitiva fechada: `origem`, `lotacaoMaxima`, `entradaFechada` e `jogadoresPermitidos`.
- `lotacaoMaxima` e validado como `2` em `sala_1x1` e `4` em `sala_2x2`.
- `jogadoresPermitidos` aceita apenas booleanos. A lotacao 2/4 deixou de ser validada por contagem nas rules e fica a cargo das transacoes Kotlin/repository.
- `matchmaking/{modo}/resultados/{playerKey}/jogadores` e `matchmaking/{modo}/matches/{matchId}` passaram a bloquear `$other` e a validar os campos esperados.
- `matchmaking` continua com `.write` no nivel de `matchmaking/{modo}` porque o cliente atual faz transacao nesse node para reclamar jogadores e criar `matches`.

Paths endurecidos:

- `jogadores`
- `categoriasPublicas`
- `jogadores/{uid}/categoriasPersonalizadas`
- `sala_2x2/{codigo}/equipaA`
- `sala_2x2/{codigo}/equipaB`
- `sala_1x1/{codigo}/jogadoresPermitidos`
- `sala_2x2/{codigo}/jogadoresPermitidos`
- `matchmaking/{modo}/resultados`
- `matchmaking/{modo}/matches`

Permissoes que continuam abertas por compatibilidade:

- `jogadores` continua com `.read=true` porque ranking, pesquisa social, login legado e resolucao de perfis por `nomeUtilizador` ainda leem o node inteiro ou fazem queries globais.
- `categorias`, `categoriasPublicas`, `salas`, `sala_1x1`, `sala_2x2` e `matchmaking` mantem leitura aberta para permitir descoberta de categorias, sala por codigo, podios, matchmaking realtime e convidados.
- `salas`, `sala_1x1` e `sala_2x2` ainda tem varios fallbacks `auth == null` porque convidados e alguns fluxos legados escrevem dados temporarios de jogo/sala.
- `matchmaking/{modo}` mantem write no nivel do modo para preservar a transacao cliente-side; as rules validam formato, mas nao provam que o conjunto escolhido era realmente o correto.
- Perfis legados com `password` ainda podem receber algumas escritas sem Auth para nao quebrar login/compatibilidade antiga. Isto deve ser removido apos migracao completa para Firebase Auth.

Convidados:

- Podem continuar a escrever apenas dados temporarios de sala/matchmaking quando o fluxo sem Auth precisa disso.
- Nao podem criar `jogadores/guest_*`.
- Nao podem escrever `historicoJogos/{uid}` porque historico exige `auth.uid == uid`.
- Nao podem publicar, avaliar nem incrementar usos em categorias publicas.
- Nao devem gravar estatisticas, XP, historico ou ranking; o Kotlin tambem bloqueia estes writes quando `uid` esta vazio ou `tipoJogador=guest`.

Limites que continuam impossiveis de resolver so com rules:

- As rules nao conseguem provar que uma resposta foi dada dentro do tempo nem que a pontuacao e legitima.
- As rules nao conseguem impedir um cliente modificado de escrever valores de sala aparentemente validos.
- A media de avaliacoes ainda e calculada client-side; as rules validam faixa e incremento basico, mas Cloud Functions seriam ideais para calcular `ratingMedio` e `totalAvaliacoes`.
- A escolha de jogadores no matchmaking e a criacao da sala continuam cliente-side; a transacao reduz duplicacao, mas nao substitui um backend autoritativo.
- `jogadoresPermitidos` limita lotacao e reduz corrida de entrada, mas ainda pode ser reservado por clientes dentro das permissoes temporarias de sala. A garantia anti-abuso completa continua a pedir backend/Cloud Functions.
- Estatisticas, XP e ranking continuam sendo writes client-side do proprio perfil autenticado; as rules validam ownership e limites basicos, nao a justica do resultado.

Plano futuro recomendado:

1. Migrar ou encerrar login legado por `jogadores/{nomeUtilizador}/password`.
2. Separar perfis publicos de dados privados e remover hashes de password da Realtime Database.
3. Mover calculo de resultados, estatisticas, XP, ranking, usos e rating para Cloud Functions.
4. Deixar o cliente escrever apenas respostas/resultados temporarios em sala.
5. Remover writes `auth == null` de salas/matchmaking quando convidados passarem por token anonimo Auth ou backend.
6. Fechar `.read=true` global de `jogadores` com um modelo separado para ranking/pesquisa publica.

## Auditoria de rules - 2026-05-03

Resumo:

- Os paths usados pelo codigo continuam alinhados com `FirebasePaths.kt`: `jogadores`, `salas`, `sala_1x1`, `sala_2x2`, `categorias`, `categoriasPersonalizadas`, `categoriasPublicas`, amigos, pedidos e convites.
- `jogadores` tem `.indexOn` para as queries atuais por `uid`, `nomeUtilizador`, `email`, `pontuacao`, `recordePontuacao` e vitorias por modo.
- Os campos novos de perfil (`xpTotal`, `nivel`, `xpNoNivelAtual`, `xpNecessarioProximoNivel`, `recordePontuacao`) estao validados e perfis antigos continuam com fallback de leitura no cliente.
- `estatisticasAtualizadas` existe e e validado nas tres familias de sala, permitindo a protecao transacional usada pelo cliente.

Riscos mantidos:

- `categoriasPublicas` ainda permite writes amplos quando `usos` aumenta ou quando `newData/avaliacoes/{auth.uid}` existe. A validacao bloqueia tipos inesperados, mas nao prova que apenas `usos` ou apenas a avaliacao mudaram.
- As rules ainda aceitam alguns fallbacks `auth == null` para convidados e dados legados. Isto e necessario para compatibilidade atual, mas nao e seguranca final.
- Convites e pedidos usam writes multipath. Quando as rules bloqueiam parte de um `updateChildren`, a operacao atomica falha; quando o codigo opta por atualizacao essencial + secundaria, pode haver divergencia temporaria entre copias.
- Resultados, vitorias, XP e ranking continuam confiados ao cliente. Rules validam formato, nao verdade de jogo.

Nao foram feitas alteracoes em `firebase-rules.json` nesta auditoria para evitar quebrar fluxos ja testados de categorias publicas, convidados, convites e compatibilidade antiga.

## Fase final UID/Auth hardening

Atualizado em 2026-04-30.

O ficheiro `firebase-rules.json` passou a preparar a fase em que `auth.uid` e a unica chave principal:

- `jogadores/{uid}` so pode ser escrito pelo proprio utilizador autenticado (`auth.uid == uid`).
- Perfis legados por nome continuam aceites sem Auth apenas para compatibilidade temporaria com o login antigo.
- `salas/{codigo}`, `sala_1x1/{codigo}` e `sala_2x2/{codigo}` validam `adminUid`/`adminId` e permitem updates de estado apenas ao admin autenticado, mantendo fallback legado para convidados/dados antigos sem Auth.
- `jogadores` dentro das salas podem ser escritos pelo proprio `uid`, pelo UID guardado no objeto do jogador ou pelo admin da sala.
- `categoriasPublicas/{id}` passa a validar `criadorUid` e permite criacao/edicao pelo criador autenticado; avaliacoes e incrementos de uso continuam tolerantes para preservar os fluxos atuais.
- `jogadores/{uid}/categoriasPersonalizadas` passa a exigir `auth.uid == uid` quando ha Auth.
- Paths desconhecidos continuam bloqueados.

Campos novos/preparados:

- `adminUid` em salas novas autenticadas.
- `criadorUid` em categorias publicas.
- `donoUid` em categorias personalizadas e salas que usam categorias personalizadas.
- `.indexOn` em `jogadores`: `uid`, `nomeUtilizador`, `email`, `pontuacao`, `recordePontuacao` e vitorias por modo.

Limites conhecidos desta fase:

- As rules impedem que um utilizador autenticado escreva diretamente em `jogadores/{uid}` de outro utilizador.
- As rules ainda nao conseguem provar estatisticas finais justas, porque a app cliente continua a calcular e escrever resultados.
- Fallbacks `auth == null` continuam em salas e alguns fluxos para nao quebrar convidados e dados antigos; devem ser removidos quando a app deixar de aceitar login legado/convidados em writes sensiveis.
- Ranking, validacao de resultados, anti-cheat e atualizacao autoritativa de estatisticas devem passar para Cloud Functions ou backend confiavel.

## Contexto atual

- A app usa Firebase Realtime Database e iniciou a migracao para Firebase Authentication.
- Novas contas Auth criam perfil em `jogadores/{uid}` com `uid`, `nomeUtilizador` e `email`.
- O login antigo por `jogadores/{nome}/password` ainda existe como compatibilidade temporaria.
- Durante a fase hibrida, varios fluxos continuam a resolver dados antigos por `nomeUtilizador`.
- As rules permitem criar/atualizar o perfil Auth em `jogadores/{uid}` apenas quando `auth.uid == uid`.
- O ownership por `auth.uid` ja esta preparado nos paths principais, mas ainda existem fallbacks legados para compatibilidade.

## Paths analisados

Os paths usados no projeto estao concentrados principalmente em `FirebasePaths.kt` e nos repositories:

- `jogadores`
- `jogadores/{nome}/categoriasPersonalizadas`
- `jogadores/{nome}/amigos`
- `jogadores/{nome}/pedidos_amizade`
- `jogadores/{nome}/convites_recebidos`
- `jogadores/{nome}/convites_enviados`
- `salas`
- `sala_1x1`
- `sala_2x2`
- `categorias`
- `categoriasPublicas`

## Indices de queries

O node `jogadores` declara:

- `.indexOn: ["uid", "nomeUtilizador", "email", "pontuacao", "recordePontuacao", "totalVitoriasModoSolo", "totalVitoriasModo1x1", "totalVitoriasModo2x2"]`

Isto suporta as queries da fase hibrida Auth, especialmente a resolucao direta por UID, a resolucao de perfis antigos por `nomeUtilizador`, o ranking global ordenado por `pontuacao`, o ranking de recordes por `recordePontuacao` e os rankings por modo. O indice por `email` fica preparado para consultas por email sem alterar a estrutura Firebase.

Tambem existem ainda alguns acessos diretos em `RegistarActivity`, `Pontuacao1x1Activity` e `Pontuacao2x2Activity`, mas esta fase nao altera codigo da app.

## O que fica protegido

- O acesso a paths desconhecidos fica bloqueado por defeito.
- `categorias` fica apenas de leitura pela app cliente.
- Criacao/atualizacao de perfil Auth em `jogadores/{uid}` exige utilizador autenticado e `auth.uid == uid`.
- Perfis Auth validam os campos atuais: `uid`, `nomeUtilizador`, `email`, `avatar`, `estado`, `pontuacao`, `recordePontuacao`, `taxaAcertos`, `totalJogos`, `totalRespostasCertas`, `totalVitorias`, `totalVitoriasModo1x1`, `totalVitoriasModo2x2` e `totalVitoriasModoSolo`.
- Criacao de jogadores legados continua a passar por validacao basica de formato: password como hash SHA-256 hexadecimal, avatar como string e estatisticas numericas.
- A password de um jogador existente nao pode ser alterada por uma escrita normal no mesmo node.
- Estruturas conhecidas de salas, categorias publicas, categorias personalizadas, convites, pedidos, amigos, pontuacoes e estatisticas passam a validar tipos basicos.
- Campos inesperados ficam bloqueados nos principais objetos atraves de `$other.validate = false`.
- Pontuacoes, totais, usos e avaliacoes ficam limitados a valores numericos nao negativos quando aplicavel.

## Compatibilidade temporaria com perfis antigos

O node `jogadores` continua legivel pelo cliente porque a fase hibrida ainda precisa de:

- resolver perfis por `nomeUtilizador`;
- suportar login antigo por `jogadores/{nome}/password`;
- manter dados existentes sem migracao destrutiva.

Para compatibilidade, escritas sem Auth em perfis antigos continuam aceites quando respeitam a validacao legada com `password` e `avatar`. Perfis novos de Firebase Auth devem ser escritos em `jogadores/{uid}` e so passam se o utilizador autenticado for o proprio `uid`.

## Riscos que continuam

Estas rules melhoram a forma dos dados e protegem o perfil Auth principal, mas alguns fluxos ainda nao tem ownership forte.

Enquanto existirem paths baseados em nome, salas ou codigos temporarios, as rules nao conseguem provar que:

- quem escreve em `jogadores/Alice` e mesmo a Alice;
- quem aceita um convite e o destinatario do convite;
- quem atualiza estatisticas finais nao esta a inflacionar resultados.

- Um utilizador autenticado ainda pode tentar inflacionar as proprias estatisticas se fabricar resultados de sala validos em termos de tipo.
- Compatibilidade com convidados/dados antigos ainda deixa algumas janelas `auth == null` em salas, usos e fluxos antigos.
- A validacao nao substitui regras de negocio server-side. Ela apenas reduz writes malformados e bloqueia paths/campos desconhecidos.
- Hashes de password continuam expostos a leitura pela app, abrindo espaco para ataques offline se a base de dados for lida por clientes nao confiaveis.

## O que ainda depende do cliente

- Suportar login legado por nome/password enquanto a migracao nao for encerrada.
- Validar se um jogador pode iniciar jogo.
- Garantir regras completas de resposta/pontuacao em dados antigos ou convidados sem Auth.
- Garantir que estatisticas sao atualizadas uma unica vez por jogador/sala.
- Validar regras completas de jogo, fim de partida e vencedores.

## Recomendacao futura

A app ja usa Firebase Authentication nos fluxos novos. A seguranca forte agora deve mover writes sensiveis para um backend confiavel e remover fallbacks legados quando a base estiver migrada.

Plano recomendado:

1. Remover login legado por `jogadores/{nome}/password` depois de migrar/criar contas Auth para todos.
2. Separar dados publicos e privados, removendo hashes de password da Realtime Database.
3. Remover fallbacks `auth == null` de salas, convites, categorias e resultados.
4. Normalizar participantes por UID em todos os paths antigos ainda por nome.
5. Mover estatisticas finais, ranking, validacao de resultados e anti-cheat para Cloud Functions.
6. Depois das Cloud Functions, bloquear writes diretos do cliente em estatisticas globais.

## Aplicacao das rules

O ficheiro `firebase-rules.json` foi criado para ser importado/publicado manualmente no Firebase Console ou via Firebase CLI.

No Firebase Console:

1. Abrir o projeto BrainBrawl.
2. Ir a Realtime Database.
3. Abrir o separador Rules.
4. Substituir o conteudo pelo JSON de `firebase-rules.json`.
5. Clicar em Publish.

Opcionalmente, via Firebase CLI, publicar com `firebase deploy --only database`.

Antes de usar em producao, validar num projeto de testes com:

- registo e login;
- criar/entrar/sair de sala de grupo;
- jogo classico, caotico e eliminatorias;
- convite 1x1 e desforra;
- convite 2x2;
- amigos, pedidos e convites;
- categorias personalizadas;
- publicar, copiar e avaliar categorias publicas;
- fim de jogo e atualizacao de estatisticas.

## Atualizacao convites 1x1/2x2

- `convites_recebidos` e `convites_enviados` agora aceitam campos explicitos de identidade do remetente e destinatario: `remetenteUid`, `remetenteChavePerfil`, `remetenteNome`, `destinatarioUid`, `destinatarioChavePerfil` e `destinatarioNome`.
- Estes campos sao necessarios porque a fase hibrida pode ter perfil real em `jogadores/{nomeUtilizador}` enquanto o Firebase Auth fornece `uid`.
- As rules de convites permitem que o remetente autenticado escreva a copia recebida no perfil real do destinatario e a copia enviada no seu proprio perfil real, sem criar `jogadores/{uid}` quando esse perfil nao existe.
- A validacao `$other: false` foi mantida; apenas os novos campos conhecidos foram adicionados.
- O ficheiro continua a exigir publicacao manual no Firebase Console ou via Firebase CLI antes dos testes reais.

## Atualizacao matchmaking automatico

Novos paths permitidos:

- `matchmaking/1x1/fila/{playerKey}`
- `matchmaking/1x1/resultados/{playerKey}`
- `matchmaking/1x1/matches/{matchId}`
- `matchmaking/2x2/fila/{playerKey}`
- `matchmaking/2x2/resultados/{playerKey}`
- `matchmaking/2x2/matches/{matchId}`

Campos validados em `fila/{playerKey}`:

- `playerKey` igual a chave do node.
- `tipoJogador = auth` com `auth.uid == playerKey` e `uid == auth.uid`.
- `tipoJogador = guest` com chave `guest_...` e `uid` vazio/ausente.
- `nomeUtilizador` opcional.
- `nomeJogador` opcional.
- `nomeDisplay`
- `avatar` opcional.
- `timestampEntrada`
- `estado = aguardando` ou `encontrado`
- `isGuest`

Campos validados em `resultados/{playerKey}`:

- `playerKey` igual a chave do node.
- `uid` opcional para convidados e preenchido para contas.
- `tipoJogador`
- `codigoSala` com 6 caracteres `[A-Z0-9]`.
- `modo` igual ao modo do path (`1x1` ou `2x2`).
- `nomeCategoria`
- `criadorId`
- `criadorUid`
- `estado = encontrado`
- `timestampEntrada`
- `jogadores/{playerKeyJogador}` com identidade coerente por `playerKey`.

Tambem foram aceites `playerKey`, `tipoJogador`, `isGuest` e `avatar` em `salas`, `sala_1x1` e `sala_2x2`, porque o matchmaking e os fluxos de sala reaproveitam a mesma identidade.

Nota de seguranca:

- As rules validam ownership forte para entradas autenticadas (`uid == auth.uid`) e limitam convidados a `playerKey` com prefixo `guest_` sem UID.
- Para permitir convidados, a leitura de `matchmaking` fica aberta ao cliente e a escrita de resultados/matches continua dependente do fluxo cliente.
- A transacao cliente reduz duplicacao de matches, mas as rules nao conseguem provar que os jogadores escolhidos eram realmente os mais antigos nem que a sala criada corresponde ao claim.
- Para producao anti-abuso forte, mover a escolha de jogadores, criacao de sala e publicacao de resultados para Cloud Functions e deixar o cliente escrever apenas a propria entrada de fila.

## Atualizacao matchmaking controlado - 2026-05-15

Alteracao minima nas rules:

- `matchmaking/{modo}/fila/{playerKey}` passou a aceitar que um utilizador autenticado marque a entrada de outro jogador auth como `estado=encontrado` quando `criadorId == auth.uid`.
- Isto e necessario porque o claim transacional do cliente agora reserva os jogadores selecionados na fila no mesmo passo em que cria `matches/{matchId}`.
- A entrada auth continua a exigir `uid == $playerKey`; a escrita normal de entrada propria continua limitada a `$playerKey == auth.uid`.
- A estrutura validada da fila continua fechada por `$other=false`.

Motivo:

- Antes, a transacao criava apenas `matches/{matchId}`. Enquanto a sala ainda estava a ser criada, outros clientes podiam continuar a selecionar o mesmo grupo de jogadores `aguardando`.
- Agora, jogadores reclamados deixam de ser candidatos para novos matches porque passam a `estado=encontrado` ate a sala/resultados serem publicados ou ate o rollback/timeout limpar a entrada.

Limites mantidos:

- `matchmaking/{modo}` ainda tem write amplo no nivel do modo para permitir a transacao cliente-side.
- Um cliente malicioso ainda pode tentar claims falsos; a validacao reduz formato errado, mas nao prova intencao de negocio.
- A solucao robusta continua a ser Cloud Functions para escolher jogadores, criar sala, publicar resultados, limpar fantasmas e aplicar anti-spam/active-room por UID.

## Atualizacao historico de jogos

Novo path:

- `historicoJogos/{uid}/{historicoId}`

Permissoes:

- Leitura apenas com `auth.uid == uid`.
- Escrita/remocao apenas com `auth.uid == uid`.
- `indexOn` em `dataHora`, usado para carregar os ultimos 50 jogos.

Campos validados:

- `modo`
- `codigoSala`
- `nomeCategoria`
- `pontuacao`
- `recordeFoiBatido`
- `respostasCertas`
- `totalPerguntas`
- `venceu`
- `empate`
- `equipa` opcional
- `dataHora`
- `jogadores/{index}` como string

Notas:

- A anti-duplicacao principal esta no cliente por transacao em `historicoJogos/{uid}/{historicoId}`.
- O `historicoId` e deterministico por modo e codigo da sala, impedindo duplicacao em reabertura normal do ecra de pontuacao.
- As rules protegem ownership por UID, mas nao substituem validacao server-side do resultado real da partida.

## Security Patch Pre-Walkthrough - 2026-05-14

### Password/hash legado

- Campo confirmado: `jogadores/{id}/password`.
- Escrita confirmada em `JogadorRepository.criarJogador(nomeJogador, passwordHash, avatar)`, usada para perfis legados por `nomeUtilizador`.
- Leitura confirmada em `JogadorRepository.toPerfilJogador()`.
- Uso ativo confirmado em `LoginViewModel.entrarLegado()`, que compara `UteisValidacao.hashPassword(password)` com `perfil.password`.
- Firebase Auth ja cobre registos e logins por email/password nos fluxos novos, mas ainda nao substitui totalmente o campo legado porque o login por nome/password continua suportado.
- Decisao desta fase: nao remover o campo nem fechar `jogadores.read=true`, para nao quebrar login legado, ranking, perfis e amigos antes de uma migracao planeada.

### Ajustes aplicados nas rules

- Adicionados limites conservadores em `categoriasPublicas/{categoriaId}` e `jogadores/{uid|nome}/categoriasPersonalizadas/{nomeCategoria}`:
  - `nome`: 1 a 60 caracteres.
  - `descricao`: ate 300 caracteres.
  - `pergunta`: 1 a 300 caracteres.
  - `respostaCorreta` e opcoes: 1 a 120 caracteres.
  - `imagem`: ate 500 caracteres quando existir.
  - `dificuldade`: mantida opcional e limitada a `facil`, `media` ou `dificil` quando existir.
- Valores antigos que ja existam e fiquem inalterados continuam aceites, para reduzir risco de quebrar categorias antigas durante updates noutros campos.
- Nao foi usado `childrenCount` nem `childrenCount()`.
- Nao foram alteradas regras de salas, convites, pontuacao, XP, ranking ou matchmaking.

### Riscos pendentes documentados

- R1 fica pendente: `jogadores` continua com leitura publica e pode expor email e hash legado. Correccao completa exige separar dados publicos/privados ou encerrar login legado.
- R2 fica pendente: pontuacao, XP, vitorias, ranking e historico continuam client-authoritative. Correccao forte exige Cloud Functions/backend autoritativo.
- R3 fica pendente: `salas`, `sala_1x1` e `sala_2x2` ainda permitem writes amplos para suportar convidados sem Auth. Correccao forte exige Firebase Anonymous Auth para convidados ou backend.
- Matchmaking aleatorio continua desativado/inacessivel; os logs residuais foram reduzidos, mas a funcionalidade nao foi reativada.

## Beta Prep UI Fixes - categorias e conquistas - 2026-05-18

Alterações aplicadas por bug real:

- Em `jogadores/{jogadorId}/categoriasPersonalizadas/{nomeCategoria}/perguntas/{perguntaId}` as validações de `pergunta`, `respostaCorreta`, `opcoes/*` e `imagem` deixaram de exigir que valores existentes permanecessem iguais.
- O objetivo é permitir edição real de perguntas pelo dono da categoria, mantendo limites de tipo e tamanho.
- A permissão `.write` do nó da categoria personalizada não foi alargada.

Conquistas:

- `conquistas/{uid}/{badgeId}/familia` passa a aceitar `XP` e `CR`, além de `RC`, `PJ` e `VT`.
- A leitura/escrita continua limitada ao próprio UID autenticado e exclui guests.

Limites:

- Estas regras não tornam pontuação/XP/ranking autoritativos.
- Avaliações e conquistas continuam client-side até existir backend/Cloud Functions.
