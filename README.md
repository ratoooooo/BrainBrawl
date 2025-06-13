# BrainBrawl 🧠⚔️

**BrainBrawl** é uma aplicação Android multiplayer de perguntas e respostas sobre **cultura portuguesa**. Os jogadores competem entre si em tempo real, escolhendo diferentes modos de jogo e categorias. É ideal para aprender, testar conhecimentos e divertir-se com amigos!

---

## 📱 Funcionalidades

- 👤 **Criação de Conta e Login**
- 🎭 **Escolha de Avatar** (12 opções disponíveis)
- 🧩 **Modos de Jogo:**  
  - **Clássico por categoria**
    - Jogador vs Jogador (1x1)
    - Equipa vs Equipa (2x2)
  - **Caótico** (perguntas aleatórias misturadas)
  - **Multiplayer em tempo real** com criação e gestão de salas
- 📊 **Dashboard de administração** (Web) com estatísticas e gestão de perguntas
- 🔢 **Pontuação e Estatísticas** armazenadas no Firebase
- ☁️ **Integração com Firebase Realtime Database**

---

## 🧰 Tecnologias Usadas

- **Kotlin (Android)**
- **Firebase Realtime Database**
- **ConstraintLayout** & **ViewBinding**
- **Android Studio**

---

## 🏗️ Estrutura da App

```bash
├── activities/
│   ├── AdicionarPerguntaActivity.kt
│   ├── AmigosActivity.kt
│   ├── AvatarGridAdapter.kt
│   ├── ConvidarAmigo1x1Activity.kt
│   ├── ConvidarAmigo2x2Activity.kt
│   ├── ConviteAdapter.kt
│   ├── EscolhaCategoriaModosActivity.kt
│   ├── EscolherCategoriaActivity.kt
│   ├── EscolherModoActivity.kt
│   ├── Jogo1x1Activity.kt
│   ├── Jogo2x2Activity.kt
│   ├── JogoActivity.kt
│   ├── LoginActivity.kt
│   ├── MainActivity.kt
│   ├── PerfilAmigoActivity.kt
│   ├── Pontuacao1x1Activity.kt
│   ├── Pontuacao2x2Activity.kt
│   ├── PontuacoesActivity.kt
│   ├── RegistarActivity.kt
│   ├── SalaDeEspera1x1Activity.kt
│   ├── SalaDeEspera2x2Activity.kt
│   ├── SalaEsperaActivity.kt
│   ├── TipoModoClassico.kt
├── data/
│   └── Pergunta.kt, Convite.kt
├── uteis/
│   └── Uteis.kt  # Funções auxiliares (validações, hashing, etc.)
├── res/
│   ├── drawable/   # Avatares, fundos e botões
│   ├── layout/     # XML das interfaces
│   └── values/     # Strings, cores e temas
