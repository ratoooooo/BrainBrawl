# Pergunta o Luso

**Pergunta o Luso** é uma aplicação Android multiplayer de perguntas e respostas, desenvolvida em Kotlin, onde os jogadores competem em tempo real através de salas, categorias e diferentes modos de jogo.

`BrainBrawl` continua a ser o nome técnico do repositório/package durante a v1.

O objetivo é simples: criar ou entrar numa sala, escolher uma categoria, responder rápido e tentar terminar no topo da pontuação. A aplicação inclui contas de jogador, avatares, amigos, convites, estatísticas, perfis e categorias personalizadas.

---

## Funcionalidades principais

### Autenticação e perfil

- Registo e login de jogadores.
- Entrada como convidado para jogar sem conta.
- Escolha de avatar no registo.
- Perfil do jogador com:
  - pontuação;
  - total de jogos;
  - total de vitórias;
  - taxa de acertos;
  - vitórias por modo;
  - conquistas/badges.

### Modos de jogo

- **Clássico**: jogo por categoria, com perguntas da categoria escolhida.
- **1x1 por convite**: duelo entre dois jogadores registados.
- **2x2**: jogo por equipas, com convite de amigos.
- **Grupo / Todos contra todos**: sala multiplayer onde vários jogadores competem pela melhor pontuação.
- **Caótico**: perguntas misturadas de várias categorias, criando uma partida mais imprevisível.
- **Eliminatórias**: modo disponível na navegação da app, preparado para partidas onde os jogadores podem ser eliminados.

O matchmaking aleatório 1x1/2x2 está desativado na v1. Os fluxos competitivos ativos são salas, entrada por código e convites.

### Multiplayer em tempo real

- Criação de salas com código.
- Entrada em salas através de código.
- Salas separadas para jogos em grupo, 1x1 e 2x2.
- Sincronização do estado da sala através do Firebase Realtime Database.
- Sala de espera antes do início do jogo.
- Convites para amigos em modos competitivos.

### Categorias

- Categorias fixas como História, Geografia, Desporto, Cultura Geral e Gentílicos.
- Criação de categorias personalizadas por jogadores registados.
- Adição, edição e remoção de perguntas em categorias pessoais.
- Jogar com categorias personalizadas.
- Exploração de categorias públicas/partilhadas.
- Possibilidade de guardar uma categoria pública como cópia editável nas categorias pessoais.

### Pontuação e rankings

- Pontuação calculada com base no tempo de resposta.
- Bónus por sequência de respostas corretas.
- Ecrã final com pódio da sala.
- Estatísticas persistidas por jogador:
  - melhor pontuação;
  - jogos realizados;
  - vitórias;
  - respostas certas;
  - taxa média de acertos.

### Amigos e convites

- Sistema de amigos.
- Pedidos de amizade.
- Convites recebidos e enviados.
- Estado online/offline dos jogadores.
- Acesso ao perfil de amigos.

---

## Tecnologias usadas

- **Kotlin**: linguagem principal da aplicação Android.
- **Android SDK**: aplicação nativa Android.
- **Firebase Realtime Database**: sincronização de salas, jogadores, perguntas, pontuações e convites.
- **Firebase Google Services**: integração Firebase através do `google-services.json`.
- **ViewBinding**: acesso seguro às views dos layouts XML.
- **ConstraintLayout**: construção das interfaces principais.
- **Material Components**: componentes visuais Android.
- **Glide**: suporte para carregamento de imagens.
- **Gradle Kotlin DSL**: configuração de build.

---

## Build e execução

### Requisitos

- Android Studio instalado.
- JDK compatível com o projeto, preferencialmente o JBR incluído no Android Studio.
- Android SDK com suporte a:
  - `compileSdk 34`;
  - `minSdk 26`.
- Dispositivo físico ou emulador Android.
- Projeto Firebase configurado com Realtime Database.

### Passos

1. Clonar ou abrir a pasta do projeto no Android Studio.

2. Confirmar que o ficheiro Firebase existe em:

   ```text
   app/google-services.json
   ```

3. Abrir o projeto no Android Studio e aguardar a sincronização do Gradle.

4. Executar o build:

   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
   ```

5. Para correr os testes unitários:

   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
   ```

6. Para validar a build completa:

   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build
   ```

7. Para gerar APK release local:

   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease
   ```

No Android Studio, selecionar um emulador ou dispositivo físico e carregar em **Run**.

### Nota sobre Firebase

A aplicação usa o Firebase Realtime Database para guardar dados como jogadores, salas, perguntas, categorias, amigos e pontuações. Para correr o projeto fora do ambiente original, é necessário configurar um projeto Firebase compatível e garantir que as regras da base de dados permitem os fluxos esperados durante testes.

### Notas de segurança v1

