# Pergunta o Luso

**Pergunta o Luso** é um jogo de perguntas para Android focado em cultura portuguesa, competição, categorias, rankings, conquistas e modos multiplayer.

O projeto ainda está em fase **pre-beta / closed beta**. Não deve ser tratado como produção final sem uma revisão adicional de segurança, regras Firebase, antifraude e qualidade.

> `BrainBrawl` continua a ser o nome técnico do repositório/package Android durante esta fase.

---

## Funcionalidades

- Registo e login de utilizadores.
- Modo convidado, quando permitido pelo fluxo.
- Perfil de jogador.
- Edição de avatar.
- XP, níveis e progressão.
- Conquistas/badges.
- Rankings.
- Histórico de partidas para utilizadores autenticados.
- Categorias oficiais.
- Categorias personalizadas.
- Categorias públicas criadas por utilizadores.
- Criação de categorias e edição de perguntas.
- Modo Solo.
- Modo Grupo / Todos contra Todos.
- Modo 1v1 por convite.
- Modo 2v2 por convite.
- Matchmaking 1v1/2v2, quando disponível no fluxo atual.
- Modo Clássico.
- Modo Caótico.
- Modo Eliminatórias.
- Pódios visuais no fim das partidas.
- Regras anti-farming para separar categorias competitivas e não competitivas.

---

## Regras importantes do produto

- Categorias oficiais são competitivas.
- Categorias personalizadas e públicas podem ser jogadas, mas são não competitivas.
- Categorias não competitivas não atualizam:
  - XP;
  - ranking;
  - recordes;
  - vitórias/derrotas competitivas;
  - badges competitivos.
- Pódios visuais, resultados e feedback da partida continuam a funcionar em todas as categorias.
- Convidados não devem guardar dados persistentes como XP, histórico, ranking ou badges.

Estas regras são importantes para evitar farming de XP/ranking com perguntas criadas pelo próprio utilizador.

---

## Stack técnica

- Android Kotlin.
- XML layouts.
- ViewBinding.
- Arquitetura em estilo MVVM.
- Firebase Realtime Database.
- Firebase Authentication.
- Gradle Kotlin DSL.

---

## Arquitetura

A aplicação está organizada de forma progressiva em camadas:

- **Activities**: gerem UI, eventos do ecrã e navegação.
- **ViewModels**: coordenam estado de ecrã, fluxos assíncronos e decisões de apresentação.
- **Repositories**: concentram acesso ao Firebase Realtime Database.
- **Services**: contêm lógica de pontuação, progressão, estatísticas e badges.
- **FirebasePaths**: centraliza nomes de paths/nós usados na base de dados.
- **GameConstants**: centraliza constantes de jogo, modos, estados e origens de categoria.

O projeto ainda contém algum código legado e compatibilidade com fluxos antigos. Antes de uma release pública, deve haver uma revisão técnica focada em segurança e autoridade de dados.

---

## Firebase

O Firebase Realtime Database é usado para:

- utilizadores;
- salas;
- jogadores em sala;
- perguntas;
- categorias oficiais;
- categorias personalizadas;
- categorias públicas;
- pontuações;
- pódios;
- histórico;
- amigos e convites;
- matchmaking.

O Firebase Authentication identifica utilizadores reais e deve ser usado para separar jogadores autenticados de convidados.

### Regras Firebase

O ficheiro `firebase-rules.json` contém as regras da Realtime Database e deve ser publicado antes de testar fluxos reais:

```bash
firebase deploy --only database
```

As regras devem ser revistas antes de qualquer beta público. O projeto ainda pode ter validações client-side em áreas sensíveis, por isso Cloud Functions ou outro backend autoritativo são recomendados para produção.

### Dados privados

Não commits:

- exports da Realtime Database;
- ficheiros `google-services.json` reais de projetos privados;
- service accounts;
- chaves privadas;
- keystores;
- passwords;
- tokens;
- ficheiros locais do Android Studio.

---

## Setup local

1. Clonar o repositório:

   ```bash
   git clone https://github.com/ratoooooo/BrainBrawl.git
   cd BrainBrawl
   ```

2. Abrir o projeto no Android Studio.

3. Criar ou selecionar um projeto Firebase.

4. Ativar Firebase Authentication e Firebase Realtime Database.

5. Adicionar o ficheiro `google-services.json` localmente em:

   ```text
   app/google-services.json
   ```

   Este ficheiro deve ficar fora do Git se contiver credenciais reais de um projeto privado.

6. Publicar as regras Firebase:

   ```bash
   firebase deploy --only database
   ```

7. Sincronizar Gradle no Android Studio.

8. Compilar e executar num emulador ou dispositivo Android.

---

## Comandos úteis

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew build
```

Se estiveres a usar o JBR incluído no Android Studio no macOS:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build
```

---

## Checklist de testes manuais

Antes de considerar uma build pronta para testes externos, validar:

- Registo.
- Login.
- Entrada como convidado.
- Perfil.
- Edição de avatar.
- Modo Solo Clássico.
- Modo Solo Caótico.
- Modo Solo Eliminatórias.
- Grupo Clássico.
- Grupo Caótico.
- Grupo Eliminatórias.
- Convite 1v1.
- Convite 2v2.
- Matchmaking 1v1.
- Matchmaking 2v2.
- Criação de categoria personalizada.
- Adição e edição de perguntas.
- Publicação/uso de categoria pública.
- Histórico.
- Ranking.
- Badges/conquistas.
- Verificações de permissões Firebase.
- Confirmação de que categorias não competitivas não atualizam XP/ranking/recordes.
- Confirmação de que convidados não guardam dados persistentes indevidos.

---

## Screenshots

Adicionar aqui capturas reais quando houver uma build visualmente estável:

```text
docs/screenshots/main.png
docs/screenshots/game.png
docs/screenshots/podium.png
docs/screenshots/categories.png
```

---

## Roadmap

- Reforçar segurança com Cloud Functions ou backend autoritativo.
- Tornar score, XP e ranking server-authoritative.
- Aumentar cobertura de testes automatizados.
- Adicionar testes instrumentados para fluxos multiplayer.
- Continuar polimento visual e acessibilidade.
- Melhorar moderação/qualidade de categorias públicas.
- Preparar pipeline de release.
- Rever suporte futuro para iOS/TestFlight como projeto separado.

---

## Limitações conhecidas

- A validação de resultados ainda depende muito do cliente.
- Mais testes automatizados são necessários.
- Fluxos multiplayer precisam de validação manual em múltiplos dispositivos/emuladores.
- Regras Firebase devem ser revistas antes de abertura pública.
- A app ainda está em pre-beta/closed beta.

---

## Segurança

- Não expor credenciais em commits.
- Não commitar exports da base de dados.
- Não commitar keystores ou ficheiros de signing.
- Rever `firebase-rules.json` antes de publicar qualquer ambiente aberto.
- Considerar que a fase atual ainda pode depender de validação client-side.

---

## Estado do projeto

Pre-beta / closed beta.

O projeto está funcional para desenvolvimento e testes controlados, mas ainda não deve ser considerado production-ready.

---

## Licença

Licença a definir.

---

## Autor

Autor a definir.
