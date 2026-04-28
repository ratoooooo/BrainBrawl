# BrainBrawl - TODO Fixes

## Erros encontrados

- Fluxos com conta temporaria perdiam `nomeJogador` ao voltar ao `MainActivity` porque varios ecras passavam apenas `nomeUtilizador`.
- As salas de espera nao tinham acao consistente para sair da sala.
- Se o admin fechasse/removesse uma sala, os outros jogadores nao tinham redirecionamento explicito com preservacao da sessao.
- Em 1x1/2x2 cada dispositivo podia escrever `perguntaHoraInicio`, criando risco de cronometros diferentes.
- O tempo de jogo era comparado com relogio local do dispositivo, permitindo diferencas se os relogios estivessem desalinhados.
- Categorias criadas em `AdicionarPerguntaActivity` eram gravadas em `categorias/{nome}` e nao ficavam associadas ao perfil do jogador.
- Nao havia fluxo de reutilizacao/edicao/eliminacao de perguntas personalizadas.
- O host/admin de modos de grupo podia aparecer no podio com 0 pontos quando era apenas administrador.
- `SalaDeEsperaGrupoActivity` estava registada no Manifest e era aberta por criacao/entrada em salas de grupo, mas nao estendia `Activity`. Isto causaria crash ao abrir sala de grupo.
- Fluxo 2x2 enviava `nomeCategoria`, mas `SalaDeEspera2x2Activity` lia apenas `categoria`.
- Convites guardavam `nomeCategoria`, mas ao aceitar convite a categoria nao era reenviada para a sala de espera.
- `UteisNavegacao.enviarPontuacaoActivity` enviava `pontuacao`, enquanto Activities de pontuacao liam `totalPontos`.
- `UteisNavegacao.enviarPontuacaoActivity` enviava `totalPerguntascertas`, enquanto Activities competitivas liam `totalRespostasCertas`.
- Em `Jogo1x1Activity` e `Jogo2x2Activity`, timeout verificava `if (!tempoDecorrido)` depois de manter `tempoDecorrido = true`, por isso perguntas sem resposta podiam ficar presas.
- `Pontuacao2x2Activity` lia respostas certas em `respostasCertasA/B`, mas `Jogo2x2Activity` escreve em `totalPerguntasCertas_A/B`.
- Mensagem de bonus em `UteisJogo` mostrava valor diferente do valor somado para sequencias de 4+ respostas certas.
- O wrapper `gradlew` nao tem permissao de execucao direta (`./gradlew` falha com permission denied); `sh ./gradlew` funciona.
- O wrapper Gradle estava corrompido: `gradlew`, `gradlew.bat` e `gradle-wrapper.jar` tinham 0 bytes.
- O ambiente estava a usar Java 25, que faz o Kotlin/Gradle falhar com `IllegalArgumentException: 25.0.2`; o projeto valida com o JDK 21 do Android Studio.
- O lint falhava por falta de `POST_NOTIFICATIONS` e por 3 strings em falta na traducao inglesa.

## Erros corrigidos

- Adicionados helpers de navegacao para preservar sempre `nomeUtilizador` e `nomeJogador`.
- Corrigidos retornos para `MainActivity` em pontuacoes e jogo para manter conta temporaria.
- Adicionado botao `Sair da Sala` nas salas de espera de grupo, 1x1 e 2x2.
- Se o jogador normal sair, remove apenas a entrada dele da sala.
- Se o admin sair, remove a sala do Firebase.
- As salas de espera escutam remocao da sala e redirecionam preservando dados do jogador.
- 1x1/2x2 agora guardam o admin da sala explicitamente ao criar convites/desforra.
- 1x1/2x2 agora criam timestamp por pergunta com `ServerValue.TIMESTAMP` e transaction em `perguntaInicios/{index}`.
- O cronometro em grupo/1x1/2x2 usa compensacao `.info/serverTimeOffset`.
- O jogo de grupo passa a escrever `perguntaHoraInicio` com `ServerValue.TIMESTAMP`.
- Categorias personalizadas passam a ser guardadas em `jogadores/{nomeUtilizador}/categoriasPersonalizadas/{categoria}`.
- Jogadores registados podem criar nova categoria personalizada, reutilizar categoria guardada para criar sala, editar perguntas, eliminar perguntas e adicionar novas.
- `PontuacoesActivity` ignora jogadores marcados como `isHostOnly`.
- `SalaDeEsperaGrupoActivity` agora e uma `AppCompatActivity` funcional, com binding, validacao de extras, entrada do jogador, lista de jogadores, listener de estado e inicio do jogo pelo admin.
- `SalaDeEsperaGrupoActivity` remove listeners em `onDestroy`.
- `SalaDeEspera2x2Activity` aceita `nomeCategoria` e `categoria`, e repassa ambos por compatibilidade.
- `Convite1x1` passou a transportar `nomeCategoria`.
- `AmigosActivity` le `nomeCategoria` do convite e envia-a ao aceitar.
- `UteisNavegacao` envia aliases compativeis: `pontuacao`/`totalPontos` e `totalPerguntascertas`/`totalRespostasCertas`.
- `Jogo1x1Activity` e `Jogo2x2Activity` agora chamam `verificarResposta(-1)` quando o tempo acaba.
- `Pontuacao2x2Activity` le os mesmos paths de respostas certas que o jogo escreve.
- `UteisJogo` mostra o bonus correto para sequencias de 4+ respostas certas.
- Criada estrutura futura de pastas sem mover ficheiros existentes.
- Wrapper Gradle regenerado com Gradle 8.9.
- Adicionada permissao `POST_NOTIFICATIONS` ao Manifest para satisfazer o lint quando a dependencia Glide expoe `NotificationTarget`.
- Adicionadas traducoes inglesas em falta para `info_todos_modos`, `nome_do_amigo` e `avatar_do_amigo`.

