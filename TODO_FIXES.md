# BrainBrawl - TODO Fixes

## Erros encontrados

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

- Autenticacao ainda e manual por Realtime Database; idealmente migrar para Firebase Auth ou reforcar regras de seguranca.
- Muitas Activities ainda usam listeners anonimos sem remocao explicita.
- Estatisticas podem ser incrementadas mais de uma vez em ecras de pontuacao se a Activity/listener disparar novamente.
- `Pergunta` esta no pacote default; compila, mas deve migrar para `models` numa fase controlada.
- Paths Firebase continuam inconsistentes entre `salas`, `sala_1x1` e `sala_2x2`.
- Falta suite real de testes para fluxos Firebase, jogo e pontuacao.
- O admin/ordem de jogadores em 1x1/2x2 depende da ordem dos children do Firebase, que nao deve ser tratada como contrato forte.
- Ha varios warnings de lint sobre dependencias desatualizadas, orientacao bloqueada e APIs desencorajadas; nao foram alterados para evitar mudancas amplas de UX/dependencias.

## Testes manuais recomendados

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