- A arquitetura nova é UID-first, mas mantém compatibilidade legado por `nomeUtilizador`.
- O login legado e campos password/hash antigos ainda existem para compatibilidade e devem ser migrados/removidos antes de beta público.
- Pontuação, XP, ranking e histórico ainda são client-authoritative; a versão robusta deve usar Cloud Functions.
- Convidados podem jogar fluxos permitidos, mas não devem gravar XP, estatísticas, histórico, ranking ou conquistas.
- O matchmaking aleatório continua desativado e só deve voltar numa branch/fase própria.

---

## Estrutura do projeto

```text
BrainBrawl/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/brainbrawl/
│       │   ├── LoginActivity.kt
│       │   ├── RegistarActivity.kt
│       │   ├── MainActivity.kt
│       │   ├── EscolherModoActivity.kt
│       │   ├── TipoModoClassico.kt
│       │   ├── EscolherCategoriaActivity.kt
│       │   ├── AdicionarPerguntaActivity.kt
│       │   ├── ExplorarCategoriasActivity.kt
│       │   ├── SalaDeEsperaActivity.kt
│       │   ├── SalaDeEspera1x1Activity.kt
│       │   ├── SalaDeEspera2x2Activity.kt
│       │   ├── SalaDeEsperaGrupoActivity.kt
│       │   ├── JogoActivity.kt
│       │   ├── Jogo1x1Activity.kt
│       │   ├── Jogo2x2Activity.kt
│       │   ├── PontuacoesActivity.kt
│       │   ├── Pontuacao1x1Activity.kt
│       │   ├── Pontuacao2x2Activity.kt
│       │   ├── AmigosActivity.kt
│       │   ├── MeuPerfilActivity.kt
│       │   ├── PerfilAmigoActivity.kt
│       │   └── Uteis*.kt
│       └── res/
│           ├── drawable/
│           ├── layout/
│           ├── raw/
│           └── values/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Pastas e ficheiros principais

- `app/src/main/java/com/example/brainbrawl/`: código Kotlin da aplicação.
- `app/src/main/res/layout/`: interfaces XML dos ecrãs.
- `app/src/main/res/drawable/`: avatares, ícones, fundos e estilos visuais.
- `app/src/main/res/raw/`: sons usados durante o jogo.
- `app/src/main/res/values/`: strings, cores e temas.
- `UteisSala.kt`: criação de salas e carregamento de perguntas.
- `UteisJogo.kt`: lógica auxiliar de pontuação, opções e sons.
- `UteisNavegacao.kt`: navegação entre ecrãs.
- `UteisValidacao.kt`: validação e hash de palavras-passe.
- `UteisConquistas.kt`: regras para badges/conquistas.

---

## Como jogar

### 1. Entrar na aplicação

O jogador pode:

- criar uma conta;
- iniciar sessão;
- jogar como convidado, indicando apenas um nome.

Jogadores registados têm acesso a mais funcionalidades, como amigos, convites, estatísticas, perfil e categorias personalizadas.

### 2. Criar uma sala

1. No menu principal, escolher **Criar Sala**.
2. Selecionar o modo de jogo.
3. Escolher a categoria.
4. A app cria uma sala e gera um código.
5. Outros jogadores podem entrar usando esse código ou através de convite, dependendo do modo.

### 3. Entrar numa sala por código

1. No menu principal, escolher **Entrar Sala**.
2. Inserir o código da sala.
3. Aguardar na sala de espera.
4. Quando o jogo começar, todos respondem às mesmas perguntas.

### 4. Jogar

- Cada pergunta apresenta quatro opções.
- Quanto mais rápido o jogador responder corretamente, maior será a pontuação.
- Sequências de respostas certas dão bónus.
- No fim da partida, a app mostra o pódio e atualiza as estatísticas dos jogadores registados.

---

## Funcionalidades futuras e melhorias possíveis

- Reforçar regras de segurança no Firebase Realtime Database.
- Melhorar o sistema de categorias públicas com filtros, pesquisa e moderação.
- Adicionar mais perguntas e categorias oficiais.
- Melhorar o modo Eliminatórias.
- Criar histórico de partidas.
- Adicionar testes instrumentados para fluxos multiplayer.
- Melhorar a adaptação visual a tablets e diferentes tamanhos de ecrã.
- Adicionar capturas de ecrã reais ao README.

---

## Imagens

Sugestão de organização para screenshots:

```text
docs/
└── screenshots/
    ├── login.png
    ├── menu-principal.png
    ├── escolher-modo.png
    ├── sala-espera.png
    ├── jogo.png
    └── pontuacao.png
```

Quando existirem imagens, podem ser adicionadas assim:

```markdown
![Menu principal](docs/screenshots/menu-principal.png)
![Ecrã de jogo](docs/screenshots/jogo.png)
```

---

## Autor

**Dinis Rato**

Projeto académico desenvolvido no contexto de aprendizagem de desenvolvimento Android, Kotlin e Firebase Realtime Database.

---

## Estado do projeto

Pergunta o Luso está preparado como v1 Android para teste interno. A base principal de autenticação, salas multiplayer, modos de jogo, categorias, pontuação, amigos, perfil e conquistas já está implementada, com riscos de segurança conhecidos documentados para resolver antes de beta público.