## Erros que ficaram por corrigir

- A pontuacao ainda e calculada no cliente; agora usa tempo de servidor estimado, mas validacao forte exigiria Cloud Functions/servidor ou regras transacionais mais rigorosas.
- O fluxo personalizado reutiliza categorias guardadas; nao foi criada uma entidade separada de "sala modelo" para agendar salas futuras com data/hora.
- A UI de edicao de perguntas personalizadas e funcional, mas simples e programatica; idealmente deve virar layout/adapters dedicados.
- Autenticacao ainda e manual por Realtime Database; idealmente migrar para Firebase Auth ou reforcar regras de seguranca.
- Muitas Activities ainda usam listeners anonimos sem remocao explicita.
- Estatisticas podem ser incrementadas mais de uma vez em ecras de pontuacao se a Activity/listener disparar novamente.
- `Pergunta` esta no pacote default; compila, mas deve migrar para `models` numa fase controlada.
- Paths Firebase continuam inconsistentes entre `salas`, `sala_1x1` e `sala_2x2`.
- Falta suite real de testes para fluxos Firebase, jogo e pontuacao.
- O admin/ordem de jogadores em 1x1/2x2 depende da ordem dos children do Firebase, que nao deve ser tratada como contrato forte.
- Ha varios warnings de lint sobre dependencias desatualizadas, orientacao bloqueada e APIs desencorajadas; nao foram alterados para evitar mudancas amplas de UX/dependencias.

## Ficheiros alterados

- `app/src/main/java/com/example/brainbrawl/UteisNavegacao.kt`
- `app/src/main/java/com/example/brainbrawl/UteisSala.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEsperaGrupoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/SalaDeEspera2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/JogoActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Jogo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/PontuacoesActivity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/Pontuacao2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo1x1Activity.kt`
- `app/src/main/java/com/example/brainbrawl/ConvidarAmigo2x2Activity.kt`
- `app/src/main/java/com/example/brainbrawl/EscolherCategoriaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/AdicionarPerguntaActivity.kt`
- `app/src/main/java/com/example/brainbrawl/TipoModoClassico.kt`
- `app/src/main/res/layout/activity_sala_de_espera_1x1.xml`
- `app/src/main/res/layout/activity_sala_de_espera2x2.xml`
- `app/src/main/res/layout/activity_adicionar_pergunta.xml`

## Fluxos afetados

- Conta temporaria
- Criacao/entrada/saida de salas de espera
- Encerramento de sala pelo admin
- Jogo em grupo/classico/caotico/eliminatorias
- Jogo 1x1
- Jogo 2x2
- Pontuacoes finais
- Criacao, reutilizacao, edicao e eliminacao de perguntas personalizadas

## Testes manuais recomendados

- Entrar sem conta, criar sala de grupo, terminar jogo e confirmar que o nome temporario continua no `MainActivity`.
- Entrar sem conta, entrar numa sala por codigo, sair/terminar fluxo e confirmar que o nome temporario nao se perde.
- Numa sala de grupo, clicar `Sair da Sala` como jogador normal e confirmar que so esse jogador sai.
- Numa sala de grupo, clicar `Sair da Sala` como admin e confirmar que todos voltam ao `MainActivity`.
- Repetir o teste de saida em salas 1x1 e 2x2.
- Em 1x1/2x2, deixar dois dispositivos/emuladores na mesma pergunta e confirmar que o cronometro arranca alinhado.
- Criar categoria personalizada com utilizador registado, adicionar perguntas, voltar mais tarde e reutilizar a categoria para criar sala.
- Editar pergunta personalizada, alterar resposta correta, eliminar pergunta e confirmar no Firebase que os dados ficam validos.
- Confirmar que host/admin de grupo nao aparece no podio quando nao joga.
- Registar novo utilizador, fazer login e logout.
- Entrar como convidado e criar sala classica de grupo.
- Criar sala de grupo, entrar por codigo noutro dispositivo/emulador, iniciar jogo e terminar.
- Criar modo caotico e confirmar que perguntas de varias categorias aparecem.
- Criar convite 1x1, aceitar pelo amigo, iniciar jogo, deixar uma pergunta expirar e confirmar que avanca.
- Criar convite 2x2, aceitar por amigos, iniciar jogo, deixar uma pergunta expirar e confirmar que avanca.
- Confirmar que categoria escolhida em 2x2 e a categoria usada no jogo.
- Confirmar que a pontuacao final 1x1/2x2 mostra pontos e respostas certas corretas.
- Reabrir Activities de pontuacao/rodar ecra e verificar se estatisticas nao duplicam.
- Verificar regras Firebase para leitura/escrita de `jogadores`, `categorias`, `salas`, `sala_1x1` e `sala_2x2`.
- Usar `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` ao correr Gradle se o terminal continuar a usar Java 25.
