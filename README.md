````markdown
# BrainBrawl 🧠⚔️

**BrainBrawl** é uma aplicação Android multiplayer de perguntas e respostas sobre **cultura portuguesa**. Os jogadores competem entre si em tempo real, escolhendo diferentes modos de jogo, categorias oficiais ou categorias personalizadas criadas por utilizadores.

O objetivo do projeto é juntar conhecimento, competição e diversão num jogo rápido, social e educativo.

---

## 📱 Funcionalidades

- 👤 **Criação de conta e login**
- 🎮 **Modo convidado**, permitindo jogar sem conta
- 🎭 **Escolha de avatar**
- 🧩 **Vários modos de jogo**
  - 1x1
  - 2x2
  - Clássico
  - Caótico
  - Eliminatórias
- 👥 **Multiplayer em tempo real**
- 🏠 **Criação e gestão de salas**
- 🔑 **Entrada em salas através de código**
- 🧠 **Perguntas por categorias**
- ✍️ **Categorias personalizadas**
  - criar categorias
  - adicionar perguntas
  - editar perguntas
  - eliminar perguntas
  - reutilizar categorias
- 🌍 **Categorias públicas partilhadas entre jogadores**
- 🤝 **Sistema de amigos**
- 📩 **Convites para partidas**
- 📊 **Pontuação e estatísticas**
  - pontuação total
  - total de jogos
  - total de vitórias
  - taxa de acertos
  - respostas certas
- 🏆 **Pódio no final da partida**
- 🔥 **Sistema de streak**, com bónus por respostas corretas consecutivas
- ☁️ **Integração com Firebase Realtime Database**

---

## 🧰 Tecnologias usadas

- **Kotlin**
- **Android SDK**
- **Firebase Realtime Database**
- **ConstraintLayout**
- **ViewBinding**
- **Gradle Kotlin DSL**
- **Android Studio**

---

## ▶️ Execução

Abrir o projeto no Android Studio, aguardar a sincronização do Gradle e executar a aplicação num emulador ou dispositivo físico.

O projeto requer um ficheiro Firebase configurado em:

```text
app/google-services.json
````

Comando opcional para gerar uma build debug:

```bash
./gradlew assembleDebug
```

Comando opcional para correr testes unitários:

```bash
./gradlew testDebugUnitTest
```

---

## 🏗️ Estrutura do projeto

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
│       │   ├── AmigosActivity.kt
│       │   ├── MeuPerfilActivity.kt
│       │   ├── PerfilAmigoActivity.kt
│       │   ├── ConvidarAmigo1x1Activity.kt
│       │   ├── ConvidarAmigo2x2Activity.kt
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
│       │   ├── AvatarGridAdapter.kt
│       │   ├── ConviteAdapter.kt
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

---

## 📂 Pastas e ficheiros principais

* `app/src/main/java/com/example/brainbrawl/`: código Kotlin principal da aplicação.
* `app/src/main/res/layout/`: ficheiros XML das interfaces.
* `app/src/main/res/drawable/`: avatares, fundos, botões, ícones e elementos visuais.
* `app/src/main/res/raw/`: sons usados durante o jogo.
* `app/src/main/res/values/`: cores, temas e strings.
* `UteisSala.kt`: criação de salas, gestão de jogadores e carregamento de perguntas.
* `UteisJogo.kt`: lógica auxiliar do jogo, pontuação, opções e sons.
* `UteisNavegacao.kt`: navegação entre ecrãs mantendo dados do utilizador.
* `UteisValidacao.kt`: validações e hash de palavras-passe.
* `UteisConquistas.kt`: regras de conquistas/badges.

---

## 🎮 Como jogar

### 1. Entrar na aplicação

O jogador pode:

* criar uma conta;
* iniciar sessão;
* jogar como convidado.

Jogadores registados têm acesso a funcionalidades adicionais, como amigos, perfil, estatísticas e categorias personalizadas.

---

### 2. Criar uma sala

1. No menu principal, escolher **Criar Sala**.
2. Selecionar o modo de jogo.
3. Escolher uma categoria.
4. A aplicação cria uma sala e gera um código.
5. Outros jogadores podem entrar usando esse código.

---

### 3. Entrar numa sala

1. No menu principal, escolher **Entrar Sala**.
2. Inserir o código da sala.
3. Aguardar na sala de espera.
4. O admin inicia a partida quando existirem jogadores suficientes.

---

### 4. Jogar

* Cada pergunta apresenta quatro opções.
* Respostas corretas dão pontos.
* Respostas mais rápidas dão mais pontos.
* Respostas corretas consecutivas aumentam o bónus de streak.
* No fim da partida, é apresentado o pódio e as estatísticas são atualizadas.

---

## 🧠 Categorias personalizadas

Jogadores registados podem criar as suas próprias categorias.

Cada categoria pode conter várias perguntas, com opções e resposta correta. O jogador pode:

* criar categorias;
* adicionar perguntas;
* editar perguntas;
* eliminar perguntas;
* guardar categorias para jogar mais tarde;
* reutilizar a mesma categoria em várias partidas.

---

## 🌍 Categorias públicas

O BrainBrawl inclui suporte para categorias públicas partilhadas entre jogadores.

Um jogador pode tornar uma categoria pública para que outros jogadores possam encontrá-la, guardar uma cópia ou jogar com ela.

---

## 🤝 Sistema de amigos

A aplicação inclui funcionalidades sociais, como:

* adicionar amigos;
* ver perfil de amigos;
* consultar estatísticas;
* convidar amigos para partidas 1x1 ou 2x2.

---

## 📊 Estatísticas

O perfil do jogador apresenta estatísticas como:

* pontuação total;
* total de jogos;
* total de vitórias;
* taxa de acertos;
* respostas corretas.

Estas informações são guardadas e atualizadas no Firebase Realtime Database.

---

## 🔥 Firebase Realtime Database

O Firebase é usado para guardar e sincronizar dados como:

* jogadores;
* salas;
* perguntas;
* categorias;
* categorias públicas;
* amigos;
* convites;
* pontuações;
* estatísticas.

A sincronização em tempo real permite que vários jogadores participem na mesma sala e recebam atualizações durante a partida.

---

## 📸 Screenshots

Sugestão de organização para imagens:

```text
docs/screenshots/
├── login.png
├── menu-principal.png
├── escolher-modo.png
├── sala-espera.png
├── jogo.png
└── pontuacao.png
```

Exemplo de uso:

```markdown
![Menu principal](docs/screenshots/menu-principal.png)
![Ecrã de jogo](docs/screenshots/jogo.png)
```

---

## 🚧 Melhorias futuras

* Melhorar filtros e pesquisa nas categorias públicas.
* Adicionar moderação para categorias públicas.
* Criar ranking global.
* Adicionar histórico de partidas.
* Melhorar segurança das regras do Firebase.
* Adicionar testes instrumentados para fluxos multiplayer.
* Melhorar adaptação visual para tablets e diferentes tamanhos de ecrã.
* Adicionar mais perguntas e categorias oficiais.

---

## 👨‍💻 Autor

**Dinis Rato**

Projeto académico desenvolvido com foco em Android, Kotlin, Firebase Realtime Database, multiplayer em tempo real e desenvolvimento de aplicações móveis.

---

## 📌 Estado do projeto

O BrainBrawl encontra-se em desenvolvimento ativo.

A aplicação já suporta autenticação, modo convidado, salas multiplayer, vários modos de jogo, categorias personalizadas, categorias públicas, amigos, convites, pontuação, pódio e estatísticas de jogador.

```
```
