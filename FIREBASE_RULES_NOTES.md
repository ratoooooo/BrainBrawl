# Firebase Realtime Database Rules

Este ficheiro acompanha `firebase-rules.json` e documenta o estado atual de seguranca do BrainBrawl.

## Contexto atual

- A app usa Firebase Realtime Database com login manual.
- O login compara a password introduzida com o hash guardado em `jogadores/{nome}/password`.
- A app nao usa Firebase Authentication, por isso as rules nao recebem `auth.uid`, email, claims, nem qualquer identidade confiavel do jogador.
- Como consequencia, o servidor nao consegue distinguir o jogador legitimo de outro cliente que escreva diretamente na base de dados.

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

Tambem existem ainda alguns acessos diretos em `RegistarActivity`, `Pontuacao1x1Activity` e `Pontuacao2x2Activity`, mas esta fase nao altera codigo da app.

## O que fica protegido

- O acesso a paths desconhecidos fica bloqueado por defeito.
- `categorias` fica apenas de leitura pela app cliente.
- Criacao de jogadores passa por validacao basica de formato: password como hash SHA-256 hexadecimal, avatar como string e estatisticas numericas.
- A password de um jogador existente nao pode ser alterada por uma escrita normal no mesmo node.
- Estruturas conhecidas de salas, categorias publicas, categorias personalizadas, convites, pedidos, amigos, pontuacoes e estatisticas passam a validar tipos basicos.
- Campos inesperados ficam bloqueados nos principais objetos atraves de `$other.validate = false`.
- Pontuacoes, totais, usos e avaliacoes ficam limitados a valores numericos nao negativos quando aplicavel.

## Limitacoes da autenticacao manual

Estas rules melhoram a forma dos dados, mas nao garantem identidade real.

Sem Firebase Auth, as rules nao conseguem provar que:

- quem escreve em `jogadores/Alice` e mesmo a Alice;
- quem aceita um convite e o destinatario do convite;
- quem altera `salas/{codigo}/estado` e o admin da sala;
- quem escreve uma pontuacao e o jogador dono dessa pontuacao;
- quem publica ou remove uma categoria publica e o criador original;
- quem atualiza estatisticas finais nao esta a inflacionar resultados.

Como a app atual precisa ler `jogadores/{nome}/password` para fazer login manual, o node `jogadores` continua legivel pelo cliente. Isto e um risco estrutural do desenho atual, mesmo com hashes em vez de passwords em texto claro.

## Riscos que continuam

- Qualquer cliente com acesso ao projeto Firebase pode tentar criar salas, alterar salas existentes ou escrever resultados validos em termos de tipo.
- Um utilizador pode tentar manipular estatisticas, pedidos de amizade, convites e categorias publicas porque as rules nao tem uma identidade autenticada para comparar.
- As rules nao conseguem impor ownership forte em `categoriasPersonalizadas`, `categoriasPublicas`, `amigos`, `convites_*` ou `pedidos_amizade`.
- A validacao nao substitui regras de negocio server-side. Ela apenas reduz writes malformados e bloqueia paths/campos desconhecidos.
- Hashes de password continuam expostos a leitura pela app, abrindo espaco para ataques offline se a base de dados for lida por clientes nao confiaveis.

## O que ainda depende do cliente

- Validar credenciais no login.
- Decidir quem e admin de uma sala.
- Validar se um jogador pode iniciar jogo.
- Garantir que cada jogador so escreve a sua propria resposta/pontuacao.
- Garantir que estatisticas sao atualizadas uma unica vez por jogador/sala.
- Impedir que um utilizador edite dados sociais ou categorias de outro utilizador.
- Validar regras completas de jogo, fim de partida e vencedores.

## Recomendacao futura

A seguranca forte deve migrar para Firebase Authentication.

Plano recomendado:

1. Criar utilizadores com Firebase Auth e guardar perfil publico em `jogadores/{uid}` ou `jogadoresPorNome/{nome}`.
2. Separar dados publicos de dados privados. Passwords deixam de existir na Realtime Database.
3. Usar `auth.uid` nas rules para permitir escrita apenas no proprio perfil.
4. Guardar `ownerUid`, `adminUid`, `criadorUid` e participantes das salas/categorias.
5. Reescrever rules para comparar `auth.uid` com esses campos.
6. Mover calculos sensiveis, como estatisticas finais e validacao de resultados, para Cloud Functions ou outro backend confiavel.

Exemplos de regras futuras com Auth:

- `jogadores/{uid}`: escrita apenas se `auth.uid == uid`.
- `salas/{codigo}/jogadores/{uid}`: escrita apenas pelo proprio jogador ou pelo admin da sala.
- `salas/{codigo}/estado`: escrita apenas se `auth.uid == data.child('adminUid').val()`.
- `categoriasPublicas/{id}`: update/delete apenas se `auth.uid == data.child('criadorUid').val()`.

## Aplicacao das rules

O ficheiro `firebase-rules.json` foi criado para ser importado/publicado manualmente no Firebase Console ou via Firebase CLI.

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
